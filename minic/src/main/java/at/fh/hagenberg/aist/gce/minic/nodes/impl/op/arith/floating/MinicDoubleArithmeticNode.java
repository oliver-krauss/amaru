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

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling all double arithmetric operations:
 * + - / * %
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "arith-double", description = "Abstract base class for arithmetic double operations")
@NodeChildren({
    @NodeChild("leftNode"),
    @NodeChild("rightNode")
})
public abstract class MinicDoubleArithmeticNode extends MinicDoubleNode {

    @NodeInfo(shortName = "+", description = "double + double")
    public abstract static class MinicDoubleAddNode extends MinicDoubleArithmeticNode {
        @Specialization
        public double add(double left, double right) {
            return left + right;
        }
    }

    @NodeInfo(shortName = "-", description = "double - double")
    public abstract static class MinicDoubleSubNode extends MinicDoubleArithmeticNode {
        @Specialization
        public double sub(double left, double right) {
            return left - right;
        }
    }

    @NodeInfo(shortName = "/", description = "double / double")
    public abstract static class MinicDoubleDivNode extends MinicDoubleArithmeticNode {
        @Specialization
        public double div(double left, double right) {
            return left / right;
        }
    }

    @NodeInfo(shortName = "*", description = "double * double")
    public abstract static class MinicDoubleMulNode extends MinicDoubleArithmeticNode {
        @Specialization
        public double mul(double left, double right) {
            return left * right;
        }
    }

    @NodeInfo(shortName = "%", description = "double % double")
    public abstract static class MinicDoubleModNode extends MinicDoubleArithmeticNode {
        @Specialization
        public double mod(double left, double right) {
            return left % right;
        }
    }
}
