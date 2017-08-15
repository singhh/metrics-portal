package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.KairosDbClient;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.FieldContext;
import net.sf.oval.exception.ConstraintsViolatedException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Executes a query against KairosDB.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class QueryRunner extends MqlBaseVisitor<Object> {
    @Inject
    public QueryRunner(final KairosDbClient kairosDbClient, final ObjectMapper mapper) {
        _kairosDbClient = kairosDbClient;
        _mapper = mapper;
    }

    @Override
    public CompletionStage<TimeSeriesResult> visitStatement(final MqlParser.StatementContext ctx) {
        final List<MqlParser.StageContext> stages = ctx.stage();
        // Build each stage and chain them together
        int x = 0;
        for (MqlParser.StageContext stage : stages) {
            _previousStage = visitStage(stage);
        }

        return _previousStage.execute();
//        return CompletableFuture.completedFuture(JsonNodeFactory.instance.objectNode().put("foo", "bar"));
    }

    @Override
    public StageExecution visitStage(final MqlParser.StageContext ctx) {
        final StageExecution execution;
        if (ctx.select() != null) {
            execution = visitSelect(ctx.select());
        } else if (ctx.agg() != null) {
            execution = visitAgg(ctx.agg());
        } else {
            execution = null;
        }
        if (ctx.timeSeriesReference() != null) {
            final String name = visitTimeSeriesReference(ctx.timeSeriesReference());
            final StageExecution previous = _stages.put(name, execution);
            if (previous != null) {
                throw new IllegalStateException("Multiple stages with name '" + name + "' found");
            }
        }

        return execution;
    }

    @Override
    public StageExecution visitAgg(final MqlParser.AggContext ctx) {
        final List<StageExecution> dependencies;
        if (ctx.ofList() != null) {
            dependencies = visitOfList(ctx.ofList());
        } else if (_previousStage != null) {
            dependencies = Collections.singletonList(_previousStage);
        } else {
            dependencies = Collections.emptyList();
        }

        final String aggregator = visitIdentifier(ctx.aggFunctionName().identifier()).toLowerCase();

        // Check to see if we can lift the aggregator to a dependent query
        if (dependencies.size() >= 1) {
            final StageExecution dependency = dependencies.get(0);
            if (dependencies.size() == 1 && dependency instanceof SelectExecution && LIFTABLE_AGGREGATIONS.contains(aggregator)) {
                final SelectExecution query = (SelectExecution) dependency;

                final SelectExecution.Builder builder = SelectExecution.Builder.from(query);

                final List<MetricsQuery.Metric> newMetrics = builder.getQuery().getMetrics().stream()
                        .map(MetricsQuery.Metric.Builder::from)
                        .map(b -> b.addAggregator(new MetricsQuery.Aggregator.Builder().setName(aggregator).build()).build())
                        .collect(Collectors.toList());
                builder.getQuery().setMetrics(newMetrics);
                return builder.build();
            } else {
                final Map<String, Object> args = visitAggArgList(ctx.aggArgList());
                final Class<? extends BaseExecution.Builder<?, ?>> clazz = AGG_BUILDERS.get(aggregator);
                if (clazz == null) {
                    throw new IllegalArgumentException("Unknown aggregator '" + aggregator + "'");
                }

                final BaseExecution.Builder<?, ?> builder = _mapper.convertValue(args, clazz);
                dependencies.forEach(builder::addDependency);
                try {
                    return builder.build();
                } catch (final ConstraintsViolatedException ex) {
                    final ConstraintViolation[] violations = ex.getConstraintViolations();
                    for (final ConstraintViolation violation : violations) {
                        if (violation.getContext() instanceof FieldContext) {
                            final Field field = ((FieldContext) violation.getContext()).getField();
                            throw new IllegalArgumentException("Illegal value for field " + field.getName() + "; " + violation.getMessage());
                        }
                    }
                    throw new IllegalArgumentException("boom!");
                }
            }
        }
        throw new IllegalStateException("Aggregator '" + aggregator + "' does not have any inputs");
    }

    @Override
    public Map<String, Object> visitAggArgList(MqlParser.AggArgListContext ctx) {
        final LinkedHashMap<String, Object> argMap = Maps.newLinkedHashMap();
        for (MqlParser.AggArgPairContext argPairContext : ctx.aggArgPair()) {
            argMap.put(visitArgName(argPairContext.argName()), visitArgValue(argPairContext.argValue()));
        }
        return argMap;
    }

    @Override
    public String visitArgName(MqlParser.ArgNameContext ctx) {
        return visitIdentifier(ctx.identifier());
    }

    @Override
    public List<StageExecution> visitOfList(final MqlParser.OfListContext ctx) {
        final List<StageExecution> ofList = Lists.newArrayList();
        final List<MqlParser.TimeSeriesReferenceContext> references = ctx.timeSeriesReference();
        for (MqlParser.TimeSeriesReferenceContext reference : references) {
            final String name = visitIdentifier(reference.identifier());
            if (!_stages.containsKey(name)) {
                throw new IllegalStateException("Referenced stage '" + name + "' does not exist for aggregation");
            }
            ofList.add(_stages.get(name));
        }
        return ofList;
    }

    @Override
    public String visitTimeSeriesReference(final MqlParser.TimeSeriesReferenceContext ctx) {
        return visitIdentifier(ctx.identifier());
    }

    @Override
    public SelectExecution visitSelect(MqlParser.SelectContext ctx) {
        // TODO: get dependent queries
        // requiredQueries = computeDepenencies

        // If there are dependencies, we compose the futures
        // CompletionStage<Void> dependenciesComplete = CompletableFuture.allOf(requiredQueries);
        // Wrap the query binding
        // dependenciesComplete.thenApply(bindAndRun(query));

        final MetricsQuery.Builder query = new MetricsQuery.Builder();

        final String metricName = visitIdentifier(ctx.metricName().identifier());
        if (ctx.timeRange() != null) {
            final TimeRange timeRange = visitTimeRange(ctx.timeRange());
            query.setStartTime(timeRange._start)
                    .setEndTime(timeRange._end);
            _timeRange = timeRange;
        } else {
            query.setStartTime(_timeRange._start)
                    .setEndTime(_timeRange._end);
        }

        final MetricsQuery.Metric.Builder metricBuilder = new MetricsQuery.Metric.Builder()
                .setName(metricName);

        if (ctx.whereClause() != null) {
            metricBuilder.setTags(visitWhereClause(ctx.whereClause()));
        }
        if (ctx.groupByClause() != null) {
            metricBuilder.setGroupBy(visitGroupByClause(ctx.groupByClause()));
        }

        query.addMetric(metricBuilder.build());
        return new SelectExecution.Builder()
                .setQueryBuilder(query)
                .setClient(_kairosDbClient)
                .build();
    }

    @Override
    public TimeRange visitTimeRange(final MqlParser.TimeRangeContext ctx) {
        final List<MqlParser.PointInTimeContext> times = ctx.pointInTime();
        final DateTime start = visitPointInTime(times.get(0));
        final DateTime end;
        if (times.size() > 1) {
            end = visitPointInTime(times.get(1));
        } else {
            end = DateTime.now();
        }
        return new TimeRange(start, end);
    }

    @Override
    public List<MetricsQuery.GroupBy> visitGroupByClause(final MqlParser.GroupByClauseContext ctx) {
        final MetricsQuery.GroupBy.Builder groupBy = new MetricsQuery.GroupBy.Builder();
        groupBy.setName("tag");
        final List<String> tags = Lists.newArrayList();
        for (final MqlParser.GroupByTermContext term : ctx.groupByTerm()) {
            tags.add(visitGroupByTerm(term));
        }
        groupBy.addParameter("tags", tags);
        return Collections.singletonList(groupBy.build());
    }

    @Override
    public String visitGroupByTerm(final MqlParser.GroupByTermContext ctx) {
        return visitIdentifier(ctx.identifier());
    }

    @Override
    public DateTime visitPointInTime(final MqlParser.PointInTimeContext ctx) {
        return (DateTime) super.visitPointInTime(ctx);
    }

    @Override
    public DateTime visitAbsoluteTime(final MqlParser.AbsoluteTimeContext ctx) {
        final String toParse = visitStringLiteral(ctx.stringLiteral());
        return DateTime.parse(toParse);
    }

    @Override
    public Multimap<String, String> visitWhereClause(final MqlParser.WhereClauseContext ctx) {
        final Multimap<String, String> map = LinkedHashMultimap.create();
        if (ctx != null) {
            final List<MqlParser.WhereTermContext> whereTerms = ctx.whereTerm();
            for (MqlParser.WhereTermContext where : whereTerms) {
                final List<String> values = visitWhereValue(where.whereValue());
                for (String value : values) {
                    map.put(visitTag(where.tag()), value);
                }
            }
        }

        return map;
    }

    @Override
    public Number visitNumericLiteral(MqlParser.NumericLiteralContext ctx) {
        if (ctx.Double() != null) {
            return Double.parseDouble(ctx.Double().getText());
        } else {
            return Long.parseLong(ctx.Integral().getText());
        }
    }

    @Override
    public String visitTag(final MqlParser.TagContext ctx) {
        return visitIdentifier(ctx.identifier());
    }

    @Override
    public List<String> visitWhereValue(final MqlParser.WhereValueContext ctx) {
        return ctx.stringLiteral().stream().map(this::visitStringLiteral).collect(Collectors.toList());
    }

    @Override
    public String visitStringLiteral(final MqlParser.StringLiteralContext ctx) {
        final String raw = ctx.getText();
        final String stripped = raw.substring(1, raw.length() - 1);
        // TODO: Escape things like octal an unicode properly
        return escapeString(stripped);
    }

    @Override
    public String visitIdentifier(MqlParser.IdentifierContext ctx) {
        return ctx.getText();
    }

    private String escapeString(final String in) {
        final StringBuilder b = new StringBuilder();
        boolean sawEscape = false;
        for (int i = 0; i < in.length(); i++) {
            final Character c = in.charAt(i);
            if (!sawEscape) {
                if (c == '\\') {
                    sawEscape = true;
                } else {
                    b.append(c);
                }
            } else {
                sawEscape = false;
                switch (c) {
                    case '\\':
                        b.append(c);
                        break;
                    case 'b':
                        b.append('\b');
                        break;
                    case 'n':
                        b.append('\n');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 'f':
                        b.append('\f');
                        break;
                    case '\'':
                        b.append('\'');
                        break;
                    case '"':
                        b.append('"');
                        break;
                    default:
                        throw new IllegalArgumentException("character '" + c + "' is not a valid escape");
                }
            }
        }
        return b.toString();
    }


    @Override
    public DateTime visitRelativeTime(final MqlParser.RelativeTimeContext ctx) {
        if (ctx.NOW() != null) {
            return DateTime.now();
        } else {
            final double number = Double.parseDouble(ctx.numericLiteral().getText());
            final MqlParser.TimeUnitContext unit = ctx.timeUnit();
            final Duration ago;
            if (unit.SECOND() != null || unit.SECONDS() != null) {
                ago = Duration.millis((long) (number * 1000));
            } else if (unit.MINUTE() != null || unit.MINUTES() != null) {
                ago = Duration.millis((long) (number * 60 * 1000));
            } else if (unit.HOUR() != null || unit.HOURS() != null) {
                ago = Duration.millis((long) (number * 60 * 60 * 1000));
            } else if (unit.DAY() != null || unit.DAYS() != null) {
                ago = Duration.millis((long) (number * 24 * 60 * 60 * 1000));
            } else if (unit.WEEK() != null || unit.WEEKS() != null) {
                ago = Duration.millis((long) (number * 7 * 24 * 60 * 60 * 1000));
            } else if (unit.MONTH() != null || unit.MONTHS() != null) {
                return DateTime.now().withField(DateTimeFieldType.monthOfYear(), DateTime.now().getMonthOfYear() - 1);
            } else {
                throw new RuntimeException("Unknown time unit " + unit.getText());
            }

            return DateTime.now().minus(ago);
        }
    }

    private StageExecution _previousStage = null;
    private TimeRange _timeRange = null;

    private final Map<String, StageExecution> _stages = Maps.newHashMap();
    private final KairosDbClient _kairosDbClient;
    private final ObjectMapper _mapper;

    private static final Set<String> LIFTABLE_AGGREGATIONS = Sets.newHashSet("min", "max", "merge", "percentile", "count", "avg", "sum");
    private static final Map<String, Class<? extends BaseExecution.Builder<?, ?>>> AGG_BUILDERS = Maps.newHashMap();

    static {
        AGG_BUILDERS.put("union", UnionAggregator.Builder.class);
        AGG_BUILDERS.put("threshold", SimpleThresholdAlertExecution.Builder.class);
    }

    private static final class TimeRange {
        private TimeRange(final DateTime start, final DateTime end) {
            _start = start;
            _end = end;
        }

        private final DateTime _start;
        private final DateTime _end;
    }
}
