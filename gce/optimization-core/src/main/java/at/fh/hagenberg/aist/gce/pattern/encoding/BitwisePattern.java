/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.encoding;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.util.Pair;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Class representing a Truffle pattern in a bit array.
 * This concerns ONLY the pattern not any database information (e.g. what trees this came from ...)
 *
 * @author Oliver Krauss on 11.04.2019
 */
public class BitwisePattern {

    /**
     * Meta information that this pattern adheres to
     */
    protected BitwisePatternMeta meta;

    /**
     * The tree contained in a DFS way
     * each entry is a meta.mask();
     */
    protected long[] pattern;

    /**
     * The open and close tags as separate structure with 0 = open and 1 = close
     * The amount of tags required is 2 bits per node (-2 for the root node)
     * Ex. 000111 -> A->B->C->D
     * Ex. 010101 -> A->(B,C,D)
     */
    protected long[] openclosetags;

    /**
     * Amount of nodes in this pattern
     */
    protected int size;

    protected BitwisePattern() {
        // protected constructor for copy purposes
    }

    public BitwisePattern(NodeWrapper node, BitwisePatternMeta meta) {
        this.size = node.treeSize();
        initDataStructures(size);
        this.meta = meta;
        pattern = dfs(new long[size], node);
    }

    /**
     * As the root class handles the pattern creation this allows sub-classes to init before dfs happens
     *
     * @param size the size the pattern has (amount of nodes in it)
     */
    protected void initDataStructures(int size) {
        openclosetags = new long[(int) Math.ceil((size - 1) / 32.0)];
    }

    /**
     * Checks if ast is a specialization of this (this is a generalization of ast)
     * It can only be a specialization IF they are structurally equivalent
     *
     * @param otherPattern possible specialization of this
     * @return true if pattern is a specialization
     */
    public boolean generalizes(BitwisePattern otherPattern) {
        if (pattern.length != otherPattern.pattern.length || !Arrays.equals(openclosetags, otherPattern.openclosetags)) {
            // not same size -> not structurally equal       // not structurally equal structure
            return false;
        }

        // check masks
        for (int i = 0; i < pattern.length; i++) {
            long size = meta.maskSize(pattern[i]);
            if (pattern[i] >>> (64 - size) != otherPattern.pattern[i] >>> (64 - size)) {
                return false;
            }
        }

        return true;
    }

    protected static final int reverseStartingPos = 63;
    protected int nodePos = 0;
    protected int openClosePos = reverseStartingPos;
    protected int openCloseLong = 0;

    protected long[] dfs(long[] pattern, NodeWrapper node) {
        // add mask
        pattern[nodePos] = meta.mask(node.getType());

        // add children
        for (OrderedRelationship orderedRelationship : node.getChildren()) {
            NodeWrapper child = orderedRelationship.getChild();

            // create closing tag by leaving 0 at pos - increment to next pos
            openClosePos--;
            if (openClosePos < 0) {
                openClosePos = reverseStartingPos;
                openCloseLong++;
            }

            // move to next tree position
            nodePos++;

            // write children
            pattern = dfs(pattern, child);

            // create closing tag by putting 1 at pos - increment to next pos
            openclosetags[openCloseLong] |= 1L << openClosePos;
            openClosePos--;
            if (openClosePos < 0) {
                openClosePos = reverseStartingPos;
                openCloseLong++;
            }
        }

        // return pattern
        return pattern;
    }

    public BitwisePatternMeta getMeta() {
        return meta;
    }

    public long[] getPattern() {
        return pattern;
    }

    public long[] getOpenclosetags() {
        return openclosetags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitwisePattern that = (BitwisePattern) o;
        return meta.equals(that.meta) &&
                Arrays.equals(openclosetags, that.openclosetags) &&
                Arrays.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(meta);
        result = 31 * result + Arrays.hashCode(openclosetags);
        result = 31 * result + Arrays.hashCode(pattern);
        return result;
    }

    public int getSize() {
        return size;
    }
}
