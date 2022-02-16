/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function Simulator(numAircraft) {
    this._aircraft = [];
    for (var i = 0; i < numAircraft; ++i)
        this._aircraft.push(new CallSign("foo" + i));
}

Simulator.prototype.simulate = function (time) {
    var frame = [];
    for (var i = 0; i < this._aircraft.length; i += 2) {
        frame.push({
            callsign: this._aircraft[i],
            position: new Vector3D(time, Math.cos(time) * 2 + i * 3, 10)
        });
        frame.push({
            callsign: this._aircraft[i + 1],
            position: new Vector3D(time, Math.sin(time) * 2 + i * 3, 10)
        });
    }
    return frame;
};

