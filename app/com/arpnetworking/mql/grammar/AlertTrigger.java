package com.arpnetworking.mql.grammar;

import com.arpnetworking.commons.builder.OvalBuilder;
import org.joda.time.DateTime;

public class AlertTrigger {
    public DateTime getTime() {
        return _time;
    }

    private AlertTrigger(final Builder builder) {
        _time = builder._time;
    }

    private final DateTime _time;

    public static class Builder extends OvalBuilder<AlertTrigger> {
        public Builder() {
            super(AlertTrigger::new);
        }

        public Builder setTime(final DateTime value) {
            _time = value;
            return this;
        }

        private DateTime _time;
    }
}
