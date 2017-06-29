package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.KairosDbClient;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

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
        final MetricsQuery query = new MetricsQuery.Builder()
                .setStartTime(DateTime.now().minusHours(2))
                .addMetric(new MetricsQuery.Metric.Builder()
                        .setName(metricName)
                        .build())
                .build();
        return _kairosDbClient.queryMetrics(query);
    }

    private final Map<String, CompletionStage<MetricsQueryResponse>> _queries = Maps.newHashMap();
    private final KairosDbClient _kairosDbClient;
}
