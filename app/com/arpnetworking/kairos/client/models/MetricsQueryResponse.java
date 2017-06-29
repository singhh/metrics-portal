package com.arpnetworking.kairos.client.models;

import com.arpnetworking.commons.builder.OvalBuilder;

/**
 * Model class to represent a metrics query response from KairosDB.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public final class MetricsQueryResponse {
    private MetricsQueryResponse(final Builder builder) {
    }

    public static final class Builder extends OvalBuilder<MetricsQueryResponse> {
        public Builder() {
            super(MetricsQueryResponse::new);
        }
    }
}
