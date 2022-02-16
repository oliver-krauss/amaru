/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.language;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionNode;
import at.fh.hagenberg.aist.gce.minic.parser.MinicBaseType;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;

import java.util.*;

/**
 * Registry for functions provided in a given MinicContext.
 * Contains all loaded Builtins (per default ALL), and all functions in a parsed c class
 * Created by Oliver Krauss on 16.06.2016.
 */
public final class MinicFunctionRegistry {

    /**
     * Functions that were loaded, and can be looked up with this registry
     */
    private final Map<String, MinicFunctionNode> functions = new HashMap<>();


    public void register(String name, MinicRootNode rootNode, MinicBaseType type) {
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        // We will always allow to override existing functions so no need for lookup here
        MinicFunctionNode result = new MinicFunctionNode(name, callTarget, type);
        functions.put(name, result);
    }

    private MinicFunctionNode lookup(String name, RootCallTarget callTarget) {
        MinicFunctionNode result = functions.get(name);
        if (result == null) {
            result = new MinicFunctionNode(name, callTarget, MinicBaseType.VOID); // per default we assume that a function returns nothing
            functions.put(name, result);
        }
        return result;
    }

    public MinicFunctionNode lookup(String name) {
        return lookup(name, null);
    }

    /**
     * Returns the list of all functions
     */
    public Collection<MinicFunctionNode> getFunctions() {
        return functions.values();
    }
}
