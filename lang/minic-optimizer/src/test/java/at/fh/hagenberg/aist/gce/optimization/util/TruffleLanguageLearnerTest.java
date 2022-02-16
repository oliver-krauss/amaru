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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Note: Tests in here disabled per default as they take >1 hours
 * Please do not commit while enabled, but rather test only locally!
 * @author Oliver Krauss on 03.12.2019
 */
public class TruffleLanguageLearnerTest {

    private TruffleLanguageInformation languageInformation = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);

    private MinicLanguageLearner learner = new MinicLanguageLearner(languageInformation);

    @Test(enabled = false)
    public void testTLL() {
        // given

        // when
        learner.learn();

        // then
        languageInformation.getInstantiableNodes().values().forEach(x -> {
            Assert.assertNotEquals(x.getWeight(), -1);
            Assert.assertNotEquals(x.getWeightUnoptimized(), -1);
        });
    }

    @Test(enabled = false, dependsOnMethods = "testTLL")
    public void testCompletenessReport() {
        // given

        // when
        learner.diagnoseImplementationState(null);

        // then
        Assert.assertTrue(new File("./c-implementationReport.md").exists());
    }

}
