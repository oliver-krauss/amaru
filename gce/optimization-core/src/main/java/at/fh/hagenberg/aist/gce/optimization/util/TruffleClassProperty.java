/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

/**
 * @author Oliver Krauss on 29.11.2019
 */

public enum TruffleClassProperty {

    /**
     * Acccesses the local scope of a function (!MaterializedFrame)
     */
    LOCAL_STATE,

    /**
     * Accesses the global state of a function (MaterializedFrame)
     */
    GLOBAL_STATE,

    /**
     * Conducts a read on a Frame
     */
    STATE_READ,

    /**
     * Conducts a read on an Argument
     */
    STATE_READ_ARGUMENT,

    /**
     * Conducts a write on a Frame
     */
    STATE_WRITE,

    /**
     * A truffle boundary indicates "something" outside of the program scope.
     * This could be a console read, log print, etc.
     * We can't really control anything here, meaning that any boundary node must be preserved with it's parameters intact.
     */
    TRUFFLE_BOUNDARY,

    /**
     * Nodes that influence the control flow in any way (CONTROL_FLOW_EXCEPTION, BRANCH, LOOP, FUNCTION_CALL are sub-enums of this)
     */
    CONTROL_FLOW,

    /**
     * Node extends from {@link com.oracle.truffle.api.nodes.ControlFlowException} meaning that it adds an edge in the control flow graph
     */
    CONTROL_FLOW_EXCEPTION,

    /**
     * Nodes containing a {@link com.oracle.truffle.api.profiles.ConditionProfile}->Binary are branching statements
     */
    BRANCH,

    /**
     * Nodes implementing {@link com.oracle.truffle.api.nodes.RepeatingNode} are a looping structure.
     */
    LOOP,

    /**
     * Node contains a {@link com.oracle.truffle.api.CallTarget} meaning that it will invoke another function
     */
    FUNCTION_CALL,
}
