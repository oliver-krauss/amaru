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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Editor that iterates through the hierarchy
 */
public class HierarchySupportingNodeEditor extends AbstractNodeEditor<NodeWrapper> {

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

    public HierarchySupportingNodeEditor(BitwisePatternMeta meta, int floor, int ceil) {
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
        reverse(hierarchy, hierarchy.length);
        int level = floor;
        int pos = Math.min(hierarchy.length, ceil);
        while (pos > 0 && level <= ceil) {
            pos--;
            level++;
            NodeWrapper wrapper = node.copy();
            wrapper.setType(hierarchy[pos]);
            queue.add(wrapper);
        }
    }

    static void reverse(String a[], int n)
    {
        int i;
        String k, t;
        for (i = 0; i < n / 2; i++) {
            t = a[i];
            a[i] = a[n - i - 1];
            a[n - i - 1] = t;
        }
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
