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

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
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
@NodeInfo(shortName = "logical-char", description = "Abstract base class for logical char operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicCharNode.class),
    @NodeChild(value = "rightNode", type = MinicCharNode.class)
})
public abstract class MinicCharLogicalNode extends MinicIntNode {

    @NodeInfo(shortName = "||", description = "char || char")
    public abstract static class MinicCharOrNode extends MinicCharLogicalNode {
        @Specialization
        public int or(char left, char right) {
            return (left != 0 || right != 0) ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "&&", description = "char && char")
    public abstract static class MinicCharAndNode extends MinicCharLogicalNode {
        @Specialization
        public int and(char left, char right) {
            return (left != 0 && right != 0) ? 1 : 0;
        }
    }

}
