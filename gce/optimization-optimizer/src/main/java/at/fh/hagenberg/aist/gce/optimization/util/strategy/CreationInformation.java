/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DataFlowGraph;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import com.oracle.truffle.api.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * The creation information class is a helper class containing information on the currently being built tree
 * For now it only contains the current currentDepth,
 * but in the future it will contain information such as the amount of nodes already existing, amount of loops, etc.
 *
 * @author Oliver Krauss on 07.11.2018
 */

public class CreationInformation {

    /**
     * The ast providing the context that will be created in.
     * Can be null if we create a completely new AST
     */
    protected NodeWrapper ast;

    /**
     * The exact point that will be injected in (in a crossover this is the crossover point; In a mutation this is the mutate point)
     * Can only be null if AST is null.
     */
    protected NodeWrapper injectionPoint;

    /**
     * Class that should be created
     */
    protected Class clazz;

    /**
     * Specific class information that was requested
     * Is usually null, and only introduced by the strategy to tell sub-strategies -> We need EXACTLY this.
     */
    protected TruffleClassInformation information;

    /**
     * Current currentDepth of subtree creation
     */
    protected int currentDepth;

    /**
     * (Approximated) Weight of currently generated subtree (starts at 0)
     * Note that only weight-sensitive strategies modify this value
     */
    protected double currentWeight = 0;

    /**
     * Data flow that must be satisfied in this creation, e.g. graph can't have dangling edges to READS
     */
    protected DataFlowGraph dataFlowGraph;

    protected CreationConfiguration configuration;

    /**
     * Requirements that MUST be met while creating
     * This can overlap with given information (ex. DataFlowGraph), where the given information is just to enable specific code logic
     */
    protected RequirementInformation requirements = new RequirementInformation(null);

    protected void setDataFlowGraph(DataFlowGraph dataFlowGraph) {
        this.dataFlowGraph = dataFlowGraph;
        if (this.dataFlowGraph == null) {
            this.dataFlowGraph = new DataFlowGraph(null, new HashMap<>(), new HashMap<>(), null);
        }
    }

    public CreationInformation(NodeWrapper ast, NodeWrapper injectionPoint, RequirementInformation requirements, DataFlowGraph dataFlowGraph, Class clazz, int currentDepth, CreationConfiguration configuration) {
        this.ast = ast;
        this.injectionPoint = injectionPoint;
        this.requirements = requirements == null ? new RequirementInformation(null) : requirements;
        setDataFlowGraph(dataFlowGraph);
        this.clazz = clazz;
        this.currentDepth = currentDepth;
        this.configuration = configuration;
    }

    public CreationInformation(NodeWrapper ast, NodeWrapper injectionPoint, RequirementInformation requirements, DataFlowGraph dataFlowGraph, Class clazz, int currentDepth, CreationConfiguration configuration, double currentWeight) {
        this(ast, injectionPoint, requirements, dataFlowGraph, clazz, currentDepth, configuration);
        this.currentWeight = currentWeight;
    }

    public CreationInformation(NodeWrapper ast, NodeWrapper injectionPoint, RequirementInformation requirements, DataFlowGraph dataFlowGraph, Class clazz, int currentDepth, CreationConfiguration configuration, double currentWeight, TruffleClassInformation information) {
        this(ast, injectionPoint, requirements, dataFlowGraph, clazz, currentDepth, configuration, currentWeight);
        this.information = information;
    }

    public Class getClazz() {
        return clazz;
    }

    public CreationInformation setClazz(Class clazz) {
        this.clazz = clazz;
        return this;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }

    public CreationInformation setCurrentDepth(int currentDepth) {
        this.currentDepth = currentDepth;
        return this;
    }

    public CreationConfiguration getConfiguration() {
        return configuration;
    }

    public CreationInformation setConfiguration(CreationConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public CreationInformation setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
        return this;
    }

    public TruffleClassInformation getInformation() {
        return information;
    }

    public CreationInformation setInformation(TruffleClassInformation information) {
        this.information = information;
        return this;
    }

    public DataFlowGraph getDataFlowGraph() {
        return dataFlowGraph;
    }

    public CreationInformation setRequirements(RequirementInformation requirements) {
        this.requirements = requirements;
        return this;
    }

    public RequirementInformation getRequirements() {
        return requirements;
    }

    public NodeWrapper getAst() {
        return ast;
    }

    public NodeWrapper getInjectionPoint() {
        return injectionPoint;
    }

    /**
     * Provides a copy of this object. All values may be modified without touching the original
     *
     * @return Copy of the creation information (CreationConfiguration stays original!)
     */
    public CreationInformation copy() {
        return new CreationInformation(this.ast, this.injectionPoint, this.requirements.copy(), this.dataFlowGraph, this.clazz, this.currentDepth, this.configuration, this.currentWeight, this.information);
    }

    @Override
    public String toString() {
        return "CreationInformation{" +
                "clazz=" + clazz +
                ", information=" + (information != null ? information.toString() : "null") +
                ", currentDepth=" + currentDepth +
                ", currentWeight=" + currentWeight +
                ", dataFlowGraph=" + dataFlowGraph +
                ", configuration=" + (configuration != null ? configuration.toString() : "null") +
                ", requirements=" + (requirements != null ? requirements.toString() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreationInformation)) return false;
        CreationInformation that = (CreationInformation) o;
        return currentDepth == that.currentDepth &&
                Double.compare(that.currentWeight, currentWeight) == 0 &&
                Objects.equals(clazz, that.clazz) &&
                ((information == null && that.information == null) || (information != null && that.information != null && Objects.equals(information.getClazz(), that.information.getClazz()))) &&
                Objects.equals(dataFlowGraph, that.dataFlowGraph) &&
                Objects.equals(configuration, that.configuration) &&
                Objects.equals(requirements, that.requirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, information, currentDepth, currentWeight, dataFlowGraph, configuration, requirements);
    }

    public CreationInformation incCurrentDepth() {
        this.currentDepth++;
        return this;
    }
}
