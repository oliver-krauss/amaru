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

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the float unary operations:
 * !
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "unary-float", description = "Abstract base class for unary float operations")
@NodeChildren({
    @NodeChild(value = "value", type = MinicFloatNode.class),
})
public abstract class MinicFloatUnaryNode extends MinicIntNode {

    @NodeInfo(shortName = "!", description = "!float (0.0 -> 1; everything else -> 0)")
    public abstract static class MinicFloatLogicalNotNode extends MinicFloatUnaryNode {
        @Specialization
        public int not(float value) {
            return value == 0.0 ? 1 : 0;
        }
    }

}
