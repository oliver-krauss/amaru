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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Dispatch node handling the call to a minic function. Used by {@link MinicInvokeNode}
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "dispatch", description = "Dispatches a function call from an invoke to the appropriate function")
public abstract class MinicDispatchNode extends Node {

    /**
     * Function inline cach maximum
     */
    protected static final int INLINE_CACHE_SIZE = 2;

    public abstract Object executeDispatch(VirtualFrame frame, MinicFunctionNode function, Object[] arguments);


    @Specialization(guards = "function.getCallTarget() == null")
    protected Object doUndefinedFunction(MinicFunctionNode function, @SuppressWarnings("unused") Object[] arguments) {
        throw new RuntimeException("Function does not exist: " + function.getName());
    }

    /**
     * Inline cached specialization of the dispatch.
     *
     * @param function       the dynamically provided function
     * @param cachedFunction the cached function of the specialization instance
     * @param callNode       the {@link DirectCallNode} specifically created for the {@link CallTarget} in
     *                       cachedFunction.
     */
    @Specialization(limit = "INLINE_CACHE_SIZE", guards = "function == cachedFunction")
    protected static Object doDirect(VirtualFrame frame, MinicFunctionNode function, Object[] arguments,   //
                                     @Cached("function") MinicFunctionNode cachedFunction,   //
                                     @Cached("create(cachedFunction.getCallTarget())") DirectCallNode callNode) {
        /* Inline cache hit, we are safe to execute the cached call target. */
        return callNode.call(arguments);
    }

    /**
     * Slow-path code for a call, used when the polymorphic inline cache exceeded its maximum size
     * specified in <code>INLINE_CACHE_SIZE</code>. Such calls are not optimized any further, e.g.,
     * no method inlining is performed.
     */
    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(MinicFunctionNode function, Object[] arguments,   //
                                       @Cached("create()") IndirectCallNode callNode) {
        /*
         * SL has a quite simple call lookup: just ask the function for the current call target, and
         * call it.
         */
        return callNode.call(function.getCallTarget(), arguments);
    }

}
