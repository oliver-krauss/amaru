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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Editor that iterates through the hierarchy and specializes for the reproduction of the
 * Energy Pattern Mining Paper in MiniC.
 * It abstracts for/while as well as data types
 */
public class EPMReproductionEditor extends AbstractNodeEditor<NodeWrapper> {

    /**
     * The lowest level the iterator will return values for
     */
    private int floor;

    /**
     * The highest level the iterator will return values for
     */
    private int ceil;

    /**
     * The hierarchy for analysis
     */
    private BitwisePatternMeta meta;

    /**
     * The stack of nodes that will be returned from bottom to top
     */
    Queue<NodeWrapper> queue;

    /**
     * Editor for abstracting values away
     */
    private ValueAbstractingNodeEditor valueEditor = new ValueAbstractingNodeEditor(false);

    public EPMReproductionEditor(BitwisePatternMeta meta, int floor, int ceil) {
        this.meta = meta;
        this.floor = floor;
        this.ceil = ceil;
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public NodeWrapper next() {
        return queue.poll();
    }

    @Override
    public void edit(NodeWrapper node) {
        queue = new LinkedList<>();
        // if explicit add this to the list
        if (floor == 0 && node.getValues() != null && !node.getValues().isEmpty()) {
            // edge case fix -> Some nodes have no values and thus would be added as identical duplicates
            // so we can only add them to the queue IF they have values
            queue.add(node);
        }

        // ensure that we do all preprocessing necessary. Hierarchy starts from level 1
        valueEditor.edit(node);
        node = valueEditor.next();
        super.edit(node);

        // load all node wrappers in the selected hierarchy levels
        String[] hierarchy = meta.hierarchy(node.getType());

        // The following generalizes ONLY the datatypes
        NodeWrapper wrapper = node.copy();
        wrapper.setType(hierarchy[hierarchy.length - 1]);
        if (wrapper.getType().contains("Int") || wrapper.getType().contains("Float")) {
            wrapper.setType(hierarchy[hierarchy.length - 2]);
        } // The following generalizes ONLY the loops
        else if (wrapper.getType().contains("MinicWhileNode") || wrapper.getType().contains("MinicForNode")) {
            wrapper.setType("at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicForNode");
        } else if ( wrapper.getType().contains("MinicForRepeatingNode")) {
            wrapper.setType("at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicRepeatingNode");
        } else {
            wrapper.setType(hierarchy[hierarchy.length - 1]);
        }

        queue.add(wrapper);
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCeil() {
        return ceil;
    }

    public void setCeil(int ceil) {
        this.ceil = ceil;
    }
}
