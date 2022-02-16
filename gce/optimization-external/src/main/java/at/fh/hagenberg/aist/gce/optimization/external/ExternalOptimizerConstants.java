/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.external;

/**
 * Class containing constant values for identifiaction in the system
 *
 * @author Oliver Krauss on 28.11.2019
 */
public class ExternalOptimizerConstants {

    /**
     * Constant for the create operator
     */
    public static final String OP_CREATOR = "SolutionCreator";

    /**
     * Constant for the crossover operator
     */
    public static final String OP_CROSSOVER = "Crossover";

    /**
     * Constant for the mutate operator
     */
    public static final String OP_MUTATOR = "Mutator";

    /**
     * Constant for identifying the fitness function
     */
    public static final String FFN = "FitnessFunction";

    /**
     * Constant for the evaluation operator (determining the quality of a solution)
     */
    public static final String OP_EVALUATOR = "Evaluator";

    /**
     * Constant for additional settings "Problem" may have (not the entire problem definition!)
     */
    public static final String PROBLEM = "Problem";
}
