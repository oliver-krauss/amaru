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

import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.SolutionGene;

/**
 * @author Oliver Krauss on 07.07.2020
 */

public class PatternPrinterHelper {

    public boolean containsProblem(SolutionGene<TrufflePattern, TrufflePatternProblem> solution, TrufflePatternProblem problem) {
        return solution.getProblemGenes().stream().anyMatch(x -> x.getGene().equals(problem));
    }

}
