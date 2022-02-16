/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function CollisionDetector() {
    this._state = new RedBlackTree();
}

CollisionDetector.prototype.handleNewFrame = function (frame) {
    var motions = [];
    var seen = new RedBlackTree();

    for (var i = 0; i < frame.length; ++i) {
        var aircraft = frame[i];

        var oldPosition = this._state.put(aircraft.callsign, aircraft.position);
        var newPosition = aircraft.position;
        seen.put(aircraft.callsign, true);

        if (!oldPosition) {
            // Treat newly introduced aircraft as if they were stationary.
            oldPosition = newPosition;
        }

        motions.push(new Motion(aircraft.callsign, oldPosition, newPosition));
    }

    // Remove aircraft that are no longer present.
    var toRemove = [];
    this._state.forEach(function (callsign, position) {
        if (!seen.get(callsign))
            toRemove.push(callsign);
    });
    for (var i = 0; i < toRemove.length; ++i)
        this._state.remove(toRemove[i]);

    var allReduced = reduceCollisionSet(motions);
    var collisions = [];
    for (var reductionIndex = 0; reductionIndex < allReduced.length; ++reductionIndex) {
        var reduced = allReduced[reductionIndex];
        for (var i = 0; i < reduced.length; ++i) {
            var motion1 = reduced[i];
            for (var j = i + 1; j < reduced.length; ++j) {
                var motion2 = reduced[j];
                var collision = motion1.findIntersection(motion2);
                if (collision)
                    collisions.push(new Collision([motion1.callsign, motion2.callsign], collision));
            }
        }
    }

    return collisions;
};
