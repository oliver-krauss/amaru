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
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface for language accessors that handle getting the data we need out of the truffle languages
 * Any acessor MUST have the Language.ID of the original language with the prefix access-
 * Ex. c -> access-c
 * @author Oliver Krauss on 29.10.2019
 */
public interface Accessor {

    /**
     * Available accessors. The CONSTRUCTOR of a language (called by the Truffle Context)
     * must call Accessor.accessors.put(id, this);
     */
    Map<String, Accessor> accessors = new HashMap<>();

    static Accessor getAccessor(String id) {
        return accessors.get(id);
    }

    /**
     * Call target of function name.
     * If you want the main function simply use "main"
     * @return call target
     */
    CallTarget getCallTarget(String functionName);

    /**
     * Root node of requested function
     * @param functionName name of function
     * @return root node of function
     */
    RootNode getRootNode(String functionName);

    /**
     * Selects the node to optimize (per default the first child)
     * @param root to select from
     * @return value
     */
    Node getNodeToOptimize(RootNode root);

    /**
     * Global Frame (heap) of the language. Optional depending on language implementation.
     * @return globalScope if it exists
     */
    MaterializedFrame getGlobalScope();
}
