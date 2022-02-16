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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Read(char) from input. Input is defined by context, and per default the System.in
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "read", description = "reads one character from the input (given by context)")
public abstract class ReadNode extends MinicBuiltinNode {

    ReadNode() {
        this.type = MinicBaseType.CHAR;
    }

    @Specialization
    public char executeChar(VirtualFrame frame) {
        return readChar(getContext().getInput());
    }

    @CompilerDirectives.TruffleBoundary
    private char readChar(BufferedReader in) {
        try {
            return (char) in.read();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from input stream.");
        }
    }
}
