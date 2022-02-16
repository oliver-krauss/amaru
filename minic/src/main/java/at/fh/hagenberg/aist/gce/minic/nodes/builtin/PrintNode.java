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
@NodeInfo(shortName = "print", description = "prints one value to the output (given by context)")
public abstract class PrintNode extends MinicArgsBuiltinNode {

    public PrintNode() {
        this.type = MinicBaseType.VOID;
    }

    @Specialization
    public char printChar(char ch) {
        doPrint(getContext() != null ? getContext().getOutput() : null, ch);
        return ch;
    }

    @Specialization
    public int printInt(int i) {
        doPrint(getContext() != null ? getContext().getOutput() : null, i);
        return i;
    }

    @Specialization
    public float printFloat(float f) {
        doPrint(getContext() != null ? getContext().getOutput() : null, f);
        return f;
    }

    @Specialization
    public double printDouble(double f) {
        doPrint(getContext() != null ? getContext().getOutput() : null, f);
        return f;
    }

    @Specialization
    public String printString(String s) {
        doPrint(getContext() != null ? getContext().getOutput() : null, s);
        return s;
    }

    @CompilerDirectives.TruffleBoundary
    private static void doPrint(PrintWriter out, Object o) {
        MinicLanguage.getCurrentContext().getOutput().println(o);
    }
}
