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

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
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
@NodeInfo(shortName = "binary-float", description = "Abstract base class for binary relational float operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicFloatNode.class),
    @NodeChild(value = "rightNode", type = MinicFloatNode.class)
})
public abstract class MinicFloatRelationalNode extends MinicIntNode {

    @NodeInfo(shortName = "==", description = "float == float")
    public abstract static class MinicFloatEqualsNode extends MinicFloatRelationalNode {

        @Specialization
        public int equals(float left, float right) {
            return left == right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "!=", description = "float != float")
    public abstract static class MinicFloatNotEqualsNode extends MinicFloatRelationalNode {
        @Specialization
        public int notEquals(float left, float right) {
            return left != right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">", description = "float > float")
    public abstract static class MinicFloatGtNode extends MinicFloatRelationalNode {
        @Specialization
        public int gt(float left, float right) {
            return left > right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">=", description = "float >= float")
    public abstract static class MinicFloatGtENode extends MinicFloatRelationalNode {
        @Specialization
        public int gtEquals(float left, float right) {
            return left >= right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<", description = "float < float")
    public abstract static class MinicFloatLtNode extends MinicFloatRelationalNode {
        @Specialization
        public int lt(float left, float right) {
            return left < right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<=", description = "float <= float")
    public abstract static class MinicFloatLtENode extends MinicFloatRelationalNode {
        @Specialization
        public int ltEquals(float left, float right) {
            return left <= right ? 1 : 0;
        }
    }

}
