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

import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleParameterInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.nodes.Node;
import science.aist.seshat.Logger;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowNode;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Random Frame Slot Strategy returns one random valid (already existing!) Frame Descriptor for read/write access
 * TODO: #158 Create a strategy that modifies the amount of valid frames
 */
public class DefaultFrameSlotStrategy extends FrameSlotStrategy {

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<? extends FrameSlot> chooser = new RandomChooser<>();

    public DefaultFrameSlotStrategy(FrameDescriptor descriptor) {
        super(descriptor);
    }

    public DefaultFrameSlotStrategy(FrameDescriptor descriptor, Frame frame) {
        super(descriptor, frame);
    }

    @Override
    public FrameSlotStrategy copy() {
        DefaultFrameSlotStrategy defaultFrameSlotStrategy = new DefaultFrameSlotStrategy(descriptor, frame);
        defaultFrameSlotStrategy.setChooser(this.chooser);
        return defaultFrameSlotStrategy;
    }

    public void setChooser(ChooseOption<? extends FrameSlot> chooser) {
        this.chooser = chooser;
    }

    @Override
    public FrameSlot create(CreationInformation information) {
        // check if we need to deal with a variable in the pattern
        Collection<Requirement> variableRequirements = information.getRequirements().getRequirements(Requirement.REQ_VALUE_RESTRICTED).stream().filter(x -> x.getProperty(Requirement.REQ_VALUE_TYPE, String.class).equals("com.oracle.truffle.api.frame.FrameSlot")).collect(Collectors.toList());
        List<FrameSlot> variableDefinitions = variableRequirements.stream().map(x -> {
            String variable = x.getProperty(Requirement.REQ_VALUE_VALUE, String.class);
            // laod the requirement that owns this value requirement
            Requirement property = x.getProperty(Requirement.REQ_REF, Requirement.class);
            Map<String, Pair<FrameSlot, TruffleClassInformation>> frameMap = property.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);

            if (frameMap == null) {
                frameMap = new HashMap<>();
                property.addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, frameMap);
                return null;
            } else {
                Pair<FrameSlot, TruffleClassInformation> possibleSlot = frameMap.getOrDefault(variable, null);
                return possibleSlot != null ? possibleSlot.getKey() : null;
            }
        }).collect(Collectors.toList());

        // find already initialized variable state and return if existing
        if (!variableDefinitions.isEmpty() && !variableDefinitions.stream().allMatch(Objects::isNull)) {
            List<FrameSlot> distinctVars = variableDefinitions.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
            FrameSlot selected;
            if (distinctVars.size() > 1) {
                logger.warn("Multiple Patterns created an impossible variable state. We have to abandon some");
                // return most used one
                selected = distinctVars.stream().max(Comparator.comparing(x -> Collections.frequency(variableDefinitions, x))).get();
            } else {
                selected = distinctVars.get(0);
            }

            variableRequirements.forEach(req -> {
                // add selection to all patterns active, in case they haven't been given a slot yet
                req.getProperty(Requirement.REQ_REF, Requirement.class).getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class).putIfAbsent(req.getProperty(Requirement.REQ_VALUE_VALUE, String.class), new Pair<>(selected, information.getInformation()));
            });

            // return the frame slot belonging to the pattern.
            return selected;
        }
        // we are happy and can continue with the code below
        if (information.getInformation().hasProperty(TruffleClassProperty.STATE_WRITE)) {
            Collection<Requirement> req;
            Collection<? extends FrameSlot> validReq = null;
            if ((req = information.getRequirements().getNecessaryRequirements(Requirement.REQ_DATA_WRITE)) != null && !req.isEmpty() && information.getDataFlowGraph().getRequiredDataItems().containsKey(frame)) {
                validReq = req.stream().map(x -> (FrameSlot) ((DataFlowNode) x.getProperties().get(Requirement.REQPR_SLOT)).getSlot())
                        // only valid Req are those that have the same slot AND data type
                        .filter(x -> information.getDataFlowGraph().getRequiredDataItems().get(frame).stream().anyMatch(di ->
                                di.getSlot().equals(x)
                                        && information.getInformation().getReadPairings().stream().anyMatch(rp -> rp.getClazz().equals(di.getNode().getClass()))))
                        .collect(Collectors.toList());
            }
            if (validReq != null && !validReq.isEmpty()) {
                // valid req already adheres to data type
                this.current = chooser.choose((Collection) validReq);
            } else {
                // restricting to only those slots with correct data type
                Map<Object, List<DataFlowNode>> adi = information.getDataFlowGraph().getAvailableDataItems();
                List<? extends FrameSlot> collect = !adi.containsKey(frame) ? descriptor.getSlots() :
                        descriptor.getSlots().stream().filter(x ->
                                // slot was never written to - we can write
                                adi.get(frame).stream().allMatch(di -> !di.getSlot().equals(x) ||
                                        // slot has same type - we can write // TODO #216 this is too restrictive. It just must satisfy the same data type being written
                                        di.getNode().getClass().equals(information.getInformation().getClazz()))
                        ).collect(Collectors.toList());
                this.current = chooser.choose((Collection) collect);
            }

            // fulfill if current write is something required
            if (req != null) {
                information.getRequirements().fullfill(information.getRequirements().getRequirements(Requirement.REQ_DATA_WRITE).stream().filter(x -> x.getProperties().get(Requirement.REQPR_SLOT).equals(current)).findFirst().orElse(null));
            }

            // when creating an allocation or write we add this to the valid slots
            if (!information.getDataFlowGraph().getAvailableDataItems().containsKey(frame)) {
                information.getDataFlowGraph().getAvailableDataItems().put(frame, new ArrayList<>());
            }
            List<Object> initializedSlots = information.getDataFlowGraph().getAvailableDataItems().get(frame).stream().map(DataFlowNode::getSlot).collect(Collectors.toList());
            if (!initializedSlots.contains(this.current)) {
                initializedSlots.add(this.current);
            }
        } else {
            // every time a READING item is created by a strategy it must only create available items with the correct data type
            List<Object> collect = information.getDataFlowGraph().getAvailableDataItems().get(frame).stream()
                    .filter(x -> information.getInformation().getWritePairings().stream().anyMatch(y -> y.getClazz().equals(x.getNode().getClass())))
                    .map(DataFlowNode::getSlot).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                this.current = chooser.choose((Collection)collect);
            } else {
                this.current = chooser.choose((Collection) information.getDataFlowGraph().getAvailableDataItems().get(frame).stream().map(DataFlowNode::getSlot).collect(Collectors.toList()));
            }

        }

        // update the patterns if necessary
        variableRequirements.forEach(req -> {
            // add selection to all patterns active, in case they haven't been given a slot yet
            Requirement parentReq = req.getProperty(Requirement.REQ_REF, Requirement.class);
            Map<String, Pair<FrameSlot, TruffleClassInformation>> frameMap = parentReq.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);

            // set new value
            if (frameMap == null) {
                frameMap = new HashMap<>();
            }
            frameMap.put(req.getProperty(Requirement.REQ_VALUE_VALUE, String.class), new Pair<>(current, information.getInformation()));

            // up-propagate as new requirement, as the regular reqs will get deleted
            Requirement requirement = Requirement.loadMatch(parentReq, information.getRequirements().getRequirements(parentReq.getName()));
            Requirement varReq = new Requirement(Requirement.REQ_PATTERN_VAR_PLACEHOLDER).addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, frameMap).addProperty(Requirement.REQ_REF, requirement);
            information.getRequirements().addRequirement(varReq);
        });

        return current;
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return descriptor.getSlots().size() > 0 && // check that we have a slot to create
                information.getInformation() != null && // we only will create frame slots for specific classes
                validateDataFlow(information) ? // check data flow validity
                satisfy(information.getInformation(), information.getRequirements())
                : null;
        // TODO #216 not sure if this works for arrays that must be allocated beforehand
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation information, Map<Node, LoadedRequirementInformation> requirementMap) {
        Requirement tciPlaceholder = requirementMap.get(ast).getRequirements().stream().filter(x -> x.getName().equals("TCI_PLACEHOLDER")).findFirst().get();
        TruffleClassInformation tci = tciPlaceholder.getProperty("TCI", TruffleClassInformation.class);
        TruffleParameterInformation pInfo = tciPlaceholder.getProperty("PINFO", TruffleParameterInformation.class);

        information.getRequirements().keySet().removeIf(req -> {
            if (!req.getName().equals(Requirement.REQ_VALUE_RESTRICTED) || !req.getProperty(Requirement.REQ_VALUE_TYPE, String.class).equals("com.oracle.truffle.api.frame.FrameSlot")) {
                return false;
            }
            // add selection to all patterns active, in case they haven't been given a slot yet
            Requirement parentReq = req.getProperty(Requirement.REQ_REF, Requirement.class);
            Map<String, Pair<FrameSlot, TruffleClassInformation>> frameMap = parentReq.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);

            String varPlaceholder = req.getProperty(Requirement.REQ_VALUE_VALUE, String.class);
            FrameSlot loaded = (FrameSlot) JavaAssistUtil.safeFieldAccess(pInfo.getName(), ast);
            // set new value
            if (frameMap == null) {
                frameMap = new HashMap<>();
            } else if (frameMap.containsKey(varPlaceholder)) {
                Pair<FrameSlot, TruffleClassInformation> existingInfo = frameMap.get(varPlaceholder);
                if (!existingInfo.getKey().getIdentifier().equals(loaded.getIdentifier())) {
                    return false;
                }
                return true;
            }

            frameMap.put(varPlaceholder, new Pair<>(loaded, tci));

            // up-propagate as new requirement, as the regular reqs will get deleted
            Requirement requirement = Requirement.loadMatch(parentReq, information.getRequirements(parentReq.getName()));
            Requirement varReq = new Requirement(Requirement.REQ_PATTERN_VAR_PLACEHOLDER).addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, frameMap).addProperty(Requirement.REQ_REF, requirement);
            requirementMap.get(ast).getRequirements().add(varReq);
            return true;
        });

        return requirementMap;
    }

    private boolean validateDataFlow(CreationInformation information) {
        if (information.getInformation().hasProperty(TruffleClassProperty.STATE_READ)) {
            // when reading check that the writes before match
            if (information.getDataFlowGraph().getAvailableDataItems().containsKey(frame)) {
                return information.getDataFlowGraph().getAvailableDataItems().get(frame).stream().anyMatch(x -> information.getInformation().getWritePairings().stream().anyMatch(y -> y.getClazz().equals(x.getNode().getClass())));
            }
            if (information.getAst() == null) {
                return true;
            }
        } else {
            // NOTE: later reads are handled in satisfy
            // check if a slot is still free OR we can write with the same tyle
            Map<Object, List<DataFlowNode>> adi = information.getDataFlowGraph().getAvailableDataItems();
            // if never written to frame we are good
            return !adi.containsKey(frame) || descriptor.getSlots().stream().anyMatch(x ->
                    // slot was never written to - we can write
                    adi.get(frame).stream().allMatch(di -> !di.getSlot().equals(x) ||
                            // slot has same type - we can write // TODO #216 this is too restrictive. It just must satisfy the same data type being written
                            di.getNode().getClass().equals(information.getInformation().getClazz()))
            );
        }
        return false;
    }

    private RequirementInformation satisfy(TruffleClassInformation information, RequirementInformation requirements) {
        if (information.hasProperty(TruffleClassProperty.STATE_WRITE)) {
            requirements.getRequirements(Requirement.REQ_DATA_WRITE).forEach(x -> {
                DataFlowNode slot = (DataFlowNode) x.properties.getOrDefault(Requirement.REQPR_SLOT, null);
                if (information.getReadPairings().parallelStream().anyMatch(rp -> rp.getClazz().equals(slot.getNode().getClass())) && descriptor.getSlots().contains(slot.getSlot())) {
                    requirements.addDegreeOfFreedom(x);
                }
            });
        }

        // check if we can validate a label
        requirements.getRequirements(Requirement.REQ_VALUE_RESTRICTED).forEach(x -> {
            Requirement ref = x.getProperty(Requirement.REQ_REF, Requirement.class);
            // consider only value reqs of labels
            if (ref != null && x.getProperty(Requirement.REQ_VALUE_TYPE, String.class).equals("com.oracle.truffle.api.frame.FrameSlot")) {
                String variableId = x.getProperty(Requirement.REQ_VALUE_VALUE, String.class);
                Map<String, Pair<FrameSlot, TruffleClassInformation>> frameMap = ref.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
                if (frameMap == null) {
                    // variable not initialized, we don't need to continue checking here.
                    requirements.addDegreeOfFreedom(x);
                    requirements.addDegreeOfFreedom(Requirement.loadMatch(ref, requirements));
                    return;
                }
                TruffleClassInformation classInfo = frameMap.get(variableId).getValue();
                if (!classInfo.getReadPairings().contains(information) && !classInfo.getWritePairings().contains(information)) {
                    return;
                }
                FrameSlot frameSlot = frameMap.get(variableId).getKey();

                NodeWrapper labelOwner = ref.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                if (labelOwner.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                    labelOwner = Requirement.findStarchild(ref, labelOwner);
                }

                // antipattern if NEG -> Satisfy current / if NOT NEG -> Satisfy if any other slot is inside.
                if ((ref.getName().equals(Requirement.REQ_ANTIPATTERN) && labelOwner.getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                        ref.getName().equals(Requirement.REQ_PATTERN) && !labelOwner.getType().startsWith(Wildcard.WILDCARD_NOT)) {
                    if (descriptor.getSlots().contains(frameSlot)) {
                        requirements.addDegreeOfFreedom(x);
                        requirements.addDegreeOfFreedom(Requirement.loadMatch(ref, requirements));
                    }
                } else {
                    if (descriptor.getSlots().size() > 1 || !descriptor.getSlots().contains(frameSlot)) {
                        requirements.addDegreeOfFreedom(x);
                        requirements.addDegreeOfFreedom(Requirement.loadMatch(ref, requirements));
                    }
                }
            }
        });

        return requirements;
    }
}
