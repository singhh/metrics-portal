package com.arpnetworking.mql.grammar;

import java.util.Map;

/**
 * Result of a TimeSeries query stage.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class TimeSeriesResult {
    public TimeSeriesResult(final Map<String, Object> node) {
        _node = node;
    }

    public Map<String, Object> getNode() {
        return _node;
    }

    private final Map<String, Object> _node;
}
