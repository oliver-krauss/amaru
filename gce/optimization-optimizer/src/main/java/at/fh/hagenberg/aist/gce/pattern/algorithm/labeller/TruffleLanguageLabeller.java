/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm.labeller;

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

public class TruffleLanguageLabeller implements VariableLabeller {

    /**
     * Language the labeller works for
     */
    TruffleLanguageInformation tli;

    public TruffleLanguageLabeller(TruffleLanguageInformation tli) {
        this.tli = tli;
    }

    @Override
    public String label(NodeWrapper wrapper) {
        TruffleClassInformation clazz = tli.getClass(wrapper.getType());
        // TODO #251 implement the class info extraction.
        return null;
    }

    @Override
    public void inject(NodeWrapper wrapper, String variableName) {
        // TODO #251 implement the class info injection.
    }
}
