package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.google.common.collect.Lists;
import models.internal.Operator;
import net.sf.oval.constraint.NotNull;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SimpleThresholdAlertExecution extends BaseAlertExecution {

    @Override
    List<AlertTrigger> evaluateAlerts(final List<TimeSeriesResult> results) {
        final List<AlertTrigger> alerts = Lists.newArrayList();
        for (final TimeSeriesResult result : results) {
            for (final MetricsQueryResponse.Query query : result.getResponse().getQueries()) {
                for (final MetricsQueryResponse.QueryResult queryResult : query.getResults()) {
                    alerts.addAll(_evaluator.getInAlertDataPoints(queryResult.getValues()));
                }
            }
        }
        return alerts;
    }

    private SimpleThresholdAlertExecution(final Builder builder) {
        super(builder);
        final Double threshold = builder._threshold;
        final Operator operator = builder._operator;
        switch (operator) {
            case EQUAL_TO:
                _evaluator = new TriggerEvaluator(threshold::equals);
                break;
            case GREATER_THAN:
                _evaluator = new TriggerEvaluator(v -> v > threshold);
                break;
            case GREATER_THAN_OR_EQUAL_TO:
                _evaluator = new TriggerEvaluator(v -> v >= threshold);
                break;
            case LESS_THAN:
                _evaluator = new TriggerEvaluator(v -> v < threshold);
                break;
            case LESS_THAN_OR_EQUAL_TO:
                _evaluator = new TriggerEvaluator(v -> v <= threshold);
                break;
            default:
                throw new IllegalArgumentException("Unknown operator on alert execution, operator=" + operator);
        }
    }

    private final TriggerEvaluator _evaluator;

    public static class Builder extends BaseAlertExecution.Builder<Builder, SimpleThresholdAlertExecution> {
        public Builder() {
            super(SimpleThresholdAlertExecution::new);
        }

        public Builder setThreshold(final Double value) {
            _threshold = value;
            return this;
        }

        public Builder setOperator(final Operator value) {
            _operator = value;
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

    private static final class TriggerEvaluator {
        private TriggerEvaluator(final Predicate<Double> eval) {
            _eval = eval;
        }

        public List<AlertTrigger> getInAlertDataPoints(final List<MetricsQueryResponse.DataPoint> points) {
            return points.stream()
                    .filter(point -> _eval.test((Double) point.getValue()))
                    .map(point -> new AlertTrigger.Builder().setTime(point.getTime()))
                    .map(AlertTrigger.Builder::build)
                    .collect(Collectors.toList());
        }

        private final Predicate<Double> _eval;
    }
}
