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

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
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
@NodeInfo(shortName = "binary-char", description = "Abstract base class for binary relational char operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicCharNode.class),
    @NodeChild(value = "rightNode", type = MinicCharNode.class)
})
public abstract class MinicCharRelationalNode extends MinicIntNode {

    @NodeInfo(shortName = "==", description = "char == char")
    public abstract static class MinicCharEqualsNode extends MinicCharRelationalNode {
        @Specialization
        public int equals(char left, char right) {
            return left == right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "!=", description = "char != char")
    public abstract static class MinicCharNotEqualsNode extends MinicCharRelationalNode {
        @Specialization
        public int notEquals(char left, char right) {
            return left != right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">", description = "char > char")
    public abstract static class MinicCharGtNode extends MinicCharRelationalNode {
        @Specialization
        public int gt(char left, char right) {
            return left > right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = ">=", description = "char >= char")
    public abstract static class MinicCharGtENode extends MinicCharRelationalNode {
        @Specialization
        public int gtEquals(char left, char right) {
            return left >= right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<", description = "char < char")
    public abstract static class MinicCharLtNode extends MinicCharRelationalNode {
        @Specialization
        public int lt(char left, char right) {
            return left < right ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<=", description = "char <= char")
    public abstract static class MinicCharLtENode extends MinicCharRelationalNode {
        @Specialization
        public int ltEquals(char left, char right) {
            return left <= right ? 1 : 0;
        }
    }
}
