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

import at.fh.hagenberg.aist.gce.optimization.util.Logger;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Current BEST implementation of iterating through all permuations of a given tree.
 * <p>
 * WARNING: SubgraphIteratator.next() can return null while .hasNext says something is left.
 * When the pattern sizes are restricted, there is still searchspace left, and we only find out it is excluded after moving into it.
 *
 * @author Oliver Krauss on 12.12.2018
 */
public class NonUnpackingStringSubgraphIterator implements Iterator<int[]> {

    private Logger logger = new Logger();

    protected static final int SUBTREE_OPEN = -128;
    protected static final int SUBTREE_CLOSE = -127;

    /**
     * Sets the maximal size of any pattern
     * Default is int max as we can't handle more nodes with int anyway
     * 1 is the minimal size, every additional node is 3 for SUBTREE_OPEN X SUBTREE_CLOSE
     */
    private long maxPatternSize = Integer.MAX_VALUE * 3L + 1;

    /**
     * List of subgraphs not yet published
     */
    protected LinkedList<int[]> subgraphs = new LinkedList<>();

    protected NodeWrapper[] nodes;

    /**
     * Trees to be searched
     */
    Iterator<Pair<NodeWrapper[], OrderedRelationship[]>> trees;

    public NonUnpackingStringSubgraphIterator(TrufflePatternSearchSpace searchSpace, int maxPatternSize) {
        // we collect all records immediately, as the permutation building takes so long, the db closes in between
        trees = searchSpace;

        if (maxPatternSize > -1) {
            maxPatternSize--;
            this.maxPatternSize = maxPatternSize * 3 + 1;
        }
    }

    public boolean hasNext() {
        return subgraphs.size() > 0 || this.trees.hasNext() || currentRelationship > 0;
    }

    /**
     * Identification of the current tree we are handling
     */
    private long treeId;

    public long getTreeId() {
        return treeId;
    }

    public int[] next() {

        // if any subgraph not yet deliver, do;
        if (subgraphs.size() > 0) {
            return subgraphs.remove(0);
        }

        // move to next relationship
        while (currentRelationship >= 0 && subgraphs.isEmpty()) {
            nextRelationship();
        }
        if (!subgraphs.isEmpty()) {
            return subgraphs.remove(0);
        }

        // if all subgraphs delivered, look at next point in db
        // create new permutations in next graph
        if (this.trees.hasNext()) {
            permute(this.trees.next());
            return subgraphs.remove(0);
        }

        // we exhausted our options. Nothing left to give
        return null;
    }


    int[][] relationships;
    OrderedRelationship[] relationshipInfo;
    ArrayList<AbstractMap.SimpleEntry<int[], Integer>>[] entryList;
    int currentRelationship = -1;
    int delivered;

    /**
     * Creates all permutations of nodes and relationships given in record.
     *
     * @param tree represented as pair of nodes and relationships source-target
     */
    private void permute(Pair<NodeWrapper[], OrderedRelationship[]> tree) {
        delivered = 0;

        // List of nodes that exist in the graph
        nodes = tree.getKey();

        // set the tree id of the tree we currently handle
        treeId = nodes[0].getId();

        /**
         * List of Entry points to add subgraphs
         * Long -> Id of node for entrance
         * List -> All subgraphs that need to be extended if Entry point gains another sub-graph
         * Pair -> Parent of tree (for deep copy entrance & hashing); Point in tree to modify for new path;
         */
        entryList = new ArrayList[nodes.length];

        // add nodes to entry list as well as subgraphs
        for (int i = 0; i < nodes.length; i++) {
            // add to entry list
            int[] graphIdentity = new int[1];
            graphIdentity[0] = i;
            ArrayList<AbstractMap.SimpleEntry<int[], Integer>> list = new ArrayList<>();
            list.add(new AbstractMap.SimpleEntry<>(graphIdentity, 1));
            entryList[i] = list;

            // add as valid permutation
            subgraphs.add(graphIdentity);
        }


        // transform relationships
        OrderedRelationship[] rels = tree.getValue();
        Map<Long, Integer>  nodePos = new HashMap<>();
        for (int i = 0; i < nodes.length; i++) {
            nodePos.put(nodes[i].getId(), i);
        }

        relationships = new int[0][2];
        relationshipInfo = rels;
        relationships = Arrays.stream(rels).map(value -> new int[]{nodePos.get(value.getParent().getId()), nodePos.get(value.getChild().getId())}).collect(Collectors.toList()).toArray(relationships);
        logger.log(Logger.LogLevel.DEBUG, "RELATIONSHIPS " + relationships.length);

        // clean entryList for irrelevant targets
        for (int i = 0; i < entryList.length; i++) {
            final int pos = i;
            if (Arrays.stream(relationships).mapToInt(rel -> rel[0]).noneMatch(r -> r == pos)) {
                entryList[i] = null;
            }
        }

        if (relationships.length > 0) {
            currentRelationship = 0;
            nextRelationship();
        } else {
            currentRelationship = -1;
        }
    }

    private void nextRelationship() {
        int[] r = relationships[currentRelationship];
        ArrayList<AbstractMap.SimpleEntry<int[], Integer>> entriesR = new ArrayList<>(entryList[r[0]]);

        // see if we still need this part of the entry list
        boolean needed = false;
        for (int j = currentRelationship + 1; j < relationships.length; j++) {
            if (relationships[j][0] == r[0]) {
                needed = true;
                break;
            }
        }
        if (!needed) {
            entryList[r[0]] = null;
        }

        // process
        entriesR.forEach(entry -> {
            if (entry.getKey().length + 3 <= maxPatternSize) {

                // create permutation
                int[] newPermutation = new int[entry.getKey().length + 3];
                System.arraycopy(entry.getKey(), 0, newPermutation, 0, entry.getValue());
                newPermutation[entry.getValue()] = SUBTREE_OPEN;
                newPermutation[entry.getValue() + 1] = r[1];
                newPermutation[entry.getValue() + 2] = SUBTREE_CLOSE;
                System.arraycopy(entry.getKey(), entry.getValue(), newPermutation, entry.getValue() + 3, entry.getKey().length - entry.getValue());
                subgraphs.add(newPermutation);

                // add new entries to list
                for (int p = 0; p < newPermutation.length; p++) {
                    int node = newPermutation[p];
                    if (newPermutation[p] > SUBTREE_CLOSE && entryList[node] != null) {
                        // Add a new possible permutation point right after the node that is the target (p+1)
                        int permutationPoint = p + 1;
                        if (newPermutation.length > permutationPoint && newPermutation[permutationPoint] == SUBTREE_OPEN) {
                            // to not invert the children we search for the last subtree joinpoint
                            int subtree = 1;
                            while (subtree > 0) {
                                permutationPoint++;
                                if (newPermutation[permutationPoint] == SUBTREE_OPEN) {
                                    subtree++;
                                } else if (newPermutation[permutationPoint] == SUBTREE_CLOSE) {
                                    subtree--;
                                }
                                if (subtree == 0) {
                                    permutationPoint++;
                                    if (newPermutation.length > permutationPoint && newPermutation[permutationPoint] == SUBTREE_OPEN) {
                                        subtree++;
                                    }
                                }
                            }
                        }
                        entryList[node].add(new AbstractMap.SimpleEntry<>(newPermutation, permutationPoint));
                    }
                }
            }
        });

        // move to next relationship
        currentRelationship++;
        delivered += subgraphs.size();
        Logger.log(Logger.LogLevel.DEBUG, "Current Permutations " + delivered + " At Rel " + currentRelationship);
        if (currentRelationship >= relationships.length) {
            currentRelationship = -1;
        }
    }
}