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
 * Determines the length of a string
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "length", description = "Returns the length of a given string")
public abstract class LengthNode extends MinicArgsBuiltinNode {

    LengthNode() {
        this.type = MinicBaseType.INT;
    }

    @Specialization
    public int length(String s) {
        return s.length();
    }
}
