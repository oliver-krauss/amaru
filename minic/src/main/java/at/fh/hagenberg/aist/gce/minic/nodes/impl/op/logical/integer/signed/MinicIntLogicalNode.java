/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.integer.signed;

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the logical operations:
 * || &&
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "logical-int", description = "Abstract base class for logical int operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicIntNode.class),
    @NodeChild(value = "rightNode", type = MinicIntNode.class)
})
public abstract class MinicIntLogicalNode extends MinicIntNode {

    @NodeInfo(shortName = "||", description = "int || int")
    public abstract static class MinicIntOrNode extends MinicIntLogicalNode {
        @Specialization
        public int or(int left, int right) {
            return (left != 0 || right != 0) ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "&&", description = "int && int")
    public abstract static class MinicIntAndNode extends MinicIntLogicalNode {
        @Specialization
        public int and(int left, int right) {
            return (left != 0 && right != 0) ? 1 : 0;
        }
    }

}
