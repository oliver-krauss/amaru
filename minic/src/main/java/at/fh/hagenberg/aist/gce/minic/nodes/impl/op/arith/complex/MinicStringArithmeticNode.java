/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.complex;

import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling all String arithmetric operations:
 * + (Concat, nothing else allowed in string!)
 * Created by Oliver Krauss on 14.06.2016.
 */
@NodeInfo(shortName = "arith-string", description = "Abstract base class for arithmetic string operations")
@NodeChildren({
    @NodeChild("leftNode"),
    @NodeChild("rightNode")
})
public abstract class MinicStringArithmeticNode extends MinicStringNode {

    @NodeInfo(shortName = "+", description = "string + string")
    public abstract static class MinicStringAddNode extends MinicStringArithmeticNode {
        @Specialization
        public String add(String left, String right) {
            return left + right;
        }
    }
}
