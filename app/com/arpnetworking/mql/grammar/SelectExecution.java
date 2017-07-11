package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.KairosDbClient;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Represents an execution of a metrics SELECT query.  Holds incoming references and binding name.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class SelectExecution extends BaseExecution {
    @Override
    public CompletionStage<TimeSeriesResult> executeWithDependencies(final Map<StageExecution, TimeSeriesResult> results) {
        return _client.queryMetrics(_query.build()).thenApply(SelectExecution::toTimeSeriesResult);
    }

    private static TimeSeriesResult toTimeSeriesResult(final MetricsQueryResponse response) {
        return new TimeSeriesResult(response.getOther());
    }

    private SelectExecution(final Builder builder) {
        super(builder);
        _query = builder._query;
        _client = builder._client;
    }

    public MetricsQuery.Builder getQuery() {
        return _query;
    }

    private final MetricsQuery.Builder _query;
    private final KairosDbClient _client;

    public static final class Builder extends BaseExecution.Builder<Builder, SelectExecution> {
        public Builder() {
            super(SelectExecution::new);
        }

        /**
         * Sets the KairosDB client.
         *
         * @param value the client
         * @return this {@link Builder}
         */
        public Builder setClient(final KairosDbClient value) {
            _client = value;
            return this;
        }

        /**
         * Sets the name of the query result.
         *
         * @param value the name
         * @return this {@link Builder}
         */
        public Builder setQueryBuilder(final MetricsQuery.Builder value) {
            _query = value;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        private MetricsQuery.Builder _query;
        private KairosDbClient _client;
    }
}
