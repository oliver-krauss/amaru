/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern;

import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Truffle Pattern Solution is one pattern occuring in an AST of NodeWrappers
 *
 * @author Oliver Krauss on 28.11.2018
 */
public class TrufflePattern {

    /**
     * Counts how often the pattern occurs in the Knowledge Base (total permutations)
     */
    public long count;

    /**
     * Size of the pattern (amount of nodes in it)
     */
    protected int size;

    /**
     * All tree roots in database that contain this specific pattern. (unique)
     * Is the sum of all nodeIds of patternNode and children
     */
    protected Set<Long> treeIds = new HashSet<>();

    /**
     * All nodes in database that are part of this specific pattern. (unique)
     * Is the sum of all nodeIds of patternNode and children
     */
    protected Set<Long> nodeIds = new HashSet<>();

    /**
     * Root node of Pattern to be able to find it in DB again
     */
    protected PatternNodeWrapper patternNode;

    /**
     * Original Bit Pattern for fast comparison operations ONLY available if mined via the PatternGrowthAlgorithm currently.
     */
    protected TracableBitwisePattern bitRepresentation;

    protected TrufflePattern() {
    }

    public TrufflePattern(Long treeId, PatternNodeWrapper patternNode) {
        this.patternNode = patternNode;
        this.size = patternNode.treeSize();
        this.treeIds.add(treeId);
        this.nodeIds = patternNode.getAllMatchedNodes();
        this.count = patternNode.matchedNodes.size();
    }

    /**
     * Constructs a regular truffle pattern for the UI from the tracable bitwise pattern
     * Note that you need the search space to accurately create the pattern
     * @param bitwisePattern the pattern to construct
     * @param patternNode    the pattern node representing the pattern
     */
    public TrufflePattern(TracableBitwisePattern bitwisePattern, PatternNodeWrapper patternNode) {
        this.patternNode = patternNode;
        this.size = bitwisePattern.getNodeIds().length;
        this.count = bitwisePattern.getCount();

        // move over tree ids
        this.treeIds.addAll(bitwisePattern.getTreeIds());
        // move over node ids
        for (int i = 0; i < bitwisePattern.getCount(); i++) {
            for (int j = 0; j < bitwisePattern.getNodeIds().length; j++) {
                this.nodeIds.add(bitwisePattern.getNodeIds()[j][i]);
            }
        }
    }

    /**
     * Constructs a regular truffle pattern for the UI from the tracable bitwise pattern
     * Note that you need the search space to accurately create the pattern
     * @param bitwisePattern the pattern to construct
     * @param patternNode    the pattern node representing the pattern
     * @param cluster        the cluster that shall be observed in the pattern
     */
    public TrufflePattern(TracableBitwisePattern bitwisePattern, PatternNodeWrapper patternNode, int cluster) {
        this.patternNode = patternNode;
        this.size = bitwisePattern.getNodeIds().length;
        this.count = bitwisePattern.getClusterCount(cluster);

        // move over node permutations
        for (int i = 0; i < bitwisePattern.getCount(); i++) {
            if (bitwisePattern.getClusterId()[i] == cluster) {
                this.treeIds.add(bitwisePattern.getTreeId()[i]);
                for (int j = 0; j < bitwisePattern.getNodeIds().length; j++) {
                    this.nodeIds.add(bitwisePattern.getNodeIds()[j][i]);
                }
            }
        }
    }


    public void addTree(Long treeId, PatternNodeWrapper patternNode) {
        this.count += patternNode.matchedNodes.stream().filter(x -> !this.nodeIds.contains(x)).count();
        this.treeIds.add(treeId);
        this.nodeIds.addAll(patternNode.getAllMatchedNodes());
        this.patternNode.addIds(patternNode);
    }

    public long getCount() {
        return count;
    }

    /**
     * @return how many trees this pattern occurs in
     */
    public long getTreeCount() {
        return treeIds.size();
    }

    public PatternNodeWrapper getPatternNode() {
        return patternNode;
    }

    public Set<Long> getTreeIds() {
        return treeIds;
    }

    public Set<Long> getNodeIds() {
        return nodeIds;
    }

    public int getSize() {
        return size;
    }

    public TracableBitwisePattern getBitRepresentation() {
        return bitRepresentation;
    }

    public void setBitRepresentation(TracableBitwisePattern bitRepresentation) {
        this.bitRepresentation = bitRepresentation;
    }
}
