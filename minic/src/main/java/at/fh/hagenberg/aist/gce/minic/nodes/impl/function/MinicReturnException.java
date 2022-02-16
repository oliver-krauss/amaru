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

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Exception that {@link at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicReturnNode} will throw, so it can be caught by a {@link MinicFunctionBodyNode}
 * Created by Oliver Krauss on 29.06.2016.
 */
public class MinicReturnException extends ControlFlowException {

    /**
     * Result that will be pushed as result of the function
     */
    private final Object result;

    public MinicReturnException(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
