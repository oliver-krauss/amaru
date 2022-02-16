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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Truffle Pattern that relates to other Truffle Patterns
 * @author Oliver Krauss on 08.07.2020
 */
public class TruffleRelatablePattern extends TrufflePattern {

    /**
     * Patterns that are contained in this pattern
     */
    private Set<TruffleRelatablePattern> contains = new LinkedHashSet<>();

    /**
     * Patterns that contain this pattern
     */
    private Set<TruffleRelatablePattern> containedIn = new LinkedHashSet<>();

    /**
     * Patterns that can be generalized into this pattern
     * Ex. this.ExpressionNode is a generalization of generalizes.IntAddNode
     */
    private Set<TruffleRelatablePattern> generalizes = new LinkedHashSet<>();

    /**
     * Patterns that can be specialized from this pattern
     * Ex. this.IntAddNode can be specialized from specializes.ExpressionNode
     */
    private Set<TruffleRelatablePattern> specializes = new LinkedHashSet<>();

    public TruffleRelatablePattern(Long treeId, PatternNodeWrapper patternNode) {
        super(treeId, patternNode);
    }


    protected TruffleRelatablePattern() {
    }

    /**
     * Copies a truffle pattern into a truffle relatable pattern
     * @param pattern to be copied
     * @return exact copy with relationship options
     */
    public static TruffleRelatablePattern copy(TrufflePattern pattern) {
        TruffleRelatablePattern result = new TruffleRelatablePattern();
        result.count = pattern.count;
        result.nodeIds.addAll(pattern.nodeIds);
        result.size = pattern.size;
        result.treeIds.addAll(pattern.treeIds);
        result.patternNode = pattern.patternNode;
        return result;
    }

    public Set<TruffleRelatablePattern> getContains() {
        return contains;
    }

    public Set<TruffleRelatablePattern> getContainedIn() {
        return containedIn;
    }

    public Set<TruffleRelatablePattern> getGeneralizes() {
        return generalizes;
    }

    public Set<TruffleRelatablePattern> getSpecializes() {
        return specializes;
    }
}
