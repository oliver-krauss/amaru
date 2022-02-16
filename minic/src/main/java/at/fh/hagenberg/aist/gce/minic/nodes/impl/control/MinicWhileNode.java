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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * While (condition) { loop }
 * Created by Oliver Krauss on 27.07.2016.
 */
@NodeInfo(shortName = "while", description = "While (condition) { loop }")
public class MinicWhileNode extends MinicNode {

    /**
     * Loop that will be executed while the condition in the RepeatingNode is true
     */
    @Child
    private LoopNode loopNode;

    public MinicWhileNode(MinicExpressionNode conditionNode, MinicNode bodyNode) {
        this.loopNode = Truffle.getRuntime().createLoopNode(new MinicRepeatingNode(conditionNode, bodyNode));
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loopNode.executeLoop(frame);
    }
}
