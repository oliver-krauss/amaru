/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.science.statistics.data.template;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.List;

public class IndividualInformation {

    /**
     * Ast from individual
     */
    private NodeWrapper ast;

    /**
     * Quality in the context of this run
     */
    private double quality;

    public IndividualInformation(NodeWrapper ast, double quality) {
        this.ast = ast;
        this.quality = quality;
    }

    public NodeWrapper getAst() {
        return ast;
    }

    public void setAst(NodeWrapper ast) {
        this.ast = ast;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }
}
