/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm;

import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import org.springframework.beans.factory.annotation.Required;

import java.util.Arrays;
import java.util.Stack;

/**
 * Adaption of {@link StringSubgraphIterator} to return the entire requested hierarchy.
 * <p>
 * WARNING: SubgraphIteratator.next() can return null while .hasNext says something is left.
 * When the pattern sizes are restricted, there is still searchspace left, and we only find out it is excluded after moving into it.
 *
 * Note that the Apriori Miner does NOT support Variable labelling. I've chosen to discontinue supporting it as IGOR {@link PatternGrowthPatternDetector}
 * is much more efficient. Neither does it Support wildcards for the same reason (also IGOR exclusive).
 *
 *
 * @author Oliver Krauss on 12.12.2018
 */
public class HierarchySupportingSubgraphIterator implements SubgraphIterator {

    /**
     * Sub-Iterator that produces all permutations of a tree
     */
    private NonUnpackingStringSubgraphIterator permutationIterator;

    /**
     * Pattern Metadata for producing hierarchies.
     */
    private BitwisePatternMeta meta;

    /**
     * Bottom of hierarchy to be used.
     */
    private int hierarchyFloor;

    /**
     * Top of hierarchy to be used.
     */
    private int hierarchyCeil;

    public HierarchySupportingSubgraphIterator(TrufflePatternSearchSpace searchSpace, int maxPatternSize, int hierarchyFloor, int hierarchyCeil) {
        permutationIterator = new NonUnpackingStringSubgraphIterator(searchSpace, maxPatternSize);
        // adjust for the "explicit" level by removing 1
        this.hierarchyFloor = hierarchyFloor - 1;
        this.hierarchyCeil = hierarchyCeil - 1;
    }

    /**
     * Id of currently processed tree
     *
     * @return tree Id
     */
    @Override
    public long getTreeId() {
        return permutationIterator.getTreeId();
    }

    @Override
    public boolean hasNext() {
        return permutationIterator.hasNext() || tree != null;
    }

    // current tree
    int[] tree;

    // current hierarchy-mod nodes
    int[] permutation;

    // maxLevel = how high UP we go into the hierarchy (0 is highest here)
    int maxLevel;
    // the metadata for each node
    String[][] hierarchyMeta;
    // the level we start at going up to maxLevel for each node
    int[] loadLevel;
    // the current level in the permutation generation
    int[] currentLevel;
    // currentPosition in the generation
    int pos;

    /**
     * Prepare the next permutation
     */
    private void loadNextPermutation() {
        tree = permutationIterator.next();

        if (tree != null && meta != null) {
            permutation = Arrays.stream(tree).filter(x -> x >= 0).toArray();
            maxLevel = hierarchyCeil > meta.maxHeight() ? 0 : meta.maxHeight() - hierarchyCeil;
            hierarchyMeta = new String[permutation.length][];
            loadLevel = new int[permutation.length];
            currentLevel = new int[permutation.length];
            pos = permutation.length - 1;

            // initialize the meta for each class and initialize the minimum levels for the iteration (we go UP to max)
            for (int i = 0; i < loadLevel.length; i++) {
                hierarchyMeta[i] = meta.hierarchy(permutationIterator.nodes[permutation[i]].getType());
                int startLevel = hierarchyMeta[i].length - 1 - hierarchyFloor;
                currentLevel[i] = startLevel >= 0 ? startLevel : 0;
                loadLevel[i] = maxLevel > hierarchyMeta[i].length ? hierarchyMeta[i].length - 1 : maxLevel;
            }
        }
    }

    @Override
    public PatternNodeWrapper next() {
        if (tree == null) {
            loadNextPermutation();
        }
        // if no next permutation can be loaded we are done.
        if (tree == null) {
            return null;
        }

        PatternNodeWrapper wrapper = deString(tree);

        // update current level iterations
        if (currentLevel[pos] > loadLevel[pos]) {
            incrementHierarchy();
        } else {
            // move to next pos (and skip empty)
            while (pos >= 0 && currentLevel[pos] <= loadLevel[pos]) {
                pos--;
            }
            if (pos >= 0) {
                // increment still must happen
                incrementHierarchy();
            }
        }

        if (pos < 0) {
            tree = null;
        }

        return wrapper;
    }

    private void incrementHierarchy() {
        // decrement at current position
        currentLevel[pos]--;

        // reset all other iterations
        for (int i = pos + 1; i < loadLevel.length; i++) {
            currentLevel[i] = hierarchyMeta[i].length - 1 - hierarchyFloor;
        }
        // set pos back to max
        pos = permutation.length - 1;
    }

    @Required
    public void setMeta(BitwisePatternMeta meta) {
        this.meta = meta;
    }

    /**
     * Turns a String into a graph
     *
     * @param s to be turned into graph
     * @return graph out of string.
     */
    protected PatternNodeWrapper deString(int[] s) {
        try {
            PatternNodeWrapper result = copy(s[0], permutationIterator.nodes[s[0]]);
            Stack<PatternNodeWrapper> parent = new Stack<>();
            PatternNodeWrapper currentParent = null;
            PatternNodeWrapper current = result;

            for (int i = 1; i < s.length; i++) {
                if (s[i] == permutationIterator.SUBTREE_OPEN) {
                    parent.push(current);
                    currentParent = current;
                } else if (s[i] == permutationIterator.SUBTREE_CLOSE) {
                    currentParent = parent.pop();
                    current = currentParent;
                } else {
                    current = copy(s[i], permutationIterator.nodes[s[i]]);
                    if (currentParent != null) {
                        Long parentId = currentParent.getId();
                        Long childId = current.getId();
                        OrderedRelationship rel = Arrays.stream(permutationIterator.relationshipInfo).filter(x -> x.getParent().getId().equals(parentId) && x.getChild().getId().equals(childId)).findFirst().orElse(null);
                        currentParent.addChild(new OrderedRelationship(currentParent, current, rel.getField(), rel.getOrder()));
                    }
                }
            }
            return NodeWrapper.reHash(result);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper function that copied the wrapper either explicitly or just per class
     *
     * @param wrapper to be copied
     * @return copied wrapper
     */
    private PatternNodeWrapper copy(int shortcut, NodeWrapper wrapper) {
        // find which node wrapper we need to modify
        int index = 0;
        for (int i = 0; i < permutation.length; i++) {
            if (shortcut == permutation[i]) {
                index = i;
                break;
            }
        }

        if (currentLevel[index] >= hierarchyMeta[index].length) {
            // outside range -> explicit node was requested
            return new PatternNodeWrapper(wrapper, wrapper.getId());
        } else {
            PatternNodeWrapper patternNodeWrapper = new PatternNodeWrapper(new NodeWrapper(hierarchyMeta[index][currentLevel[index]]), wrapper.getId());
            patternNodeWrapper.setId(wrapper.getId());
            return patternNodeWrapper;
        }
    }
}