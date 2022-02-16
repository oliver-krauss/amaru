/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.values;

import java.util.Arrays;
import java.util.LinkedList;

public class CharDefault extends DefaultValues<Character> {

    public CharDefault() {
        super(new LinkedList<>(Arrays.asList(
            (char) 9, (char) 10, (char) 13, (char) 32, (char) 39,
            (char) 92, (char) 48, (char) 49, (char) 50, (char) 51,
            (char) 52, (char) 53, (char) 53, (char) 54, (char) 55,
            (char) 56, (char) 57, (char) 65, (char) 66, (char) 67,
            (char) 68, (char) 69, (char) 70, (char) 71, (char) 72,
            (char) 73, (char) 74, (char) 75, (char) 76, (char) 77,
            (char) 78, (char) 79, (char) 80, (char) 81, (char) 82,
            (char) 83, (char) 84, (char) 85, (char) 86, (char) 87,
            (char) 88, (char) 89, (char) 90, (char) 97, (char) 98,
            (char) 99, (char) 100, (char) 101, (char) 102, (char) 103,
            (char) 104, (char) 105, (char) 106, (char) 107, (char) 108,
            (char) 109, (char) 110, (char) 111, (char) 112, (char) 113,
            (char) 114, (char) 115, (char) 116, (char) 117, (char) 118,
            (char) 119, (char) 120, (char) 121, (char) 122
        )));
    }

}
