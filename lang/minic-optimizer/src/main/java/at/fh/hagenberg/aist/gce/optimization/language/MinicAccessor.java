/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.language;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * @author Oliver Krauss on 29.10.2019
 */
@TruffleLanguage.Registration(id = MinicAccessor.ID, name = "Mini ANSI C11 Accessible", version = "0.0", mimeType = {MinicLanguage.MINIC_IR_MIME_TYPE})
public final class MinicAccessor extends MinicLanguage implements Accessor {

    public static final String OPTIC_IR_MIME_TYPE = "application/x-accesc";
    public static final String ID = "access-c";

    private CallTarget lastParsed = null;

    public MinicAccessor() {
        Accessor.accessors.put(MinicAccessor.ID, this);
    }

    public MinicContext getContext() {
        return this.getContextReference().get();
    }

    @Override
    public CallTarget getCallTarget(String functionName) {
        return getContext().getFunctionRegistry().lookup(functionName).getCallTarget();
    }

    @Override
    public RootNode getRootNode(String functionName) {
        return getContext().getFunctionRegistry().lookup(functionName).getCallTarget().getRootNode();
    }

    @Override
    public Node getNodeToOptimize(RootNode root) {
        root.adoptChildren();
        Node result = root.getChildren().iterator().next();
        if (result instanceof MinicFunctionBodyNode) {
            result = result.getChildren().iterator().next();
        }
        return result;
    }

    @Override
    public MaterializedFrame getGlobalScope() {
        return getContext().getGlobalStorage();
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        return lastParsed = super.parse(request);
    }
}
