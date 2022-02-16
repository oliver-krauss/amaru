/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.function;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Created by Oliver Krauss on 06.07.2016.
 */
@NodeInfo(shortName = "function-literal", description = "Loads a function with the given name from the Function-Registry")
public final class MinicFunctionLiteralNode extends MinicExpressionNode {

    /**
     * Name of function that will be loaded
     */
    private final String name;

    /**
     * Cached function if already loaded from
     */
    @CompilerDirectives.CompilationFinal
    private MinicFunctionNode cachedFunction;

    /**
     * Context reference providing the function registry for loading
     */
    @CompilerDirectives.CompilationFinal
    private final TruffleLanguage.ContextReference<MinicContext> reference;

    public MinicFunctionLiteralNode(String name) {
        this.name = name;
        reference = MinicLanguage.INSTANCE.getContextReference();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (cachedFunction == null) {
            /* We are about to change a @CompilationFinal field. */
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* First execution of the node: lookup the function in the function registry. */
            cachedFunction = reference.get().getFunctionRegistry().lookup(name);
        }
        return cachedFunction;
    }

    public String getName() {
        return name;
    }
}
