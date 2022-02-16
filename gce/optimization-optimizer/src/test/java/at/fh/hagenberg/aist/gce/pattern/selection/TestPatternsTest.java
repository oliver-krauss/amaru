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
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.BeforeClass;

/**
 * @author Oliver Krauss on 03.10.2019
 */

public class TestPatternsTest {

    protected NodeWrapper t1, t2, t3, t4, t5, t3a, t3b, t6, t7;


    @BeforeClass
    public void setUp() {
        t1 = new NodeWrapper("Const");
        t1.getValues().put("value", "123");

        t2 = new NodeWrapper("=");
        NodeWrapper t21 = new NodeWrapper("Variable");
        t21.getValues().put("value", "x");
        NodeWrapper t22 = new NodeWrapper("Const");
        t22.getValues().put("value", "123");
        t2.addChild(t21, "field", 0);
        t2.addChild(t22, "value", 0);

        t3 = new NodeWrapper("If");
        NodeWrapper t31 = new NodeWrapper("Const");
        t31.getValues().put("value", "true");
        NodeWrapper t32 = new NodeWrapper("=");
        NodeWrapper t321 = new NodeWrapper("Variable");
        t321.getValues().put("value", "x");
        NodeWrapper t322 = new NodeWrapper("Const");
        t322.getValues().put("value", "123");
        NodeWrapper t33 = new NodeWrapper("=");
        NodeWrapper t331 = new NodeWrapper("Variable");
        t331.getValues().put("value", "x");
        NodeWrapper t332 = new NodeWrapper("Const");
        t332.getValues().put("value", "456");
        t3.addChild(t31, "condition", 0);
        // NOTE: yes this should be "t"hen but we sort by fieldname and I want this to be easily understood
        // when manually checking thus condition -> dhen -> else
        t3.addChild(t32, "dhen", 0);
        t3.addChild(t33, "else", 0);
        t32.addChild(t321, "field", 0);
        t32.addChild(t322, "value", 0);
        t33.addChild(t331, "field", 0);
        t33.addChild(t332, "value", 0);

        t3a = new NodeWrapper("If");
        NodeWrapper t31a = new NodeWrapper("Const");
        t31a.getValues().put("value", "true");
        NodeWrapper t32a = new NodeWrapper("=");
        NodeWrapper t321a = new NodeWrapper("Variable");
        t321a.getValues().put("value", "y");
        NodeWrapper t322a = new NodeWrapper("Const");
        t322a.getValues().put("value", "123");
        NodeWrapper t33a = new NodeWrapper("=");
        NodeWrapper t331a = new NodeWrapper("Variable");
        t331a.getValues().put("value", "y");
        NodeWrapper t332a = new NodeWrapper("Const");
        t332a.getValues().put("value", "456");
        t3a.addChild(t31a, "condition", 0);
        t3a.addChild(t32a, "dhen", 0);
        t3a.addChild(t33a, "else", 0);
        t32a.addChild(t321a, "field", 0);
        t32a.addChild(t322a, "value", 0);
        t33a.addChild(t331a, "field", 0);
        t33a.addChild(t332a, "value", 0);

        t3b = new NodeWrapper("If");
        NodeWrapper t31b = new NodeWrapper("Const");
        t31b.getValues().put("value", "true");
        NodeWrapper t32b = new NodeWrapper("=");
        NodeWrapper t321b = new NodeWrapper("Variable");
        t321b.getValues().put("value", "x");
        NodeWrapper t322b = new NodeWrapper("Const");
        t322b.getValues().put("value", "123");
        NodeWrapper t33b = new NodeWrapper("=");
        NodeWrapper t331b = new NodeWrapper("Variable");
        t331b.getValues().put("value", "y");
        NodeWrapper t332b = new NodeWrapper("Const");
        t332b.getValues().put("value", "456");
        t3b.addChild(t31b, "condition", 0);
        t3b.addChild(t32b, "dhen", 0);
        t3b.addChild(t33b, "else", 0);
        t32b.addChild(t321b, "field", 0);
        t32b.addChild(t322b, "value", 0);
        t33b.addChild(t331b, "field", 0);
        t33b.addChild(t332b, "value", 0);

        t4 = new NodeWrapper("If");
        NodeWrapper t41 = new NodeWrapper("Const");
        t41.getValues().put("value", "true");
        NodeWrapper t42 = new NodeWrapper("!=");
        NodeWrapper t421 = new NodeWrapper("Variable");
        t421.getValues().put("value", "x");
        NodeWrapper t422 = new NodeWrapper("Const");
        t422.getValues().put("value", "123");
        NodeWrapper t43 = new NodeWrapper("!=");
        NodeWrapper t431 = new NodeWrapper("Variable");
        t431.getValues().put("value", "x");
        NodeWrapper t432 = new NodeWrapper("Const");
        t432.getValues().put("value", "456");
        t4.addChild(t41, "condition", 0);
        t4.addChild(t42, "dhen", 0);
        t4.addChild(t43, "else", 0);
        t42.addChild(t421, "field", 0);
        t42.addChild(t422, "value", 0);
        t43.addChild(t431, "field", 0);
        t43.addChild(t432, "value", 0);

        t5 = new NodeWrapper("=");
        NodeWrapper t51 = new NodeWrapper("=");
        NodeWrapper t52 = new NodeWrapper("=");
        t5.addChild(t51, "field", 0);
        t5.addChild(t52, "value", 0);

        // this is for variable label testing. We should get the largest pattern as labels 0 1 2 1 0
        t6 = new NodeWrapper("If");
        NodeWrapper t61 = new NodeWrapper("Variable");
        t61.getValues().put("value", "x");
        NodeWrapper t62 = new NodeWrapper("=");
        NodeWrapper t621 = new NodeWrapper("Variable");
        t621.getValues().put("value", "y");
        NodeWrapper t622 = new NodeWrapper("Variable");
        t622.getValues().put("value", "z");
        NodeWrapper t63 = new NodeWrapper("=");
        NodeWrapper t631 = new NodeWrapper("Variable");
        t631.getValues().put("value", "y");
        NodeWrapper t632 = new NodeWrapper("Variable");
        t632.getValues().put("value", "x");
        t6.addChild(t61, "condition", 0);
        t6.addChild(t62, "dhen", 0);
        t6.addChild(t63, "else", 0);
        t62.addChild(t621, "field", 0);
        t62.addChild(t622, "value", 0);
        t63.addChild(t631, "field", 0);
        t63.addChild(t632, "value", 0);

        // this is for variable label testing. We should get the largest pattern as labels 0 1 2 1 0
        t7 = new NodeWrapper("If");
        NodeWrapper t71 = new NodeWrapper("Variable");
        t71.getValues().put("value", "x");
        NodeWrapper t72 = new NodeWrapper("=");
        NodeWrapper t721 = new NodeWrapper("Variable");
        t721.getValues().put("value", "x");
        NodeWrapper t722 = new NodeWrapper("Variable");
        t722.getValues().put("value", "x");
        NodeWrapper t73 = new NodeWrapper("=");
        NodeWrapper t731 = new NodeWrapper("Variable");
        t731.getValues().put("value", "x");
        NodeWrapper t732 = new NodeWrapper("Variable");
        t732.getValues().put("value", "x");
        t7.addChild(t71, "condition", 0);
        t7.addChild(t72, "dhen", 0);
        t7.addChild(t73, "else", 0);
        t72.addChild(t721, "field", 0);
        t72.addChild(t722, "value", 0);
        t73.addChild(t731, "field", 0);
        t73.addChild(t732, "value", 0);
    }
}
