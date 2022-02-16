/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm;

import at.fh.hagenberg.aist.gce.pattern.TruffleDifferentialPatternSolution;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.selection.PatternSearchSpaceRepository;
import at.fh.hagenberg.machinelearning.core.AbstractAlgorithm;
import at.fh.hagenberg.machinelearning.core.Problem;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class that combines settings that all algorithms MUST be able to understand
 * @author Oliver Krauss on 07.10.2019
 */
public abstract class AbstractPatternDetectorAlgorithm<ST> extends AbstractAlgorithm<ST, TrufflePatternProblem> {

    /**
     * maximum size of patterns that will be mined
     */
    protected int maxPatternSize = -1;

    /**
     * floor of hierarchy that patterns will be generated for
     */
    protected int hierarchyFloor = 0;

    /**
     * ceiling of hierarchy that patterns will be generated for
     */
    protected int hierarchyCeil = Integer.MAX_VALUE;

    /**
     * Repository that lets us access the Nodes
     */
    protected PatternSearchSpaceRepository repository;

    /**
     * checks if the log is already initialized
     */
    protected boolean logInitialized = false;

    /**
     * checks if the log was alredy finalized
     */
    protected boolean logFinalized = false;

    @Override
    protected Map<String, Descriptor> getSpecificOptions() {
        Map<String, Descriptor> options = new HashMap<>();
        options.put("maxPatternSize", new Descriptor<>(this.maxPatternSize));
        options.put("hierarchyFloor", new Descriptor<>(this.hierarchyFloor));
        return options;
    }

    @Override
    protected boolean setSpecificOption(String name, Descriptor descriptor) {
        if (name.equals("maxPatternSize")) {
            this.setMaxPatternSize((Integer) descriptor.getValue());
        } else if (name.equals("hierarchyFloor")) {
            this.setHierarchyFloor((Integer) descriptor.getValue());
        }
        return true;
    }

    @Override
    public Solution<ST, TrufflePatternProblem> solve(Problem<TrufflePatternProblem> problem) {
        return solve(problem, new Solution<>());
    }

    public void setMaxPatternSize(int maxPatternSize) {
        this.maxPatternSize = maxPatternSize;
    }

    public void setHierarchyFloor(int hierarchyFloor) {
        this.hierarchyFloor = hierarchyFloor;
    }

    public void setHierarchyCeil(int hierarchyCeil) {
        this.hierarchyCeil = hierarchyCeil;
    }

    /**
     * Helper function that initializes the logging of the algorithm
     *
     * @param problem to be solved
     */
    protected void initializeLog(Problem<TrufflePatternProblem> problem) {
        if (analytics != null && !logInitialized) {
            logInitialized = true;
            analytics.startAnalytics();
            analytics.logParam("problemSize", problem.getProblemSize());
            List<String> headers = new ArrayList<>();
            // we have no headers
            analytics.logAlgorithmStepHeaders(headers);
        }
    }


    /**
     * Helper function that is called after the last generation was created
     */
    protected void finalizeLog(Problem<TrufflePatternProblem> problem) {
        if (logFinalized) {
            return;
        }

        if (analytics != null) {
            analytics.logProblem(problem);
            logSolution(getSolution());
            analytics.finishAnalytics();
        }
        logFinalized = true;
    }

    protected abstract Solution<ST, TrufflePatternProblem> getSolution();

    public void logSolution(Solution<ST, TrufflePatternProblem> solution) {
        getAnalytics().logSolution(solution);
    }
}
