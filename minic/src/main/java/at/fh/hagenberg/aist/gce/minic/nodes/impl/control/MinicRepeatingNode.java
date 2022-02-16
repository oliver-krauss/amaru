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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;

/**
 * Node used by {@link com.oracle.truffle.api.nodes.LoopNode}.
 * The repeating node calls it's body node as long as the given condition is true
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "loop-body", description = "While condition is true, the node will call its body repeatedly")
public final class MinicRepeatingNode extends MinicNode implements RepeatingNode {

    /**
     * The condition of the loop.
     */
    @Child
    private MinicExpressionNode conditionNode;

    /**
     * Statement (or {@link MinicBlockNode block}) executed as long as the condition is true.
     */
    @Child
    private MinicNode bodyNode;

    public MinicRepeatingNode(MinicExpressionNode conditionNode, MinicNode bodyNode) {
        this.conditionNode = conditionNode;
        this.bodyNode = bodyNode;
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        if (evaluateCondition(frame)) {
            bodyNode.executeVoid(frame);
            return true;
        } else {
            return false;
        }
    }

    private boolean evaluateCondition(VirtualFrame frame) {
        return !this.conditionNode.executeGeneric(frame).equals(0);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        executeRepeating(frame);
    }
}
