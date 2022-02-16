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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TrufflePublicAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.impl.DefaultCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * Acessor for {@link com.oracle.truffle.js.lang.JavaScriptLanguage}
 *
 * @author Oliver Krauss on 29.10.2019
 */
@TruffleLanguage.Registration(id = JavaScriptAccessor.ID, name = "JavaScript Accessible", version = "0.0", mimeType = {JavaScriptLanguage.APPLICATION_MIME_TYPE})
public class JavaScriptAccessor extends JavaScriptLanguage implements Accessor {

    public static final String ID = "access-js";

    public JavaScriptAccessor() {
        Accessor.accessors.put(JavaScriptAccessor.ID, this);
    }

    private TrufflePublicAccess<JavaScriptLanguage, JSRealm> access = new TrufflePublicAccess<>(JavaScriptAccessor.class);

    @Override
    public CallTarget getCallTarget(String functionName) {
        try {
            // TODO #166 check if still required
            // Load code into the Context (and execute it once)
            //org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder(JavaScriptLanguage.ID, getSourceFile()).build();
            //Context.getCurrent().eval(src);

            // find the call target, and give it to the optimizer (only works after running the js once).
            return JSFunction.getFunctionData((DynamicObject) access.getCurrentContext().getGlobalObject().get(functionName)).getCallTarget();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RootNode getRootNode(String functionName) {
        return ((DefaultCallTarget) JSFunction.getFunctionData((DynamicObject) access.getCurrentContext().getGlobalObject().get(functionName)).getCallTarget()).getRootNode();
    }

    @Override
    public Node getNodeToOptimize(RootNode root) {
        root.adoptChildren();
        return root.getChildren().iterator().next();
    }

    @Override
    public MaterializedFrame getGlobalScope() {
        // TODO #166 check if this actually does return the correct scope
        return (MaterializedFrame) access.getCurrentContext().getGlobalScope();
    }
}
