/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed;

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling all char arithmetric operations:
 * + - / * %
 * Char arithmetric always returns int thus the char nodes extend from MinicIntNode
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "arith-char", description = "Abstract base class for arithmetic char operations")
@NodeChildren({
    @NodeChild(value = "leftNode", type = MinicCharNode.class),
    @NodeChild(value = "rightNode", type = MinicCharNode.class)
})
public abstract class MinicCharArithmeticNode extends MinicIntNode {

    @NodeInfo(shortName = "+", description = "char + char")
    public abstract static class MinicCharAddNode extends MinicCharArithmeticNode {
        @Specialization
        public int add(char left, char right) {
            return left + right;
        }
    }

    @NodeInfo(shortName = "-", description = "char - char")
    public abstract static class MinicCharSubNode extends MinicCharArithmeticNode {
        @Specialization
        public int sub(char left, char right) {
            return left - right;
        }
    }

    @NodeInfo(shortName = "/", description = "char / char")
    public abstract static class MinicCharDivNode extends MinicCharArithmeticNode {
        @Specialization
        public int div(char left, char right) {
            return left / right;
        }
    }

    @NodeInfo(shortName = "*", description = "char * char")
    public abstract static class MinicCharMulNode extends MinicCharArithmeticNode {
        @Specialization
        public int mul(char left, char right) {
            return left * right;
        }
    }

    @NodeInfo(shortName = "%", description = "char % char")
    public abstract static class MinicCharModNode extends MinicCharArithmeticNode {
        @Specialization
        public int mod(char left, char right) {
            return left % right;
        }
    }
}
