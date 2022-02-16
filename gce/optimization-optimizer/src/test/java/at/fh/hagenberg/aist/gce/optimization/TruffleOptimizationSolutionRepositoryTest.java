/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization;

import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 04.11.2019
 */
@ContextConfiguration(locations = {"classpath*:truffleRepositoryConfig.xml"})
public class TruffleOptimizationSolutionRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    TruffleOptimizationSolutionRepository repository;

    private Long id;

    @BeforeClass
    public void setUp() {
        TruffleOptimizationSolution solution = new TruffleOptimizationSolution(new MinicIfNode(new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(4), new MinicSimpleLiteralNode.MinicIntLiteralNode(4)), null);
        id = repository.save(solution).getId();
    }

    @Test
    public void testLoadWithTree() {
        // given
        // id in setup

        // when
        TruffleOptimizationSolution truffleOptimizationSolution = repository.loadWithTree(id);

        // then
        Assert.assertNotNull(truffleOptimizationSolution);
        Assert.assertEquals(truffleOptimizationSolution.getId(), id);
        Assert.assertNotNull(truffleOptimizationSolution.getTree());
        Assert.assertEquals(truffleOptimizationSolution.getTree().getChildren().size(), 3);
    }

}
