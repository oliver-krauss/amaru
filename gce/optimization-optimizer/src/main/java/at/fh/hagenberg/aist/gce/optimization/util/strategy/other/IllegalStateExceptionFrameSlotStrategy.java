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
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowNode;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy hardcoded to induce an illegal state exception.
 * ONLY FOR PATTERN VERIFICATION.
 */
public class IllegalStateExceptionFrameSlotStrategy extends FrameSlotStrategy {

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<? extends FrameSlot> chooser = new RandomChooser<>();


    public IllegalStateExceptionFrameSlotStrategy(FrameDescriptor descriptor) {
        super(descriptor);
    }

    public IllegalStateExceptionFrameSlotStrategy(FrameDescriptor descriptor, Frame frame) {
        super(descriptor, frame);
    }

    @Override
    public FrameSlotStrategy copy() {
        IllegalStateExceptionFrameSlotStrategy nonCheckingFrameSlotStrategy = new IllegalStateExceptionFrameSlotStrategy(descriptor, frame);
        nonCheckingFrameSlotStrategy.setChooser(this.chooser);
        return nonCheckingFrameSlotStrategy;
    }

    public void setChooser(ChooseOption<? extends FrameSlot> chooser) {
        this.chooser = chooser;
    }

    @Override
    public FrameSlot create(CreationInformation information) {

        // IllegalArgException -> Slot exists but NOT in the GLobal slots
        List<Object> unresolved = this.descriptor.getSlots().stream().filter(x -> information.getDataFlowGraph().getAvailableDataItems().get(frame).stream().noneMatch(slot -> ((FrameSlot) slot.getSlot()).getIdentifier().equals(x.getIdentifier()))).collect(Collectors.toList());
        current = chooser.choose((Collection) unresolved);
        System.out.println("Created an IllegalStateException");
        return current;
    }

    @Override
    public void setDescriptor(FrameDescriptor frameDescriptor) {
        // DO NOTHING, THIS IS THE ONLY WAY THE FD CAN CONFUSE THE INPUTS
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        if (information.getDataFlowGraph() != null && information.getDataFlowGraph().getAvailableDataItems().containsKey(frame)) {
            List<Object> unresolved = this.descriptor.getSlots().stream().filter(x -> information.getDataFlowGraph().getAvailableDataItems().get(frame).stream().noneMatch(slot -> ((FrameSlot) slot.getSlot()).getIdentifier().equals(x.getIdentifier()))).collect(Collectors.toList());
            if (unresolved.isEmpty()) {
                return null;
            }
        }
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
