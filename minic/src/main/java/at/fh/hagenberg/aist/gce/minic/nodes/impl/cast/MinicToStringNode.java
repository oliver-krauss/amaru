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
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicCharArray;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Casting operations to the string datatype
 * Created by Oliver Krauss on 18.07.2016.
 */
@NodeInfo(shortName = "(string)", description = "Container for casting to string")
public abstract class MinicToStringNode extends MinicCastNode {

    @NodeInfo(shortName = "char[].toString()", description = "Casts a char-array to string")
    @NodeChild(value = "fromNode", type = MinicExpressionNode.class)
    public abstract static class MinicCharArrayToStringNode extends MinicStringNode {
        @Specialization
        public String cast(Object fromNode) {
            MinicCharArray array = (MinicCharArray) fromNode;
            return array.toString();
        }
    }

}
