/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer;

import at.fh.hagenberg.aist.gce.optimization.util.JavaAssistUtil;
import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageContextProvider;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowGraph;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Truffle Entry Point strategy serves to only create nodes that can safely replace the original problem
 * It should only be used at the top level of a strategy tree.
 *
 * @author Oliver Krauss on 04.01.2019
 */
public class TruffleEntryPointStrategy implements TruffleCombinedStrategy<Node> {

    /**
     * Strategy to access the patterns in the system
     */
    private TruffleMasterStrategy rootStrategy;

    /**
     * Configuration for subtree creation
     */
    CreationConfiguration configuration;

    /**
     * Lists all the entry classes that can safely replace the node to be optimized
     */
    private List<Class> validEntryClasses = new LinkedList<>();

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<Class> chooser = new RandomChooser<>();


    /**
     * strategy used to create a subtree
     */
    private TruffleHierarchicalStrategy<Node> strategy;

    /**
     * Stack and Heap context for the entry point
     */
    private DataFlowGraph dataFlowGraph = null;

    /**
     * Context that new nodes will be created in
     * Can be null if an entirely new ast is being created.
     */
    private NodeWrapper ast;

    /**
     * raw version of AST that has not been wrapped
     */
    private Node astRaw;

    /**
     * Position that new nodes will be created at in the ast
     * Can only be null if ast is null.
     */
    private NodeWrapper injectionPoint;

    /**
     * Raw version of the injection point.
     */
    private Node injectionPointRaw;

    public TruffleEntryPointStrategy(TruffleLanguageContextProvider provider, Node ast, Node nodeToOptimize, TruffleHierarchicalStrategy<Node> strategy, CreationConfiguration configuration) {
        this.strategy = strategy;
        this.configuration = configuration;
        this.ast = ast != null ? NodeWrapper.wrap(ast) : null;
        this.astRaw = ast;
        this.injectionPoint = nodeToOptimize != null ? NodeWrapper.wrap(nodeToOptimize) : null;
        this.injectionPointRaw = nodeToOptimize;

        if (nodeToOptimize == null || nodeToOptimize.getParent() == null) {
            // there is no parent, we have no idea what a parent would require as child
            validEntryClasses.addAll(strategy.getManagedClasses());
        } else {
            // find out which field the node occupies in the parent
            Node parent = nodeToOptimize.getParent();
            Class parentClass = parent.getClass();
            Class fieldClass = null;

            // search through the fields in the class hierarchy
            while (fieldClass == null && parentClass != null) {
                for (Field field : parentClass.getDeclaredFields()) {
                    if (field.getAnnotation(Node.Child.class) != null && nodeToOptimize.equals(JavaAssistUtil.safeFieldAccess(field, parent))) {
                        // found field
                        fieldClass = field.getType();
                        break;
                    }
                    if (field.getAnnotation(Node.Children.class) != null) {
                        // possibly hiding in array
                        Object[] o = (Object[]) JavaAssistUtil.safeFieldAccess(field, parent);
                        for (Object o1 : o) {
                            if (nodeToOptimize.equals(o1)) {
                                fieldClass = field.getType().getComponentType();
                                break;
                            }
                        }
                    }
                }
                parentClass = parentClass.getSuperclass();
            }
            final Class requiredClass = fieldClass; // TODO #87 -> derive from The class info if the field class is accurate (ex. class is object, but constructor checks for specific class)

            // derive which classes are assignable from field class
            strategy.getManagedClasses().forEach(x -> {
                if (requiredClass.isAssignableFrom(x)) {
                    validEntryClasses.add(x);
                }
            });
        }
        validEntryClasses = validEntryClasses.stream().filter(x -> TruffleClassInformation.informationForClass(provider, x).getMinimalSubtreeSize() <= configuration.getMaxDepth()).collect(Collectors.toList());
    }

    /**
     * last node that was created
     */
    Node current = null;

    @Override
    public Node current() {
        if (current == null) {
            return next();
        }
        return current;
    }

