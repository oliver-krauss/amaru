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

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.hlc.core.ExternalOptimizationWorker;
import at.fh.hagenberg.aist.hlc.core.messages.*;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.fitness.Evaluator;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * External optimization worker that processes commands according to the "hlc" projects protocols.
 *
 * @author Oliver Krauss on 26.10.2019
 */
public class GceExternalOptimizationWorker implements ExternalOptimizationWorker {

    /**
     * Experiment context <Id, Context> to map the requests to the appropriate states
     */
    private static Map<String, GceWorkContext> experimentContext = new HashMap<>();

    private static final TruffleLanguageInformationRepository tliRepository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);

    @Override
    public Message operate(String id, Message request) {
        if (request.getClass().equals(SolutionCreatorRequest.class)) {
            return create(id, (SolutionCreatorRequest) request);
        } else if (request.getClass().equals(EvaluatorRequest.class)) {
            return evaluate(id, (EvaluatorRequest) request);
        } else if (request.getClass().equals(CrossoverRequest.class)) {
            return cross(id, (CrossoverRequest) request);
        } else if (request.getClass().equals(ManipulatorRequest.class)) {
            return mutate(id, (ManipulatorRequest) request);
        } else {
            throw new UnsupportedOperationException("Operator of type " + request.getClass() + " is not supported.");
        }
    }

    @Override
    public StartAlgorithmResponse configure(String id, StartAlgorithmRequest request) {
        String errorMessage = null;
        if (experimentContext.containsKey(id)) {
            errorMessage = "This experiment has already been started.";
        } else if (id == null) {
            errorMessage = "An experiment MUST have an ID.";
        } else {
            // reserve this run id
            experimentContext.put(id, null);
        }

        ProblemDefinition problemDefinition = request.getProblemDefinition();

        // do sanity checks for the configuration
        if (problemDefinition.getSourceCode().isEmpty()) {
            errorMessage = "Source code is empty.";
        } else if (!problemDefinition.getSourceCode().contains(problemDefinition.getFunctionName())) {
            errorMessage = "Source code does not contain function '" + problemDefinition.getFunctionName() + "'.";
        } else if (problemDefinition.getInput().isEmpty()) {
            errorMessage = "No input data provided.";
        } else if (problemDefinition.getOutput().split("\\n").length != problemDefinition.getInput().split("\\n").length) {
            errorMessage = "Number of outputs must match number of inputs.";
        } else if (problemDefinition.getOutput().isEmpty()) {
            errorMessage = "No output data provided.";
        } else if (!ExternalOptimizationContextRepository.hasRepository(problemDefinition.getLanguageId())) {
            errorMessage = "Language is not supported";
        }

        if (errorMessage != null) {
            return StartAlgorithmResponse.newBuilder().setSuccess(false).setErrorMessage(errorMessage).build();
        }

        // Load TLI
        ExternalOptimizationContextRepository repository = ExternalOptimizationContextRepository.getRepository(problemDefinition.getLanguageId());
        ExternalOptimizationFunctionalityProvider functionality = new ExternalOptimizationFunctionalityProvider();
        TruffleLanguageInformation information = tliRepository.loadOrCreateByLanguageId(repository.getLanguage());

        // load excludes and prepare search space
        List<Class> excludes = new ArrayList<>();
        request.getSymbolConfigurationList().forEach(x -> {
            if (!x.getEnabled()) {
                TruffleClassInformation truffleClassInformation = information.getNodes().values().stream().filter(n -> n.getId() == x.getSymbolId()).findFirst().orElse(null);
                if (truffleClassInformation != null) {
                    excludes.add(truffleClassInformation.getClazz());
                }
            }
        });
        TruffleLanguageSearchSpace searchSpace = new TruffleLanguageSearchSpace(information, excludes);

        // see if we can create the problem
        Evaluator<TruffleOptimizationSolution, TruffleOptimizationProblem> evaluator = functionality.getEvaluator(request.getOptionConfiguration());
        TruffleOptimizationProblem problem = repository.getProblem(searchSpace, problemDefinition.getSourceCode(), problemDefinition.getFunctionName(), problemDefinition.getInput(), problemDefinition.getOutput(), evaluator.evaluationIdentity());

        // set the additional problem parameters
        if (request.getOptionConfigurationMap().containsKey("Problem.TruffleOptimizationProblem.configuration.maxWidth")) {
            problem.getConfiguration().setMaxWidth(Integer.valueOf(request.getOptionConfigurationMap().get("Problem.TruffleOptimizationProblem.configuration.maxWidth")));
        }
        if (request.getOptionConfigurationMap().containsKey("Problem.TruffleOptimizationProblem.configuration.maxDepth")) {
            problem.getConfiguration().setMaxDepth(Integer.valueOf(request.getOptionConfigurationMap().get("Problem.TruffleOptimizationProblem.configuration.maxDepth")));
        }
        if (request.getOptionConfigurationMap().containsKey("Problem.TruffleOptimizationProblem.description")) {
            problem.setDescription(request.getOptionConfigurationMap().get("Problem.TruffleOptimizationProblem.description"));
        }
        if (request.getOptionConfigurationMap().containsKey("Problem.TruffleOptimizationProblem.repeats")) {
            problem.setOption("repeats", new Descriptor<>(Integer.valueOf(request.getOptionConfigurationMap().get("Problem.TruffleOptimizationProblem.repeats"))));
        }

        GceWorkContext ctx = new GceWorkContext(id,
            problem,
            functionality.getGeneCreator(request.getOptionConfiguration()),
            evaluator,
            functionality.getCrossover(request.getOptionConfiguration()),
            functionality.getMutator(request.getOptionConfiguration()),
            repository.getStrategies(problemDefinition.getSourceCode()),
            repository.getTerminalStrategies(problemDefinition.getSourceCode())
        );

        experimentContext.put(id, ctx);
        return StartAlgorithmResponse.newBuilder().setSuccess(true).build();
    }

    public SolutionCreatorResponse create(String id, SolutionCreatorRequest solutionCreatorRequest) {
        if (!experimentContext.containsKey(id)) {
            return null;
        }

        // generate solution
        GceWorkContext ctx = experimentContext.get(id);
        TruffleOptimizationSolution solution = ctx.create();
        ctx.evaluate(solution);

        // Encapsulate tree
        TreeNode encapsulatedTree = recursiveTreeEncapsulation(solution.getTree(), ctx.getLanguage());

        // encapsultate in Response
        return SolutionCreatorResponse.newBuilder()
            .setSolutionId(solution.getId())
            .setTree(encapsulatedTree).build();
    }

    public EvaluatorResponse evaluate(String id, EvaluatorRequest evaluatorRequest) {
        if (!experimentContext.containsKey(id)) {
            return null;
        }

        // load tree to be evaluated
        GceWorkContext ctx = experimentContext.get(id);
        Solution s = ctx.evaluate(evaluatorRequest.getSolutionId());

        // encapsulate the answer
        return EvaluatorResponse.newBuilder()
            .setSolutionId(evaluatorRequest.getSolutionId())
            .setQuality(s.getQuality()).build();
    }

    public CrossoverResponse cross(String id, CrossoverRequest crossoverRequest) {
        if (!experimentContext.containsKey(id)) {
            return null;
        }

        // do the crossover
        GceWorkContext ctx = experimentContext.get(id);
        TruffleOptimizationSolution breed = ctx.crossover(crossoverRequest.getParentSolutionId1(), crossoverRequest.getParentSolutionId2());
        ctx.evaluate(breed);

        TreeNode encapsulatedTree = recursiveTreeEncapsulation(breed.getTree(), ctx.getLanguage());

        // Encapsulate the new solution
        return CrossoverResponse.newBuilder()
            .setParentSolutionId1(crossoverRequest.getParentSolutionId1())
            .setParentSolutionId2(crossoverRequest.getParentSolutionId2())
            .setChildSolutionId(breed.getId())
            .setTree(encapsulatedTree).build();
    }

    public ManipulatorResponse mutate(String id, ManipulatorRequest manipulatorRequest) {
        if (!experimentContext.containsKey(id)) {
            return null;
        }

        // do the mutation
        GceWorkContext ctx = experimentContext.get(id);
        TruffleOptimizationSolution mutant = ctx.mutate(manipulatorRequest.getSolutionId());
        ctx.evaluate(mutant);

        TreeNode encapsulatedTree = recursiveTreeEncapsulation(mutant.getTree(), ctx.getLanguage());

        // encapsulate
        return ManipulatorResponse.newBuilder()
            .setSolutionId(manipulatorRequest.getSolutionId())
            .setManipulatedSolutionId(mutant.getId())
            .setTree(encapsulatedTree).build();
    }

    @Override
    public void shutdown(String id, StopAlgorithmRequest request) {
        if (request.getExecutionState() == ExecutionState.STOPPED) {
            if (!experimentContext.containsKey(id)) {
                return;
            }
            // End the analytics phase
            experimentContext.get(id).finishRun();
            experimentContext.remove(id);
        } else if (request.getExecutionState() == ExecutionState.PAUSED) {
            // during pause we don't need to do anything
        }
    }

    private TreeNode recursiveTreeEncapsulation(NodeWrapper wrapper, TruffleLanguageInformation information) {
        return TreeNode.newBuilder()
            .setId(
                wrapper.getId() == null ?
                    -1 :
                    wrapper.getId())
            .setSymbolId(information.getInstantiableNodes().get(ClassLoadingHelper.loadClassByName(wrapper.getType())).getId())
            .addAllChildren(
                wrapper.getChildren().stream().map(child -> {
                    if (ExtendedNodeUtil.isAPINode(child.getChild())) {
                        if (child.getChild().getChildren().size() == 1) {
                            // skip api nodes
                            return recursiveTreeEncapsulation(child.getChild().getChildren().iterator().next().getChild(), information);
                        } else {
                            throw new RuntimeException("API nodes with != 0 child nodes are unknown");
                        }
                    }
                    return recursiveTreeEncapsulation(child.getChild(), information);
                }).collect(Collectors.toList())
            )
            .build();
    }
}
