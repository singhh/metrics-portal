package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.KairosDbClient;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Executes a query against KairosDB.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class QueryRunner extends MqlBaseVisitor<Object> {
    public QueryRunner(final KairosDbClient kairosDbClient) {
        _kairosDbClient = kairosDbClient;
    }

    @Override
    public CompletionStage<JsonNode> visitStatement(final MqlParser.StatementContext ctx) {
        final List<MqlParser.StageContext> stages = ctx.stage();
        // Build each stage and chain them together
        stages.forEach(this::visit);


        return CompletableFuture.completedFuture(JsonNodeFactory.instance.objectNode().put("foo", "bar"));
    }

    @Override
    public Object visitStage(MqlParser.StageContext ctx) {
        return super.visitStage(ctx);
    }

    @Override
    public CompletionStage<MetricsQueryResponse> visitSelect(MqlParser.SelectContext ctx) {
        final String metricName = ctx.metricName().Identifier().getText();
        final TimeRange timeRange = visitTimeRange(ctx.timeRange());
        final MetricsQuery query = new MetricsQuery.Builder()
                .setStartTime(timeRange._start)
                .setEndTime(timeRange._end)
                .addMetric(new MetricsQuery.Metric.Builder()
                        .setName(metricName)
                        .build())
                .build();
        return _kairosDbClient.queryMetrics(query);
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

    private final Map<String, CompletionStage<MetricsQueryResponse>> _queries = Maps.newHashMap();
    private final KairosDbClient _kairosDbClient;

    private static final class TimeRange {
        private TimeRange(final DateTime start, final DateTime end) {
            _start = start;
            _end = end;
        }

        private final DateTime _start;
        private final DateTime _end;
    }
}
