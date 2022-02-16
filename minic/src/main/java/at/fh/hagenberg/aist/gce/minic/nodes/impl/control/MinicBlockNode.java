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

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Block node {}
 * Created by Oliver Krauss on 29.06.2016.
 */
@NodeInfo(shortName = "block", description = "{...} block, containing statements")
public class MinicBlockNode extends MinicNode {

    /**
     * Statements in block
     */
    @Children
    private final MinicNode[] bodyNodes;

    public MinicBlockNode(MinicNode... bodyNodes) {
        this.bodyNodes = bodyNodes;
    }

    @Override
    @ExplodeLoop
    public void executeVoid(VirtualFrame frame) {
        if (bodyNodes != null) {
            /*
             * This assertion illustrates that the array length is really a constant during compilation.
             */
            CompilerAsserts.partialEvaluationConstant(bodyNodes.length);
            for (MinicNode statement : bodyNodes) {
                statement.executeVoid(frame);
            }
        }
    }

}
