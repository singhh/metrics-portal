package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.MetricsQueryResponse;

/**
 * Result of a TimeSeries query stage.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class TimeSeriesResult {
    public TimeSeriesResult(final MetricsQueryResponse response) {
        _response = response;
    }

    public MetricsQueryResponse getResponse() {
        return _response;
    }

    private final MetricsQueryResponse _response;
}
