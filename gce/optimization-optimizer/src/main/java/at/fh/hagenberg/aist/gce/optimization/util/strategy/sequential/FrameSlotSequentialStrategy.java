/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.sequential;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

import java.util.ArrayList;

/**
 * Special Strategy for the Truffle Node Sequence Creator.
 * Should not be used in any other context.
 */
public class FrameSlotSequentialStrategy extends TruffleSequentialStrategy<FrameSlot> {

    private final FrameDescriptor descriptor;

    int i = -1;

    private FrameSlot current = null;

    public FrameSlotSequentialStrategy(FrameDescriptor descriptor) {
        super(new ArrayList<>()); // We Override EVERYTHING
        this.descriptor = descriptor;
    }

    @Override
    public boolean hasNext() {
        return descriptor.getSize() < 1 ? false : i >= descriptor.getSize();
    }

    @Override
    public void resetSequence() {
        i = -1;
    }

    @Override
    public FrameSlot current() {
        if (current == null) {
            return next();
        }
        return current;
    }

    @Override
    public FrameSlot next() {
        // check if descriptor has size
        if (descriptor.getSize() < 1) {
            return null;
        }

        // increment and cycle back
        i++;
        if (i > descriptor.getSize()) {
            i = 0;
        }
        current = descriptor.getSlots().get(i);
        return current;
    }
}
