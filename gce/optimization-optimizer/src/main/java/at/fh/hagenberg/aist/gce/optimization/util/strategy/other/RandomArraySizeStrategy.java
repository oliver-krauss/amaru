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

import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import com.oracle.truffle.api.nodes.Node;
import zmq.socket.reqrep.Req;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Oliver Krauss on 07.11.2018
 */

public class RandomArraySizeStrategy extends DefaultObservableAndObserverStrategy implements TruffleHierarchicalStrategy<Object> {

    /**
     * Strategy for nonterminals. Note: If you want to use more than one strategy refer to the
     * {@link SelectorStrategy}
     */
    protected TruffleHierarchicalStrategy nonTerminalStrategy;

    private NodeWrapperWeightUtil weightUtil;

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    public RandomArraySizeStrategy(TruffleHierarchicalStrategy nonTerminalStrategy, NodeWrapperWeightUtil weightUtil) {
        this.weightUtil = weightUtil;
        this.nonTerminalStrategy = nonTerminalStrategy;
        if (nonTerminalStrategy instanceof DefaultObservableStrategy) {
            subscribeAndValidate((TruffleObservableStrategy) nonTerminalStrategy);
        }
    }

    @Override
    public Object create(CreationInformation information) {
        // Check if the class to be instantiated is an array
        if (information.getClazz().isArray()) {
            // if we have reached max depth, just return an empty array
            if (information.getCurrentDepth() >= information.getConfiguration().getMaxDepth()) {
                return Array.newInstance(information.getClazz().getComponentType(), 0);
            }

            // create array size
            Object[] resultArray;
            // check if a pattern forces the width of the array. If it does randomly select one of the enforced widths
            List<Map<String, Object>> collect = information.getRequirements().getRequirements().keySet().stream().filter(x -> x.getName().equals(Requirement.REQ_PATTERN) || x.getName().equals(Requirement.REQ_ANTIPATTERN))
                    .map(x -> x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getValues()).filter(x -> x.containsKey("PATTERN:" + Requirement.REQPR_MAX_WIDTH)).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                resultArray = (Object[]) Array.newInstance(information.getClazz().getComponentType(), (Integer) collect.get(0).get("PATTERN:" + Requirement.REQPR_MAX_WIDTH));
            } else {
                // minimal size is either 1 or how many spaces we need to satisfy all requirements
                int minSize = information.getRequirements().getRequirements().keySet().stream().map(this::requiredSizeToRight).filter(x -> x > 0).max(Integer::compareTo).orElse(1);
                int bound = information.getConfiguration().getMaxWidth() - minSize + 1;
                resultArray = (Object[]) Array.newInstance(information.getClazz().getComponentType(), (bound > 0 ? RandomUtil.random.nextInt(bound) : 0) + minSize);
            }
            double remainingWeight = information.getConfiguration().getMaxWeight() - information.getCurrentWeight();

            // TODO #63 THIS IS DEBUG CODE IF WE EVER CHANGE THE LOGIC ON HOW PATTERNS WORK
//            System.out.println("MINSIZE " + minSize + " SIZE " + resultArray.length);

            for (int i = 0; i < resultArray.length; i++) {
                // collect DOF
                CreationInformation arrayInfo = information.copy().setClazz(information.getClazz().getComponentType()).setInformation(null).setCurrentWeight(information.getConfiguration().getMaxWeight() - remainingWeight);
                RequirementInformation dof = nonTerminalStrategy.canCreateVerbose(arrayInfo);
                // RM the forward-pointer in this case
                if (dof == null) {
                    throw new RuntimeException("Creation moved into a dead branch for param creation in " + information.getInformation().getClazz().getName());
                }
                dof.getRequirements().keySet().removeIf(x -> x.containsProperty("FORWARDPOINTER"));
                dof = combine(information.getRequirements().copy(), dof);
                if (dof != null) {
                    // calculate remaining degrees of freedom in upcoming array indices and add the parent indices
                    int remainingDof = i + 1;
                    int finalI = i;
                    dof.getRequirements().entrySet().forEach(x -> {
                                if (x.getKey().getName().equals(Requirement.REQ_ANTIPATTERN) || x.getKey().getName().equals(Requirement.REQ_PATTERN)) {
                                    String matchType = x.getKey().getProperty(Requirement.REQ_PATTERN_MATCH_TYPE, String.class);
                                    if (matchType != null && matchType.equals(Requirement.REQ_PATTERN_MATCH_TYPE_EVERYWHERE)) {
                                        x.setValue(0); // alyways tell antipatterns that they still must be prevented at all cost
                                    } else {
                                        int right = requiredSizeToRight(x.getKey());
                                        x.setValue(Math.max(0, (resultArray.length - (finalI + right))));
                                        // TODO #63 THIS IS DEBUG CODE IF WE EVER CHANGE THE LOGIC ON HOW PATTERNS WORK
//                                        System.out.println("DOOF " + x.getValue() + " / " + right + " / " + (x.getKey().containsProperty("pattern-ltr-owner") ? x.getKey().getProperty("pattern-ltr-owner", Requirement.class).getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) : ""));
//                                        if (x.getKey().containsProperty("pattern-ltr-owner")) {
//                                            System.out.println("D  " + x.getKey().getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType());
//                                        }
                                    }
                                } else {
                                    x.setValue(
                                            // parent values
                                            information.getRequirements().getRequirements().get(x.getKey()) +
                                                    // following values
                                                    x.getValue() * (resultArray.length - remainingDof));
                                }
                            }
                    );
                    arrayInfo.setRequirements(dof);


                    // if weight not exceeded add one more
                    Object o = nonTerminalStrategy.create(arrayInfo);
                    updateVariableMap(information.getRequirements(), arrayInfo.getRequirements());
                    combineSatisfiedRequirements(information.getRequirements(), arrayInfo.getRequirements());
                    if (weightUtil != null && o instanceof Node) {
                        remainingWeight -= weightUtil.weight((Node) o);
                    }
                    resultArray[i] = o;
                } else {
                    System.out.println("That didn't work for some reason");
                    // check if we violated a dof constraint and if so redo the last entry
                    if (i > 0 && !arrayInfo.getRequirements().getRequirements().isEmpty()) {
                        // undo the weight measure and re-create with NO dof left to the side
                        remainingWeight += weightUtil.weight((Node) resultArray[i - 1]);
                        arrayInfo.setCurrentWeight(arrayInfo.getCurrentWeight() + remainingWeight);
                        resultArray[i - 1] = nonTerminalStrategy.create(arrayInfo);
                    }

                    // if weight exceeded return as is
                    Object[] newResultArray = (Object[]) Array.newInstance(information.getClazz().getComponentType(), i + 1);
                    System.arraycopy(resultArray, 0, newResultArray, 0, i + 1);
                    return newResultArray;
                }
            }
            return resultArray;
        }
        return nonTerminalStrategy.create(information);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        // TODO #179 canCreate must consider the minimal size
        return !isDisabled() ? multiCanCreate(information) : null;
    }

    /**
     * Multiplies the canCreate of the contained strategy by the size of the array, as each size adds 1 DOF to all elements
     *
     * @param information to be fullfilled
     * @return requirements of sub-strategy times the max width of the code
     */
    private RequirementInformation multiCanCreate(CreationInformation information) {
        boolean isArray = false;
        if (information.getClazz().isArray()) {
            information.setClazz(information.getClazz().getComponentType());
            isArray = true;
        }
        if (!isArray) {
            // no array -> not our problem (here)
            return nonTerminalStrategy.canCreateVerbose(information);
        }
        Collection<Requirement> patterns = information.getRequirements().getRequirements(Requirement.REQ_ANTIPATTERN);
        patterns.addAll(information.getRequirements().getRequirements(Requirement.REQPR_PATTERN));
        if (patterns.isEmpty()) {
            // without patterns or antipatterns just return the DOF
            RequirementInformation requirementInformation = nonTerminalStrategy.canCreateVerbose(information);
            if (requirementInformation != null) {
                requirementInformation.getRequirements().entrySet().forEach(x -> x.setValue(x.getValue() * information.getConfiguration().getMaxWidth()));
            }
            return requirementInformation;
        }

        // see if we can satisfy or forward the anti/patterns
        CreationInformation infoCopy = information.copy();
        RequirementInformation satisfactory = infoCopy.getRequirements().copy();
        for (int i = 0; i < information.getConfiguration().getMaxWidth(); i++) {
            RequirementInformation requirementInformation = nonTerminalStrategy.canCreateVerbose(infoCopy);
            requirementInformation.getRequirements().entrySet().forEach(x -> {
                // satisfy or forward pos
                if (x.getValue() > 0) {
                    if (x.getKey().containsProperty("FORWARDPOINTER")) {
                        x.getKey().getProperties().remove("FORWARDPOINTER");
                        Integer maxPos = loadWildcardDependent(x.getKey(), Requirement.REQPR_PATTERN_LTRPOS_MAX);
                        Integer currPos = loadWildcardDependent(x.getKey(), Requirement.REQPR_PATTERN_LTRPOS);
                        if (currPos + 1 == maxPos) {
                            Requirement match = Requirement.loadMatch(x.getKey(), infoCopy.getRequirements());
                            infoCopy.getRequirements().getRequirements().keySet().removeIf(r -> r.equals(match));
                        } else {
                            thinkOfTheChildren(infoCopy.getRequirements().getRequirements().keySet(), x.getKey(), true);
                        }
                    } else if (x.getKey().containsProperty(Requirement.REQPR_PATTERN_LTROWNER)) {
                        // advance via positioning
                        Requirement parent = x.getKey().getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
                        Integer maxPos = loadWildcardDependent(parent, Requirement.REQPR_PATTERN_LTRPOS_MAX);
                        Integer currPos = loadWildcardDependent(parent, Requirement.REQPR_PATTERN_LTRPOS);
                        if (currPos + 1 == maxPos) {
                            Requirement match = Requirement.loadMatch(x.getKey(), infoCopy.getRequirements());
                            infoCopy.getRequirements().getRequirements().keySet().removeIf(r -> r.equals(match));
                        } else {
                            thinkOfTheChildren(infoCopy.getRequirements().getRequirements().keySet(), x.getKey(), true);
                        }
                    } else {
                        // pattern satisified and needs no position
                        Requirement match = Requirement.loadMatch(x.getKey(), infoCopy.getRequirements());
                        infoCopy.getRequirements().getRequirements().remove(match);
                    }
                }
            });
            if (infoCopy.getRequirements().getRequirements(Requirement.REQ_ANTIPATTERN).isEmpty() && infoCopy.getRequirements().getRequirements(Requirement.REQ_PATTERN).isEmpty()) {
                // if all reqs satisfied we are done son.
                break;
            }
        }
        // fulfill all "finished" forwarded antipatterns
        satisfactory.getRequirements().keySet().forEach(sat -> {
            Requirement match = Requirement.loadMatch(sat, infoCopy.getRequirements());
            if (match == null) {
                satisfactory.addDegreeOfFreedom(sat);
            }
        });
        // deal with all unfulfilled forwarded antipatterns
        infoCopy.getRequirements().getRequirements().entrySet().stream().filter(x -> x.getValue() == 0).forEach(x -> {
            if (x.getKey().containsProperty(Requirement.REQPR_PATTERN_LTRPOS)) {
                Requirement replyMatch = Requirement.loadMatch(x.getKey(), satisfactory);
                if (replyMatch != null) {
                    Integer newPos = x.getKey().getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
                    if (!replyMatch.containsProperty(Requirement.REQPR_PATTERN_LTRPOS) || replyMatch.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) < newPos) {
                        replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRPOS, newPos);
                        if (!replyMatch.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX)) {
                            replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRFIELD, x.getKey().getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class));
                            replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, x.getKey().getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class));
                        }
                    }
                }
            }
        });

        // fast forward all non-antipattern reqs
        satisfactory.getRequirements().entrySet().stream().filter(x -> !x.getKey().getName().equals(Requirement.REQ_ANTIPATTERN) && !x.getKey().getName().equals(Requirement.REQ_PATTERN)).forEach(x -> x.setValue(x.getValue() * information.getConfiguration().getMaxWidth()));
        return satisfactory;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return canCreate(information);
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // NOTHING - The Array Size strategy does not deal with creation checks
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // NOTHING - The Array Size strategy does not deal with creation checks
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        if (this.nonTerminalStrategy == null) {
            if (strategy instanceof RandomArraySizeStrategy) {
                this.nonTerminalStrategy = ((RandomArraySizeStrategy) strategy).nonTerminalStrategy;
            } else {
                this.nonTerminalStrategy = strategy;
            }
        }
    }

    @Override
    public TruffleHierarchicalStrategy<Object> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    @Override
    public Collection<Class> getManagedClasses() {
        return nonTerminalStrategy.getManagedClasses();
    }

    @Override
    public double minWeight(CreationInformation information) {
        // TODO #179 if minimal size > 0 we can't just return 0
        return 0;
    }

    public TruffleHierarchicalStrategy getNonTerminalStrategy() {
        return nonTerminalStrategy;
    }

    public void setNonTerminalStrategy(TruffleHierarchicalStrategy nonTerminalStrategy) {
        this.nonTerminalStrategy = nonTerminalStrategy;
        if (nonTerminalStrategy instanceof DefaultObservableStrategy) {
            subscribeAndValidate((TruffleObservableStrategy) nonTerminalStrategy);
        }
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // delegate as everything is handled by the reflective subtree strat
        return this.nonTerminalStrategy.loadRequirements(ast, parentInformation, requirementMap);
    }
}
