/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.cast;

import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Casting operations to the double datatype
 * Created by Oliver Krauss on 18.07.2016.
 */
@NodeInfo(shortName = "(double)", description = "Container for casting to double")
public abstract class MinicToDoubleNode extends MinicCastNode  {

    @NodeInfo(shortName = "(double) char", description = "Casts char to double")
    @NodeChild(value = "fromNode", type = MinicCharNode.class)
    public abstract static class MinicCharToDoubleNode extends MinicDoubleNode {
        @Specialization
        public double cast(char fromNode) {
            return (double) fromNode;
        }
    }

    @NodeInfo(shortName = "(double) int", description = "Casts int to double")
    @NodeChild(value = "fromNode", type = MinicIntNode.class)
    public abstract static class MinicIntToDoubleNode extends MinicDoubleNode {
        @Specialization
        public double cast(int fromNode) {
            return (double) fromNode;
        }
    }

    @NodeInfo(shortName = "(double) float", description = "Casts float to double")
    @NodeChild(value = "fromNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatToDoubleNode extends MinicDoubleNode {
        @Specialization
        public double cast(float fromNode) {
            return (double) fromNode;
        }
    }
}
