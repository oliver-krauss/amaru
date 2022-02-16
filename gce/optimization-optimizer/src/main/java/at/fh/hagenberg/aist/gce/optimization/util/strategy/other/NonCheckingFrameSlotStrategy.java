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
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import com.oracle.truffle.api.nodes.Node;
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
 * Strategy that does not check any validity of slots. Used for Language Learning.
 */
public class NonCheckingFrameSlotStrategy extends FrameSlotStrategy {

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<? extends FrameSlot> chooser = new RandomChooser<>();


    public NonCheckingFrameSlotStrategy(FrameDescriptor descriptor) {
        super(descriptor);
    }

    public NonCheckingFrameSlotStrategy(FrameDescriptor descriptor, Frame frame) {
        super(descriptor, frame);
    }

    @Override
    public FrameSlotStrategy copy() {
        NonCheckingFrameSlotStrategy nonCheckingFrameSlotStrategy = new NonCheckingFrameSlotStrategy(descriptor, frame);
        nonCheckingFrameSlotStrategy.setChooser(this.chooser);
        return nonCheckingFrameSlotStrategy;
    }

    public void setChooser(ChooseOption<? extends FrameSlot> chooser) {
        this.chooser = chooser;
    }

    @Override
    public FrameSlot create(CreationInformation information) {
        current = chooser.choose((Collection) descriptor.getSlots());
        return current;
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
