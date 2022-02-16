/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating;

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the relational binary operations:
 * ==, !=, >, <, >=, <=
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "binary-double", description = "Abstract base class for binary relational double operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicDoubleNode.class),
    @NodeChild(value = "rightNode", type = MinicDoubleNode.class)
})
public abstract class MinicDoubleRelationalNode extends MinicIntNode {

    @NodeInfo(shortName = "==", description = "double == double")
    public abstract static class MinicDoubleEqualsNode extends MinicDoubleRelationalNode {

        @Specialization
        public int equals(double left, double right) {
            return left == right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "!=", description = "double != double")
    public abstract static class MinicDoubleNotEqualsNode extends MinicDoubleRelationalNode {
        @Specialization
        public int notEquals(double left, double right) {
            return left != right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">", description = "double > double")
    public abstract static class MinicDoubleGtNode extends MinicDoubleRelationalNode {
        @Specialization
        public int gt(double left, double right) {
            return left > right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">=", description = "double >= double")
    public abstract static class MinicDoubleGtENode extends MinicDoubleRelationalNode {
        @Specialization
        public int gtEquals(double left, double right) {
            return left >= right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<", description = "double < double")
    public abstract static class MinicDoubleLtNode extends MinicDoubleRelationalNode {
        @Specialization
        public int lt(double left, double right) {
            return left < right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<=", description = "double <= double")
    public abstract static class MinicDoubleLtENode extends MinicDoubleRelationalNode {
        @Specialization
        public int ltEquals(double left, double right) {
            return left <= right ? 1 : 0;
        }
    }
}
