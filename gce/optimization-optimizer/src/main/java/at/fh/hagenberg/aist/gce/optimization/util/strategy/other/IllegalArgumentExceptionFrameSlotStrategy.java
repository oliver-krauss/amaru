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

import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.LoadedRequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.Requirement;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import java.util.Collection;
import java.util.Map;

/**
 * Strategy hardcoded to induce an illegal argument exception.
 * ONLY FOR PATTERN VERIFICATION.
 */
public class IllegalArgumentExceptionFrameSlotStrategy extends FrameSlotStrategy {

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<? extends FrameSlot> chooser = new RandomChooser<>();


    public IllegalArgumentExceptionFrameSlotStrategy(FrameDescriptor descriptor) {
        super(descriptor);
    }

    public IllegalArgumentExceptionFrameSlotStrategy(FrameDescriptor descriptor, Frame frame) {
        super(descriptor, frame);
    }

    @Override
    public FrameSlotStrategy copy() {
        IllegalArgumentExceptionFrameSlotStrategy nonCheckingFrameSlotStrategy = new IllegalArgumentExceptionFrameSlotStrategy(descriptor, frame);
        nonCheckingFrameSlotStrategy.setChooser(this.chooser);
        return nonCheckingFrameSlotStrategy;
    }

    public void setChooser(ChooseOption<? extends FrameSlot> chooser) {
        this.chooser = chooser;
    }

    @Override
    public FrameSlot create(CreationInformation information) {

        // IllegalArgException -> Slot exists but NOT in the GLobal slots
        current = chooser.choose((Collection) descriptor.getSlots());
        System.out.println("Created an IllegalArgumentExceptionNode");
        return current;
    }

    @Override
    public void setDescriptor(FrameDescriptor frameDescriptor) {
        // DO NOTHING, THIS IS THE ONLY WAY THE FD CAN CONFUSE THE INPUTS
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return satisfy(information.getRequirements());
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // we don't check - we don't do requirements
        return requirementMap;
    }

    // we just satisfy everything
    private RequirementInformation satisfy(RequirementInformation requirements) {
        requirements.getRequirements(Requirement.REQ_DATA_WRITE).forEach(requirements::addDegreeOfFreedom);
        return requirements;
    }
}
