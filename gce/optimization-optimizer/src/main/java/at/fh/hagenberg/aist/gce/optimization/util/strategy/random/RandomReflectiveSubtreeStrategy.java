/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.random;

import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableAndObserverStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.FrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.SelectorStrategy;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import org.springframework.beans.factory.annotation.Required;
import zmq.socket.reqrep.Req;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The random subtree strategy creates a completely random subtree of the requested class
 *
 * @author Oliver Krauss on 07.11.2018
 */
public class RandomReflectiveSubtreeStrategy<T extends Node> extends DefaultObservableAndObserverStrategy implements TruffleHierarchicalStrategy<T> {

    /**
     * Class information for object
     */
    protected TruffleClassInformation information;

    /**
     * Per default the strategy manages the initializer that has the smallest amount of child nodes
     */
    protected TruffleClassInitializer initializer;

    /**
     * Classes that are supported by this creator
     */
    protected List<Class> classes;

    /**
     * Strategies for terminal values
     */
    protected Map<String, TruffleVerifyingStrategy> terminalStrategies;

    /**
     * Strategy for nonterminals. Note: If you want to use more than one strategy refer to the
     * {@link SelectorStrategy}
     */
    protected TruffleHierarchicalStrategy nonTerminalStrategy;

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    /**
     * All requirements that must be checked upon creation. Mostly about patterns, but also allowed / forbidden values.
     */
    protected Map<String, List<Requirement>> createRequirements = new HashMap<>();

    private double weight;

    protected boolean recursionForbidden = true;

    protected Random random = new Random();

    public RandomReflectiveSubtreeStrategy(TruffleClassInformation information, List<Class> classes, Map<String, TruffleVerifyingStrategy> terminalStrategies, TruffleHierarchicalStrategy nonTerminalStrategy) {
        this.information = information;
        this.classes = classes;

        this.initializer = information.getInitializersForCreation().stream().filter(x -> x.getMinimalSubtreeSize() > -1).min(Comparator.comparingInt(TruffleClassInitializer::getMinimalSubtreeSize)).get();
        this.weight = information.getSystemWeight() > 0 ? information.getSystemWeight() : 0;

        AtomicBoolean terminalOnly = new AtomicBoolean(true);
        this.terminalStrategies = new HashMap<>();
        List<Class> requiredTerminals = Arrays.stream(initializer.getParameters()).map(TruffleParameterInformation::getClazz).distinct().collect(Collectors.toList());
        requiredTerminals.forEach(x -> {
            if (terminalStrategies.containsKey(x.getName())) {
                // add only terminals we need.
                TruffleVerifyingStrategy truffleStrategy = terminalStrategies.get(x.getName());
                this.terminalStrategies.put(x.getName(), truffleStrategy);
                // subscribe to observable strategies, and validate state at startup
                if (truffleStrategy instanceof TruffleObservableStrategy) {
                    subscribeAndValidate((TruffleObservableStrategy) truffleStrategy);
                }
            } else {
                terminalOnly.set(false);
            }
        });

        // prepare strategy to also work with globals
        if (information.getProperties().contains(TruffleClassProperty.GLOBAL_STATE) && this.terminalStrategies.containsKey("com.oracle.truffle.api.frame.FrameSlot") && terminalStrategies.containsKey("com.oracle.truffle.api.frame.MaterializedFrame")) {
            MaterializedFrame globalFrame = (MaterializedFrame) terminalStrategies.get("com.oracle.truffle.api.frame.MaterializedFrame").create(null);
            if (globalFrame != null) {
                // copy regular strategy and move in global context instead of local one.
                FrameSlotStrategy frameSlotStrategy = (FrameSlotStrategy) this.terminalStrategies.get("com.oracle.truffle.api.frame.FrameSlot");
                frameSlotStrategy = frameSlotStrategy.copy();
                frameSlotStrategy.setDescriptor(globalFrame.getFrameDescriptor());
                frameSlotStrategy.setFrame(globalFrame);
                this.terminalStrategies.put("com.oracle.truffle.api.frame.FrameSlot", frameSlotStrategy);
            } else {
                // prevent unintentional access to a local frame
                this.terminalStrategies.remove("com.oracle.truffle.api.frame.FrameSlot");
            }
        }

        this.nonTerminalStrategy = nonTerminalStrategy;
        // Only observe this strategy (and make this strategy dependent) IF this reflective class requires non-terminals
        if (!terminalOnly.get() && nonTerminalStrategy instanceof TruffleObservableStrategy) {
            subscribeAndValidate((TruffleObservableStrategy) nonTerminalStrategy);
        }
    }

    protected Object selectParam(TruffleParameterInformation parameter, CreationInformation information) throws ClassNotFoundException {
        return terminalStrategies.containsKey(parameter.getType().getName()) ?
                terminalStrategies.get(parameter.getType().getName()).create(information) :
                nonTerminalStrategy.create(information.setClazz(parameter.getType()).incCurrentDepth());
    }

