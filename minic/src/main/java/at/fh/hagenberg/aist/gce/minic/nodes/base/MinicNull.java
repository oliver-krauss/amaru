/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.base;

import com.oracle.truffle.api.interop.TruffleObject;

/**
 * Class representing NULL in Minic
 * Created by Oliver Krauss on 16.06.2016.
 */
public final class MinicNull implements TruffleObject {

    /**
     * The canonical value to represent {@code null} in SL.
     */
    public static final MinicNull SINGLETON = new MinicNull();

    /**
     * Disallow instantiation from outside to ensure that the {@link #SINGLETON} is the only
     * instance.
     */
    private MinicNull() {
    }

    /**
     * This method is, e.g., called when using the {@code null} value in a string concatenation. So
     * changing it has an effect on SL programs.
     */
    @Override
    public String toString() {
        return "null";
    }

}

