package com.arpnetworking.mql.grammar;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.google.common.collect.Lists;
import net.sf.oval.constraint.NotNull;

import java.util.List;

/**
 * Result of a TimeSeries query stage.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class TimeSeriesResult {
    public MetricsQueryResponse getResponse() {
        return _response;
    }

    public List<String> getWarnings() {
        return _warnings;
    }

    public List<String> getErrors() {
        return _errors;
    }

    private TimeSeriesResult(final Builder builder) {
        _response = builder._response;
        _warnings = Lists.newArrayList(builder._warnings);
        _errors = Lists.newArrayList(builder._errors);
    }

    private final MetricsQueryResponse _response;
    private final List<String> _warnings;
    private final List<String> _errors;


    public static final class Builder extends OvalBuilder<TimeSeriesResult> {

        public static Builder from(final TimeSeriesResult result) {
            final Builder builder = new Builder();
            builder._response = result._response;
            builder._warnings = Lists.newArrayList(result._warnings);
            builder._errors = Lists.newArrayList(result._errors);
            return builder;
        }

        public Builder() {
            super(TimeSeriesResult::new);
        }

        public Builder setResponse(final MetricsQueryResponse value) {
            _response = value;
            return this;
        }

        public Builder setErrors(final List<String> value) {
            _errors = value;
            return this;
        }

        public Builder addError(final String value) {
            _errors.add(value);
            return this;
        }

        public Builder setWarnings(final List<String> value) {
            _warnings = value;
            return this;
        }

        public Builder addWarning(final String value) {
            _warnings.add(value);
            return this;
        }

        @NotNull
        private MetricsQueryResponse _response;
        @NotNull
        private List<String> _errors = Lists.newArrayList();
        @NotNull
        private List<String> _warnings = Lists.newArrayList();
    }
}
