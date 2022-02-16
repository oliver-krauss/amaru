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

import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import com.oracle.truffle.api.nodes.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class implementing both obeservable and observer. All strategies that Observe, should also be able to be observed
 *
 * @author Oliver Krauss on 21.11.2018
 */
public abstract class DefaultObservableAndObserverStrategy extends DefaultObservableStrategy implements TruffleStrategyObserver {


    /**
     * list of all strategies being observed for (in)validation
     */
    protected List<TruffleObservableStrategy> observing = new ArrayList<>();

    @Override
    public void extended(TruffleObservableStrategy strategy) {
        // per default don't propagate as this leads to endless loops
    }

    @Override
    public void restricted(TruffleObservableStrategy strategy) {
        // per default don't propagate as this leads to endless loops
    }

    @Override
    public void disabled(TruffleObservableStrategy strategy) {
        // one single dependent strategy is USUALLY enough to invalidate this class. If not override this.
        notifyDisable();
    }

    @Override
    public void enabled(TruffleObservableStrategy strategy) {
        // if all dependencies are valid, re-validate this class. If class has further dependencies override this.
        if (observing.stream().allMatch(x -> !x.isDisabled())) {
            notifyEnable();
        }
    }

    /**
     * Helper function subscribing to another strategy
     *
     * @param observable to be observed
     */
    protected void subscribe(TruffleObservableStrategy observable) {
        observable.register(this);
        observing.add(observable);
    }

    /**
     * Helper function subscribing to another strategy, and propagating the disabled state
     *
     * @param observable to be observed (if observable.isDisabled -> this class will also be disabled)
     */
    protected void subscribeAndValidate(TruffleObservableStrategy observable) {
        if (observable.isDisabled()) {
            this.disabled = true;
        }
        observable.register(this);
        observing.add(observable);
    }

