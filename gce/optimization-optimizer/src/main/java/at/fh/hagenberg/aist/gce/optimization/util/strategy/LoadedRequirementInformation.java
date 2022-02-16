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

import com.oracle.truffle.api.nodes.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for Strategies. Contains information LOADED from an existing AST for a specific Node in the AST.
 *
 * @author Oliver Krauss on 31.07.2020
 */
public class LoadedRequirementInformation {

    /**
     * Node that this RQI is for in the tree
     */
    private Node node;

    /**
     * List of Requirements that this node actually matches (patterns, antipatterns)
     * Note that this does NOT mean a failure.
     */
    private Collection<Requirement> requirements = new ArrayList<>();

    /**
     * List of Requirements that this node actually matches (patterns, antipatterns)
     * Note that this does NOT mean a failure either, just that this particular node is not fulfilled by its children.
     */
    private Collection<Requirement> unmatchedRequirements = new ArrayList<>();

    /**
     * This is the requirement information that must be used in case the node should be replaced
     */
    private RequirementInformation requirementInformation;

    /**
     * If the branch failed any of its targets
     */
    private boolean failed = false;

    public LoadedRequirementInformation(Node node) {
        this.node = node;
    }

    public LoadedRequirementInformation(Node node, Collection<Requirement> requirements, RequirementInformation requirementInformation) {
        this.node = node;
        this.requirements = requirements;
        this.requirementInformation = requirementInformation;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Collection<Requirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(Collection<Requirement> requirements) {
        this.requirements = requirements;
    }

    public RequirementInformation getRequirementInformation() {
        return requirementInformation;
    }

    public void setRequirementInformation(RequirementInformation requirementInformation) {
        this.requirementInformation = requirementInformation;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Collection<Requirement> getUnmatchedRequirements() {
        return unmatchedRequirements;
    }

    public void setUnmatchedRequirements(ArrayList<Requirement> requirements) {
        this.unmatchedRequirements = requirements;
    }
}