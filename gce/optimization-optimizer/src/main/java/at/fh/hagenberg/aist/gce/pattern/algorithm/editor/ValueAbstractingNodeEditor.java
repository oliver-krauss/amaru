/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.editor;

import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple editor that returns a tree with or without any values inside.
 * If the specialValues was set AND set to explicit it will reduce nodes to ONLY those special values
 */
public class ValueAbstractingNodeEditor extends AbstractNodeEditor<NodeWrapper> {

    /**
     * explicit
     * true = return the node as is
     * false = return just the type
     */
    private boolean explicit;

    /**
     * This editor only has a next if it wasn't returned yet
     */
    private boolean next;

    /**
     * List of values that will be kept while the value abstraction happens
     */
    private Set<String> specialValues = new HashSet<>();

    public ValueAbstractingNodeEditor(boolean explicit) {
        this.explicit = explicit;
    }

    @Override
    public void edit(NodeWrapper node) {
        super.edit(node);
        this.next = true;
    }

    @Override
    public boolean hasNext() {
        return next;
    }

    @Override
    public NodeWrapper next() {
        next = false;
        NodeWrapper nodeWrapper = node;
        if (!explicit) {
            nodeWrapper = new NodeWrapper(node.getType());
            nodeWrapper.setId(node.getId());
        } else if (!specialValues.isEmpty()) {
            nodeWrapper = new NodeWrapper(node.getType());
            nodeWrapper.setId(node.getId());
            NodeWrapper finalNodeWrapper = nodeWrapper;
            node.getValues().forEach((key, value) -> {
                if (specialValues.contains(value.toString())) {
                    finalNodeWrapper.getValues().put(key, value);
                }
            });
        }
        return nodeWrapper;
    }

    public Set<String> getSpecialValues() {
        return specialValues;
    }

    public void setSpecialValues(Set<String> specialValues) {
        this.specialValues = specialValues;
    }

    public void addSpecialValue(String specialValue) {
        this.specialValues.add(specialValue);
    }
}
