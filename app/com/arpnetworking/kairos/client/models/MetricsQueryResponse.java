package com.arpnetworking.kairos.client.models;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Model class to represent a metrics query response from KairosDB.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public final class MetricsQueryResponse {
    @JsonAnyGetter
    public Map<String, Object> getOther() {
        return _other;
    }

    private MetricsQueryResponse(final Builder builder) {
        _other = builder._other;
    }

    private final Map<String, Object> _other;

    public static final class Builder extends OvalBuilder<MetricsQueryResponse> {
        public Builder() {
            super(MetricsQueryResponse::new);
        }

        @JsonAnySetter
        public Builder setOther(final String key, final Object value) {
            _other.put(key, value);
            return this;
        }

        private Map<String, Object> _other = Maps.newHashMap();
    }
}
