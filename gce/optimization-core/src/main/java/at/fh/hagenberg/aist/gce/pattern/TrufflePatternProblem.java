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

import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;

import java.util.List;
import java.util.Map;

/**
 * The Truffle Pattern problem describes what kind of pattern should be found
 *
 * @author Oliver Krauss on 28.11.2018
 */
public class TrufflePatternProblem {

    private TrufflePatternSearchSpace searchSpace;

    /**
     * Human readable name
     */
    private String name;

    /**
     * Language the nodes are in, for loading the hierarchy.
     * Is allowed to be null
     */
    private String language;

    /**
     * Hierarchy that shall be mined for this problem.
     */
    private BitwisePatternMeta hierarchy;

    public TrufflePatternProblem() {
    }

    public TrufflePatternProblem(String language, TrufflePatternSearchSpace searchSpace, String name) {
        this.language = language;
        this.searchSpace = searchSpace;
        this.name = name;
    }

    public TrufflePatternProblem(String language, TrufflePatternSearchSpace searchSpace, String name, BitwisePatternMeta meta) {
        this.language = language;
        this.searchSpace = searchSpace;
        this.name = name;
        this.hierarchy = meta;
    }

    public TrufflePatternProblem(String language, TrufflePatternSearchSpace searchSpace, String name, Map<String, List<String>> hierarchy, String root) {
        this.language = language;
        this.searchSpace = searchSpace;
        this.name = name;
        this.hierarchy = new BitwisePatternMeta(hierarchy, root);
    }

    public String getName() {
        return name;
    }

    public TrufflePatternSearchSpace getSearchSpace() {
        return searchSpace;
    }

    public String getLanguage() {
        return language;
    }

    public BitwisePatternMeta getHierarchy() {
        return hierarchy;
    }
}
