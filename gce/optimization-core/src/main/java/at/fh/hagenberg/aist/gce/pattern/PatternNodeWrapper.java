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

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;

import java.util.*;

/**
 * @author Oliver Krauss on 07.07.2020
 */

public class PatternNodeWrapper extends NodeWrapper {

    /**
     * Nodes corresponding to exactly this Pattern Node
     */
    protected Set<Long> matchedNodes = new LinkedHashSet<>();

    private void copyPrototype(NodeWrapper proto) {
        id = proto.getId();
        type = proto.getType();
        values = new HashMap<>(proto.getValues());
        hash = proto.getHash();
        children = new TreeSet<>();
        proto.getChildren().forEach(x -> {
            NodeWrapper child = x.getChild();
            PatternNodeWrapper childCopy = (child instanceof PatternNodeWrapper) ? new PatternNodeWrapper(child, ((PatternNodeWrapper) child).getMatchedNodes()) : new PatternNodeWrapper(child, Collections.emptyList());
            children.add(new OrderedRelationship(this, childCopy, x.getField(), x.getOrder()));
        });
    }

    public PatternNodeWrapper(NodeWrapper proto, Long matchedNode) {
        copyPrototype(proto);
        matchedNodes.add(matchedNode);
    }

    public PatternNodeWrapper(NodeWrapper proto, Collection<Long> matchedNodes) {
        copyPrototype(proto);
        this.matchedNodes.addAll(matchedNodes);
    }

    public Set<Long> getMatchedNodes() {
        return matchedNodes;
    }

    public void setMatchedNodes(Set<Long> matchedNodes) {
        this.matchedNodes = matchedNodes;
    }

    /**
     * Returns all matches from all nodes in the subgraph
     *
     * @return all node matches in graph
     */
    protected Set<Long> getAllMatchedNodes() {
        Set<Long> nodes = new HashSet<>();
        nodes = this.getAllMatchedNodes(nodes);
        return nodes;
    }

    /**
     * Returns all matches from all nodes in the subgraph
     *
     * @param ids all node matches in graph
     * @return all node matches in graph
     */
    protected Set<Long> getAllMatchedNodes(Set<Long> ids) {
        ids.addAll(this.matchedNodes);
        children.forEach(x -> ((PatternNodeWrapper) x.getChild()).getAllMatchedNodes(ids));
        return ids;
    }

    @Override
    public NodeWrapper copy() {
        return new PatternNodeWrapper(this, matchedNodes);
    }

    /**
     * Adds all ids from given pattern node to self
     *
     * @param patternNode to load ids from
     */
    public void addIds(PatternNodeWrapper patternNode) {
        this.matchedNodes.addAll(patternNode.getMatchedNodes());
        Iterator<OrderedRelationship> selfIt = this.children.iterator();
        Iterator<OrderedRelationship> addIt = patternNode.children.iterator();
        // add all ids
        while (selfIt.hasNext()) {
            ((PatternNodeWrapper) selfIt.next().getChild()).addIds((PatternNodeWrapper) addIt.next().getChild());
        }
    }

}
