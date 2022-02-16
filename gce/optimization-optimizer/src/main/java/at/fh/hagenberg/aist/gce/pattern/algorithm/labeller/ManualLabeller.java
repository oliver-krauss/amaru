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
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassProperty;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;

import java.util.Map;

/**
 * Labelling implementation for variable labelling of non-truffle languages.
 * Where the variables are has to be defined manually.
 */
public class ManualLabeller implements VariableLabeller {

    /**
     * Map of Type to Field that contains the variable name
     */
    Map<String, String> labelMap;

    public ManualLabeller(Map<String, String> labelMap) {
        this.labelMap = labelMap;
    }

    @Override
    public String label(NodeWrapper wrapper) {
        String field = labelMap.getOrDefault(wrapper.getType(), null);
        Object value = field != null ? wrapper.getValues().getOrDefault(field, null) : null;
        if (value != null) {
            wrapper.getValues().remove(field);
            return value.toString();
        }
        return null;
    }

    @Override
    public void inject(NodeWrapper wrapper, String variableName) {
        String field = labelMap.getOrDefault(wrapper.getType(), null);
        if (field != null) {
            wrapper.getValues().put(field, variableName);
        }
    }
}
