/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.function;

import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * C function representation. Has a fixed call target because C does not allow function rewrites
 * Created by Oliver Krauss on 15.06.2016.
 */
public final class MinicFunctionNode implements TruffleObject {

    /**
     * Return type of the function
     */
    private final MinicBaseType type;

    /**
     * Name of function
     */
    private final String name;

    /**
     * The current implementation of this function.
     */
    private final RootCallTarget callTarget;

    public MinicFunctionNode(String name, RootCallTarget callTarget, MinicBaseType type) {
        this.name = name;
        this.callTarget = callTarget;
        this.type = type;
    }

    public MinicBaseType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }
}