    @Override
    public Node next() {
        if (validEntryClasses.size() == 0) {
            // this can essentially only happen when a class unmanaged by the TLI is selected, which only occurs on intermediate-nodes created by the language in a constructor
            Logger.log(Logger.LogLevel.WARN, "TruffleEntryPointStrategy.next() -> entry classes were empty. This should not happen");
            return null;
        }
        try {
            CreationInformation information = null;

            // do some requirements engineering
            RequirementInformation requirements = new RequirementInformation(null);
            if (strategy instanceof TruffleMasterStrategy && this.astRaw != null && this.injectionPointRaw != null) {
                Logger.log(Logger.LogLevel.INFO, "Loading requirements from patterns" + new Date());
                LoadedRequirementInformation loadedInfo = ((TruffleMasterStrategy) strategy).loadRequirements(this.astRaw, this.injectionPointRaw);
                Logger.log(Logger.LogLevel.INFO, "Finished loading requirements from patterns" + new Date());
                if (loadedInfo != null) {
                    // requirements can be missing if we attempt to mutate a class that we aren't supposed to (dispatches, ...)
                    requirements = loadedInfo.getRequirementInformation();
                }
            } else {
                RequirementInformation finalRequirements = requirements;
                dataFlowGraph.getRequiredDataItems().forEach((k, v) -> {
                    if (v != null) {
                        v.forEach(s -> finalRequirements.addRequirement(new Requirement(Requirement.REQ_DATA_WRITE).addProperty(Requirement.REQPR_SLOT, s)));
                    }
                });
            }

            // select the correct entry point that can satify the criteria
            List<Class> trialClasses = new LinkedList<>(this.validEntryClasses);
            boolean requirementsNotMet = true;
            while (requirementsNotMet && !trialClasses.isEmpty()) {
                information = new CreationInformation(ast, injectionPoint, requirements.copy(), dataFlowGraph, chooser.choose(trialClasses), 0, configuration);
                trialClasses.remove(information.getClazz());
                RequirementInformation requirementInformation = strategy.canCreateVerbose(information);
                requirementsNotMet = requirementInformation == null || !requirementInformation.fullfillsAll();
            }
            // TODO #231
//            if (strategy instanceof TruffleMasterStrategy) {
//                ((TruffleMasterStrategy) strategy).cancreates = 0;
//                ((TruffleMasterStrategy) strategy).minweights = 0;
//                ((TruffleMasterStrategy) strategy).creates = 0;
//            }
            Node node = strategy.create(information);
            // TODO #231
//            if (strategy instanceof TruffleMasterStrategy) {
//                System.out.println("Entry Can Creates: " + ((TruffleMasterStrategy) strategy).cancreates + " creates" + ((TruffleMasterStrategy) strategy).creates + "minweights " + ((TruffleMasterStrategy) strategy).minweights);
//                System.out.println("Trace Map State:");
//                TruffleMasterStrategy.traceMap.entrySet().stream().sorted(java.util.Map.Entry.comparingByValue()).forEach(x -> System.out.println("  " + x.getKey() + " " + x.getValue()));
//            }
            return node;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO #231
//        if (strategy instanceof TruffleMasterStrategy) {
//            System.out.println("Entry Can Creates: " + ((TruffleMasterStrategy) strategy).cancreates + " creates" + ((TruffleMasterStrategy) strategy).creates + "minweights " + ((TruffleMasterStrategy) strategy).minweights);
//            System.out.println("Trace Map State:");
//            TruffleMasterStrategy.traceMap.forEach((k, v) -> System.out.println("  " + k + " " + v));
//        }
        return null;
    }

    /**
     * Helper function that checks if next() will be able to return anything at all
     */
    public boolean canCreateNext() {
        if (validEntryClasses.size() == 0) {
            // this can essentially only happen when a class unmanaged by the TLI is selected, which only occurs on intermediate-nodes created by the language in a constructor
            Logger.log(Logger.LogLevel.WARN, "TruffleEntryPointStrategy.next() -> entry classes were empty. This should not happen");
            return false;
        }
        CreationInformation information = new CreationInformation(ast, injectionPoint, new RequirementInformation(null), dataFlowGraph, chooser.choose(validEntryClasses), 0, configuration);
        return strategy.canCreate(information) != null;
    }

    @Override
    public Node create(CreationInformation information) {
        return strategy.create(information);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        return strategy.canCreate(information);
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return strategy.canCreateVerbose(information);
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // NOTHING. The entry point strategy does not deal with creation by itself
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // NOTHING. The entry point strategy does not deal with creation by itself
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        if (this.strategy == null) {
            strategy.attach(strategy);
        }
    }

    @Override
    public TruffleHierarchicalStrategy<Node> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        this.rootStrategy = rootStrategy;
        return this;
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        return this.strategy.loadRequirements(ast, parentInformation, requirementMap);
    }

    @Override
    public Collection<Class> getManagedClasses() {
        return validEntryClasses;
    }

    @Override
    public double minWeight(CreationInformation information) {
        return strategy.minWeight(information);
    }

    public void setChooser(ChooseOption<Class> chooser) {
        this.chooser = chooser;
    }

    /**
     * You MUST set this if you attempt to create subtrees instead of completely new trees
     *
     * @param dataFlowGraph
     */
    public void setDataFlowGraph(DataFlowGraph dataFlowGraph) {
        this.dataFlowGraph = dataFlowGraph;
    }
}
