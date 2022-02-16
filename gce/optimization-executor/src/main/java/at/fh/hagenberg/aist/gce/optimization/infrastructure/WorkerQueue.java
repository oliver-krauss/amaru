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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Queue that implements the Paranoid Pirate Pattern, based on http://zguide.zeromq.org/java:ppqueue.
 *
 * @author Daniel Dorfmeister on 2019-06-19
 */
public class WorkerQueue implements Iterable<HeartbeatEntity> {

    private java.util.Queue<HeartbeatEntity> availableWorkers = new java.util.LinkedList<>();

    /**
     * Adds a worker to the end of the queue.
     * @param worker: The worker to add.
     */
    public void push(HeartbeatEntity worker) {
        Iterator<HeartbeatEntity> it = availableWorkers.iterator();
        while (it.hasNext()) {
            HeartbeatEntity w = it.next();
            if (worker.getIdentity().equals(w.getIdentity())) {
                it.remove();
                break;
            }
        }
        availableWorkers.add(worker);
    }

    /**
     * Returns the first available worker.
     * @return A ZFrame containing the address of the first available worker.
     */
    public ZFrame pop() {
        HeartbeatEntity worker = availableWorkers.poll();
        if (worker == null)
            throw new IllegalStateException("no worker registered");
        ZFrame frame = worker.getAddress();
        return frame;
    }

    /**
     * Removes expired workers.
     * @return List of purged workers.
     */
    public List<HeartbeatEntity> purge() {
        Iterator<HeartbeatEntity> it = availableWorkers.iterator();
        List<HeartbeatEntity> purgedWorkers = new ArrayList<>();
        while (it.hasNext()) {
            HeartbeatEntity worker = it.next();
            //  We hold availableWorkers from oldest to most recent, so we stop at the first alive worker.
            if (System.currentTimeMillis() < worker.getExpiry()) {
                break;
            }
            purgedWorkers.add(worker);
            it.remove();
        }
        return purgedWorkers;
    }

    public int size() {
        return availableWorkers.size();
    }

    public void clear() {
        availableWorkers.clear();
    }

    @Override
    public Iterator iterator() {
        return availableWorkers.iterator();
    }
}

