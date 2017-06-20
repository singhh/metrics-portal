package models.view;

import com.arpnetworking.logback.annotations.Loggable;

/**
 * View model for metrics queries.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
@Loggable
public final class MetricsQuery {
    public String getQuery() {
        return _query;
    }

    public void setQuery(String query) {
        _query = query;
    }

    private String _query;
}
