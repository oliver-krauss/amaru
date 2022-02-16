/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * Represents a function signature, e.g. information about the arguments of a function
 * Currently does not support output
 */
public class TruffleFunctionSignature {

    /**
     * The function node this signature is for
     */
    private CallTarget function;

    /**
     * Arguments in order from left to right
     * The value is the java class of the argument
     */
    private String[] arguments;

    public TruffleFunctionSignature(CallTarget function, String[] arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    public CallTarget getFunction() {
        return function;
    }

    public String[] getArguments() {
        return arguments;
    }

    public int size() {
        return arguments.length;
    }
}
