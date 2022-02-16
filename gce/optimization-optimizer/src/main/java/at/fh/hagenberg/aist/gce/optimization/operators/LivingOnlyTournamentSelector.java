/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.operators;

import at.fh.hagenberg.machinelearning.algorithm.ga.selector.TournamentSelector;
import at.fh.hagenberg.machinelearning.core.Solution;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Selector that prevents selection of "dead" ASTs
 */
public class LivingOnlyTournamentSelector<GT, PT> extends TournamentSelector<GT, PT> {

    @Override
    public Solution<GT, PT> select(List<Solution<GT, PT>> population) {
        List<Solution<GT, PT>> livingPopulation = population.stream().filter(x -> x.getQuality() <= 9.9).collect(Collectors.toList());
        if (!livingPopulation.isEmpty()) {
            return super.select(livingPopulation);
        } else {
            return super.select(population);
        }
    }

}
