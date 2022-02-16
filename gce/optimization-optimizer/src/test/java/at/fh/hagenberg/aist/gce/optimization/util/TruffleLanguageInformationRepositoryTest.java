/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 13.11.2019
 */
public class TruffleLanguageInformationRepositoryTest {

    private TruffleLanguageInformationRepository repository;

    private TruffleLanguageInformation originalConstruct;

    @BeforeClass
    public void setUp() {
        ClassPathXmlApplicationContext ctx = ApplicationContextProvider.getCtx();
        repository = ctx.getBean(TruffleLanguageInformationRepository.class);
        repository.queryTyped("MATCH (n) detach delete n", null);
    }

    @AfterClass
    public void after() {
        repository.queryTyped("MATCH (n) detach delete n", null);
    }

    @Test
    public void testLoadOrCreate() throws InterruptedException {
        // given
        // force reset the System Information so it isn't created at the wrong place at the wrong time
        SystemInformation.getCurrentSystem().setId(null);

        // when
        TruffleLanguageInformation information = repository.loadOrCreateByLanguageId(MinicLanguage.ID);

        // then
        Assert.assertTrue(information.getId() != null);
    }

    @Test(dependsOnMethods = "testLoadOrCreate")
    public void testSave() {
        // given
        originalConstruct = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

        // when
        repository.save(originalConstruct);

        // then
        Assert.assertTrue(originalConstruct.getId() != null);
    }

    @Test(dependsOnMethods = "testSave")
    public void testLoad() {
        // given
        String id = MinicLanguage.ID;

        // when
        TruffleLanguageInformation information = repository.loadByLanguageId(id);

        // assert
        Assert.assertNotNull(information);
        Assert.assertNotNull(information.getClassHierarchy());
        Assert.assertNotNull(information.getClassHierarchy().get(Object.class));
        Assert.assertEquals(information.getClassHierarchy().get(Object.class).size(), 1);
        Assert.assertEquals(information.getClassHierarchy().get(Object.class).get(0).getName(), "com.oracle.truffle.api.nodes.Node");
        Assert.assertEquals(information.getClassHierarchy().size(), originalConstruct.getClassHierarchy().size());
        Assert.assertEquals(information.getUnreachableClasses().size(), originalConstruct.getUnreachableClasses().size());
        Assert.assertEquals(information.getOperators().size(), originalConstruct.getOperators().size());
        Assert.assertEquals(information.getOperands().size(), originalConstruct.getOperands().size());
        Assert.assertEquals(information.terminalNodeClasses.size(), originalConstruct.terminalNodeClasses.size());
        Assert.assertEquals(information.getNodes().size(), originalConstruct.getNodes().size());
        Assert.assertEquals(information.getInstantiableNodes().size(), originalConstruct.getInstantiableNodes().size());
        Assert.assertNotNull(information.getInstantiableNodes().get(MinicSimpleLiteralNode.MinicIntLiteralNode.class));
        TruffleClassInformation tci = information.getInstantiableNodes().get(MinicSimpleLiteralNode.MinicIntLiteralNode.class);
        Assert.assertEquals(tci.getInitializers().size(), 1);
        Assert.assertNotNull(tci.getInitializers().get(0).getClazz());
        Assert.assertEquals(tci.getInitializers().get(0).clazz, tci);
        Assert.assertEquals(tci.getInitializers().get(0).getParameters().length, 1);
        Assert.assertEquals(tci.getInitializers().get(0).getParameters()[0].getClazz(), int.class);
        Assert.assertEquals(tci.getInitializers().get(0).getParameters()[0].getType(), int.class);
        Assert.assertNotNull(tci.getInitializers().get(0).getParameters()[0].getField());
        Assert.assertNotNull(tci.getInitializers().get(0).getParameters()[0].getId());
    }
}
