/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.base;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * The root for a truffle ast that allows executing a given calltarget in MiniC
 * Created by Oliver Krauss on 16.06.2016.
 */
@NodeInfo(shortName = "eval-root", description = "Evaluation root node, allowing a call to a given function in MiniC")
public final class MinicEvalRootNode extends RootNode {

    /**
     * If the function is registered
     */
    @CompilerDirectives.CompilationFinal
    private boolean registered;

    /**
     * Language context the call will be made in
     */
    private final TruffleLanguage.ContextReference<MinicContext> reference;

    @Child
    private DirectCallNode mainCallNode;

    public MinicEvalRootNode(MinicLanguage language, RootCallTarget rootFunction) {
        super(null); // internal frame
        this.mainCallNode = rootFunction != null ? DirectCallNode.create(rootFunction) : null;
        this.reference = language.getContextReference();
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    public String getName() {
        return "root eval";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        if (mainCallNode == null) {
            /* The source code did not have a "main" function, so nothing to execute. */
            return MinicNull.SINGLETON;
        } else {
            /* Conversion of arguments to types understood by SL. */
            Object[] arguments = frame.getArguments();
            CompilerAsserts.partialEvaluationConstant(arguments.length);
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = MinicContext.fromForeignValue(arguments[i]);
            }
            return mainCallNode.call(arguments);
        }
    }
}
