package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public abstract class BaseAlertExecution extends BaseExecution {
    @Override
    public CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results) {
        final List<MetricsQueryResponse.Query> queries = Lists.newArrayList();
        for (final StageExecution execution : dependencies()) {
            queries.addAll(results.get(execution).getResponse().getQueries());
        }
        final MetricsQueryResponse newResponse = new MetricsQueryResponse.Builder().setQueries(queries).build();
        evaluateAlerts(Lists.newArrayList(results.values()));
        return CompletableFuture.completedFuture(new TimeSeriesResult(newResponse));
    }

    abstract List<AlertTrigger> evaluateAlerts(final List<TimeSeriesResult> results);

    protected BaseAlertExecution(final Builder<?, ?> builder) {
        super(builder);
    }

    public abstract static class Builder<B extends Builder<B, E>, E extends BaseAlertExecution> extends BaseExecution.Builder<B, E> {
        protected Builder(final Function<B, E> targetConstructor) {
            super(targetConstructor);
        }
    }
}
