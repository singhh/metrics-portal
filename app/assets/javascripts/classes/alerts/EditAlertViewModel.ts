/*
 * Copyright 2016 Smartsheet.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import AlertData = require('./AlertData');
import ko = require('knockout');
import $ = require('jquery');
import Operator = require("./Operator");
import Quantity = require("../Quantity");
import uuid = require('../Uuid');
import flotr = require('flotr2');
import moment = require('moment');
import d3 = require('d3');
import ErrorTextStatus = JQuery.Ajax.ErrorTextStatus;
import jqXHR = JQuery.jqXHR;
import {max} from "d3-array";

class OperatorOption {
    text: string;
    value: string;

    constructor(text, value) {
        this.text = text;
        this.value = value;
    }
}

class MetricsResponse {
    response: QueryResponse;
    errors: String[];
    warnings: String[];
}

class QueryResponse {
    queries: Query[];
}

class Query {
    results: Results[];
}
type Datapoint = [number, number];

class Results {
    values: Datapoint[];
}

class Series {
    id: string;
    values: Datapoint[];
}

class RangeDatapoint {
    constructor(timestamp: number, min: number, max: number) {
        this.timestamp = timestamp;
        this.min = min;
        this.max = max;
    }

    timestamp: number;
    min: number;
    max: number;
}

class RangeSeries {
    id: string;
    values: RangeDatapoint[] = [];
}

class EditAlertViewModel {
    id = ko.observable<string>("");
    name = ko.observable<string>("");
    query = ko.observable<string>("from 2 hours ago to now select").extend({ rateLimit: { timeout: 500, method: "notifyWhenChangesStop" } });
    period = ko.observable<string>("PT1M");
    operator = ko.observable<string>("GREATER_THAN");
    value = ko.observable<number>(0);
    valueUnit = ko.observable<string>(null);
    container: HTMLElement;
    queryError = ko.observable<string>(null);
    queryWarning = ko.observable<string>(null);
    dateRange = ko.observable<any[]>([moment().subtract(2, 'hours'), moment()]);
    formattedDateRange = ko.computed(() => {
        let range = this.dateRange();
        let start = range[0];
        let end = range[1];
        return start.calendar() + " to " + end.calendar();
    });

    formattedQuery = ko.computed(() => {
        let dateRange = this.dateRange();
        let startTime = dateRange[0];
        let endTime = dateRange[1];
        return "from '" + startTime.toISOString() + "' to '" + endTime.toISOString() + "' " + this.query();
    });

    operators = [
        new OperatorOption("<", "LESS_THAN"),
        new OperatorOption("<=", "LESS_THAN_OR_EQUAL_TO"),
        new OperatorOption(">", "GREATER_THAN"),
        new OperatorOption(">=", "GREATER_THAN_OR_EQUAL_TO"),
        new OperatorOption("=", "EQUAL_TO"),
        new OperatorOption("!=", "NOT_EQUAL_TO"),
    ];

    constructor() {
        this.formattedQuery.subscribe((newValue) => this.queryChanged(newValue));
    }

    activate(id: String) {
        if (id != null) {
            this.loadAlert(id);
        } else {
            this.id(uuid.v4());
        }
    }
    compositionComplete() {
        this.container = document.getElementById('graph');
    }

    loadAlert(id: String): void {
        $.getJSON("/v1/alerts/" + id, {}, (data) => {
            this.id(data.id);
            this.name(data.name);
            this.query(data.query);
            this.period(data.period);
            this.operator(data.operator);
            this.value(data.value.value);
            this.valueUnit(data.value.unit);
        });
    }

    queryChanged(newValue: string): void {
        this.executeQuery(newValue);
    }

    private executeQuery(query: string): any {
        $.ajax({
            type: 'POST',
            url: '/v1/metrics/query',
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({'query': query}),
            success: (response) => this.queryDataLoad(response),
            error: (request, status, error) => this.queryFailed(request)
        });
    }

    private queryFailed(request: jqXHR) {
        if (request.status / 100 == 4) {
            if (request.responseJSON != null && request.responseJSON.errors != null && request.responseJSON.errors.length > 0) {
                this.queryError(request.responseJSON.errors[0]);
            }
        } else if (request.status / 100 == 5) {
            this.queryError("An unknown error has occurred, please try again later");
        }
    }

    private queryDataLoad(response: MetricsResponse) {
        this.queryError(null);
        this.queryWarning(null);
        let warnings = [];
        let errors = [];
        let series: Series[] = [];
        let rangeSeriesList: RangeSeries[] = [];
        let i = 0;
        warnings.push(response.warnings);
        errors.push(response.errors);
        response.response.queries.forEach((query) => {
            query.results.forEach((result) => {
                //TODO: walk the values to look for duplicates, if duplicates create a RangeSeries from it
                let values = result.values;
                let last = null;
                let range = false;
                for (let j = 0; j < values.length; j++) {
                    if (values[j][0] == last) {
                        range = true;
                        break;
                    }
                    last = values[j][0];
                }
                if (!range) {
                    series.push({values: result.values, id: String(i++)});
                } else {
                    warnings.push("Query has a series with multiple values for a given time.");

                    last = null;

                    let rangeSeries: RangeSeries = new RangeSeries();
                    rangeSeries.id = String(i++);
                    let rangeIndex = -1;
                    for (let j = 0; j < values.length; j++) {
                        if (values[j][0] != last) {
                            rangeSeries.values.push(new RangeDatapoint(values[j][0], values[j][1], values[j][1]));
                            rangeIndex++;
                        } else {
                            if (rangeSeries.values[rangeIndex].max < values[j][1]) {
                                rangeSeries.values[rangeIndex].max = values[j][1];
                            }
                            if (rangeSeries.values[rangeIndex].min > values[j][1]) {
                                rangeSeries.values[rangeIndex].min = values[j][1];
                            }
                        }
                        last = values[j][0];
                    }

                    rangeSeriesList.push(rangeSeries);
                }
            });
        });

        this.queryWarning(warnings.join("\r\n"));
        this.queryError(errors.join("\r\n"));
        let svg = d3.select(this.container);
        svg.select("g").remove();
        let margin = {top: 20, right: 20, bottom: 30, left: 20};
        let width = 0;
        let height = 0;
        if (svg.node() != null) {
            width = svg.node().getBoundingClientRect().width - margin.left - margin.right;
            height = svg.node().getBoundingClientRect().height - margin.top - margin.bottom;
        }
        let g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");
        let x = d3.scaleTime()
            .rangeRound([0, width]);

        let y = d3.scaleLinear()
            .rangeRound([height, 0]);
        let z = d3.scaleOrdinal(d3.schemeCategory10);

        let line = d3.line<Datapoint>()
            .x((d) => { return x(d[0]); })
            .y((d) => { return y(d[1]); });

        let xrange = [
            Math.min(
                d3.min(series, ts => d3.min(ts.values, d => d[0]))
                        || Infinity,
                d3.min(rangeSeriesList, rs => d3.min(rs.values, d => d.timestamp))
                        || Infinity,
                +this.dateRange()[0]),
            Math.max(
                d3.max(series, ts => d3.max(ts.values, d => d[0]))
                        || -Infinity,
                d3.max(rangeSeriesList, rs => d3.max(rs.values, d => d.timestamp))
                        || -Infinity,
                +this.dateRange()[1])];
        x.domain(xrange);
        let yrange = [
            Math.min(
                d3.min(series, ts => d3.min(ts.values, d => d[1]))
                        || Infinity,
                d3.min(rangeSeriesList, rs => d3.min(rs.values, d => d.min))
                || Infinity),
            Math.max(
                d3.max(series, ts => d3.max(ts.values, d => d[1]))
                || -Infinity,
                d3.max(rangeSeriesList, rs => d3.max(rs.values, d => d.max))
                || -Infinity)];
        // let yrange = [d3.min(series[0], function(d) { return d[1]; }), d3.max(series[0], function(d) { return d[1]; })];
        y.domain(yrange);

        let area = d3.area<RangeDatapoint>()
            .x((d) => { return x(d.timestamp); })
            .y0((d) => { return y(d.min); })
            .y1((d) => { return y(d.max); });

        g.append("g")
            .attr("transform", "translate(0," + height + ")")
            .call(d3.axisBottom(x));

        g.append("g")
            .call(d3.axisLeft(y))
            .append("text");

        let ts = g.selectAll(".ts")
            .data(series)
            .enter().append("g")
            .attr("class", "ts");

        ts.append("path")
            .attr("class", "line")
            .attr("fill", "none")
            .attr("d", function(d) { return line(d.values); })
            .style("stroke", function(d) { return z(d.id); });

        let ats = g.selectAll(".ats")
            .data(rangeSeriesList)
            .enter().append("g")
            .attr("class", "ats");
        ats.append("path")
            .attr("class", "area")
            .attr("fill", function(d) { return z(d.id); })
            .attr("d", function(d) { return area(d.values); })
            .style("opacity", 0.3)
            .style("stroke", function(d) {return z(d.id); });

        // ts.append("text")
        //     .datum(function(d) { return {id: d.id, value: d.values[d.values.length - 1]}; })
        //     .attr("transform", function(d) { return "translate(" + x(d.value.date) + "," + y(d.value.temperature) + ")"; })
        //     .attr("x", 3)
        //     .attr("dy", "0.35em")
        //     .style("font", "10px sans-serif")
        //     .text(function(d) { return d.id; });

        // let care = series[0];
        // g.append("path")
        //     .datum(care)
        //     .attr("fill", "none")
        //     .attr("stroke", "steelblue")
        //     // .attr("stroke-linejoin", "round")
        //     // .attr("stroke-linecap", "round")
        //     .attr("stroke-width", 1.5)
        //     .attr("d", line);

        // if (this.container != null) {
        //     flotr.draw(this.container, series, {
        //         yaxis: {
        //         //     max: graphMax,
        //         //     min: graphMin
        //             autoscale: true,
        //             // autoscaleMargin: 10,
        //         },
        //         xaxis: {
        //             mode: 'time',
        //             noTicks: 3,
        //             // min: graphStart,
        //             // max: graphEnd,
        //             timeMode: "local"
        //
        //         },
        //         title: 'Backtesting',
        //         HtmlText: true,
        //         mouse: {
        //             track: true,
        //             sensibility: 8,
        //             radius: 15,
        //             relative: true,
        //             crosshair: 'y',
        //             trackFormatter: (descriptor: any) => { return descriptor.y}
        //         },
        //         legend: {
        //             show: true
        //         }
        //     });
        // }
    }

    save(): void {
        $.ajax({
            type: "PUT",
            url: "/v1/alerts",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                "id": this.id(),
                "query": this.query(),
                "name": this.name(),
                "period": this.period(),
                "operator": this.operator(),
                "value": {"value": this.value(), "unit": this.valueUnit()}
            }),
        }).done(() => {
            window.location.href = "/#alerts";
        });
    }

    cancel(): void {
        window.location.href = "/#alerts";
    }
}

export = EditAlertViewModel;
