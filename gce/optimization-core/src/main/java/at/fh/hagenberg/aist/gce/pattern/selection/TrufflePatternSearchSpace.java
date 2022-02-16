/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.selection;

import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;

import java.util.*;

/**
 * This class is a part of the {@link TrufflePatternProblem} and defines the search space that patterns will be looked for
 * For re-usability purposes it is a separate class.
 *
 * @author Oliver Krauss on 02.10.2019
 */
public class TrufflePatternSearchSpace implements Iterator<Pair<NodeWrapper[], OrderedRelationship[]>> {

    /**
     * The search space is defined as a List of ALL trees in the search space
     * Every tree is represented as:
     * an array of nodes
     * an array of relationships that point from SOURCE to TARGET -> long[relationshipNo][0 == sourcePosition, 1 == targetPosition] (the positions refer to the NodeWrapper[])
     * Note that the FIRST node in a the list is also the root (treeId)
     */
    LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> searchSpace = new LinkedList<>();

    Iterator<Pair<NodeWrapper[], OrderedRelationship[]>> iterator = searchSpace.iterator();

    public TrufflePatternSearchSpace() {
    }

    /**
     * Adds a tree to the search space
     *
     * @param nodes
     * @param relationships
     */
    public void addTree(NodeWrapper[] nodes, OrderedRelationship[] relationships) {
        searchSpace.addLast(new Pair<>(nodes, relationships));
        iterator = searchSpace.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Pair<NodeWrapper[], OrderedRelationship[]> next() {
        return iterator.next();
    }

    public LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> getSearchSpace() {
        return searchSpace;
    }

    public List<NodeWrapper> asAsts() {
        ArrayList<NodeWrapper> asts = new ArrayList<>();
        for (Pair<NodeWrapper[], OrderedRelationship[]> rawAst : searchSpace) {
            NodeWrapper root = rawAst.getKey()[0];
            asts.add(root);
            for (OrderedRelationship relationship : rawAst.getValue()) {
                relationship.getParent().addChild(relationship);
            }
        }
        return asts;
    }

    public NodeWrapper toAst(Pair<NodeWrapper[], OrderedRelationship[]> rawAst) {
        NodeWrapper root = rawAst.getKey()[0];
        for (OrderedRelationship relationship : rawAst.getValue()) {
            relationship.getParent().addChild(relationship);
        }
        return root;
    }

    public void reset() {
        iterator = searchSpace.iterator();
    }

    /**
     * Reduces the search space to only contain trees that adhere to the given patterns
     *
     * @param patterns that must be contained in trees (outer list OR, inner list AND)
     */
    public void filterPatterns(List<List<TrufflePattern>> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return;
        }

        // hash all patterns
        patterns.forEach(group -> group.forEach(individual -> NodeWrapper.reHash(individual.getPatternNode())));

        LinkedList<Pair<NodeWrapper[], OrderedRelationship[]>> check = new LinkedList<>(searchSpace);

        check.forEach(unresolvedTree -> {
            // TODO #250 -> this will fail on wildcard patterns
            NodeWrapper resolvedTree = resolve(unresolvedTree);
            NodeWrapper.reHash(resolvedTree);
            // for each pattern group check if all patterns are contained in the tree, if not a single group is fulfilled remove the tree
            if (patterns.stream().noneMatch(andGroup -> andGroup.stream().allMatch(pattern -> resolvedTree.contains(pattern.getPatternNode())))) {
                searchSpace.remove(unresolvedTree);
            }
        });

        iterator = searchSpace.iterator();
    }

    private NodeWrapper resolve(Pair<NodeWrapper[], OrderedRelationship[]> unresolvedTree) {
        Map<NodeWrapper, NodeWrapper> sourceTargetMap = new HashMap<>();

        for (NodeWrapper nodeWrapper : unresolvedTree.getKey()) {
            sourceTargetMap.put(nodeWrapper, nodeWrapper.copy());
        }

        for (OrderedRelationship orderedRelationship : unresolvedTree.getValue()) {
            NodeWrapper parent = sourceTargetMap.get(orderedRelationship.getParent());
            OrderedRelationship relationship = new OrderedRelationship(parent, sourceTargetMap.get(orderedRelationship.getChild()), orderedRelationship.getField(), orderedRelationship.getOrder());
            relationship.setId(orderedRelationship.getId());
            parent.addChild(relationship);
        }

        return sourceTargetMap.get(unresolvedTree.getKey()[0]);
    }
}
