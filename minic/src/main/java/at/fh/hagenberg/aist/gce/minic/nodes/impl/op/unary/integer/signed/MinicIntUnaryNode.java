/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.integer.signed;

import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the int unary operations:
 * !
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "unary-int", description = "Abstract base class for unary integer operations")
@NodeChildren({
    @NodeChild(value = "value", type = MinicIntNode.class),
})
public abstract class MinicIntUnaryNode extends MinicIntNode {

    @NodeInfo(shortName = "!", description = "!int (0 -> 1; everything else -> 0)")
    public abstract static class MinicIntLogicalNotNode extends MinicIntUnaryNode {
        @Specialization
        public int not(int value) {
            return value == 0 ? 1 : 0;
        }
    }

}
