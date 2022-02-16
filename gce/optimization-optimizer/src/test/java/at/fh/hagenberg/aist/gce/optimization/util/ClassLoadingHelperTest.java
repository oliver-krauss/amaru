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

import at.fh.hagenberg.aist.gce.optimization.util.strategy.TruffleHierarchicalStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableStrategy;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author Oliver Krauss on 03.11.2019
 */

public class ClassLoadingHelperTest {

    @Test
    public void testFindRules() {
        // given
        ClassLoadingHelper helper = new ClassLoadingHelper();

        // when
        List<Class> classes = helper.findClasses();

        // then
        Assert.assertNotNull(classes);
        Assert.assertTrue(classes.size() > 1);
    }

    @Test
    public void testFindRulesRestrictedSuperclass() {
        // given
        ClassLoadingHelper helper = new ClassLoadingHelper();
        helper.setPackages(Collections.singletonList("at.fh.hagenberg.aist.gce.optimization.util"));
        helper.setParentClasses(Collections.singletonList(DefaultObservableStrategy.class));
        helper.setRealOnly(true);

        // when
        List<Class> classes = helper.findClasses();

        // then
        Assert.assertNotNull(classes);
        Assert.assertEquals(classes.size(), 12);
    }

    @Test
    public void testFindRulesRestrictedInterface() {
        // given
        ClassLoadingHelper helper = new ClassLoadingHelper();
        helper.setPackages(Collections.singletonList("at.fh.hagenberg.aist.gce.optimization.util"));
        helper.setParentClasses(Collections.singletonList(TruffleHierarchicalStrategy.class));

        // when
        List<Class> classes = helper.findClasses();

        // then
        Assert.assertNotNull(classes);
        Assert.assertEquals(classes.size(), 9);
    }

}
