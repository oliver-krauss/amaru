/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function benchmark() {
    var verbosity = 0;
    var numAircraft = 1000;
    var numFrames = 200;
    var expectedCollisions = 14484;
    var percentile = 95;

    var simulator = new Simulator(numAircraft);
    var detector = new CollisionDetector();
    var lastTime = currentTime();
    var results = [];
    for (var i = 0; i < numFrames; ++i) {
        var time = i / 10;

        var collisions = detector.handleNewFrame(simulator.simulate(time));

        var before = lastTime;
        var after = currentTime();
        lastTime = after;
        var result = {
            time: after - before,
            numCollisions: collisions.length
        };
        if (verbosity >= 2)
            result.collisions = collisions;
        results.push(result);
    }

    if (verbosity >= 1) {
        for (var i = 0; i < results.length; ++i) {
            var string = "Frame " + i + ": " + results[i].time + " ms.";
            if (results[i].numCollisions)
                string += " (" + results[i].numCollisions + " collisions.)";
            print(string);
            if (verbosity >= 2 && results[i].collisions.length)
                print("    Collisions: " + results[i].collisions);
        }
    }

    // Check results.
    var actualCollisions = 0;
    for (var i = 0; i < results.length; ++i)
        actualCollisions += results[i].numCollisions;
    if (actualCollisions != expectedCollisions) {
        throw new Error("Bad number of collisions: " + actualCollisions + " (expected " +
            expectedCollisions + ")");
    }

    // Find the worst 5% 
    var times = [];
    for (var i = 0; i < results.length; ++i)
        times.push(results[i].time);

    return averageAbovePercentile(times, percentile);
}
