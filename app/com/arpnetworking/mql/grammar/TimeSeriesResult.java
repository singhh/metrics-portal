package com.arpnetworking.mql.grammar;

import com.arpnetworking.kairos.client.models.KairosQuery;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;

/**
 * Result of a TimeSeries query stage.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class TimeSeriesResult {
    public TimeSeriesResult(final KairosQuery response) {
        _response = response.getResponse();
    }

    public TimeSeriesResult(final MetricsQueryResponse response) {
        _response = response;
    }

    public MetricsQueryResponse getResponse() {
        return _response;
    }

    private final MetricsQueryResponse _response;
}