    @Override
    public T create(CreationInformation information) {
        // load antipatterns and patterns
        CreationInformation infoForChildren = information.copy();
        // prevent downward propagation of var-maps
        infoForChildren.getRequirements().getRequirements().keySet().removeIf(x -> x.getName().equals(Requirement.REQ_PATTERN_VAR_PLACEHOLDER));
        List<Requirement> antipatterns = loadMatchingPatterns(infoForChildren.getRequirements());
        // fulfill antipatterns that we do not match
        combineSatisfiedRequirements(information.getRequirements(), antipatterns, true);

        // remove unfulfillable patterns
        removeUnsatisfiableAntipatterns(antipatterns);

        // TODO #63 THIS IS DEBUG CODE IF WE EVER CHANGE THE LOGIC ON HOW PATTERNS WORK
//        System.out.println(indent(information.getCurrentDepth()) + "Creating " + this.information.getClazz().getSimpleName() + " (" + antipatterns.size() + ") - " + antipatterns.stream().map(x -> String.valueOf(x.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class).getId())).collect(Collectors.joining(",")));
//        if (!antipatterns.isEmpty()) {
//            System.out.println(indent(information.getCurrentDepth()) + antipatterns.stream().map(x -> {
//                String type = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType();
//                Requirement match = Requirement.loadMatch(x, information.getRequirements());
//                if (Wildcard.WILDCARD_ANYWHERE.equals(type)) {
//                    type = "[" + x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) + "/" + x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class) + "]" + x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).humanReadable();
//                }
//                if (match != null) {
//                    type = information.getRequirements().getRequirements().get(match) + " Dof " + type;
//                }
//                return type;
//            }).collect(Collectors.joining(",")));
//        }

        // add information for parameters which class we are creating
        information.setInformation(this.information);

        // create parameters
        Object[] params = new Object[initializer.getParameters().length];

        // collect degrees of freedom per parameter

        List<Pair<TruffleParameterInformation, RequirementInformation>> dof = Arrays.stream(initializer.getParameters()).map(x ->
                new Pair<>(x, terminalStrategies.containsKey(x.getType().getName()) ?
                        terminalStrategies.get(x.getType().getName()).canCreate(information.copy()) :
                        nonTerminalStrategy.canCreateVerbose(
                                infoForChildren.copy().setClazz(x.getType()).incCurrentDepth())
                )).collect(Collectors.toList());

        for (int i = 0; i < params.length; i++) {
            try {
                // load DOF
                TruffleParameterInformation pInfo = initializer.getParameters()[i];
                Pair<TruffleParameterInformation, RequirementInformation> pairInfo = dof.stream().filter(x -> x.getKey().equals(pInfo)).findAny().orElseThrow();
                dof.remove(pairInfo);
                RequirementInformation rInfo = pairInfo.getValue();
                RequirementInformation paramRInfo = new RequirementInformation(rInfo.getRequirements().keySet());

                // We create a new requirements Information containing the dof of the parent and side paths but NOT itself (so we force it to create something that it sait it could do itself)
                // We ALSO remove any unfullfillable requirement that can be fulfilled by other paths
                rInfo.getRequirements().forEach((k, v) -> {
                    // tell param that other branches or the parent could fulfill this as well
                    paramRInfo.addDegreeOfFreedom(k, dof.stream().filter(x -> x.getValue().getRequirements().containsKey(k)).mapToInt(x -> x.getValue().getRequirements().get(k)).sum());
                    paramRInfo.addDegreeOfFreedom(k, information.getRequirements().getRequirements().getOrDefault(k, 0));
                });

                // deal with anti pattern restrictions
                modifyRequirementInformationForParameter(antipatterns, paramRInfo, pInfo, information.getRequirements());

                RequirementInformation paramRInfoFulfilled = paramRInfo.copy();

                // TODO #63 THIS IS DEBUG CODE IF WE EVER CHANGE THE LOGIC ON HOW PATTERNS WORK
//                System.out.println(indent(information.getCurrentDepth()) + "  Selecting param " + pInfo.getName());

                params[i] = selectParam(pInfo, information.copy().setRequirements(paramRInfoFulfilled));
                // set variables for parent
                updateVariableMap(information.getRequirements(), paramRInfoFulfilled);
                // set variables for next params
                updateVariableMap(new RequirementInformation(antipatterns), paramRInfoFulfilled);

                // remove fulfilled requirements from other parameters and parent
                paramRInfo.getRequirements().keySet().forEach(x -> {
                    // only satisfy requirements that were actually given to the child
                    if (x.getName().equals(Requirement.REQ_ANTIPATTERN) || x.getName().equals(Requirement.REQ_PATTERN)) {
                        // deal with antipatterns and patterns
                        Requirement requirement = paramRInfoFulfilled.getRequirements().keySet().stream().filter(z -> x.getProperty("ID", Integer.class).equals(z.getProperty("ID", Integer.class))).findFirst().orElse(null);
                        if (requirement == null) {
                            // fulfill finished pattern path
                            Requirement match = Requirement.loadMatch(x, information.getRequirements());
                            if (match != null) {
                                if (thinkOfTheChildren(information.getRequirements().getRequirements().keySet(), match, true)) {
                                    // we must satisfy the next child of the parentage -> reloading in case parent changed is easier
                                    antipatterns.clear();
                                    antipatterns.addAll(loadMatchingPatterns(information.copy().getRequirements()));
                                    removeUnsatisfiableAntipatterns(antipatterns);
                                } else {
                                    // deal with other types
                                    information.getRequirements().fullfill(match);
                                    antipatterns.remove(Requirement.loadMatch(x, antipatterns));
                                }
                            }
                        } else {
                            // move position
                            combineRequirementsFullfilledByChild(antipatterns, requirement);
                            combineRequirementsFullfilledByChild(information.getRequirements().getRequirements().keySet(), requirement);
                        }
                        dof.parallelStream().forEach(y -> y.getValue().fullfill(x));
                    } else if (x.getName().equals(Requirement.REQ_VALUE_RESTRICTED)) {
                        Requirement xParent = x.getProperty(Requirement.REQ_REF, Requirement.class);
                        Requirement nonMatch = Requirement.loadMatch(x, paramRInfoFulfilled.getRequirements(Requirement.REQ_VALUE_RESTRICTED).stream().map(vr -> vr.getProperty(Requirement.REQ_REF, Requirement.class)).collect(Collectors.toList()));
                        if (nonMatch == null) {
                            Requirement match = Requirement.loadMatch(xParent, information.getRequirements());
                            if (match != null) {
                                NodeWrapper pos = match.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                                if (pos.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                                    pos = Requirement.findStarchild(match, pos);
                                }
                                assert pos != null;
                                if ((match.getName().equals(Requirement.REQ_ANTIPATTERN) && pos.getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                                        (match.getName().equals(Requirement.REQ_PATTERN) && !pos.getType().startsWith(Wildcard.WILDCARD_NOT))) {
                                    information.getRequirements().fullfill(match);
                                    antipatterns.remove(Requirement.loadMatch(xParent, antipatterns));
                                }
                            }
                        }
                    } else {
                        // deal with other types
                        information.getRequirements().fullfill(x);
                        dof.parallelStream().forEach(y -> y.getValue().fullfill(x));
                    }
                });

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // create node
        return (T) initializer.instantiate(params);
    }

    private static String indent(int indent) {
        return "  ".repeat(Math.max(0, indent));
    }

    /**
     * Injects all antipattern requirement for the given parameter into the Requirement Information
     *
     * @param antipatterns antipatterns to inject
     * @param information  information to inject into
     * @param pInfo        Parameter to inject for
     * @param parentInfo   Parent information that may have DOF for us that we need to transpose
     * @return information (same object as input but with added requirements!)
     */
    private RequirementInformation modifyRequirementInformationForParameter(Collection<Requirement> antipatterns, RequirementInformation information, TruffleParameterInformation pInfo, RequirementInformation parentInfo) {
        // clear out the parent antipatterns so we don't duplicate reqs
        information.getPatternRequirements().forEach(information::fullfill);

        antipatterns.forEach(x -> {
            NodeWrapper antipattern = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);

            // collect requirements to be able to also consider siblings
            List<Requirement> reqsToAdd = new ArrayList<>();

            boolean anywhere = false;

            if (antipattern.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {


                NodeWrapper child = Requirement.findStarchild(x, antipattern);

                if (child == null) {
                    return;
                }

                // if the antipattern is a match HERE we must not propagate the STAR wildcard but rather its child.
                if (child.getChildren().isEmpty() ||
                        !TruffleMasterStrategy.loadTypes(child, rootStrategy.metaForPattern(x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class), x.getName()))
                                .contains(this.information.getClazz().getName())) {
                    anywhere = true;

                    // just copy over the star wildcard
                    Requirement starWildcard = x.copy();
                    // for EVERYWHERE matches add the info as well
                    if ((x.getName().equals(Requirement.REQ_ANTIPATTERN) && !antipattern.getChildren().stream().iterator().next().getChild().getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                            (x.getName().equals(Requirement.REQ_PATTERN) && antipattern.getChildren().stream().iterator().next().getChild().getType().startsWith(Wildcard.WILDCARD_NOT))) {
                        starWildcard.addProperty(Requirement.REQ_PATTERN_MATCH_TYPE, Requirement.REQ_PATTERN_MATCH_TYPE_EVERYWHERE);
                    }
                    // consider that star wildcard has more than one child
                    modifyStarWildcardChildren(antipattern, starWildcard);

                    if (terminalStrategies.containsKey(pInfo.getType().getName())) {
                        // Find active child in star wildcard
                        child.getValues().keySet().forEach(valkey -> {
                            // add value restriction on all options
                            String[] split = valkey.split(":");
                            generateValueRequirement(Requirement.loadMatch(x, parentInfo), child, split[0], split[1], reqsToAdd, x.getName());
                        });
                    }

                    reqsToAdd.add(starWildcard);
                } else {
                    antipattern = child;
                }
            }

            // descend if the star wildcard did not activate
            if (!anywhere) {
                generateValueRequirement(x, antipattern, pInfo.getName(), pInfo.getType().getName(), reqsToAdd, x.getName());

                // Check if we need to tell the child node about the anti pattern position
                if (!antipattern.getChildren().isEmpty()) {
                    NodeWrapper finalAntipattern = antipattern;
                    antipattern.getChildren().forEach(child -> {
                        if (child.getField() == null || child.getField().isEmpty() || child.getField().equals(pInfo.getName())) {
                            Collection<OrderedRelationship> children = finalAntipattern.getChildren(child.getField());
                            if (children.size() > 1) {
                                Integer pos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
                                if (pos != null && pos != child.getOrder()) {
                                    // we do not match right now!
                                    return;
                                }
                                if (pos == null) {
                                    // initialize ourselves as 1.
                                    x.addProperty(Requirement.REQPR_PATTERN_LTRPOS, child.getOrder());
                                    x.addProperty(Requirement.REQPR_PATTERN_LTRFIELD, child.getField());
                                    Integer maxPos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class);
                                    if (maxPos == null || maxPos < children.size()) {
                                        x.addProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, children.size());
                                    }
                                }
                            }
                            Requirement patternPos = new Requirement(x.getName()).addProperty(Requirement.REQ_PATTERN, x.getProperty(Requirement.REQ_PATTERN, NodeWrapper.class)).addProperty("ID", x.getProperty("ID", Integer.class)).addProperty(Requirement.REQPR_PATTERN_POS, child.getChild());

                            // don't forget to copy over required stuff such as variables (we can't do copy here because of the positioning / parent code)
                            if (x.containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER)) {
                                patternPos.addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, x.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class));
                            }

                            if (child.getChild().getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                                modifyStarWildcardChildren(child.getChild(), patternPos);
                            }
                            // for EVERYWHERE matches add the info as well (no need to check position, as this is a NEW pos and we start enforcing the FIRST child.
                            if (child.getChild().getType().equals(Wildcard.WILDCARD_ANYWHERE) && (
                                    (x.getName().equals(Requirement.REQ_ANTIPATTERN) && !child.getChild().getChildren().stream().iterator().next().getChild().getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                                            (x.getName().equals(Requirement.REQ_PATTERN) && child.getChild().getChildren().stream().iterator().next().getChild().getType().startsWith(Wildcard.WILDCARD_NOT))
                            )) {
                                patternPos.addProperty(Requirement.REQ_PATTERN_MATCH_TYPE, Requirement.REQ_PATTERN_MATCH_TYPE_EVERYWHERE);
                            }
                            // to be able to modify within array positions as well add the owner (we must find out that this pos must be increased even if it is a duplicate of the REF
                            if (children.size() > 1) {
                                patternPos.addProperty(Requirement.REQPR_PATTERN_LTROWNER, x);
                            }
                            reqsToAdd.add(patternPos);
                        }
                    });
                }
            }

            if (!reqsToAdd.isEmpty()) {
                reqsToAdd.forEach(information::addRequirement);

                // find parent req that we possibly need to carry over dof from
                Requirement match = Requirement.loadMatch(x, parentInfo);
                if (match != null) {
                    Integer parentDof = parentInfo.getRequirements().get(match);
                    if (parentDof != null && parentDof > 0) {
                        reqsToAdd.forEach(req -> information.addDegreeOfFreedom(req, parentDof));
                    }
                }

                // increase DOF for each sibling to the right
                if (antipattern.getChildren().size() > 1) {
                    Requirement childReq = reqsToAdd.stream().filter(r -> r.getName().equals(Requirement.REQ_ANTIPATTERN) || r.getName().equals(Requirement.REQ_PATTERN)).findAny().orElse(null);
                    NodeWrapper pos = childReq.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);

                    if (pos == antipattern) {
                        // if this is the same we are in a star wildcard and need to consider the current child position
                        pos = antipattern.getChildren().stream().filter
                                (child -> child.getField().equals(childReq.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class)) &&
                                        childReq.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class).equals(child.getOrder())).findFirst().orElse(null).getChild();
                    }

                    NodeWrapper finalPos = pos;
                    OrderedRelationship relationship = antipattern.getChildren().stream().filter(rel -> rel.getChild().equals(finalPos)).findAny().get();
                    String name = relationship.getField();

                    List<NodeWrapper> rightSiblings;
                    if (name == null || name.isEmpty()) {
                        // collect all right siblings
                        rightSiblings = antipattern.getChildren().stream().filter(rel -> rel.getOrder() > relationship.getOrder()).map(OrderedRelationship::getChild).collect(Collectors.toList());
                    } else {
                        // find the order
                        List<String> validRightSiblings = new ArrayList<>();
                        TruffleParameterInformation[] parameters = this.initializer.getParameters();
                        boolean foundThisRel = false;
                        for (TruffleParameterInformation parameter : parameters) {
                            if (foundThisRel) {
                                validRightSiblings.add(parameter.getName());
                            }
                            if (parameter.getName().equals(name)) {
                                foundThisRel = true;
                            }
                        }
                        rightSiblings = antipattern.getChildren().stream().filter(rel -> validRightSiblings.contains(rel.getField())).map(OrderedRelationship::getChild).collect(Collectors.toList());
                    }

                    int dof = !rightSiblings.isEmpty() && findSkippable(antipattern, childReq.getName().equals(Requirement.REQ_ANTIPATTERN)) ? 1 : 0;
                    // dof adds sum of all not neg right siblings
                    rightSiblings = rightSiblings.stream().filter(sib -> this.findSkippable(sib, childReq.getName().equals(Requirement.REQ_ANTIPATTERN))).collect(Collectors.toList());
                    dof += rightSiblings.size();
                    if (dof > 0) {
                        int finalDof = dof;
                        reqsToAdd.forEach(req -> information.addDegreeOfFreedom(req, finalDof));
                    }
                }
            }
        });

        return information;
    }

    /**
     * Generates a value requirement for the currently active node in the modify function
     *
     * @param parent      parent requirement that contains the value requirement
     * @param antipattern that shall be checked for requirements
     * @param name        that must match to inject the restriction
     * @param type        that must match to inject the restriction
     * @param reqsToAdd   list of requirements that can be added here
     */
    private void generateValueRequirement(Requirement parent, NodeWrapper antipattern, String name, String type, List<Requirement> reqsToAdd, String patternType) {
        // Forbid specific values in anti-patterns
        if (!antipattern.getValues().isEmpty() && parent != null) {
            List<String> strings = TruffleMasterStrategy.loadTypes(antipattern, rootStrategy.metaForPattern(parent.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class), patternType));
            if (strings.contains(this.information.getClazz().getName())) {
                // for all restrictions where we are the last option to prevent an antipattern FORCE remove the value
                String identifier = name + ":" + type;
                if (antipattern.getValues().containsKey(identifier)) {
                    reqsToAdd.add(new Requirement(Requirement.REQ_VALUE_RESTRICTED).addProperty(Requirement.REQ_REF, parent).addProperty(Requirement.REQ_VALUE_TYPE, type).addProperty(Requirement.REQ_VALUE_VALUE, antipattern.getValues().get(identifier)));
                }
            }
        }
    }

    private void modifyStarWildcardChildren(NodeWrapper antipattern, Requirement x) {
        Collection<OrderedRelationship> children = antipattern.getChildren();
        if (children.size() > 1) {
            Integer pos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
            if (pos == null) {
                OrderedRelationship child = children.iterator().next();
                // initialize ourselves as 1.
                x.addProperty(Requirement.REQPR_PATTERN_LTRPOS, child.getOrder());
                x.addProperty(Requirement.REQPR_PATTERN_LTRFIELD, child.getField());
                Integer maxPos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class);
                if (maxPos == null || maxPos < children.size()) {
                    x.addProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, children.size());
                }
            }
        }
    }

    /**
     * Calculates the size of positions that we MUST fulfill alltogether when multiple children exist in a pattern
     * Basically just counts all NOT nodes in the children
     *
     * @param children to be checked
     * @return minimum nodes neede
     */
    private int calculateSize(Collection<OrderedRelationship> children) {
        // Note: we can only count direct children as children of children are in their own subtree requiring their own sizes
        return (int) children.stream().mapToInt(c -> c.getChild().getType().startsWith(Wildcard.WILDCARD_NOT) ? 1 : 0).sum();
    }

    private List<Requirement> loadMatchingPatterns(RequirementInformation information) {
        List<Requirement> collect = new LinkedList<>();

        // perform a lookahead on anywhere wildcards
        Collection<Requirement> antipatterns = information.getPatternRequirements().stream()
                // filter out antipatterns that were already fulfilled but must inform the parent requirements over the positioning
                .filter(x -> !x.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX) ||
                        !x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class).equals(x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class))
                ).collect(Collectors.toList());
        ;

        int[] maxId = new int[1];
        maxId[0] = antipatterns.stream().map(a -> a.getProperty("ID", Integer.class)).max(Integer::compareTo).orElse(-1);
        maxId[0]++;
        List<Requirement> stars = antipatterns.stream().filter(x -> x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().equals(Wildcard.WILDCARD_ANYWHERE)).collect(Collectors.toList());

        stars.forEach(x -> {
            NodeWrapper pos = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
            x.addProperty("TMP", pos);
            x.addProperty(Requirement.REQPR_PATTERN_POS, Requirement.findStarchild(x, pos));
        });

        // check all patterns that WE match
        List<Requirement> createReqs = new ArrayList<>();
        if (this.createRequirements.containsKey(Requirement.REQ_ANTIPATTERN)) {
            createReqs.addAll(this.createRequirements.get(Requirement.REQ_ANTIPATTERN));
        }
        if (this.createRequirements.containsKey(Requirement.REQ_PATTERN)) {
            createReqs.addAll(this.createRequirements.get(Requirement.REQ_PATTERN));
        }
        // only add non active star wildcards
        createReqs.addAll(rootStrategy.getRootAntiPatterns().stream().filter(x -> antipatterns.stream().map(y -> y.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class)).noneMatch(y -> y.equals(x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class)))).collect(Collectors.toList()));
        createReqs.addAll(rootStrategy.getRootPatterns().stream().filter(x -> antipatterns.stream().map(y -> y.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class)).noneMatch(y -> y.equals(x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class)))).collect(Collectors.toList()));

        ArrayList<Requirement> clashingPattern = new ArrayList<>();

        createReqs.forEach(x -> {
            NodeWrapper pattern = x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);
            NodeWrapper pos = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);

            if (pattern == pos) {
                if (antipatterns.stream().anyMatch(anti -> anti.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(pattern)) && recursionForbidden) {
                    // prevent endless recursion of patterns (ex MinicBlockNode -> MinicBlockNode would endless recurse the pattern)
                    // if an antipattern is active it won't reactivate
                    return;
                }

                if (x.getName().equals(Requirement.REQ_PATTERN)) {
                    // the root of a pattern activates with a random chance
                    double activationChance = x.getProperty(Requirement.REQ_PATTERN_ACTIVATION_CHANCE, Double.class);
                    double activate = random.nextDouble();
                    if (activate < activationChance) {
                        Requirement newRoot = x.copy().addProperty("ID", maxId[0]++);
                        collect.add(newRoot);
                        clashingPattern.add(newRoot);
                    }
                } else {
                    // the root of an anti-pattern always applies
                    collect.add(x.copy().addProperty("ID", maxId[0]++));
                }
            }

            // consider pre-existing antipattern locations (not for patterns as these do match == no need to fulfill anymore!)
            ArrayList<Requirement> rmPatterns = new ArrayList<>();
            antipatterns.stream().filter(ap -> ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).equals(pos)).forEach(ap -> {
                if (ap.getName().equals(Requirement.REQ_ANTIPATTERN)) {
                    collect.add(ap);
                } else {
                    NodeWrapper antipattern = ap.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);
                    NodeWrapper node = ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                    List<String> strings = TruffleMasterStrategy.loadTypes(node, rootStrategy.metaForPattern(antipattern, ap.getName()));
                    if (!strings.contains(this.information.getClazz().getName())) {
                        // does not match
                        collect.add(ap);
                    } else if (node.getValues().keySet().stream().anyMatch(key -> !key.startsWith("PATTERN:"))) {
                        // values must be resolved
                        collect.add(ap);
                    } else if (strings.contains(this.information.getClazz().getName()) && !node.getChildren().isEmpty()) {
                        // Children still must be fulfilled before this can be resolved
                        collect.add(ap);
                    } else {
                        rmPatterns.add(ap);
                    }
                }
            });
            // clear out matched patterns
            antipatterns.removeAll(rmPatterns);
        });

        // ensure that patterns that compete with each other select only one of them
        if (clashingPattern.size() > 1) {
            clashingPattern.remove(random.nextInt(clashingPattern.size()));
            collect.removeAll(clashingPattern);
        }

        // consider all nodes that have no valid match
        antipatterns.stream().filter(ap -> ap.getName().equals(Requirement.REQ_PATTERN) && !ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().startsWith(Wildcard.WILDCARD_NOT) && !collect.contains(ap)).forEach(collect::add);

        // Consider NOT WILDCARD for Antipatterns e.g. "if I am not this, I don't match" -> this lets us add DOF to matching ones.
        antipatterns.stream().filter(ap -> ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().startsWith(Wildcard.WILDCARD_NOT)).forEach(ap -> {
            NodeWrapper antipattern = ap.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);
            NodeWrapper node = ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
            List<String> strings = TruffleMasterStrategy.loadTypes(node, rootStrategy.metaForPattern(antipattern, ap.getName()));

            if (ap.getName().equals(Requirement.REQ_ANTIPATTERN)) {
                if (!strings.contains(this.information.getClazz().getName())) {
                    // does not match
                    collect.add(ap);
                } else if (!node.getValues().isEmpty()) {
                    // values must be resolved
                    collect.add(ap);
                } else if (strings.contains(this.information.getClazz().getName()) && !node.getChildren().isEmpty()) {
                    // Children still must be fulfilled before this can be resolved
                    collect.add(ap);
                }
            } else {
                if (strings.contains(this.information.getClazz().getName())) {
                    // does not match
                    collect.add(ap);
                } else if (!node.getValues().isEmpty()) {
                    // values must be resolved
                    collect.add(ap);
                } else if (!strings.contains(this.information.getClazz().getName()) && !node.getChildren().isEmpty()) {
                    // Children still must be fulfilled before this can be resolved
                    collect.add(ap);
                }
            }
        });

        // return star wildcard to correct place if we don't match
        stars.forEach(x -> {
            if (collect.contains(x)) {
                // NOT antipattern nodes still doesn't match - leave Star in
                x.addProperty(Requirement.REQPR_PATTERN_POS, x.getProperty("TMP", NodeWrapper.class));
            } else if ((x.getName().equals(Requirement.REQ_ANTIPATTERN) && !x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                    (x.getName().equals(Requirement.REQ_PATTERN) && x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().startsWith(Wildcard.WILDCARD_NOT))
            ) {
                // for all antipatterns that have a positive-valued child (Must not match anywhere) leave the requirement in
                if (!this.isTerminal()) {
                    x.addProperty(Requirement.REQPR_PATTERN_POS, x.getProperty("TMP", NodeWrapper.class));
                    collect.add(x);
                } else if (x.getProperty("TMP", NodeWrapper.class).getChildren().stream().anyMatch(pC -> collect.stream().anyMatch(u -> {
                    // for all terminals also maintain the * as unsatisfied if we failed the checks above
                    NodeWrapper unsatisfiedPos = u.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                    return unsatisfiedPos.equals(pC.getChild());
                }))) {
                    x.addProperty(Requirement.REQPR_PATTERN_POS, x.getProperty("TMP", NodeWrapper.class));
                    collect.add(x);
                }
            }
            // Positive nodes still need to be prevented!
            x.properties.remove("TMP");
        });

        // clean out antipattern requirements as we loaded them in this function
        information.getRequirements().keySet().removeIf(x -> x.getName().equals(Requirement.REQ_ANTIPATTERN) || x.getName().equals(Requirement.REQ_PATTERN));
        return collect;
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return !isDisabled() && // if disabled -> can create nothing
                ((information.getConfiguration().getMaxWeight() == Double.MAX_VALUE || minWeight(information) < information.getConfiguration().getMaxWeight() - information.getCurrentWeight()) && // weight is not exceeded
                        (isTerminal() || (information.getCurrentDepth() + initializer.getMinimalSubtreeSize() <= information.getConfiguration().getMaxDepth())) && // terminals always allow creation || non-terminals must remain in depth
                        classes.contains(information.getClazz())) ? // finally verify patterns (most expensive operation)
                information.getRequirements() : null; // requested class can be used
    }


    private RequirementInformation verifyPatterns(CreationInformation information) {
        // prep the requirements we need to pass to the children
        CreationInformation informationCopy = information.copy();
        RequirementInformation combinedRequirements = informationCopy.getRequirements();
        // cleanse DOF as the DOF calculation is additive (we don't want to + those gotten from the parent)
        combinedRequirements.getRequirements().entrySet().forEach(e -> e.setValue(0));
        // prep the answer
        RequirementInformation reply = information.getRequirements().copy();

        // find antipatterns all patterns that are UNSATISFIED (and strip out satisfied ones)
        List<Requirement> unsatisfied = loadMatchingPatterns(combinedRequirements);

        // add DOF to all SATISFIED antipatterns (they are auto-removed in loadMatchingAntipatterns)
        reply.getPatternRequirements().forEach(x -> {
            if (!unsatisfied.contains(x)) {
                this.information.getClazz();
                if (x.containsProperty(Requirement.REQPR_PATTERN_LTRPOS)) {
                    Integer ltrpos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
                    Integer maxPos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class);
                    if (ltrpos < maxPos) {
                        x.addProperty("FORWARDPOINTER", null);
                    }
                }
                reply.addDegreeOfFreedom(x);
            }
        });

        // add all requirements that this node would introduce as NEW antipattern
        unsatisfied.stream().forEach(x -> {
            if (x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).equals(x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class))) {
                reply.addRequirement(x);
            }
        });

        // filter out all requirements that can't be satisfied because this node doesn't match the pattern
        removeUnsatisfiableAntipatterns(unsatisfied);

        if (unsatisfied.isEmpty()) {
            // if no patterns apply, do regular checkup (cheaper than what we do with the antipatterns below)
            if (Arrays.stream(this.initializer.getParameters()).allMatch(x ->
                    (terminalStrategies.containsKey(x.getClazz().getName()) ?
                            null != combine(combinedRequirements, terminalStrategies.get(x.getClazz().getName()).canCreate(informationCopy.copy().setRequirements(combinedRequirements).setInformation(this.information))) : // parameter is a terminal with available strategy
                            null != combine(combinedRequirements, nonTerminalStrategy.canCreateVerbose(informationCopy.copy().setRequirements(combinedRequirements).setClazz(x.getType()).incCurrentDepth())))))// parameter is a nonterminal with available strategy
            {
                // check antipatterns from sub-nodes
                combinedRequirements.getRequirements().forEach((k, v) -> {
                    if ((k.getName().equals(Requirement.REQ_ANTIPATTERN) || k.getName().equals(Requirement.REQ_PATTERN)) && k.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).equals(k.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class))) {
                        // if we create a new unsatisfiable antipattern we must also propagate this upwards
                        reply.addRequirement(k, v);
                    }
                });
                return reply;
            }
        } else {
            // for all UNSATISFIED antipatterns we must modify the information before passing it to the parameter checks
            if (Arrays.stream(this.initializer.getParameters()).allMatch(x -> {
                        // modify requirements for the given parameter
                        CreationInformation infoCopy = informationCopy.copy();
                        modifyRequirementInformationForParameter(unsatisfied, infoCopy.getRequirements(), x, information.getRequirements());


                        // cleanse DOF as the DOF calculation is additive (we don't want to + those gotten from the parent)
                        infoCopy.getRequirements().getRequirements().entrySet().forEach(e -> e.setValue(0));

                        // do the same as above -> just check which strategy, and combine the REQS
                        boolean canCreate = terminalStrategies.containsKey(x.getClazz().getName()) ?
                                null != combine(combinedRequirements, terminalStrategies.get(x.getClazz().getName()).canCreate(infoCopy.setInformation(this.information))) :  // parameter is a terminal with available strategy
                                null != combine(combinedRequirements, nonTerminalStrategy.canCreateVerbose(infoCopy.setClazz(x.getType()).incCurrentDepth())); // parameter is a nonterminal with available strategy


                        new HashMap<>(combinedRequirements.getRequirements()).forEach((req, degree) -> {
                            if (degree > 0) {
                                // move value reqs up in context
                                if (req.getName().equals(Requirement.REQ_VALUE_RESTRICTED)) {
                                    Requirement match = Requirement.loadMatch(req.getProperty(Requirement.REQ_REF, Requirement.class), unsatisfied);
                                    if (match != null) {
                                        unsatisfied.remove(match);
                                        Requirement replyMatch = Requirement.loadMatch(match, reply.getRequirements().keySet());
                                        reply.addDegreeOfFreedom(replyMatch);
                                    }
                                }

                                // forwarding of child requirements during the cancreate phase
                                NodeWrapper pos = req.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                                if (req.containsProperty(Requirement.REQPR_PATTERN_LTROWNER) && !pos.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                                    // check if this is a new requirement solving one here
                                    thinkOfTheChildren(combinedRequirements.getRequirements().keySet(), req, true);
                                    Requirement parentMatch = Requirement.loadMatch(req.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class), unsatisfied);
                                    if (parentMatch != null && parentMatch.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX)) {
                                        if (parentMatch.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class).equals(parentMatch.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class)) && unsatisfied.contains(parentMatch)) {
                                            Requirement replyReq = Requirement.loadMatch(parentMatch, reply.getPatternRequirements());
                                            if (replyReq != null) {
                                                unsatisfied.remove(parentMatch);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        return canCreate;
                    }
            )) {
                // check if the NOT wildcard is satisfactory or not
                combinedRequirements.getRequirements().forEach((k, v) -> {
                    Requirement replyMatch = Requirement.loadMatch(k, reply);
                    if (replyMatch != null && k.containsProperty(Requirement.REQPR_PATTERN_LTRPOS)) {
                        Integer newPos = k.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
                        if (!replyMatch.containsProperty(Requirement.REQPR_PATTERN_LTRPOS) || replyMatch.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) < newPos) {
                            replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRPOS, newPos);
                            if (!replyMatch.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX)) {
                                replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRFIELD, k.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class));
                                replyMatch.addProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, k.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class));
                            }
                        }
                    }
                    if (replyMatch != null) {
                        if (v > 0) {
                            reply.addDegreeOfFreedom(replyMatch, v);
                        }
                    } else if ((k.getName().equals(Requirement.REQ_ANTIPATTERN) || k.getName().equals(Requirement.REQ_PATTERN)) && k.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).equals(k.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class))) {
                        // if we create a new unsatisfiable antipattern we must also propagate this upwards
                        reply.addRequirement(k, v);
                    }
                });
                return reply;
            }
        }
        return null;
    }

    private List<Requirement> removeUnsatisfiableAntipatterns(List<Requirement> antipatterns) {
        antipatterns.removeIf(ap -> {
            NodeWrapper node = ap.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
            // star wildcards must remain
            if (node.getType().startsWith(Wildcard.WILDCARD_ANYWHERE)) {
                if (this.isTerminal()) {
                    NodeWrapper antipattern = ap.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);
                    NodeWrapper starchild = Requirement.findStarchild(ap, node);
                    List<String> strings = TruffleMasterStrategy.loadTypes(starchild, rootStrategy.metaForPattern(antipattern, ap.getName()));
                    if (ap.getName().equals(Requirement.REQ_ANTIPATTERN)) {
                        if (strings.contains(this.information.getClazz().getName())) {
                            return !starchild.getType().startsWith(Wildcard.WILDCARD_NOT) && starchild.getValues().isEmpty();
                        }
                        return starchild.getType().startsWith(Wildcard.WILDCARD_NOT);
                    } else {
                        if (strings.contains(this.information.getClazz().getName())) {
                            return starchild.getType().startsWith(Wildcard.WILDCARD_NOT) && starchild.getValues().isEmpty();
                        }
                        return !starchild.getType().startsWith(Wildcard.WILDCARD_NOT);
                    }
                }
                return false;
            }

            NodeWrapper antipattern = ap.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);

            List<String> strings = TruffleMasterStrategy.loadTypes(node, rootStrategy.metaForPattern(antipattern, ap.name));
            if (ap.getName().equals(Requirement.REQ_ANTIPATTERN)) {
                if (strings.contains(this.information.getClazz().getName())) {
                    return !node.getType().startsWith(Wildcard.WILDCARD_NOT) && !node.getChildren().isEmpty() && !node.getValues().isEmpty();
                }
                return node.getType().startsWith(Wildcard.WILDCARD_NOT);
            } else {
                if (strings.contains(this.information.getClazz().getName())) {
                    return node.getType().startsWith(Wildcard.WILDCARD_NOT) && !node.getChildren().isEmpty() && !node.getValues().isEmpty();
                }
                return !node.getType().startsWith(Wildcard.WILDCARD_NOT);
            }
        });
        return antipatterns;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return canCreate(information) != null
                ? verifyPatterns(information) : null; // check non functional features and then verify patterns (most expensive operation)
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        if (!createRequirements.containsKey(requirement.getName())) {
            createRequirements.put(requirement.getName(), new ArrayList<>());
        }
        createRequirements.get(requirement.getName()).add(requirement);
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        if (createRequirements.containsKey(requirement.getName())) {
            createRequirements.get(requirement.getName()).removeIf(x -> x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(requirement.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class)));
        }
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        if (this.nonTerminalStrategy == null) {
            this.nonTerminalStrategy = strategy;
        }
    }

    @Override
    public TruffleHierarchicalStrategy<T> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    private Boolean terminal;

    private boolean isTerminal() {
        if (terminal == null) {
            terminal = Arrays.stream(initializer.getParameters()).allMatch(x -> terminalStrategies.containsKey(x.getClazz().getName()));
        }
        return terminal;
    }

    @Override
    public Collection<Class> getManagedClasses() {
        ArrayList<Class> classes = new ArrayList<>();
        classes.add(information.getClazz().isArray() ? information.getClazz().getComponentType() : information.getClazz());
        return classes;
    }

    @Override
    public double minWeight(CreationInformation information) {
        double childWeight = weight;
        for (TruffleParameterInformation parameter : initializer.getParameters()) {
            if (!terminalStrategies.containsKey(parameter.getType().getName())) {
                CreationInformation copy = information.copy().setCurrentWeight(childWeight).setCurrentDepth(information.getCurrentDepth() + 1);
                childWeight += nonTerminalStrategy.minWeight(copy);
            }
        }

        return childWeight;
    }

    public Map<String, TruffleVerifyingStrategy> getTerminalStrategies() {
        return terminalStrategies;
    }

    public void setTerminalStrategies(Map<String, TruffleVerifyingStrategy> terminalStrategies) {
        this.terminalStrategies = terminalStrategies;
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

    public TruffleClassInformation getInformation() {
        return information;
    }

    public TruffleClassInitializer getInitializer() {
        return initializer;
    }


    protected boolean overrideSelectorStrategy = false;

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // load antipatterns
        RequirementInformation preservedParentInformation = parentInformation.copy();
        List<Requirement> matches = loadMatchingPatterns(parentInformation.copy());
        // fulfill antipatterns that we do not match
        combineSatisfiedRequirements(parentInformation, matches, true);
        // don't deal with broken patterns
        removeUnsatisfiableAntipatterns(matches);

        if (ast instanceof RepeatingNode) {
            // get parent info
            Node parent = loadParent(ast);
            // TODO #229 FIX because our patterns are for creation -> they don't know about the injected nodes we should do this better
            Collection<Requirement> parentInfo = requirementMap.containsKey(parent) ? requirementMap.get(parent).getRequirements() : new ArrayList<>();
            parentInfo.forEach(x -> {
                matches.add(x.copy());
                Requirement mod = x.copy();
                // mod the info to force this pattern to match the repeating node instead
                NodeWrapper moddedNode = mod.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).deepCopy();
                moddedNode.setType(ast.getClass().getName());
                mod.addProperty(Requirement.REQPR_PATTERN_POS, moddedNode);
                preservedParentInformation.addRequirement(mod);
            });
        }

        List<Requirement> requirements = new ArrayList<>();
        matches.forEach(x -> requirements.add(x.copy()));

        LoadedRequirementInformation loadedRequirementInformation = new LoadedRequirementInformation(ast, matches, preservedParentInformation);
        requirementMap.put(ast, loadedRequirementInformation);

        // deal with terminals
        Requirement tciPlaceholder = new Requirement("TCI_PLACEHOLDER").addProperty("TCI", this.getInformation());
        matches.add(tciPlaceholder);
        Arrays.stream(this.initializer.getParameters()).filter(x -> !Node.class.isAssignableFrom(x.getClazz())).forEach(x -> {
            RequirementInformation modifiedInfo = new RequirementInformation(null);
            modifiedInfo = modifyRequirementInformationForParameter(requirements.stream().filter(r -> r != tciPlaceholder).collect(Collectors.toList()), modifiedInfo, x, parentInformation);

            // we must allow all terminals to still introduce other requirements that have nothing to do with patterns
            RequirementInformation modifiedInfoFulfilled = modifiedInfo.copy();
            tciPlaceholder.addProperty("PINFO", x);
            TruffleVerifyingStrategy strat = this.terminalStrategies.get(x.getClazz().getName());

            if (strat == null) {
                Logger.log(Logger.LogLevel.WARN, "Warning, no strategy configured for terminal type " + x.getClazz().getName());
                return;
            }

            strat.loadRequirements(ast, modifiedInfoFulfilled, requirementMap);

            if (!modifiedInfo.getRequirements().isEmpty()) {

                // set variables for parent
                updateVariableMap(parentInformation, requirementMap.get(ast).getRequirements());

                modifiedInfo.getRequirements().keySet().forEach(req -> {
                    if (req.getName().equals(Requirement.REQ_VALUE_RESTRICTED)) {
                        Requirement xParent = req.getProperty(Requirement.REQ_REF, Requirement.class);
                        Requirement nonMatch = Requirement.loadMatch(xParent, modifiedInfoFulfilled.getRequirements(Requirement.REQ_VALUE_RESTRICTED).stream().map(vr -> vr.getProperty(Requirement.REQ_REF, Requirement.class)).collect(Collectors.toList()));
                        if (nonMatch == null) {
                            Requirement match = Requirement.loadMatch(xParent, parentInformation.getRequirements().keySet());
                            if (match != null) {
                                NodeWrapper pos = match.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                                if (pos.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                                    pos = Requirement.findStarchild(match, pos);
                                }
                                assert pos != null;
                                if ((match.getName().equals(Requirement.REQ_ANTIPATTERN) && pos.getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                                        (match.getName().equals(Requirement.REQ_PATTERN) && !pos.getType().startsWith(Wildcard.WILDCARD_NOT)) ||
                                        !req.getProperty(Requirement.REQ_VALUE_TYPE, String.class).equals("com.oracle.truffle.api.frame.FrameSlot")) {
                                    parentInformation.fullfill(match);
                                    requirements.remove(Requirement.loadMatch(xParent, requirements));
                                }
                            }
                        }
                    }
                });

            }
        });
        matches.remove(tciPlaceholder);

        ast.getChildren().forEach(x -> {
            if (ExtendedNodeUtil.isAPINode(x)) {
                // TODO #229 we currently don't consider multi child api nodes
                if (x.getChildren().iterator().hasNext()) {
                    x = x.getChildren().iterator().next();
                } else {
                    return;
                }
            }
            if (x instanceof RepeatingNode) {
                // TODO #229 FIX because our patterns are for creation -> they don't know about the injected nodes we should do this better
                this.nonTerminalStrategy.loadRequirements(x, parentInformation, requirementMap);
                return;
            }

            // modify the info for our children
            String parameterName = findParamName(x, ast);
            // find param hard match - as backup use starts with match
            TruffleParameterInformation pInfo = Arrays.stream(this.getInitializer().getParameters()).filter(p -> p.getName().equals(parameterName)).findFirst()
                    .orElse(Arrays.stream(this.getInitializer().getParameters()).filter(p -> parameterName.startsWith(p.getName())).findFirst()
                            .orElse(Arrays.stream(this.getInitializer().getParameters()).filter(p -> p.getName().startsWith(parameterName)).findFirst().orElse(null)));
            if (pInfo == null) {
                // skip unknown parameters TODO #77
                // Logger.log(Logger.LogLevel.WARN, "Parameter " + this.getInformation().getClazz().getName() + "." + parameterName + " does not match field name. We currently can't support this (issue #77)");
                return;
            }

            RequirementInformation infoCopy = new RequirementInformation(null);
            RequirementInformation modifiedInfo = modifyRequirementInformationForParameter(requirements, infoCopy, pInfo, parentInformation);
            RequirementInformation modifiedInfoFulfilled = modifiedInfo.copy();
            if (overrideSelectorStrategy) {
                this.rootStrategy.loadRequirements(x, modifiedInfoFulfilled, requirementMap);
            } else {
                this.nonTerminalStrategy.loadRequirements(x, modifiedInfoFulfilled, requirementMap);
            }

            // set variables for parent
            updateVariableMap(parentInformation, modifiedInfoFulfilled);
            updateVariableMap(new RequirementInformation(requirements), modifiedInfoFulfilled);

            // ONLY forward the position of LTR patterns here. We want all anti-pattern locations after all
            combineSatisfiedRequirements(modifiedInfo, modifiedInfoFulfilled);
            modifiedInfo.getRequirements().keySet().forEach(req -> {
                // only satisfy requirements that were actually given to the child
                if (req.getName().equals(Requirement.REQ_ANTIPATTERN) || req.getName().equals(Requirement.REQ_PATTERN)) {
                    // deal with antipatterns
                    Requirement requirement = modifiedInfoFulfilled.getRequirements().keySet().stream().filter(z -> req.getProperty("ID", Integer.class).equals(z.getProperty("ID", Integer.class))).findFirst().orElse(null);
                    if (requirement == null) {
                        // fulfill finished pattern path
                        Requirement match = Requirement.loadMatch(req, parentInformation.getRequirements().keySet());
                        if (match != null) {
                            if (thinkOfTheChildren(parentInformation.getRequirements().keySet(), match, true)) {
                                // we must satisfy the next child of the parentage -> reloading in case parent changed is easier
                                requirements.clear();
                                requirements.addAll(loadMatchingPatterns(parentInformation.copy()));
                                removeUnsatisfiableAntipatterns(requirements);
                            } else {
                                // deal with other types
                                parentInformation.fullfill(match);
                                requirements.remove(Requirement.loadMatch(req, requirements));
                            }
                        }
                    }
                }
            });
            new ArrayList<>(requirements).forEach(req -> {
                // only satisfy requirements that were actually given to the child
                if (req.getName().equals(Requirement.REQ_ANTIPATTERN) || req.getName().equals(Requirement.REQ_PATTERN)) {
                    // deal with antipatterns and patterns
                    Requirement requirement = modifiedInfoFulfilled.getRequirements().keySet().stream().filter(z -> req.getProperty("ID", Integer.class).equals(z.getProperty("ID", Integer.class))).findFirst().orElse(null);
                    if (requirement == null) {
                        // fulfill finished pattern path
                        Requirement match = Requirement.loadMatch(req, parentInformation.getRequirements().keySet());
                        if (match != null) {
                            if (thinkOfTheChildren(parentInformation.getRequirements().keySet(), match, true)) {
                                // we must satisfy the next child of the parentage -> reloading in case parent changed is easier
                                requirements.clear();
                                requirements.addAll(loadMatchingPatterns(parentInformation.copy()));
                                removeUnsatisfiableAntipatterns(requirements);
                            } else {
                                // deal with other types
                                parentInformation.fullfill(match);
                                requirements.remove(Requirement.loadMatch(req, requirements));
                            }
                        }
                    }
                }
            });

        });

        // Fill with FAIL info
        if (!requirements.isEmpty() && !parentInformation.fullfillsAll()) {
            loadedRequirementInformation.setFailed(true);
        }
        loadedRequirementInformation.setUnmatchedRequirements(new ArrayList<>(requirements));

        return requirementMap;
    }

    /**
     * Loads the field name under which node is contained in the ast.
     *
     * @param node that is contained in ast
     * @param ast  that contains node
     * @return name of field
     */
    private String findParamName(Node node, Node ast) {
        // TODO #77 we can use this as base for the 77 code
        Class clazz = ast.getClass();
        List<Field> fields = JavaAssistUtil.getFields(clazz);

        String fieldname = fields.stream().map(f -> {
            Object o = JavaAssistUtil.safeFieldAccess(f, ast);
            if (o != null) {
                if (o.getClass().isArray() && Arrays.asList((Object[]) o).contains(node)) {
                    if (f.getName().equals("argumentNodes")) {
                        // special handling for invokes
                        return "argument_" + Arrays.asList((Object[]) o).indexOf(node);
                    }
                    return f.getName();
                }
                if (node.equals(o)) {
                    return f.getName();
                }
            }
            return null;
        }).filter(Objects::nonNull).findFirst().orElse("");

        // remove trailing _ as these are introduced by the "gen" nodes
        if (fieldname.endsWith("_")) {
            fieldname = fieldname.substring(0, fieldname.length() - 1);
        }

        return fieldname;
    }
}
