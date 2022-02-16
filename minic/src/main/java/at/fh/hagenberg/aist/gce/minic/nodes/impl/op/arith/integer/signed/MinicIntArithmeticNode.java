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

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling all int arithmetic operations:
 * + - / * %
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "arith-int", description = "Abstract base class for arithmetic int operations")
@NodeChildren({
    @NodeChild("leftNode"),
    @NodeChild("rightNode")
})
public abstract class MinicIntArithmeticNode extends MinicIntNode {

    @NodeInfo(shortName = "+", description = "int + int")
    public abstract static class MinicIntAddNode extends MinicIntArithmeticNode {
        @Specialization
        public int add(int left, int right) {
            return left + right;
        }
    }

    @NodeInfo(shortName = "-", description = "int - int")
    public abstract static class MinicIntSubNode extends MinicIntArithmeticNode {
        @Specialization
        public int sub(int left, int right) {
            return left - right;
        }
    }

    @NodeInfo(shortName = "/", description = "int / int")
    public abstract static class MinicIntDivNode extends MinicIntArithmeticNode {
        @Specialization
        public int div(int left, int right) {
            return left / right;
        }
    }

    @NodeInfo(shortName = "*", description = "int * int")
    public abstract static class MinicIntMulNode extends MinicIntArithmeticNode {
        @Specialization
        public int mul(int left, int right) {
            return left * right;
        }
    }

    @NodeInfo(shortName = "%", description = "int % int")
    public abstract static class MinicIntModNode extends MinicIntArithmeticNode {
        @Specialization
        public int mod(int left, int right) {
            return left % right;
        }
    }
}
