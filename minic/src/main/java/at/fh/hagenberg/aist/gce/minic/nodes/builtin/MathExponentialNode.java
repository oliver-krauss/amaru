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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.io.PrintWriter;

/**
 * Prints to the output given by the context. Usually System.out
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "exp", description = "returns e^val (e = euler constant)")
public abstract class MathExponentialNode extends MinicArgsBuiltinNode {

    public MathExponentialNode() {
        this.type = MinicBaseType.FLOAT;
    }

    @Specialization
    public float calculateFloat(float val) {
        return (float) Math.exp(val);
    }

    @Specialization
    public double calculateDouble(double val) {
        return Math.exp(val);
    }

}