    /**
     * Combines the returned requirement information of all parameters into one requirement for the parent strategy
     *
     * @param combinedRequirements the combined result
     * @param canCreate            the information returned by the sub-strategy
     * @return combined DOF for all child-paths
     */
    protected RequirementInformation combine(RequirementInformation combinedRequirements, RequirementInformation canCreate) {
        if (canCreate == null) {
            return null;
        }
        canCreate.getRequirements().forEach((k, v) -> {
            int increment = v;
            if (k.containsProperty("FORWARDPOINTER")) {
                increment = 0;
                k.addProperty(Requirement.REQPR_PATTERN_LTRPOS, k.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) + 1);
                k.getProperties().remove("FORWARDPOINTER");
            }
            Requirement match = Requirement.loadMatch(k, combinedRequirements);
            if (match != null) {
                int finalIncrement = increment;
                combinedRequirements.getRequirements().entrySet().stream().filter(x -> x.getKey().equals(match)).forEach(x -> x.setValue(x.getValue() + finalIncrement));
            } else {
                combinedRequirements.getRequirements().put(k, increment);
            }
        });
        return combinedRequirements;
    }

    /**
     * Finds unskippable branch from right order siblings
     *
     * @param sibling to check
     * @return true if skippable
     */
    protected boolean findSkippable(NodeWrapper sibling, boolean antipattern) {
        boolean notWildcard = antipattern == sibling.getType().startsWith(Wildcard.WILDCARD_NOT);
        if (notWildcard || (!sibling.getChildren().isEmpty() && sibling.getChildren().stream().noneMatch(x -> findSkippable(x.getChild(), antipattern)))) {
            return false;
        }

        return true;
    }

    /**
     * Similar action as combine requirements, but instead of using it for the "canCreate" it is used by "create"
     * to remove requirements that were satisfied, and thus don't need to be considered anymore by following nodes.
     *
     * @param requirements to be propagated up
     * @param requirements that were just satisfied
     */
    protected RequirementInformation combineSatisfiedRequirements(RequirementInformation requirements, RequirementInformation satisfied) {
        return combineSatisfiedRequirements(requirements, satisfied.getRequirements().keySet(), false);
    }

    /**
     * Similar action as combine requirements, but instead of using it for the "canCreate" it is used by "create"
     * to remove requirements that were satisfied, and thus don't need to be considered anymore by following nodes.
     *
     * @param requirements to be propagated up
     * @param requirements that were just satisfied
     */
    protected RequirementInformation combineSatisfiedRequirements(RequirementInformation requirements, Collection<Requirement> satisfied, boolean allowInc) {
        updateVariableMap(requirements, satisfied);

        // clear out everything that isn't a pattern
        satisfied.removeIf(x -> !(x.getName().equals(Requirement.REQ_ANTIPATTERN) || x.getName().equals(Requirement.REQ_PATTERN)));
        List<Map.Entry<Requirement, Integer>> stored = new ArrayList();
        requirements.getRequirements().entrySet().removeIf(x -> {
            if (!(x.getKey().getName().equals(Requirement.REQ_ANTIPATTERN) || x.getKey().getName().equals(Requirement.REQ_PATTERN))) {
                stored.add(x);
                return true;
            }
            return false;
        });

        // cleanup ltr positions satisfied by star child relationship
        satisfied.removeIf(x -> {
            Integer pos = loadWildcardDependent(x, Requirement.REQPR_PATTERN_LTRPOS);
            if (pos != null) {
                Integer maxPos = loadWildcardDependent(x, Requirement.REQPR_PATTERN_LTRPOS_MAX);
                if (maxPos == pos) {
                    // also check upwards if there is a parent that must have its position increased
                    Requirement match = Requirement.loadMatch(x, requirements);
                    if (match != null) {
                        match.addProperty(Requirement.REQPR_PATTERN_LTRPOS, maxPos);
                    }
                    return true;
                }
            }
            return false;
        });

        // cleanup ltr positions already finished by child relationship
        requirements.getRequirements().keySet().removeIf(x -> {
            Integer pos = loadWildcardDependent(x, Requirement.REQPR_PATTERN_LTRPOS);
            if (pos != null) {
                Integer maxPos = loadWildcardDependent(x, Requirement.REQPR_PATTERN_LTRPOS_MAX);
                if (maxPos == pos) {
                    return true;
                }
            }
            return false;
        });

        Collection<Requirement> remove = new ArrayList<>();
        Set<Requirement> rqCopy = new HashSet<>(requirements.getRequirements().keySet());
        rqCopy.forEach(requirement -> {
            if (!satisfied.contains(requirement) &&
                    requirement.getProperty(Requirement.REQ_PATTERN_MATCH_TYPE, String.class) == null &&
                    !thinkOfTheChildren(requirements.getRequirements().keySet(),
                            satisfied.stream().filter(x -> x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(requirement.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class))
                                    && x.getProperty("ID", Integer.class).equals(requirement.getProperty("ID", Integer.class))).findFirst().orElse(requirement), allowInc)
            ) {
                remove.add(requirement);
            }
        });
        remove.forEach(requirements.getRequirements().keySet()::remove);

        stored.forEach(x -> requirements.getRequirements().put(x.getKey(), x.getValue()));
        // re-inject the removed values
        return requirements;
    }

    protected void updateVariableMap(RequirementInformation requirements, RequirementInformation satisfied) {
        updateVariableMap(requirements, satisfied.getRequirements().keySet());
    }

    protected void updateVariableMap(RequirementInformation requirements, Collection<Requirement> satisfied) {
        // map created variables into parent
        satisfied.stream().filter(x -> x.getName().equals(Requirement.REQ_PATTERN_VAR_PLACEHOLDER)).forEach(requirement -> {
            Map varMap = requirement.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
            Requirement parentReq = Requirement.loadMatch(requirement.getProperty(Requirement.REQ_REF, Requirement.class), requirements);
            if (parentReq != null) {
                Map parentMap = parentReq.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class);
                if (parentMap == null) {
                    parentReq.addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, varMap);
                } else {
                    varMap.forEach(parentMap::putIfAbsent);
                }
            }
            // propagate UP (with free DOF as this is a purely informal req)
            requirements.addRequirement(requirement, 1000);
        });
    }

    /**
     * Similar action as combine requirements, but instead of using it for the "canCreate" it is used by "create"
     * to remove requirements that were satisfied, and thus don't need to be considered anymore by following nodes.
     *
     * @param requirements to be propagated up
     * @param requirements that were just satisfied
     */
    protected Collection<Requirement> combineRequirementsFullfilledByChild(Collection<Requirement> requirements, Requirement satisfied) {
        thinkOfTheChildren(requirements, satisfied, false);
        return requirements;
    }

    /**
     * Helper function loading the position information considering wildcards
     *
     * @param x    requirement to load from
     * @param type what to load
     * @return value being loaded
     */
    protected Integer loadWildcardDependent(Requirement x, String type) {
        if (x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
            // return the current wildcard pos
            return x.getProperty(type, Integer.class);
        } else {
            // return the parent info or the current info if no parent is present
            return x.containsProperty(Requirement.REQ_REF) ? x.getProperty(Requirement.REQ_REF, Requirement.class).getProperty(type, Integer.class) : x.getProperty(type, Integer.class);
        }
    }

    /**
     * If a sibling exists it moves the function pointer forward to that child
     *
     * @param requirements
     * @param satisfied
     * @return
     */
    protected boolean thinkOfTheChildren(Collection<Requirement> requirements, Requirement satisfied, boolean inc) {
        Integer pos = loadWildcardDependent(satisfied, Requirement.REQPR_PATTERN_LTRPOS);
        if (pos != null) {
            // move positional pointer forward
            Integer maxPos = loadWildcardDependent(satisfied, Requirement.REQPR_PATTERN_LTRPOS_MAX);
            if (maxPos - 1 == pos && inc) {
                // if last pos reached, remove requirement
                requirements.removeIf(requirement -> {
                    if (satisfied.equals(requirement) && requirement.getProperty(Requirement.REQ_PATTERN_MATCH_TYPE, String.class) == null) {
                        Requirement property = satisfied.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
                        // tell parent that we are done
                        if (property != null) {
                            property.addProperty(Requirement.REQPR_PATTERN_LTRPOS, pos + 1);
                        } else if (satisfied == requirement) {
                            // prevent a requirement becoming unresolvable by ANYWHERE containment
                            satisfied.addProperty(Requirement.REQPR_PATTERN_LTRPOS, pos + 1);
                            return false;
                        }
                        return true;
                    } else if (satisfied.equals(requirement.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class))) {
                        Requirement property = satisfied.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
                        // tell parent that we are done
                        if (property != null) {
                            property.addProperty(Requirement.REQPR_PATTERN_LTRPOS, pos + 1);
                        } else if (satisfied == requirement) {
                            // prevent a requirement becoming unresolvable by ANYWHERE containment
                            satisfied.addProperty(Requirement.REQPR_PATTERN_LTRPOS, pos + 1);
                            return false;
                        }
                        return true;
//                        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFfff");
                    }
                    return false;
                });
            } else {
                Integer id = satisfied.getProperty("ID", Integer.class);
                NodeWrapper pattern = satisfied.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class);
                // move forward to actual requirement
                requirements.stream().filter(x ->
                        x.containsProperty("ID") && x.containsProperty(Requirement.REQPR_PATTERN) && x.getProperty("ID", Integer.class).equals(id) && x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(pattern)).forEach(x -> {
                            int newPos = inc ? pos + 1 : pos;

                            // forward * wildcard, otherwise just forward the LTR owner (in case of n child with l children we must not forward current requirement)
                            if (x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getType().equals(Wildcard.WILDCARD_ANYWHERE) && x.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX)) {
                                x.addProperty(Requirement.REQPR_PATTERN_LTRPOS, newPos);
                                return;
                            }

                            // check if we are in a sub-space
                            if (satisfied.containsProperty(Requirement.REQPR_PATTERN_LTROWNER) &&
                                    satisfied.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class).getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class)
                                            .equals(x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class))) {
                                if (maxPos - 1 == pos) {
                                    if (!x.containsProperty(Requirement.REQPR_PATTERN_LTRPOS)) {
                                        x = x.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
                                    }
                                    int parentNewPos = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) + 1;
                                    x.addProperty(Requirement.REQPR_PATTERN_LTRPOS, parentNewPos);
                                    System.out.println("NEW POS " + parentNewPos);
                                    NodeWrapper antipattern = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                                    List<OrderedRelationship> collect = antipattern.getChildren(x.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class)).stream().filter(c -> c.getOrder() == (parentNewPos)).collect(Collectors.toList());
                                    if (collect.size() == 1) {
                                        x.addProperty(Requirement.REQPR_PATTERN_POS, collect.get(0).getChild());
                                    } else {
                                        throw new RuntimeException("We don't support multiple-same position pattern options");
                                    }
                                }
                                return;
                            }

                            // increment parent & move forward the class of x
                            Requirement property = x.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
                            if (property != null) {
                                // we are in a regular node.
                                property.addProperty(Requirement.REQPR_PATTERN_LTRPOS, newPos);
                                NodeWrapper antipattern = property.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
                                List<OrderedRelationship> collect = antipattern.getChildren(property.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class)).stream().filter(c -> c.getOrder() == (newPos)).collect(Collectors.toList());
                                if (collect.size() == 1) {
                                    x.addProperty(Requirement.REQPR_PATTERN_POS, collect.get(0).getChild());
                                } else {
                                    throw new RuntimeException("We don't support multiple-same position pattern options");
                                }
                            } else if (x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).equals(satisfied.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class))) {
                                // forward ONLY (never INC) in special case -> star wildcard ownership (possibly without being owned by another node)
                                x.addProperty(Requirement.REQPR_PATTERN_LTRPOS, pos);
                                if (!x.containsProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX)) {
                                    x.addProperty(Requirement.REQPR_PATTERN_LTRFIELD, satisfied.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class));
                                    x.addProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, maxPos);
                                }
                            }
                            // no else - in a star wildcard simply moving the pos forward is enough
                        }
                );
            }
            return true;
        } else if (satisfied.containsProperty(Requirement.REQPR_PATTERN_LTROWNER)) {
            Requirement parent = satisfied.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);

            // check if we are in a sub-space
            requirements.forEach(x -> {
                if (x.containsProperty(Requirement.REQPR_PATTERN_LTROWNER) && parent.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class)
                        .equals(x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class))) {
                    Integer maxPos = loadWildcardDependent(parent, Requirement.REQPR_PATTERN_LTRPOS_MAX);
                    Integer forwardPos = loadWildcardDependent(parent, Requirement.REQPR_PATTERN_LTRPOS);
                    if (maxPos - 1 == forwardPos) {
                        // This is really ugly but I don't know how to fix it otherwise - edge case if LTR-LTR sandwich we must increment by 1 instead of move forward
                        parent.addProperty(Requirement.REQPR_PATTERN_LTRPOS, x.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class).getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class) + 1);
                    }
                }
            });

            // we must consider a star wildcard being child of a multi-child-relationship pattern
            return thinkOfTheChildren(requirements, parent, inc);
        }
        return false;
    }

    /**
     * Calculates the MINIMAL amount of nodes to the right that are still needed to fulfill a pattern
     *
     * @param x Requirement (pattern)
     * @return how many nodes are still needed to the right
     */
    protected int requiredSizeToRight(Requirement x) {
        Integer max = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS_MAX, Integer.class);
        NodeWrapper node = x.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class);
        boolean antipattern = x.getName().equals(Requirement.REQ_ANTIPATTERN);

        if (node == null) {
            return 0;
        }

        if (max != null) {
            Integer current = x.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
            // remove already preceded siblings from equation
            max = max - current;
            return max;
        } else if (x.containsProperty(Requirement.REQPR_PATTERN_LTROWNER)) {
            // also consider the owners size requirements above
            Requirement parent = x.getProperty(Requirement.REQPR_PATTERN_LTROWNER, Requirement.class);
            Integer current = parent.getProperty(Requirement.REQPR_PATTERN_LTRPOS, Integer.class);
            if (current != null) {
                String field = parent.getProperty(Requirement.REQPR_PATTERN_LTRFIELD, String.class);

                // calculate siblings of parent if we have an anywhere wildcard
                int maxParent = (int) parent.getProperty(Requirement.REQPR_PATTERN_POS, NodeWrapper.class).getChildren().stream()
                        .filter(c -> current < c.getOrder() && ((field == null && c.getField() == null) || (field != null && field.equals(c.getField()))) && !findSkippable(c.getChild(), antipattern)).count();
                return maxParent + (findSkippable(node, antipattern) ? 0 : 1);
            }
        }
        return findSkippable(node, antipattern) ? 0 : 1;
    }

    /**
     * For every node return the patterns / antipatterns that the node currently matches
     *
     * @param ast               to be turned into pattern/antipattern list
     * @param parentInformation the info of the parent to consider
     * @param requirementMap    currently collected requirements
     * @return patterns/antipatterns for every node
     */
    public abstract Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap);

    /**
     * Loads the parent of the node that we can process (e.g. skipping truffle nodes)
     *
     * @param node whose parent we need to identify
     * @return parent node
     */
    protected Node loadParent(Node node) {
        Node parent = node.getParent();
        while (ExtendedNodeUtil.isAPINode(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }
}
