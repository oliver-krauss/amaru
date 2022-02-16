/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.executor;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.PrintNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.optimization.language.MinicAccessor;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * WARNING: The tests will fail if the -ea VM option si active
 * @author Oliver Krauss on 29.10.2019
 */
public class InternalExecutorTest {

    @Test
    public void testInternalExecutor() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";

        // when
        InternalExecutor test = new InternalExecutor(language, code, function, function);

        // then
        Assert.assertNotNull(test.getMain());
        Assert.assertNotNull(test.getOut());
        Assert.assertNotNull(test.getOrigin());
        Assert.assertNotNull(test.getRoot());
    }

    @Test
    public void testInternalExecutorTestOrigin() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";
        InternalExecutor test = new InternalExecutor(language, code, function, function);

        // when
        ExecutionResult result = test.test(test.getOrigin(), null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), 0);
        Assert.assertEquals(result.getOutStreamValue(), "8\n");
    }

    @Test
    public void testInternalExecutorTest() {
        // given
        String language = "c";
        String code = "int main() {\n" +
            "    print(3 + 5);\n" +
            "    return 0;\n" +
            "}";
        String function = "main";
        InternalExecutor test = new InternalExecutor(language, code, function, function);

        MinicExpressionNode[] ex = {new MinicSimpleLiteralNode.MinicIntLiteralNode(1)};
        PrintNode printNode = PrintNodeFactory.create(ex, MinicAccessor.getCurrentContext());

        // when
        ExecutionResult result = test.test(printNode, null);

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getReturnValue(), null);
        Assert.assertEquals(result.getOutStreamValue(), "1\n");
    }
}
