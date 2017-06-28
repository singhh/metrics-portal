package com.arpnetworking.mql.grammar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by brandon on 6/27/17.
 */
public class QueryRunner extends MqlBaseVisitor<CompletionStage<JsonNode>> {
    @Override
    public CompletionStage<JsonNode> visitStatement(final MqlParser.StatementContext ctx) {
        final List<MqlParser.StageContext> stages = ctx.stage();
        // Build each stage and chain them together

        return CompletableFuture.completedFuture(JsonNodeFactory.instance.objectNode().put("foo", "bar"));
    }

    f
}
