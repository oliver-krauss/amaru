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
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.CreationConfiguration;
import at.fh.hagenberg.aist.hlc.core.messages.StartAlgorithmRequest;
import at.fh.hagenberg.aist.hlc.worker.Worker;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.HashSet;

/**
 * @author Oliver Krauss on 27.11.2019
 */

public class GceExternalOptimizationWorkerTest {

    private GceExternalOptimizationWorker worker = new GceExternalOptimizationWorker();

    @BeforeClass
    public void setUp() {
        // We are registering the language with 0L in this case, as the ID always changes in the DB!
        ExternalOptimizationContextRepository.registerRepository(0L, new ExternalOptimizationContextRepository() {

            @Override
            public TruffleOptimizationProblem getProblem(TruffleLanguageSearchSpace space, String file, String function, String input, String output, String evaluationIdentity) {
                return new TruffleOptimizationProblem(MinicLanguage.ID, file, function, function, new MinicSimpleLiteralNode.MinicIntLiteralNode(1), null, space, new CreationConfiguration(5, 5, Double.MAX_VALUE), new HashSet<>(), 1, null);

            }

            @Override
            public String getLanguage() {
                return MinicLanguage.ID;
            }
        });
    }

    /**
     * Notice: This is a HELPER function that records an algorithm request as we are too lazy to stitch that monster together on our own.
     */
    @Test(enabled = false)
    public void recordStartAlgorithmRequest() throws Exception {
        // given
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx("messageCollectionWorkerConfig.xml");
        Worker worker = ctx.getBean(Worker.class);
        worker.work();

        // when
        // you have to run the broker and heuristic lab.

        // then
    }

    @Test
    public void testStartAlgorighmRequest() throws IOException, ClassNotFoundException {
        // given
        ObjectInputStream oi = new ObjectInputStream(this.getClass().getClassLoader().getResourceAsStream("sampleStartAlgorithmRequest.msg"));
        StartAlgorithmRequest request = (StartAlgorithmRequest) oi.readObject();

        // when
        worker.configure("test", request);

        // then
        // nothing. We just want no exceptions
    }
}
