package com.arpnetworking.mql.grammar;

import net.sf.oval.constraint.NotNull;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Base aggregator that assists higher level aggregators by bucketing the data to aggregate.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class TimeWindowBaseAggregator extends BaseExecution {
    @Override
    public CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results) {
        return null;
    }

    protected TimeWindowBaseAggregator(final Builder<?, ?> builder) {
        super(builder);
    }

    public enum BucketAlignment {
        TIME,
        SAMPLES
    }

    public abstract static class Builder<B extends BaseExecution.Builder<B, E>, E extends TimeWindowBaseAggregator> extends BaseExecution.Builder<B, E> {
        /**
         * Set the alignment. Cannot be null. Defaults to TIME.
         *
         * @param value the Alignment
         * @return this {@link Builder}
         */
        public B setAlign(final BucketAlignment value) {
            _align = value;
            return self();
        }
        /**
         * Protected constructor for subclasses.
         *
         * @param targetConstructor The constructor for the concrete type to be created by this builder.
         */
        protected Builder(final Function<B, E> targetConstructor) {
            super(targetConstructor);
        }

        @NotNull
        private BucketAlignment _align = BucketAlignment.TIME;
    }
}
