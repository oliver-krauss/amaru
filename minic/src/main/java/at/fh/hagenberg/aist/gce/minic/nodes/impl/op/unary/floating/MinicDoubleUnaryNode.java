/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating;

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the double unary operations:
 * !
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "unary-double", description = "Abstract base class for unary double operations")
@NodeChildren({
    @NodeChild(value = "value", type = MinicDoubleNode.class),
})
public abstract class MinicDoubleUnaryNode extends MinicIntNode {

    @NodeInfo(shortName = "!", description = "!double (0.0 -> 1; everything else -> 0)")
    public abstract static class MinicDoubleLogicalNotNode extends MinicDoubleUnaryNode {
        @Specialization
        public int not(double value) {
            return value == 0.0 ? 1 : 0;
        }
    }

}
