/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function compareNumbers(a, b) {
    if (a == b)
        return 0;
    if (a < b)
        return -1;
    if (a > b)
        return 1;

    // We say that NaN is smaller than non-NaN.
    if (a == a)
        return 1;
    return -1;
}

function averageAbovePercentile(numbers, percentile) {
    // Don't change the original array.
    numbers = numbers.slice();

    // Sort in ascending order.
    numbers.sort(function (a, b) {
        return a - b;
    });

    // Now the elements we want are at the end. Keep removing them until the array size shrinks too much.
    // Examples assuming percentile = 99:
    //
    // - numbers.length starts at 100: we will remove just the worst entry and then not remove anymore,
    //   since then numbers.length / originalLength = 0.99.
    //
    // - numbers.length starts at 1000: we will remove the ten worst.
    //
    // - numbers.length starts at 10: we will remove just the worst.
    var numbersWeWant = [];
    var originalLength = numbers.length;
    while (numbers.length / originalLength > percentile / 100)
        numbersWeWant.push(numbers.pop());

    var sum = 0;
    for (var i = 0; i < numbersWeWant.length; ++i)
        sum += numbersWeWant[i];

    var result = sum / numbersWeWant.length;

    // Do a sanity check.
    if (numbers.length && result < numbers[numbers.length - 1]) {
        throw "Sanity check fail: the worst case result is " + result +
        " but we didn't take into account " + numbers;
    }

    return result;
}

var currentTime;
if (this.performance && performance.now)
    currentTime = function () {
        return performance.now()
    };
else if (preciseTime)
    currentTime = function () {
        return preciseTime() * 1000;
    };
else
    currentTime = function () {
        return 0 + new Date();
    };
