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

import at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import at.fh.hagenberg.util.Pair;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Chooser that Allows prioritizing choices over others. The selection is still random, but not with an even distribution.
 *
 * @author Oliver Krauss on 03.04.2019
 */
public class BiasedChooser<T> implements ChooseOption<T> {

    /**
     * Biasing for options. The higher the value in the pair, the more likely the option will be chosen.
     * Ex. 1 -> x, 2 -> y, 3 -> z means that x is chosen with 1/6 chance, y with a 2/6 chance and z with a 1/2 chance
     */
    private Collection<Pair<Double, T>> optionBias;

    @Override
    public T choose(Collection<T> options) {
        // prepare the bias options
        Collection<Pair<Double, T>> currentBias = new LinkedList<>(optionBias);
        currentBias.removeIf(x -> !options.contains(x.getValue()));

        // select according to bias
        Double next = RandomUtil.random.nextDouble() * currentBias.stream().mapToDouble(x -> x.getKey()).sum();

        for (Pair<Double, T> x : currentBias) {
            // find the correct quartile
            next -= x.getKey();
            if (next < 0) {
                return x.getValue();
            }
        }

        // emergency fallback
        return options.iterator().next();
    }

    @Required
    public void setOptionBias(Collection<Pair<Double, T>> optionBias) {
        this.optionBias = optionBias;
    }

    public Collection<Pair<Double, T>> getOptionBias() {
        return optionBias;
    }


    @Override
    public Map<String, Descriptor> getOptions() {
        // Note: not publishing id, code, tests, originalSolution, bestKnownSolution

        Map<String, Descriptor> options = new HashMap<>();
        options.put("optionBias", new Descriptor<>(optionBias));
        return options;
    }

    @Override
    public boolean setOption(String name, Descriptor descriptor) {
        try {
            switch (name) {
                case "selector":
                    setOptionBias((Collection<Pair<Double, T>>) descriptor.getValue());
                    break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
