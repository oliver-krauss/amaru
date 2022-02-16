/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function Vector2D(x, y) {
    this.x = x;
    this.y = y;
}

Vector2D.prototype.plus = function (other) {
    return new Vector2D(this.x + other.x,
        this.y + other.y);
};

Vector2D.prototype.minus = function (other) {
    return new Vector2D(this.x - other.x,
        this.y - other.y);
};

Vector2D.prototype.toString = function () {
    return "[" + this.x + ", " + this.y + "]";
};

Vector2D.prototype.compareTo = function (other) {
    var result = compareNumbers(this.x, other.x);
    if (result)
        return result;
    return compareNumbers(this.y, other.y);
};

