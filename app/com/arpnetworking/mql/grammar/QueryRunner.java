package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.KairosDbClient;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;

import java.util.List;
import java.util.Map;
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
    public CompletionStage<JsonNode> visitStatement(final MqlParser.StatementContext ctx) {
        final List<MqlParser.StageContext> stages = ctx.stage();
        // Build each stage and chain them together
        int x = 0;
        for (MqlParser.StageContext stage : stages) {
            _previousStage = visitStage(stage);
        }

        return _previousStage.execute().thenApply(TimeSeriesResult::getNode).thenApply(_mapper::valueToTree);
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
        //TODO: build it
        final UnionAggregator.Builder builder = new UnionAggregator.Builder();
        if (ctx.ofList() != null) {
            final List<StageExecution> dependencies = visitOfList(ctx.ofList());
            for (StageExecution dependency : dependencies) {
                builder.addDependency(dependency);
            }
        } else {
            builder.addDependency(_previousStage);
        }

        return builder.build();
//        return null;
    }

    @Override
    public List<StageExecution> visitOfList(final MqlParser.OfListContext ctx) {
        final List<StageExecution> ofList = Lists.newArrayList();
        final List<MqlParser.TimeSeriesReferenceContext> references = ctx.timeSeriesReference();
        for (MqlParser.TimeSeriesReferenceContext reference : references) {
            final String name = reference.Identifier().getText();
            if (!_stages.containsKey(name)) {
                throw new IllegalStateException("Referenced stage '" + name + "' does not exist for aggregation");
            }
            ofList.add(_stages.get(name));
        }
        return ofList;
    }

    @Override
    public String visitTimeSeriesReference(final MqlParser.TimeSeriesReferenceContext ctx) {
        return ctx.Identifier().getText();
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

        final String metricName = ctx.metricName().Identifier().getText();
        if (ctx.timeRange() != null) {
            final TimeRange timeRange = visitTimeRange(ctx.timeRange());
            query.setStartTime(timeRange._start)
                    .setEndTime(timeRange._end);
            _timeRange = timeRange;
        }

        query.addMetric(new MetricsQuery.Metric.Builder()
                .setName(metricName)
                .setTags(visitWhereClause(ctx.whereClause()))
                .build());
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
    public DateTime visitPointInTime(final MqlParser.PointInTimeContext ctx) {
        return (DateTime) super.visitPointInTime(ctx);
    }

    @Override
    public DateTime visitAbsoluteTime(final MqlParser.AbsoluteTimeContext ctx) {
        return DateTime.parse(ctx.StringLiteral().getText());
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
    public String visitTag(final MqlParser.TagContext ctx) {
        return ctx.Identifier().getText();
    }

    @Override
    public List<String> visitWhereValue(final MqlParser.WhereValueContext ctx) {
        return ctx.quotedString().stream().map(this::visitQuotedString).collect(Collectors.toList());
    }

    @Override
    public String visitQuotedString(final MqlParser.QuotedStringContext ctx) {
        final String raw = ctx.getText();
        final String stripped = raw.substring(1, raw.length() - 1);
        // TODO: Escape things like octal an unicode properly
        return escapeString(stripped);
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
                if (c == '\\') {
                    b.append(c);
                } else if (c == 'b') {
                    b.append('\b');
                } else if (c == 'n') {
                    b.append('\n');
                } else if (c == 't') {
                    b.append('\t');
                } else if (c == 'r') {
                    b.append('\r');
                } else if (c == 'f') {
                    b.append('\f');
                } else if (c == '\'') {
                    b.append('\'');
                } else if (c == '"') {
                    b.append('"');
                } else {
                    throw new IllegalArgumentException("character '" + c + "' is not a valid escape");
                }
            }
        }
        return b.toString();
    }

    @Override
    public Object visitRelativeTime(final MqlParser.RelativeTimeContext ctx) {
        if (ctx.NOW() != null) {
            return DateTime.now();
        } else {
            final double number = Double.parseDouble(ctx.NumericLiteral().getText());
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

    private static final class TimeRange {
        private TimeRange(final DateTime start, final DateTime end) {
            _start = start;
            _end = end;
        }

        private final DateTime _start;
        private final DateTime _end;
    }
}
