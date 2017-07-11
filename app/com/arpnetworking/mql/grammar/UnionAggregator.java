package com.arpnetworking.mql.grammar;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Unions TimeSeriesResults for output.
 */
public class UnionAggregator extends BaseExecution {
    @Override
    public CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results) {
        // TODO: implement me
        final Map<String, Object> result = Maps.newHashMap();
        Integer x = 0;
        for (Map.Entry<StageExecution, TimeSeriesResult> entry : results.entrySet()) {
            result.put(x.toString(), entry.getValue().getNode());
            x++;
        }
        return CompletableFuture.completedFuture(new TimeSeriesResult(result));
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
