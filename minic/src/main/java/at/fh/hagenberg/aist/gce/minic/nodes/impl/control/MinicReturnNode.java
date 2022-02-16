/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.control;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReturnException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Handles returning a value to end a function execution.
 * A return node doesn't need to be in a function since Functions in C can be of void (returning null).
 * The return statement returns its value by throwing an exception that contains the value.
 * This automatically ends the function execution, and prevents succeeding statements to be executed.
 * Created by Oliver Krauss on 29.06.2016.
 */
@NodeInfo(shortName = "return", description = "Returns the result of its statement to as a ReturnException")
public class MinicReturnNode extends MinicNode {

    /**
     * Statement that will be returned in {@link MinicReturnException}
     */
    @Node.Child
    private MinicExpressionNode returnStatement;

    public MinicReturnNode(MinicExpressionNode returnStatement) {
        this.returnStatement = returnStatement;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object result = null;
        if (returnStatement != null) {
            result = returnStatement.executeGeneric(frame);
        }
        throw new MinicReturnException(result);
    }
}
