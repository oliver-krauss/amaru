/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.other;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.Requirement;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleVerifyingStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import science.aist.seshat.Logger;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowNode;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base Class for all frame slot strategies to enable setting of the descriptor and frame
 */
public abstract class FrameSlotStrategy implements TruffleVerifyingStrategy<FrameSlot> {

    /**
     * Logger for data flow
     */
    protected static final Logger logger = Logger.getInstance();

    /**
     * Frame descriptor that represents our frame
     */
    protected FrameDescriptor descriptor;

    /**
     * Frame that owns the frame descriptor -> used to check if we are dealing with a local or global frame
     * usually is empty if local frame
     */
    protected Frame frame;

    /**
     * As default assumption is local frame (stack)
     */
    protected boolean local = true;

    /**
     * Current frame slot of frame descriptor
     */
    protected FrameSlot current;

    public FrameSlotStrategy(FrameDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public FrameSlotStrategy(FrameDescriptor descriptor, Frame frame) {
        this.descriptor = descriptor;
        setFrame(frame);
    }

    public FrameDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(FrameDescriptor frameDescriptor) {
        this.descriptor = frameDescriptor;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
        if (frame != null && MaterializedFrame.class.isAssignableFrom(frame.getClass())) {
            local = false;
        } else {
            local = true;
        }
    }

    public abstract FrameSlotStrategy copy();
}
