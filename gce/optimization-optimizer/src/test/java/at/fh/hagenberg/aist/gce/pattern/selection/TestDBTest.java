/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.selection;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.util.NanoProfiler;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.BeforeClass;

/**
 * @author Oliver Krauss on 03.10.2019
 */

public class TestDBTest extends TestPatternsTest {

    protected PatternSearchSpaceRepository repository;


    @BeforeClass
    public void setUp() {
        super.setUp();
        NanoProfiler.SILENCE = true;

        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        repository = ctx.getBean(PatternSearchSpaceRepository.class);

        repository.queryTyped("MATCH (n) detach delete n", null);

        repository.save(t1);
        repository.save(t2);
        repository.save(t3);
        repository.save(t3a);
        repository.save(t3b);
        repository.save(t4);
        repository.save(t5);
        repository.save(t6);
        repository.save(t7);

        // insert the minimal stuff so this is actually a valid db for mining
        repository.queryTyped("MATCH (n) where not ()-[:CHILD]->(n) CREATE (sg:SolutionGene)-[:RWGENE]->(t:TruffleOptimizationSolution)-[:TREE]->(n)", null);
    }
}
