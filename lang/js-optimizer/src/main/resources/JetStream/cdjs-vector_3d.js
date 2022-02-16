/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function Vector3D(x, y, z) {
    this.x = x;
    this.y = y;
    this.z = z;
}

Vector3D.prototype.plus = function (other) {
    return new Vector3D(this.x + other.x,
        this.y + other.y,
        this.z + other.z);
};

Vector3D.prototype.minus = function (other) {
    return new Vector3D(this.x - other.x,
        this.y - other.y,
        this.z - other.z);
};

Vector3D.prototype.dot = function (other) {
    return this.x * other.x + this.y * other.y + this.z * other.z;
};

Vector3D.prototype.squaredMagnitude = function () {
    return this.dot(this);
};

Vector3D.prototype.magnitude = function () {
    return Math.sqrt(this.squaredMagnitude());
};

Vector3D.prototype.times = function (amount) {
    return new Vector3D(this.x * amount,
        this.y * amount,
        this.z * amount);
};

Vector3D.prototype.as2D = function () {
    return new Vector2D(this.x, this.y);
};

Vector3D.prototype.toString = function () {
    return "[" + this.x + ", " + this.y + ", " + this.z + "]";
};


