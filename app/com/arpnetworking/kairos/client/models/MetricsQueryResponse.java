package com.arpnetworking.kairos.client.models;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;

import java.util.List;
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

    public List<Query> getQueries() {
        return _queries;
    }

    private MetricsQueryResponse(final Builder builder) {
        _other = builder._other;
        _queries = builder._queries;
    }

    private final Map<String, Object> _other;
    private final List<Query> _queries;

    public static final class Builder extends OvalBuilder<MetricsQueryResponse> {
        public Builder() {
            super(MetricsQueryResponse::new);
        }

        @JsonAnySetter
        public Builder setOther(final String key, final Object value) {
            _other.put(key, value);
            return this;
        }

        public Builder setQueries(final List<Query> value) {
            _queries = value;
            return this;
        }

        @NotNull
        private List<Query> _queries;
        private Map<String, Object> _other = Maps.newHashMap();
    }

    public static final class Query {
        private Query(final Builder builder) {
            _other = builder._other;
            _sampleSize = builder._sampleSize;
            _results = builder._results;
        }

        @JsonAnyGetter
        public Map<String, Object> getOther() {
            return _other;
        }

        @JsonProperty("sample_size")
        public long getSampleSize() {
            return _sampleSize;
        }

        public List<QueryResult> getResults() {
            return _results;
        }

        private final Map<String, Object> _other;
        private final long _sampleSize;
        private final List<QueryResult> _results;

        public static final class Builder extends OvalBuilder<Query> {
            public Builder() {
                super(Query::new);
            }

            @JsonAnySetter
            public Builder setOther(final String key, final Object value) {
                _other.put(key, value);
                return this;
            }

            @JsonProperty("sample_size")
            public Builder setSampleSize(final long value) {
                _sampleSize = value;
                return this;
            }

            public Builder setResults(final List<QueryResult> value) {
                _results = value;
                return this;
            }

            @NotNull
            private List<QueryResult> _results;
            private long _sampleSize = 0;
            private Map<String, Object> _other = Maps.newHashMap();
        }
    }

    public static final class QueryResult {
        @JsonAnyGetter
        public Map<String, Object> getOther() {
            return _other;
        }

        public List<DataPoint> getValues() {
            return _values;
        }

        private QueryResult(final Builder builder) {
            _other = builder._other;
            _values = builder._values;
        }

        private final Map<String, Object> _other;
        private final List<DataPoint> _values;

        public static final class Builder extends OvalBuilder<QueryResult> {
            public Builder() {
                super(QueryResult::new);
            }

            @JsonAnySetter
            public Builder setOther(final String key, final Object value) {
                _other.put(key, value);
                return this;
            }

            public Builder setValues(final List<DataPoint> value) {
                _values = value;
                return this;
            }

            private List<DataPoint> _values = Lists.newArrayList();
            private Map<String, Object> _other = Maps.newHashMap();
        }
    }

    public static final class DataPoint {
        public DateTime getTime() {
            return _time;
        }

        public Object getValue() {
            return _value;
        }

        private DataPoint(final Builder builder) {
            _time = builder._time;
            _value = builder._value;
        }

        @JsonValue
        private List<Object> serialize() {
            return Lists.newArrayList(_time.getMillis(), _value);
        }

        private final DateTime _time;
        private final Object _value;

        public static final class Builder extends OvalBuilder<DataPoint> {
            @JsonCreator
            public Builder(final List<Object> arr) {
                super(DataPoint::new);
                final long timestamp = (long) arr.get(0);
                _time = new DateTime(timestamp);
                _value = arr.get(1);
            }

            @NotNull
            private DateTime _time;
            @NotNull
            private Object _value;
        }
    }
}
