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

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * Casting operations to the float datatype
 * Created by Oliver Krauss on 18.07.2016.
 */
@NodeInfo(shortName = "(float)", description = "Container for casting to float")
public abstract class MinicToFloatNode extends MinicCastNode  {

    @NodeInfo(shortName = "(float) char", description = "Casts char to float")
    @NodeChild(value = "fromNode", type = MinicCharNode.class)
    public abstract static class MinicCharToFloatNode extends MinicFloatNode {
        @Specialization
        public float cast(char fromNode) {
            return (float) fromNode;
        }
    }

    @NodeInfo(shortName = "(float) int", description = "Casts int to float")
    @NodeChild(value = "fromNode", type = MinicIntNode.class)
    public abstract static class MinicIntToFloatNode extends MinicFloatNode {
        @Specialization
        public float cast(int fromNode) {
            return (float) fromNode;
        }
    }

    @NodeInfo(shortName = "(float) double", description = "Casts double to float")
    @NodeChild(value = "fromNode", type = MinicDoubleNode.class)
    public abstract static class MinicDoubleToFloatNode extends MinicFloatNode {
        @Specialization
        public float cast(double fromNode) {
            return (float) fromNode;
        }
    }

    @NodeInfo(shortName = "(float) Object", description = "Casts object to float")
    @NodeChild(value = "fromNode", type = MinicExpressionNode.class)
    public abstract static class MinicGenericToFloatNode extends MinicFloatNode {
        @Specialization
        public float cast(Object fromNode) {
            return (float) fromNode;
        }
    }
}
