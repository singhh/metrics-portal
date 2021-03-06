/*
 * Copyright 2014 Groupon.com
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

import ConnectionVM = require('./ConnectionVM');
import StatisticView = require('./StatisticView');
import ViewDuration = require('./ViewDuration');
import GraphSpec = require('./GraphSpec')
import ko = require('knockout');

class GaugeVM implements StatisticView {
    id: string;
    container: HTMLElement = null;
    started: boolean = false;
    gauge: Gauge = null;
    name: string;
    paused: boolean;
    targetFrameRate: number = 60;
    spec: GraphSpec;
    renderDots: KnockoutObservable<boolean>     = ko.observable<boolean>(false);
    renderStacked: KnockoutObservable<boolean>  = ko.observable<boolean>(false);
    graphType: KnockoutObservable<string>       = ko.observable<string>("");

    constructor(id: string, name: string, spec: GraphSpec) {
        this.id = id;
        this.name = name;
        this.spec = spec;
    }

    render() {
        return;
    }

    setPause(pause: boolean) {
        this.paused = pause;
    }

    start(paused: boolean) {
        if (this.started == true) {
            return;
        }
        this.started = true;
        this.paused = paused;
        this.container = document.getElementById(this.id);

        var config: any =
            {
                size: 250,
                label: "",
                min: 0,
                max: 100,
                minorTicks: 5
            };

        this.gauge = new Gauge(this.id, config);
        this.gauge.render();
        // TODO(vkoskela): Support target frame rate in gauge [AINT-?]
    }

    postData(server: string, timestamp: number, dataValue: number, cvm: ConnectionVM) {
        if (this.paused) {
            return;
        }
        var val = dataValue;
        this.gauge.redraw(val, 2000);
    }

    setViewDuration(duration: ViewDuration) {  }

    updateColor(cvm: ConnectionVM) {  }

    disconnectConnection(cvm: ConnectionVM) {  }

    shutdown() {  }
}

export = GaugeVM;
