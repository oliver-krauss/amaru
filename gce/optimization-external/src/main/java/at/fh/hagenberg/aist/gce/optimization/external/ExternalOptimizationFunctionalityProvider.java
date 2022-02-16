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

import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableCrossover;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableGeneCreator;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableMutator;
import at.fh.hagenberg.aist.gce.optimization.util.ClassLoadingHelper;
import science.aist.seshat.Logger;
import at.fh.hagenberg.machinelearning.core.Configurable;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.machinelearning.core.fitness.Evaluator;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provider for functions that the internal optimization framework provides.
 *
 * @author Oliver Krauss on 17.04.2019
 */
public class ExternalOptimizationFunctionalityProvider {

    private Logger logger = Logger.getInstance();

    /**
     * Returns the correct gene creator for this problem
     *
     * @param configurationOptions that defines what shall be returned
     * @return gene creator
     */
    public ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem> getGeneCreator(Map<String, String> configurationOptions) {
        return loadObject(ExternalOptimizerConstants.OP_CREATOR, ConfigurableGeneCreator.class, configurationOptions);
    }

    /**
     * Returns the crossover operator for this problem
     *
     * @param configurationOptions that defines what shall be returned
     * @return crossover operator
     */
    public ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem> getCrossover(Map<String, String> configurationOptions) {
        return loadObject(ExternalOptimizerConstants.OP_CROSSOVER, ConfigurableCrossover.class, configurationOptions);
    }

    /**
     * Returns the evaluator for this problems solutions
     *
     * @param configurationOptions that defines what shall be returned
     * @return evaluator operator
     */
    public Evaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> getEvaluator(Map<String, String> configurationOptions) {
        TruffleEvaluatorImpl evaluator = new TruffleEvaluatorImpl();

        // set timeout for evaluator
        if (configurationOptions.containsKey(ExternalOptimizerConstants.OP_EVALUATOR + ".timeout")) {
            evaluator.setTimeout(new Long(configurationOptions.get(ExternalOptimizerConstants.OP_EVALUATOR + ".timeout")));
        }

        Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> evalMap = new HashMap<>();

        // we are skipping the evaluator as we only allow the TruffleEvaluatorImpl currently.
        String key = ExternalOptimizerConstants.OP_EVALUATOR + "." + ExternalOptimizerConstants.FFN;
        String value = configurationOptions.getOrDefault(key, "0");
        int[] positions = Arrays.stream(value.split(",")).mapToInt(x -> Integer.valueOf(x.trim())).toArray();
        CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>[] cachets = getAvailableCachets().toArray(new CachetEvaluator[0]);
        for (int position : positions) {
            evalMap.put(cachets[position], 1.0 / cachets.length);
        }

        evaluator.setCachetEvaluators(evalMap);
        return evaluator;

    }

    /**
     * Returns the mutator operator for this problem
     *
     * @param configurationOptions that defines what shall be returned
     * @return mutator operator
     */
    public ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> getMutator(Map<String, String> configurationOptions) {
        return loadObject(ExternalOptimizerConstants.OP_MUTATOR, ConfigurableMutator.class, configurationOptions);
    }

    private <T extends Configurable> T loadObject(String identifier, Class clazz, Map<String, String> configurationOptions) {
        return this.loadObject(identifier, clazz, configurationOptions, true);
    }

