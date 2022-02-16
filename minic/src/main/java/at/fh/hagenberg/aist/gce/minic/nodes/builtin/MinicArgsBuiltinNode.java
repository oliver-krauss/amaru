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

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Base class for all built in functions
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "builtin-arg", description = "Abstract base class for builtins that take arguments. Builtins contain java functionality made available for MiniC")
@NodeChild(value = "arguments", type = MinicExpressionNode[].class)
public abstract class MinicArgsBuiltinNode extends MinicBuiltinNode {

    /**
     * Return type of the builtin. If VOID, nothing is returned
     */
    protected MinicBaseType type = MinicBaseType.VOID; // per default we assume the type is void

    public MinicBaseType getType() {
        return type;
    }

    public abstract MinicContext getContext();
}
