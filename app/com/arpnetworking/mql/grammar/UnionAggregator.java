package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Unions TimeSeriesResults for output.
 */
public class UnionAggregator extends BaseExecution {
    @Override
    public CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results) {
        final List<MetricsQueryResponse.Query> queries = Lists.newArrayList();
        for (final StageExecution execution : dependencies()) {
            queries.addAll(results.get(execution).getResponse().getQueries());
        }
        final MetricsQueryResponse newResponse = new MetricsQueryResponse.Builder().setQueries(queries).build();
        return CompletableFuture.completedFuture(new TimeSeriesResult(newResponse));
    }

    private UnionAggregator(final Builder builder) {
        super(builder);
    }

    public static final class Builder extends BaseExecution.Builder<Builder, UnionAggregator> {
        public Builder() {
            super(UnionAggregator::new);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
