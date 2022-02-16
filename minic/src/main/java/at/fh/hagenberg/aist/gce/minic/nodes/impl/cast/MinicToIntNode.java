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
 * Casting operations to the int datatype
 * Created by Oliver Krauss on 18.07.2016.
 */
@NodeInfo(shortName = "(int)", description = "Container for casting to int")
public abstract class MinicToIntNode extends MinicCastNode  {

    @NodeInfo(shortName = "(int) char", description = "Casts char to int")
    @NodeChild(value = "fromNode", type = MinicCharNode.class)
    public abstract static class MinicCharToIntNode extends MinicIntNode {
        @Specialization
        public int cast(char fromNode) {
            return (int) fromNode;
        }
    }

    @NodeInfo(shortName = "(int) float", description = "Casts float to int")
    @NodeChild(value = "fromNode", type = MinicFloatNode.class)
    public abstract static class MinicFloatToIntNode extends MinicIntNode {
        @Specialization
        public int cast(float fromNode) {
            return (int) fromNode;
        }
    }

    @NodeInfo(shortName = "(int) double", description = "Casts double to int")
    @NodeChild(value = "fromNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleToIntNode extends MinicIntNode {
        @Specialization
        public int cast(double fromNode) {
            return (int) fromNode;
        }
    }
}
