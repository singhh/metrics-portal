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
import d3 = require('d3');

class OperatorOption {
    text: string;
    value: string;

    constructor(text, value) {
        this.text = text;
        this.value = value;
    }
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

class EditAlertViewModel {
    id = ko.observable<string>("");
    name = ko.observable<string>("");
    query = ko.observable<string>("from 2 hours ago to now select").extend({ rateLimit: { timeout: 500, method: "notifyWhenChangesStop" } });
    period = ko.observable<string>("PT1M");
    operator = ko.observable<string>("GREATER_THAN");
    value = ko.observable<number>(0);
    valueUnit = ko.observable<string>(null);
    container: HTMLElement;
    operators = [
        new OperatorOption("<", "LESS_THAN"),
        new OperatorOption("<=", "LESS_THAN_OR_EQUAL_TO"),
        new OperatorOption(">", "GREATER_THAN"),
        new OperatorOption(">=", "GREATER_THAN_OR_EQUAL_TO"),
        new OperatorOption("=", "EQUAL_TO"),
        new OperatorOption("!=", "NOT_EQUAL_TO"),
    ];

    constructor() {
        this.query.subscribe((newValue) => this.queryChanged(newValue));
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

    executeQuery(query: string): any {
        $.ajax({
            type: 'POST',
            url: '/v1/metrics/query',
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({'query': query}),
            success: (response) => this.queryDataLoad(response)
        });
    }

    queryDataLoad(response: QueryResponse) {
        let series: Series[] = [];
        let i = 0;
        response.queries.forEach((query) => {
            query.results.forEach((result) => {
                series.push({values: result.values, id: String(i++)});
            });
        });

        let svg = d3.select(this.container);
        svg.select("g").remove();
        let margin = {top: 20, right: 80, bottom: 30, left: 50};
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

        let line = d3.line<number[]>()
            .x((d) => { return x(d[0]); })
            .y((d) => { return y(d[1]); });

        let xrange = [d3.min(series, ts => d3.min(ts.values, d => d[0])), d3.max(series, ts => d3.max(ts.values, d => d[0]))];
        x.domain(xrange);
        let yrange = [d3.min(series, ts => d3.min(ts.values, d => d[1])), d3.max(series, ts => d3.max(ts.values, d => d[1]))];
        // let yrange = [d3.min(series[0], function(d) { return d[1]; }), d3.max(series[0], function(d) { return d[1]; })];
        y.domain(yrange);


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
