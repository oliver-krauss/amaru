/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.complex;

import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Class handling the string unary operations:
 * !
 * Created by Oliver Krauss on 19.07.2016.
 */
@NodeInfo(shortName = "unary-string", description = "Abstract base class for unary string operations")
@NodeChildren({
    @NodeChild(value = "value", type = MinicStringNode.class),
})
public abstract class MinicStringUnaryNode extends MinicIntNode {

    @NodeInfo(shortName = "!", description = "!string (0 and false -> 1; everything else -> 0)")
    public abstract static class MinicStringLogicalNotNode extends MinicStringUnaryNode {
        @Specialization
        public int not(String value) {
            return (value != null && !value.equals("0") && !value.equals("false")) ? 1 : 0;
        }
    }

}
