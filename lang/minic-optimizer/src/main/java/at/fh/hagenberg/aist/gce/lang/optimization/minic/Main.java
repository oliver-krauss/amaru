/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.lang.optimization.minic;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.external.ExternalOptimizationContextRepository;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.aist.gce.optimization.util.MinicLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultFrameSlotStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.StaticObjectStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicFunctionLiteralStrategy;
import at.fh.hagenberg.aist.hlc.worker.Worker;
import at.fh.hagenberg.machinelearning.analytics.graph.SystemInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapperWeightUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for the External optimization worker, connecting with heuristic lab.
 *
 * @author Oliver Krauss on 13.03.2019
 */
public class Main {

    /**
     * Really crappy helper class that we can only improve once we
     */
    private ExternalOptimizationContextRepository minicRepository = new ExternalOptimizationContextRepository() {

        /**
         * Map of request to optimizer
         */
        private Map<String, TruffleOptimizationProblem> problemMap = new HashMap<>();

        private NodeWrapperWeightUtil weightUtil;

        @Override
        public TruffleOptimizationProblem getProblem(TruffleLanguageSearchSpace space, String file, String function, String input, String output, String evaluationIdentity) {
            weightUtil = new NodeWrapperWeightUtil(MinicLanguage.ID);

            if (problemMap.containsKey(file)) {
                return problemMap.get(file);
            }


            InternalExecutor executor = new InternalExecutor(getLanguage(), file, function, function);
            Node node = executor.getOrigin();
            TruffleOptimizationProblem problem = new TruffleOptimizationProblem(getLanguage(), file, function, function,
                node, null, space,
                new CreationConfiguration(5, 5, weightUtil.weight(executor.getOrigin())), MinicExternalOptimizer.getTestCases(input, output), 1, evaluationIdentity);
            problem.setDescription(function);
            problemMap.put(file, problem);
            return problem;
        }

        @Override
        public String getLanguage() {
            return MinicLanguage.ID;
        }

        @Override
        public List<TruffleHierarchicalStrategy> getStrategies(String file) {
            List<TruffleHierarchicalStrategy> strategies = super.getStrategies(file);
            strategies.add(new MinicFunctionLiteralStrategy(MinicLanguage.INSTANCE.getContextReference().get()));
            return strategies;
        }

        @Override
        public Map<String, TruffleVerifyingStrategy> getTerminalStrategies(String file) {
            Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
            if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
                strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
            }
            strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
            strategies.putAll(DefaultStrategyUtil.defaultStrategies());
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problemMap.get(file).getNode().getRootNode().getFrameDescriptor()));
            return strategies;
        }
    };

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.publishWorker();
    }

    public void publishWorker() {
        ClassPathXmlApplicationContext dbContext = ApplicationContextProvider.getCtx();

        // pre-load system information
        SystemInformationRepository systemInformationRepository = dbContext.getBean(SystemInformationRepository.class);
        systemInformationRepository.save(SystemInformation.getCurrentSystem());

        TruffleLanguageInformationRepository tliRepository = dbContext.getBean(TruffleLanguageInformationRepository.class);
        TruffleLanguageInformation tli = tliRepository.loadOrCreateByLanguageId(MinicLanguage.ID);
        ExternalOptimizationContextRepository.registerRepository(tli.getId(), minicRepository);

        // ENSURE that we have a fully learned language
        MinicLanguageLearner learner = new MinicLanguageLearner(tli);
        learner.setSaveToDB(true);
        learner.learn();

        // load the worker
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx("workerConfig.xml");
        Worker worker = ctx.getBean(Worker.class);
        worker.setSupportedLanguages(Collections.singletonList(tli.getId()));
        worker.work();
    }
}
