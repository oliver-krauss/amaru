/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed;

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
@NodeInfo(shortName = "binary-int", description = "Abstract base class for binary relational integer operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicIntNode.class),
    @NodeChild(value = "rightNode", type = MinicIntNode.class)
})
public abstract class MinicIntRelationalNode extends MinicIntNode {

    @NodeInfo(shortName = "==", description = "int == int")
    public abstract static class MinicIntEqualsNode extends MinicIntRelationalNode {
        @Specialization
        public int equals(int left, int right) {
            return left == right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "!=", description = "int != int")
    public abstract static class MinicIntNotEqualsNode extends MinicIntRelationalNode {
        @Specialization
        public int notEquals(int left, int right) {
            return left != right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">", description = "int > int")
    public abstract static class MinicIntGtNode extends MinicIntRelationalNode {
        @Specialization
        public int gt(int left, int right) {
            return left > right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">=", description = "int >= int")
    public abstract static class MinicIntGtENode extends MinicIntRelationalNode {
        @Specialization
        public int gtEquals(int left, int right) {
            return left >= right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<", description = "int < int")
    public abstract static class MinicIntLtNode extends MinicIntRelationalNode {
        @Specialization
        public int lt(int left, int right) {
            return left < right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<=", description = "int <= int")
    public abstract static class MinicIntLtENode extends MinicIntRelationalNode {
        @Specialization
        public int ltEquals(int left, int right) {
            return left <= right ? 1 : 0;
        }
    }
}
