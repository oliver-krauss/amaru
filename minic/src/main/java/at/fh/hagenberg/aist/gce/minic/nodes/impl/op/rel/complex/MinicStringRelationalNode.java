/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.complex;

import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
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
@NodeInfo(shortName = "binary-string", description = "Abstract base class for binary relational string operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicStringNode.class),
    @NodeChild(value = "rightNode", type = MinicStringNode.class)
})
public abstract class MinicStringRelationalNode extends MinicIntNode {

    @NodeInfo(shortName = "==", description = "string == string")
    public abstract static class MinicStringEqualsNode extends MinicStringRelationalNode {

        @Specialization
        public int equals(String left, String right) {
            return (left == null && right == null || (left != null && left.equals(right))) ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "!=", description = "string != string")
    public abstract static class MinicStringNotEqualsNode extends MinicStringRelationalNode {
        @Specialization
        public int notEquals(String left, String right) {
            return (left == null && right == null || (left != null && left.equals(right))) ? 0 : 1;
        }
    }

    @NodeInfo(shortName = ">", description = "string > string")
    public abstract static class MinicStringGtNode extends MinicStringRelationalNode {
        @Specialization
        public int gt(String left, String right) {
            return (left == null && right == null || (left != null && left.compareTo(right) > 0)) ? 0 : 1;
        }
    }

    @NodeInfo(shortName = ">=", description = "string >= string")
    public abstract static class MinicStringGtENode extends MinicStringRelationalNode {
        @Specialization
        public int gtEquals(String left, String right) {
            return (left == null && right == null || (left != null && left.compareTo(right) <= 0)) ? 1 : 0;
        }
    }

    @NodeInfo(shortName = "<", description = "string < string")
    public abstract static class MinicStringLtNode extends MinicStringRelationalNode {
        @Specialization
        public int lt(String left, String right) {
            return (left == null && right == null || (left != null && left.compareTo(right) < 0)) ? 0 : 1;
        }
    }

    @NodeInfo(shortName = "<=", description = "string <= string")
    public abstract static class MinicStringLtENode extends MinicStringRelationalNode {
        @Specialization
        public int ltEquals(String left, String right) {
            return (left == null && right == null || (left != null && left.compareTo(right) >= 0)) ? 1 : 0;
        }
    }
}