    private <T extends Configurable> T loadObject(String identifier, Class clazz, Map<String, String> configurationOptions, boolean typed) {
        int id = configurationOptions.containsKey(identifier) ? Integer.valueOf(configurationOptions.get(identifier)) : 0;
        T object = (T) (typed ? findClassesTyped(clazz) : findClassesUntyped(clazz)).get(id);

        Map<String, String> objectOptions = new HashMap<>();
        configurationOptions.forEach((key, value) -> {
            if (key.startsWith(identifier + ".")) {
                objectOptions.put(key.substring(key.indexOf(".") + 1), value);
            }
        });

        Class objectClass = object.getClass();
        objectOptions.entrySet().stream().filter(x -> !x.getKey().contains(".")).forEach((entry) -> {
            Object val = entry.getValue();
            Field field = Arrays.stream(objectClass.getDeclaredFields()).filter(x -> x.getName().equals(entry.getKey())).findFirst().orElse(null);
            if (field == null) {
                field = Arrays.stream(objectClass.getFields()).filter(x -> x.getName().equals(entry.getKey())).findFirst().orElse(null);
            }
            if (field != null) {
                // Check if we have a sub-configurable on our hands
                Class type = field.getType();
                if (Configurable.class.isAssignableFrom(type)) {
                    val = loadObject(entry.getKey(), type, objectOptions, false);
                } else {
                    // cast to primitives
                    switch (type.getName()) {
                        case "int":
                        case "java.lang.Integer":
                            val = Integer.valueOf((String) val);
                            break;
                        case "double":
                        case "java.lang.Double":
                            val = Double.valueOf((String) val);
                            break;
                        case "boolean":
                        case "java.lang.Boolean":
                            val = Boolean.valueOf((String) val);
                            break;
                        case "java.time.LocalDateTime":
                            // not sure how local date time is sent by C#. Is not used for now
                            break;
                        case "java.time.Duration":
                            // not sure how duration is sent by C#. Is not used for now
                            break;
                    }
                }
            }
            object.setOption(entry.getKey(), new Descriptor<>(val));
        });

        return object;
    }

    /**
     * Provides all cachets available in the system
     *
     * @return all cachets
     */
    public List<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>> getAvailableCachets() {
        return findClassesTyped(CachetEvaluator.class);
    }

    /**
     * Provides all mutators available in the system
     *
     * @return all mutators
     */
    public List<ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem>> getAvailableMutators() {
        return findClassesTyped(ConfigurableMutator.class);
    }

    /**
     * Provides all crossovers available in the system
     *
     * @return all crossovers
     */
    public List<ConfigurableCrossover<TruffleOptimizationSolution, TruffleOptimizationProblem>> getAvailableCrossovers() {
        return findClassesTyped(ConfigurableCrossover.class);
    }

    /**
     * Provides all creators available in the system
     *
     * @return all creators
     */
    public List<ConfigurableGeneCreator<TruffleOptimizationSolution, TruffleOptimizationProblem>> getAvailableCreators() {
        return findClassesTyped(ConfigurableGeneCreator.class);
    }

    /**
     * Helper to find implementations
     */
    private ClassLoadingHelper helper = new ClassLoadingHelper(Collections.singletonList("at.fh.hagenberg"));

    protected <T> List<T> findClassesTyped(Class root) {
        helper.setParentClasses(Collections.singletonList(root));
        return helper.findClasses().stream().filter(x -> {
            while (x.getSuperclass() != Object.class) {
                x = x.getSuperclass();
            }
            if (x.getGenericInterfaces() != null && x.getGenericInterfaces().length == 1 &&
                x.getGenericInterfaces()[0] instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) x.getGenericInterfaces()[0];
                return t.getActualTypeArguments() != null && t.getActualTypeArguments().length == 2 &&
                    t.getActualTypeArguments()[0].equals(TruffleOptimizationSolution.class) &&
                    t.getActualTypeArguments()[1].equals(TruffleOptimizationProblem.class);
            }
            return false;
        }).map(x -> {
            try {
                return (T) x.getConstructor(null).newInstance();
            } catch (Exception e) {
                logger.error("Every implementation of our gene creators must have a default constructor");
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected <T> List<T> findClassesUntyped(Class root) {
        helper.setParentClasses(Collections.singletonList(root));
        List<Class> classes = helper.findClasses();
        if (!Modifier.isAbstract(root.getModifiers())) {
            // also add root if it is a real instantiable class
            classes.add(root);
        }
        return classes.stream().map(x -> {
            try {
                return (T) x.getConstructor(null).newInstance();
            } catch (Exception e) {
                logger.error("Every implementation of a configurable must have a default constructor");
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
