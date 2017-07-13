package com.arpnetworking.kairos.client.models;

import com.arpnetworking.commons.builder.OvalBuilder;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model class to represent a metrics query.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public final class MetricsQuery {

    private MetricsQuery(final Builder builder) {
        _startTime = builder._startTime;
        _endTime = builder._endTime;
        _metrics = ImmutableList.copyOf(builder._metrics);
    }

    @JsonProperty("start_absolute")
    private long startMillis() {
        return _startTime.getMillis();
    }

    @JsonProperty("end_absolute")
    private long endMillis() {
        return _endTime.getMillis();
    }

    private final DateTime _startTime;
    private final DateTime _endTime;
    @JsonProperty("metrics")
    private final List<Metric> _metrics;

    /**
     * Implementation of the builder pattern for MetricsQuery.
     */
    public static final class Builder extends OvalBuilder<MetricsQuery> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(MetricsQuery::new);
        }

        /**
         * Sets the start time of the query. Cannot be null.
         *
         * @param value the start time
         * @return this {@link Builder}
         */
        public Builder setStartTime(final DateTime value) {
            _startTime = value;
            return this;
        }

        /**
         * Sets the end time of the query. Cannot be null.  Default is now.
         *
         * @param value the end time
         * @return this {@link Builder}
         */
        public Builder setEndTime(final DateTime value) {
            _endTime = value;
            return this;
        }

        /**
         * Sets list of metrics. Cannot be null.  Cannot be empty.
         *
         * @param value the metrics
         * @return this {@link Builder}
         */
        public Builder setMetrics(final List<Metric> value) {
            _metrics.clear();
            _metrics.addAll(value);
            return this;
        }

        /**
         * Adds a metric to the list of metrics. Cannot be null.
         *
         * @param value a metric
         * @return this {@link Builder}
         */
        public Builder addMetric(final Metric value) {
            _metrics.add(value);
            return this;
        }

        public Builder copy() {
            return new Builder().setStartTime(_startTime)
                    .setEndTime(_endTime)
                    .setMetrics(_metrics.stream()
                            .map(Metric.Builder::from)
                            .map(Metric.Builder::build)
                            .collect(Collectors.toList()));
        }

        public DateTime getStartTime() {
            return _startTime;
        }

        public DateTime getEndTime() {
            return _endTime;
        }

        public List<Metric> getMetrics() {
            return _metrics;
        }

        @NotNull
        private DateTime _startTime;
        @NotNull
        private DateTime _endTime = DateTime.now();
        @NotNull
        @NotEmpty
        private List<Metric> _metrics = Lists.newArrayList();
    }

    /**
     * Holds the data for a Metric element of the query.
     */
    public static final class Metric {
        private Metric(final Builder builder) {
            _name = builder._name;
            _tags = LinkedHashMultimap.create(builder._tags);
            _aggregators = Lists.newArrayList(builder._aggregators);
            _groupBy = builder._groupBy;
        }

        @JsonProperty("name")
        private final String _name;
        @JsonProperty("tags")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final Multimap<String, String> _tags;
        @JsonProperty("aggregators")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final List<Aggregator> _aggregators;
        @JsonProperty("group_by")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final List<GroupBy> _groupBy;

        public static final class Builder extends OvalBuilder<Metric> {
            public static Builder from(final Metric metric) {
                return new Builder().setTags(LinkedHashMultimap.create(metric._tags))
                        .setAggregators(Lists.newArrayList(metric._aggregators))
                        .setGroupBy(Lists.newArrayList(metric._groupBy))
                        .setName(metric._name);
            }

            /**
             * Public constructor.
             */
            public Builder() {
                super(Metric::new);
            }

            /**
             * Sets the name of the metric. Cannot be null or empty.
             *
             * @param value the name of the metric
             * @return this {@link Builder}
             */
            public Builder setName(final String value) {
                _name = value;
                return this;
            }

            /**
             * Sets the tags. Cannot be null
             *
             * @param value the tags
             * @return this {@link Builder}
             */
            public Builder setTags(final Multimap<String, String> value) {
                _tags.clear();
                _tags.putAll(value);
                return this;
            }

            /**
             * Sets the group by. Cannot be null
             *
             * @param value the group by clauses
             * @return this {@link Builder}
             */
            public Builder setGroupBy(final List<GroupBy> value) {
                _groupBy.clear();
                _groupBy.addAll(value);
                return this;
            }

            /**
             * Sets the aggregators. Cannot be null
             *
             * @param value the aggregators
             * @return this {@link Builder}
             */
            public Builder setAggregators(final List<Aggregator> value) {
                _aggregators.clear();
                _aggregators.addAll(value);
                return this;
            }

            /**
             * Add an aggregator. Cannot be null
             *
             * @param value the aggregator to add
             * @return this {@link Builder}
             */
            public Builder addAggregator(final Aggregator value) {
                _aggregators.add(value);
                return this;
            }

            /**
             * Add a group by. Cannot be null
             *
             * @param value the aggregator to add
             * @return this {@link Builder}
             */
            public Builder addGroupBy(final GroupBy value) {
                _groupBy.add(value);
                return this;
            }

            public String getName() {
                return _name;
            }

            public Multimap<String, String> getTags() {
                return _tags;
            }

            public List<Aggregator> getAggregators() {
                return _aggregators;
            }

            @NotNull
            @NotEmpty
            private String _name;

            @NotNull
            private List<Aggregator> _aggregators = Lists.newArrayList();

            @NotNull
            private List<GroupBy> _groupBy = Lists.newArrayList();

            @NotNull
            private Multimap<String, String> _tags = LinkedHashMultimap.create();
        }
    }

    public static final class Aggregator {
        private Aggregator(final Builder builder) {
            _name = builder._name;
            _alignSampling = builder._alignSampling;
            _sampling = builder._sampling;
        }

        @JsonProperty("name")
        private final String _name;
        @JsonProperty("align_sampling")
        private final boolean _alignSampling;
        @JsonProperty("sampling")
        private final Sampling _sampling;

        public static final class Builder extends OvalBuilder<Aggregator> {
            public Builder() {
                super(Aggregator::new);
            }

            /**
             * Sets the name of the aggregator. Cannot be null or empty.
             *
             * @param value the name of the aggregator
             * @return this {@link Builder}
             */
            public Builder setName(final String value) {
                _name = value;
                return this;
            }
            @NotNull
            @NotEmpty
            private String _name;
            private boolean _alignSampling = true;
            private Sampling _sampling = new Sampling.Builder().build();
        }
    }

    public static final class Sampling {
        private Sampling(final Builder builder) {
            _unit = builder._unit;
            _value = builder._value;
        }

        @JsonProperty("unit")
        private final String _unit;
        @JsonProperty("value")
        private final int _value;

        public static final class Builder extends OvalBuilder<Sampling> {
            public Builder() {
                super(Sampling::new);
            }

            private int _value = 1;
            private String _unit = "minutes";
        }
    }

    public static final class GroupBy {

        private GroupBy(final Builder builder) {
            _parameters = builder._parameters;
            _name = builder._name;
        }

        @JsonAnyGetter
        public Map<String, Object> getParameters() {
            return _parameters;
        }

        public String getName() {
            return _name;
        }

        private final Map<String, Object> _parameters;
        private final String _name;

        public static final class Builder extends OvalBuilder<GroupBy> {
            public Builder() {
                super(GroupBy::new);
            }

            public Builder setName(final String value) {
                _name = value;
                return this;
            }

            @JsonAnySetter
            public Builder addParameter(final String key, final Object value) {
                _parameters.put(key, value);
                return this;
            }

            @NotNull
            @NotEmpty
            private String _name;
            @NotNull
            private final Map<String, Object> _parameters = Maps.newLinkedHashMap();
        }
    }
}
