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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * The root for any Truffle Mini C AST
 * Created by Oliver Krauss on 16.06.2016.
 */
@NodeInfo(shortName = "root", description = "Root node, representing a callable Truffle Function")
public final class MinicRootNode extends RootNode {

    /**
     * The function body that is executed, and specialized during execution.
     */
    @Child
    private MinicExpressionNode bodyNode;

    /**
     * The name of the function, for printing purposes only.
     */
    private final String name;

    /**
     * if the function is allowed to be cloned
     */
    @CompilerDirectives.CompilationFinal
    private boolean isCloningAllowed;

    /**
     * Language Class containing the "owner" class. Was a failed attempt to enter the Graal-ClassLoader
     * Needed to inject Access-C
     */
    private Class languageClass;

    @SuppressWarnings("unused")
    public MinicRootNode(MinicLanguage language, MinicContext ignore, FrameDescriptor frameDescriptor, MinicExpressionNode bodyNode, String name) {
        super(language, frameDescriptor);
        this.languageClass = language.getClass();
        this.bodyNode = bodyNode;
        this.name = name;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert lookupContextReference(languageClass).get() != null;
        if (bodyNode != null) {
            return bodyNode.executeGeneric(frame);
        }
        return MinicNull.SINGLETON;
    }

    public void setCloningAllowed(boolean isCloningAllowed) {
        this.isCloningAllowed = isCloningAllowed;
    }

    @Override
    public boolean isCloningAllowed() {
        return isCloningAllowed;
    }

    @Override
    public String getName() {
        return name;
    }
}
