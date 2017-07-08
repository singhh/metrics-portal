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

class OperatorOption {
    text: string;
    value: string;

    constructor(text, value) {
        this.text = text;
        this.value = value;
    }
}

class EditAlertViewModel {
    id = ko.observable<string>("");
    name = ko.observable<string>("");
    query = ko.observable<string>("from 2 hours ago to now select elapsed");
    period = ko.observable<string>("PT1M");
    operator = ko.observable<string>("GREATER_THAN");
    value = ko.observable<number>(0);
    valueUnit = ko.observable<string>(null);
    operators = [
        new OperatorOption("<", "LESS_THAN"),
        new OperatorOption("<=", "LESS_THAN_OR_EQUAL_TO"),
        new OperatorOption(">", "GREATER_THAN"),
        new OperatorOption(">=", "GREATER_THAN_OR_EQUAL_TO"),
        new OperatorOption("=", "EQUAL_TO"),
        new OperatorOption("!=", "NOT_EQUAL_TO"),
    ];

    constructor() {
        this.name.subscribe((newValue) => this.queryChanged());
    }

    activate(id: String) {
        if (id != null) {
            this.loadAlert(id);
        } else {
            this.id(uuid.v4());
        }
    }

    loadAlert(id: String): void {
        $.getJSON("/v1/alerts/" + id, {}, (data) => {
            console.log(data);
            this.id(data.id);
            this.name(data.name);
            this.query(data.query);
            this.period(data.period);
            this.operator(data.operator);
            this.value(data.value.value);
            this.valueUnit(data.value.unit);
        });
    }

    queryChanged(): void {
        console.log("value changed: ", this.query());
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
