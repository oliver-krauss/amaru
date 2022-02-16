/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.builtin;

import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Prints to the output given by the context. Usually System.out
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "sqrt", description = "returns square root")
public abstract class MathSqrtNode extends MinicArgsBuiltinNode {

    public MathSqrtNode() {
        this.type = MinicBaseType.FLOAT;
    }

    @Specialization
    public float calculateFloat(float val) {
        return (float) Math.sqrt(val);
    }

    @Specialization
    public double calculateDouble(double val) {
        return Math.sqrt(val);
    }

}
