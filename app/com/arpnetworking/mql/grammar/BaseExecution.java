package com.arpnetworking.mql.grammar;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Base stage executor.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public abstract class BaseExecution implements StageExecution {
    /**
     * Called when all the dependencies have been resolved.
     *
     * @param results results of the dependencies
     * @return a new CompletionStage for the execution of this stage
     */
    public abstract CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results);

    @Override
    public CompletionStage<TimeSeriesResult> execute() {
        final ConcurrentMap<StageExecution, TimeSeriesResult> results = Maps.newConcurrentMap();
        final CompletableFuture<?>[] dependencies = dependencies().stream()
                .map(dependency -> dependency.execute().thenApply((result) -> results.put(dependency, result)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(dependencies)
                .thenCompose(v -> executeWithDependencies(results)
                .thenApply(result -> {
                        final TimeSeriesResult.Builder builder = TimeSeriesResult.Builder.from(result);

                        _unknownArgs.keySet().forEach(key -> builder.addWarning("Unknown argument " + key));
                        return builder.build();
                }));
    }

    @Override
    public List<StageExecution> dependencies() {
        return _dependencies;
    }

    protected BaseExecution(final Builder<?, ?> builder) {
        _dependencies = builder._dependencies;
        _unknownArgs = builder._other;
    }

    private final List<StageExecution> _dependencies;
    private final Map<String, Object> _unknownArgs;

    public abstract static class Builder<B extends Builder<B, E>, E extends StageExecution> extends OvalBuilder<E> {
        /**
         * Add a dependency.
         *
         * @param value the dependency
         * @return this {@link Builder}
         */
        public B addDependency(final StageExecution value) {
            _dependencies.add(value);
            return self();
        }

        @JsonAnySetter
        private void setOthers(final String key, final Object value) {
            _other.put(key, value);
        }

        private Map<String, Object> getOther() {
            return Maps.newHashMap(_other);
        }

        /**
         * Protected constructor for subclasses.
         *
         * @param targetConstructor The constructor for the concrete type to be created by this builder.
         */
        protected Builder(final Function<B, E> targetConstructor) {
            super(targetConstructor);
        }

        protected abstract B self();

        private final List<StageExecution> _dependencies = Lists.newArrayList();
        private final Map<String, Object> _other = Maps.newHashMap();
    }
}
