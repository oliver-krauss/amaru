/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating;

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "arith-float", description = "Abstract base class for arithmetic float operations")
@NodeChildren({
    @NodeChild("leftNode"),
    @NodeChild("rightNode")
})
public abstract class MinicFloatArithmeticNode extends MinicFloatNode {

    @NodeInfo(shortName = "+", description = "float + float")
    public abstract static class MinicFloatAddNode extends MinicFloatArithmeticNode {
        @Specialization
        public float add(float left, float right) {
            return left + right;
        }
    }

    @NodeInfo(shortName = "-", description = "float - float")
    public abstract static class MinicFloatSubNode extends MinicFloatArithmeticNode {
        @Specialization
        public float sub(float left, float right) {
            return left - right;
        }
    }

    @NodeInfo(shortName = "/", description = "float / float")
    public abstract static class MinicFloatDivNode extends MinicFloatArithmeticNode {
        @Specialization
        public float div(float left, float right) {
            return left / right;
        }
    }

    @NodeInfo(shortName = "*", description = "float * float")
    public abstract static class MinicFloatMulNode extends MinicFloatArithmeticNode {
        @Specialization
        public float mul(float left, float right) {
            return left * right;
        }
    }

    @NodeInfo(shortName = "%", description = "float % float")
    public abstract static class MinicFloatModNode extends MinicFloatArithmeticNode {
        @Specialization
        public float mod(float left, float right) {
            return left % right;
        }
    }

}
