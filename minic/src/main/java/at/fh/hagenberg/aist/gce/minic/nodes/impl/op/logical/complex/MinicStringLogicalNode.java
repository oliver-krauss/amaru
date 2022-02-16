/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.complex;

import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
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
@NodeInfo(shortName = "logical-string", description = "Abstract base class for logical string operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicStringNode.class),
    @NodeChild(value = "rightNode", type = MinicStringNode.class)
})
public abstract class MinicStringLogicalNode extends MinicIntNode {

    @NodeInfo(shortName = "||", description = "string || string")
    public abstract static class MinicStringOrNode extends MinicStringLogicalNode {

        @Specialization
        public int or(String left, String right) {
            return ((left != null && !left.equals("0") && !left.equals("false")) || (right != null && !right.equals("0") && !right.equals("false"))) ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "&&", description = "string && string")
    public abstract static class MinicStringAndNode extends MinicStringLogicalNode {
        @Specialization
        public int and(String left, String right) {
            return ((left != null && !left.equals("0") && !left.equals("false")) && (right != null && !right.equals("0") && !right.equals("false"))) ? 1 : 0;
        }
    }

}
