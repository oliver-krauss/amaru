/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

function CallSign(value) {
    this._value = value;
}

CallSign.prototype.compareTo = function (other) {
    return this._value.localeCompare(other._value);
}

CallSign.prototype.toString = function () {
    return this._value;
}

