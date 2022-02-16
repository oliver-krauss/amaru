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
 * Casting operations to the char datatype
 * Created by Oliver Krauss on 18.07.2016.
 */
@NodeInfo(shortName = "(char)", description = "Container for casting to char")
public abstract class MinicToCharNode extends MinicCastNode  {

    @NodeInfo(shortName = "(char) int", description = "Casts int to char")
    @NodeChild(value = "fromNode", type = MinicIntNode.class)
    public abstract static class MinicIntToCharNode extends MinicCharNode {
        @Specialization
        public char cast(int fromNode) {
            return (char) fromNode;
        }
    }

    @NodeInfo(shortName = "(char) float", description = "Casts float to char")
    @NodeChild(value = "fromNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatToCharNode extends MinicCharNode {
        @Specialization
        public char cast(float fromNode) {
            return (char) fromNode;
        }
    }

    @NodeInfo(shortName = "(char) double", description = "Casts double to char")
    @NodeChild(value = "fromNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleToCharNode extends MinicCharNode {
        @Specialization
        public char cast(double fromNode) {
            return (char) fromNode;
        }
    }
}
