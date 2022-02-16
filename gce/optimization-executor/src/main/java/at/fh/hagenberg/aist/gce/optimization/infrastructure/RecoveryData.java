/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.infrastructure;

import org.zeromq.ZMsg;

/**
 * Recovery data for when a message worker crashed.
 */
public class RecoveryData {

    /**
     * The System time in milliseconds that we expected the response to come in at the latest
     */
    long expectedResponse;

    /**
     * The message that was sent to the worker that (probably) crashed
     */
    ZMsg message;

    /**
     * Check if this message has been investigated
     */
    boolean investigated = false;

    String workerId = null;

    public RecoveryData(long expectedResponse, ZMsg message, String workerId) {
        this.expectedResponse = expectedResponse;
        this.message = message;
        this.workerId = workerId;
    }

    public long getExpectedResponse() {
        return expectedResponse;
    }

    public ZMsg getMessage() {
        return message;
    }

    public boolean isInvestigated() {
        return investigated;
    }

    public void setInvestigated(boolean investigated) {
        this.investigated = investigated;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
