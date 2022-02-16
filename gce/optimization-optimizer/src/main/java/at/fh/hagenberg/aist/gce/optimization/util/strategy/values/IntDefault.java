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

public class IntDefault extends DefaultValues<Integer> {

    public IntDefault() {
        super(new LinkedList<>(Arrays.asList(0, 1, 2, 3)));
    }

}