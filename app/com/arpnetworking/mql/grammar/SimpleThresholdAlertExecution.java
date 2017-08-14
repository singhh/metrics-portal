package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.NonNull;
import models.internal.Alert;
import models.internal.Operator;
import net.sf.oval.constraint.NotNull;

import java.util.List;

public class SimpleThresholdAlertExecution extends BaseAlertExecution {
    @Override
    List<AlertTrigger> evaluateAlerts(final List<TimeSeriesResult> results) {
        final List<AlertTrigger> alerts = Lists.newArrayList();
        for (final TimeSeriesResult result : results) {
            for (final MetricsQueryResponse.Query query : result.getResponse().getQueries()) {
                for (final MetricsQueryResponse.QueryResult queryResult : query.getResults()) {
                    for (final MetricsQueryResponse.DataPoint dataPoint : queryResult.getValues()) {
                        if (dataPoint)
                    }
                }
            }
        }
        return alerts;
    }

    private SimpleThresholdAlertExecution(final Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseAlertExecution.Builder<Builder, SimpleThresholdAlertExecution> {
        public Builder() {
            super(SimpleThresholdAlertExecution::new);
        }

        public Builder setThreshold(final Double value) {
            _threshold = value;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @NotNull
        private Double _threshold = null;

        @NotNull
        private Operator _operator = null;
    }
}
