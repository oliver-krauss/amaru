/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.nodes.impl.function;

import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Created by Oliver Krauss on 15.06.2016.
 */
@NodeInfo(shortName = "invoke", description = "Abstract base class for invoking functions")
public abstract class MinicInvokeNode extends MinicExpressionNode {

    @NodeInfo(shortName = "invoke-void", description = "Invokes a function without a return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeVoidNode extends MinicExpressionNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeVoidNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

    @NodeInfo(shortName = "invoke-char", description = "Invokes a function with a char return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeCharNode extends MinicCharNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeCharNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public char executeChar(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return (char) dispatchNode.executeDispatch(frame, function, argumentValues);
        }

    }

    @NodeInfo(shortName = "invoke-int", description = "Invokes a function with an int return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeIntNode extends MinicIntNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeIntNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public int executeInt(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return (int) dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

    @NodeInfo(shortName = "invoke-float", description = "Invokes a function with a float return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeFloatNode extends MinicFloatNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeFloatNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public float executeFloat(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return (float) dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

    @NodeInfo(shortName = "invoke-double", description = "Invokes a function with a double return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeDoubleNode extends MinicDoubleNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeDoubleNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public double executeDouble(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return (double) dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

    @NodeInfo(shortName = "invoke-string", description = "Invokes a function with a string return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeStringNode extends MinicStringNode {

        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeStringNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public String executeString(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return (String) dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

    @NodeInfo(shortName = "invoke-array", description = "Invokes a function with an array return value")
    @NodeChildren({@NodeChild(value = "functionNode", type = MinicExpressionNode.class)})
    public static abstract class MinicInvokeArrayNode extends MinicExpressionNode {
        // TODO #8 we should replace this with a pointer invocation as soon as pointers are implemented
        /**
         * Arguments to be passed to function
         */
        @Children
        protected final MinicExpressionNode[] argumentNodes;

        /**
         * Dispatch node that will call the function
         */
        @Child
        private MinicDispatchNode dispatchNode;

        MinicInvokeArrayNode(MinicExpressionNode[] argumentNodes) {
            this.argumentNodes = argumentNodes;
            this.dispatchNode = MinicDispatchNodeGen.create();
        }

        @Specialization
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame, MinicFunctionNode function) {

            // Collect and execute arguments for function
            int argNodeSize = argumentNodes != null ? argumentNodes.length : 0;
            CompilerAsserts.partialEvaluationConstant(argNodeSize);
            Object[] argumentValues = new Object[argNodeSize];
            for (int i = 0; i < argNodeSize; i++) {
                argumentValues[i] = argumentNodes[i].executeGeneric(frame);
            }

            // execute function
            return dispatchNode.executeDispatch(frame, function, argumentValues);
        }
    }

}
