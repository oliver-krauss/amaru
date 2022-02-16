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

import at.fh.hagenberg.aist.gce.optimization.util.RandomUtil;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternDetector;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.constraint.SolutionConstraint;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class that automatically creates a bias from the DB
 *
 * @author Oliver Krauss on 03.04.2019
 */
public class BiasedPatternMiningChooser extends BiasedChooser<Class> {

    /**
     * determines if the good values are used in the bias
     */
    private boolean useGood = true;

    /**
     * determines if the bad values are used in the bias
     */
    private boolean useBad = true;

    /**
     * Scaling factor between good and bad values
     */
    private double scaleFactor = 1.0;

    private BiasCollisionStrategy strategy = BiasCollisionStrategy.GOOD;

    /**
     * Strategies to deal with patterns that occur both in the good and the bad patterns
     */
    protected enum BiasCollisionStrategy {
        GOOD, // Prefer good patterns and ignore bad values
        BAD,  // prefer bad patterns and ignore good values
        UGLY; // calculate diff between good and bad
    }

    private TrufflePatternDetector detector = new TrufflePatternDetector();

    private TrufflePatternSearchSpaceDefinition goodSpace = new TrufflePatternSearchSpaceDefinition(null, new SolutionConstraint(1.1, 0.0), null, null, null, true);

    private TrufflePatternSearchSpaceDefinition badSpace = new TrufflePatternSearchSpaceDefinition(null, new SolutionConstraint(null, 1.1), null, null, null, true);

    public BiasedPatternMiningChooser(Collection<Class> classes) {
        super();
        detector.setMaxPatternSize(1);

        Map<Class, Double> biasMap = new HashMap<>();

        if (useGood) {
            List<SolutionGene<TrufflePattern, TrufflePatternProblem>> solutionGenes = detector.findPatterns(null, goodSpace, "good").getSolutionGenes();
            if (!solutionGenes.isEmpty()) {
                double max = (double) solutionGenes.stream().mapToLong(x -> x.getGene().getCount()).max().getAsLong();
                solutionGenes.forEach(x -> {
                    try {
                        Class good = java.lang.Class.forName(x.getGene().getPatternNode().getType());
                        if (classes.contains(good)) {
                            biasMap.put(good, (double) x.getGene().getCount() / max + scaleFactor);
                            classes.remove(good);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        if (useBad) {
            detector.findPatterns(null, badSpace, "bad").getSolutionGenes().forEach(x -> {
                try {
                    Class bad = java.lang.Class.forName(x.getGene().getPatternNode().getType());
                    if (biasMap.containsKey(bad)) {
                        switch (strategy) {
                            case GOOD:
                                break; // nothing to do, good already done
                            case BAD:
                                biasMap.put(bad, 1.0 / (double) x.getGene().getCount()); // just override it
                                break;
                            case UGLY:
                                biasMap.put(bad, biasMap.get(bad) - 1.0 / (double) x.getGene().getCount()); // split the difference
                                break;
                        }
                    } else if (classes.contains(bad)) {
                        biasMap.put(bad, 1.0 / (double) x.getGene().getCount());
                        classes.remove(bad);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }

        // add non-detected classes with 1
        ArrayList<Pair<Double, Class>> bias = new ArrayList<>();
        classes.forEach(x -> {
            bias.add(new Pair<>(1.0, x));
        });
        // transfer map to bias
        biasMap.entrySet().forEach(x -> {
            bias.add(new Pair<>(x.getValue(), x.getKey()));
        });
        this.setOptionBias(bias);
    }

    public boolean isUseGood() {
        return useGood;
    }

    public void setUseGood(boolean useGood) {
        this.useGood = useGood;
    }

    public boolean isUseBad() {
        return useBad;
    }

    public void setUseBad(boolean useBad) {
        this.useBad = useBad;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
}
