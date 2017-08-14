package com.arpnetworking.kairos.client.models;

import com.arpnetworking.commons.builder.OvalBuilder;
import net.sf.oval.constraint.NotNull;

/**
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class KairosQuery {
    public MetricsQuery getRequest() {
        return _request;
    }

    public MetricsQueryResponse getResponse() {
        return _response;
    }

    protected KairosQuery(final Builder builder) {
        _request = builder._request;
        _response = builder._response;
    }

    private final MetricsQuery _request;
    private final MetricsQueryResponse _response;

    public static class Builder extends OvalBuilder<KairosQuery> {
        public Builder setRequest(final MetricsQuery value) {
            _request = value;
            return this;
        }

        public Builder setResponse(final MetricsQueryResponse value) {
            _response = value;
            return this;
        }

        public Builder() {
            super(KairosQuery::new);
        }

        @NotNull
        private MetricsQuery _request;
        @NotNull
        private MetricsQueryResponse _response;
    }
}
