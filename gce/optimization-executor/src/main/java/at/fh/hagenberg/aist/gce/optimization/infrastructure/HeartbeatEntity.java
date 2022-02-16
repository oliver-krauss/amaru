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

import org.zeromq.ZFrame;

import java.util.List;
import java.util.UUID;

/**
 * Represents a Paranoid Pirate worker, based on http://zguide.zeromq.org/java:ppqueue.
 *
 * @author Daniel Dorfmeister on 2019-06-19
 */
public class HeartbeatEntity {
    private ZFrame address;  //  Address of worker
    private String identity; //  Printable identity
    private long expiry;   //  Expires at this time

    private int heartbeatInterval;
    private int heartbeatLiveness;

    /**
     * Only available for workers. If they are associated with a command plane then this will be set.
     */
    private String commandPlane;

    protected HeartbeatEntity(ZFrame address, int heartbeatInterval, int heartbeatLiveness, String commandPlane) {
        this.address = address;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatLiveness = heartbeatLiveness;
        this.commandPlane = commandPlane;

        this.identity = UuidHelper.getUUIDFromBytes(address.getData()).toString();
        resetExpiry();
    }

    /**
     * Resets the expiry time of the worker.
     */
    public void resetExpiry() {
        expiry = System.currentTimeMillis() + heartbeatInterval * heartbeatLiveness;
    }

    public ZFrame getAddress() {
        // if frame is sent, its memory is freed, which also frees the workers address
        // thus, return a copy of the address
        return address.duplicate();
    }

    public String getIdentity() {
        return identity;
    }

    public String getCommandPlane() {
        return commandPlane;
    }

    public long getExpiry() {
        return expiry;
    }

    @Override
    public String toString() {
        return identity;
    }
}

