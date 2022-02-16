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
import java.util.Random;

/**
 * Read(char) from input. Input is defined by context, and per default the System.in
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "rand", description = "returns random number")
public abstract class RandomNode extends MinicBuiltinNode {

    RandomNode() {
        this.type = MinicBaseType.INT;
    }

    // Constant from C as Java creates exclusive upper bound RAND_MAX is in C 32767 (guaranteed to be this or greater!)
    private static final int RAND_MAX = 32768;

    private final Random rnd = new Random();

    @Specialization
    public int executeInt(VirtualFrame frame) {
        return rnd.nextInt(RAND_MAX);
    }

}
