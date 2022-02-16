/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy.selection;

import at.fh.hagenberg.aist.gce.optimization.util.strategy.RequirementInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Oliver Krauss on 31.07.2020
 */

public class DegreeOfFreedomChooser extends RandomChooser<Pair<TruffleHierarchicalStrategy, RequirementInformation>> {

    @Override
    public Pair<TruffleHierarchicalStrategy, RequirementInformation> choose(Collection<Pair<TruffleHierarchicalStrategy, RequirementInformation>> options) {
        // we want to prevent any choices that have a 0 in the requirements but otherwise we don't care
        Collection<Pair<TruffleHierarchicalStrategy, RequirementInformation>> collect = options.stream().filter(x -> x.getValue().fullfillsAll()).collect(Collectors.toList());
        List<Pair<TruffleHierarchicalStrategy, RequirementInformation>> collectNoPartialForward = collect.stream().filter(x -> x.getValue().getRequirements().keySet().stream().noneMatch(r -> r.containsProperty("FORWARDPOINTER"))).collect(Collectors.toList());
        if (!collectNoPartialForward.isEmpty()) {
            collect = collectNoPartialForward;
        }
        if (collect.isEmpty()) {
            // without any satisfactory answer we just choose the least bad problem
            int minSize = options.stream().mapToInt(x -> x.getValue().getRequirements().size()).min().orElse(0);
            collect = options.stream().filter(x -> x.getValue().getRequirements().size() == minSize).collect(Collectors.toList());
        }
        return super.choose(collect);
    }

    @Override
    public Map<String, Descriptor> getOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        return options;
    }

    @Override
    public boolean setOption(String s, Descriptor descriptor) {
        return false;
    }
}
