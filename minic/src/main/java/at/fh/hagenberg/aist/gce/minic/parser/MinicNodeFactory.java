/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.minic.parser;

import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicExpressionNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicNode;
import at.fh.hagenberg.aist.gce.minic.nodes.base.MinicRootNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.*;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.*;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionBodyNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicReadFunctionArgumentNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.complex.MinicStringArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicFloatArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicCharArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.complex.MinicStringLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicDoubleLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.floating.MinicFloatLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.integer.signed.MinicCharLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.logical.integer.signed.MinicIntLogicalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.complex.MinicStringRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicDoubleRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.floating.MinicFloatRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicCharRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.rel.integer.signed.MinicIntRelationalNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.complex.MinicStringUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicDoubleUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.floating.MinicFloatUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.integer.signed.MinicCharUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.unary.integer.signed.MinicIntUnaryNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.complex.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicDoubleNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicIntNode;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import java.util.*;

/**
 * Helper Class for the Parser, handles the construction of the truffle class objects
 * Created by Oliver Krauss on 29.06.2016.
 */
public class MinicNodeFactory {

    /* State while parsing a source unit. */
    private final MinicContext context;
    private final Source source;
    private final FrameDescriptor globalFrameDescriptor;
    private final MaterializedFrame globalFrame;
    private final Map<String, MinicStructNode> structList;

    public MinicNodeFactory(MinicContext context, Source source) {
        this.context = context;
        this.source = source;
        this.globalFrameDescriptor = new FrameDescriptor();
        globalFrame = Truffle.getRuntime().createMaterializedFrame(null, globalFrameDescriptor);
        this.globalScope = new LexicalScope(null);
        this.context.setGlobalStorage(globalFrame);
        this.structList = new HashMap<>();
    }

    public MinicNode createForLoop(MinicNode initNode, MinicExpressionNode conditionNode, MinicNode stepNode, MinicNode bodyNode) {
        final MinicForNode forNode = new MinicForNode(initNode, conditionNode, stepNode, bodyNode);
        return forNode;
    }

    /**
     * Local variable names that are visible in the current block. Variables are not visible outside
     * of their defining block, to prevent the usage of undefined variables. Because of that, we can
     * decide during parsing if a name references a local variable or is a function name.
     */
    static class LexicalScope {
        protected final LexicalScope outer;
        protected final Map<String, LexicalVariable> locals;

        LexicalScope(LexicalScope outer) {
            this.outer = outer;
            this.locals = new HashMap<>();
            // TODO #8: the following might be a bug. Check and delete if true
            //if (outer != null) {
            //    locals.putAll(outer.locals);
            //}
        }
    }

    static class LexicalVariable {
        protected final MinicBaseType type;
        protected final FrameSlot frameSlot;
        protected final boolean constant;
        protected final MinicExpressionNode[] arraySize;

        LexicalVariable(MinicBaseType type, FrameSlot frameSlot) {
            this.type = type;
            this.frameSlot = frameSlot;
            this.constant = false;
            this.arraySize = null;
        }

        LexicalVariable(MinicBaseType type, FrameSlot frameSlot, boolean constant) {
            this.type = type;
            this.frameSlot = frameSlot;
            this.constant = constant;
            this.arraySize = null;
        }

        LexicalVariable(MinicBaseType type, FrameSlot frameSlot, MinicExpressionNode[] arraySize) {
            this.type = type;
            this.frameSlot = frameSlot;
            this.arraySize = arraySize;
            this.constant = false;
        }
    }

    static class LexicalStruct extends LexicalVariable {

        protected final LexicalScope scope;
        protected final MinicStructNode definition;
        protected final FrameDescriptor descriptor;

        LexicalStruct(MinicBaseType type, FrameSlot frameSlot, LexicalScope scope, MinicStructNode definition, FrameDescriptor descriptor) {
            super(type, frameSlot);
            this.scope = scope;
            this.definition = definition;
            this.descriptor = descriptor;
        }

        LexicalStruct(MinicBaseType type, FrameSlot frameSlot, MinicExpressionNode[] arraySize, LexicalScope scope, MinicStructNode definition, FrameDescriptor descriptor) {
            super(type, frameSlot, arraySize);
            this.scope = scope;
            this.definition = definition;
            this.descriptor = descriptor;
        }
    }

    /* State while parsing a function. */
    private String functionName;
    private MinicExpressionNode functionReturn;
    private int parameterCount;
    private FrameDescriptor frameDescriptor;
    private List<MinicNode> methodNodes;

    /* State while parsing a block. */
    private LexicalScope lexicalScope;

    private LexicalScope globalScope;

    public void startFunction(Token nameToken, MinicBaseType type) {
        assert functionName == null;
        assert parameterCount == 0;
        assert frameDescriptor == null;
        assert lexicalScope == null;
        assert functionReturn == null;

        functionName = nameToken.val;
        frameDescriptor = new FrameDescriptor();
        methodNodes = new ArrayList<>();
        startBlock();

        // pre register the function so self-recursion is possible
        final MinicRootNode rootNode = new MinicRootNode(MinicLanguage.INSTANCE, this.context, frameDescriptor, null, functionName);
        context.getFunctionRegistry().register(functionName, rootNode, type);
    }

    private Stack<Pair<LexicalScope, FrameDescriptor>> structStack = new Stack<>();

    public MinicNode startStruct(Token structName) {
        structStack.push(new Pair<>(lexicalScope, frameDescriptor));
        String name = structName.val;
        MinicStructNode node = null;
        if (name == null || !structList.containsKey(name)) {
            lexicalScope = new LexicalScope(null);
            frameDescriptor = new FrameDescriptor();
            node = new MinicStructNode(lexicalScope, frameDescriptor);
            if (name != null) {
                // if the struct is unnamed it can only be used in this scope
                // by a parent struct or for unnamed initialization of a struct-var
                structList.put(name, node);
            }
        } else if (structList.containsKey(name)) {
            node = structList.get(name);
            lexicalScope = node.getScope();
            frameDescriptor = node.getDescriptor();
        }
        return node;
    }

    public void finishStruct() {
        Pair<LexicalScope, FrameDescriptor> pair = structStack.pop();
        lexicalScope = pair.getKey();
        frameDescriptor = pair.getValue();
    }

    public void enterStruct(Token structName) {
        LexicalVariable variable = findVariable(structName);
        if (!(variable instanceof LexicalStruct)) {
            throw new RuntimeException("Can't access non-struct '" + structName.val + "' with '.'");
        }
        LexicalStruct struct = (LexicalStruct) variable;
        structStack.push(new Pair<LexicalScope, FrameDescriptor>(lexicalScope, frameDescriptor));

        lexicalScope = struct.scope;
        frameDescriptor = struct.descriptor;
    }

    public void leaveStruct() {
        while (!structStack.empty()) {
            Pair<LexicalScope, FrameDescriptor> pair = structStack.pop();
            lexicalScope = pair.getKey();
            frameDescriptor = pair.getValue();
        }
    }

    public void addStructField(MinicNode structDecl, MinicNode variable) {
        ((MinicStructNode) structDecl).addInitializationNode(variable);
        // TODO #8 -> still don't know where to call those init-nodes
    }

    public MinicNode defineVariable(Token nameToken, Object type, List<MinicExpressionNode> arraySize) {
        if (type instanceof MinicBaseType) {
            return defineBaseVariable(nameToken, (MinicBaseType) type, arraySize);
        } else if (type instanceof MinicStructNode) {
            return defineBaseVariable(nameToken, MinicBaseType.STRUCT, (MinicStructNode) type, arraySize);
        }
        return null;
    }

    private MinicNode defineBaseVariable(Token nameToken, MinicBaseType type, List<MinicExpressionNode> arraySize) {
        return defineBaseVariable(nameToken, type, null, arraySize);
    }

    private LexicalVariable createVariable(MinicBaseType type, FrameSlot frameSlot, MinicExpressionNode[] arraySize, MinicStructNode structDefinition) {
        if (!type.equals(MinicBaseType.STRUCT)) {
            return new LexicalVariable(type, frameSlot, arraySize);
        } else {
            FrameDescriptor descriptor = structDefinition.descriptor.copy();
            LexicalScope variableScope = createStructMemory(structDefinition, descriptor);
            return new LexicalStruct(type, frameSlot, arraySize, variableScope, structDefinition, descriptor);
        }
    }

    /**
     * Create a new struct reference to a FrameDescriptor
     *
     * @param description struct description
     * @param descriptor  the memory this scope is mapped to
     * @return new variable from struct
     */
    private LexicalScope createStructMemory(MinicStructNode description, FrameDescriptor descriptor) {
        LexicalScope scope = new LexicalScope(lexicalScope);

        description.scope.locals.forEach((key, value) -> {
            if (value.type.equals(MinicBaseType.STRUCT)) {
                scope.locals.put(key, createVariable(value.type, descriptor.findFrameSlot(key), value.arraySize, ((LexicalStruct) value).definition));
            } else {
                scope.locals.put(key, createVariable(value.type, descriptor.findFrameSlot(key), value.arraySize, null));
            }

        });

        return scope;
    }

    private MinicNode defineBaseVariable(Token nameToken, MinicBaseType type, MinicStructNode structDefinition, List<MinicExpressionNode> arraySize) {
        MinicIntNode[] size = null;
        if (arraySize != null && arraySize.size() > 0) {
            size = arraySize.toArray(new MinicIntNode[arraySize.size()]);
        }
        if (lexicalScope == null) {
            // global variable
            if (globalScope.locals.containsKey(nameToken.val)) {
                throw new AssertionError("global variable " + nameToken.val + " already exists");
            }
            globalScope.locals.put(nameToken.val, createVariable(type, null, size, structDefinition));

            // Assign global array immediately
            if (size != null) {
                int[] evaluatedSize = new int[size.length];
                for (int i = 0; i < size.length; i++) {
                    evaluatedSize[i] = (int) size[i].executeGeneric(null);
                }
                switch (type) {
                    case CHAR:
                        globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), new MinicCharArray(evaluatedSize));
                        globalFrame.getFrameDescriptor().setFrameSlotKind(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), FrameSlotKind.Object);
                        break;
                    case INT:
                        globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), new MinicIntArray(evaluatedSize));
                        globalFrame.getFrameDescriptor().setFrameSlotKind(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), FrameSlotKind.Object);
                        break;
                    case FLOAT:
                        globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), new MinicFloatArray(evaluatedSize));
                        globalFrame.getFrameDescriptor().setFrameSlotKind(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), FrameSlotKind.Object);
                        break;
                    case DOUBLE:
                        globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), new MinicDoubleArray(evaluatedSize));
                        globalFrame.getFrameDescriptor().setFrameSlotKind(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), FrameSlotKind.Object);
                        break;
                    case STRING:
                        globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), new MinicStringArray(evaluatedSize));
                        globalFrame.getFrameDescriptor().setFrameSlotKind(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), FrameSlotKind.Object);
                        break;
                    case STRUCT:
                        System.out.println("MinicNodeFactory.defineBaseVariable structGlobalArray -> not yet implemented");
                        break; // TODO #11 add a MinicStructArray
                    default:
                        throw new AssertionError(nameToken.val);
                }
            }
        } else {
            // local variable
            LexicalVariable variable = lexicalScope.locals.get(nameToken.val);
            if (variable != null) {
                throw new AssertionError("variable " + nameToken.val + " already exists in scope");
            }
            FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(nameToken.val);

            lexicalScope.locals.put(nameToken.val, createVariable(type, frameSlot, size, structDefinition));

            // create array allocator for local array
            if (size != null) {
                switch (type) {
                    case CHAR:
                        return AllocateArrayNodeFactory.MinicAllocateCharArrayNodeGen.create(size, frameSlot);
                    case INT:
                        return AllocateArrayNodeFactory.MinicAllocateIntArrayNodeGen.create(size, frameSlot);
                    case FLOAT:
                        return AllocateArrayNodeFactory.MinicAllocateFloatArrayNodeGen.create(size, frameSlot);
                    case DOUBLE:
                        return AllocateArrayNodeFactory.MinicAllocateDoubleArrayNodeGen.create(size, frameSlot);
                    case STRING:
                        return AllocateArrayNodeFactory.MinicAllocateStringArrayNodeGen.create(size, frameSlot);
                    case STRUCT:
                        System.out.println("MinicNodeFactory.defineBaseVariable structLocalArray -> not yet implemented");
                        break; // TODO #11 add a MinicAllocateStructArray
                    default:
                        throw new AssertionError(nameToken.val);
                }
            }
        }

        return null; // withouth arrays nothing needs to be allocated
    }

    public MinicNode defineConstant(Token nameToken, MinicBaseType type, MinicExpressionNode value) {
        if (lexicalScope == null) {
            // global constant
            globalScope.locals.put(nameToken.val, new LexicalVariable(type, null, true));
            // Global constants will be preassigned unlike local constants.
            switch (type) {
                case CHAR:
                    globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), ((MinicCharNode) value).executeChar(null));
                    break;
                case INT:
                    globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), ((MinicIntNode) value).executeInt(null));
                    break;
                case FLOAT:
                    globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), ((MinicFloatNode) value).executeFloat(null));
                    break;
                case DOUBLE:
                    globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), ((MinicDoubleNode) value).executeDouble(null));
                    break;
                case STRING:
                    globalFrame.setObject(globalFrameDescriptor.findOrAddFrameSlot(nameToken.val), ((MinicStringNode) value).executeString(null));
                    break;
                default:
                    throw new AssertionError(nameToken.val);
            }
            return null;
        } else {
            // local constant
            LexicalVariable variable = lexicalScope.locals.get(nameToken.val);
            if (variable != null) {
                throw new AssertionError("variable " + nameToken.val + " already exists in scope");
            }
            FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(nameToken.val);
            lexicalScope.locals.put(nameToken.val, new LexicalVariable(type, frameSlot, true));
            return createAssignment(nameToken, value, findVariable(nameToken), frameSlot);
        }
    }

    public void addFormalParameter(Token nameToken, MinicBaseType type, MinicNode variableNode) {
        /*
         * Method parameters are assigned to local variables at the beginning of the method. This
         * ensures that accesses to parameters are specialized the same way as local variables are
         * specialized.
         */
        MinicExpressionNode readArg = null;
        MinicNode assignment = null;

        if (variableNode == null) {
            // simple value assignment
            switch (type) {
                case CHAR:
                    readArg = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentCharNodeGen.create(parameterCount);
                    break;
                case INT:
                    readArg = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentIntNodeGen.create(parameterCount);
                    break;
                case FLOAT:
                    readArg = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentFloatNodeGen.create(parameterCount);
                    break;
                case DOUBLE:
                    readArg = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentDoubleNodeGen.create(parameterCount);
                    break;
                case STRING:
                    readArg = MinicReadFunctionArgumentNodeFactory.MinicReadFunctionArgumentStringNodeGen.create(parameterCount);
                    break;
                default:
                    throw new AssertionError("Argument Type of " + nameToken.val + " not known");
            }
            // add an assignment for when the method is called
            assignment = createAssignment(nameToken, readArg);
        } else {
            // C does everything by ref and also doesn't check the size so we will simply read and store
            // C itself doesn't really check ranges, so we won't either
            readArg = MinicReadFunctionArgumentNodeFactory.MinicReadGenericFunctionArgumentNodeGen.create(parameterCount);
            assignment = CopyArrayNodeFactory.MinicCopyGenericArrayNodeGen.create(readArg, ((AllocateArrayNode) variableNode).getSlot());
        }


        methodNodes.add(assignment);
        parameterCount++;
    }


    public void finishFunction(MinicNode bodyNode, MinicBaseType type) {
        // check if function is allowed to have return
        if (type == MinicBaseType.VOID && functionReturn != null) {
            throw new AssertionError("function " + functionName + " is void. Return statement invalid");
        }
        if (type != MinicBaseType.VOID && functionReturn == null) {
            throw new AssertionError("function " + functionName + " is not void. Return statement must be provided");
        }
        if (type != MinicBaseType.VOID && type != findType(functionReturn)) {
            throw new AssertionError("function " + functionName + " is of wrong return type");
        }

        // the method nodes consist of all parameters and the body which make up a new "virtual" block statement
        methodNodes.add(bodyNode);
        final MinicNode methodBlock = finishBlock(methodNodes);
        assert lexicalScope == null : "Wrong scoping of blocks in parser";

        final MinicExpressionNode functionBodyNode = new MinicFunctionBodyNode(methodBlock);

        final MinicRootNode rootNode = new MinicRootNode(MinicLanguage.INSTANCE, this.context, frameDescriptor, functionBodyNode, functionName);

        context.getFunctionRegistry().register(functionName, rootNode, type);

        functionName = null;
        parameterCount = 0;
        frameDescriptor = null;
        lexicalScope = null;
        functionReturn = null;
    }

    public void finishFunctionDeclaration(MinicBaseType type) {
        // pre register the function so self-recursion is possible
        final MinicRootNode rootNode = new MinicRootNode(MinicLanguage.INSTANCE, this.context, frameDescriptor, null, functionName);
        context.getFunctionRegistry().register(functionName, rootNode, type);

        functionName = null;
        parameterCount = 0;
        frameDescriptor = null;
        lexicalScope = null;
        functionReturn = null;
    }

    public MinicNode finishBlock(List<MinicNode> bodyNodes) {
        lexicalScope = lexicalScope.outer;

        List<MinicNode> flattenedNodes = new ArrayList<>(bodyNodes.size());
        flattenBlocks(bodyNodes, flattenedNodes);
        return new MinicBlockNode(flattenedNodes.toArray(new MinicNode[flattenedNodes.size()]));
    }

    private void flattenBlocks(Iterable<? extends Node> bodyNodes, List<MinicNode> flattenedNodes) {
        for (Node n : bodyNodes) {
            if (n instanceof MinicBlockNode) {
                flattenBlocks(n.getChildren(), flattenedNodes);
            } else {
                flattenedNodes.add((MinicNode) n);
            }
        }
    }

    /**
     * Starts a new lexical scope in the program execution.
     * Per Definition C
     */
    public void startBlock() {
        lexicalScope = new LexicalScope(lexicalScope);
    }


    public MinicNode createWhileLoop(MinicExpressionNode conditionNode, MinicNode bodyNode) {
        final MinicWhileNode whileNode = new MinicWhileNode(conditionNode, bodyNode);
        return whileNode;
    }

    private LexicalVariable findVariable(Token nameToken) {
        LexicalScope callStackScope = lexicalScope.outer;
        // look for local variable
        LexicalVariable variable = lexicalScope.locals.get(nameToken.val);
        // search up in scope until variable found or nothing
        while (callStackScope != null && variable == null) {
            variable = callStackScope.locals.get(nameToken.val);
            callStackScope = callStackScope.outer;
        }
        // finally try looking through the global variables
        if (variable == null) {
            variable = globalScope.locals.get(nameToken.val);
        }
        // if no variable found ask developer kindly to supply it
        if (variable == null) {
            // Assignments are only valid if the variable was defined beforehand
            throw new AssertionError("variable " + nameToken.val + " doesn't exist in scope");
        }

        return variable;
    }

    public MinicNode createAssignment(Token nameToken, MinicExpressionNode valueNode) {
        // look for variable
        LexicalVariable var = findVariable(nameToken);
        FrameSlot frameSlot = var.frameSlot;

        if (var.constant) {
            throw new AssertionError("Constant " + nameToken.val + " cannot' be assigned");
        }

        return createAssignment(nameToken, valueNode, var, frameSlot);
    }

    public MinicNode createAssignment(Token nameToken, MinicExpressionNode valueNode, LexicalVariable var, FrameSlot frameSlot) {
        if (frameSlot == null) {
            // global
            FrameSlot globalFrameSlot = globalFrameDescriptor.findOrAddFrameSlot(nameToken.val);
            switch (var.type) {
                case CHAR:
                    return MinicWriteGlobalNodeFactory.MinicCharWriteGlobalNodeGen.create((MinicCharNode) valueNode, globalFrameSlot, globalFrame);
                case INT:
                    return MinicWriteGlobalNodeFactory.MinicIntWriteGlobalNodeGen.create((MinicIntNode) valueNode, globalFrameSlot, globalFrame);
                case FLOAT:
                    return MinicWriteGlobalNodeFactory.MinicFloatWriteGlobalNodeGen.create((MinicFloatNode) valueNode, globalFrameSlot, globalFrame);
                case DOUBLE:
                    return MinicWriteGlobalNodeFactory.MinicDoubleWriteGlobalNodeGen.create((MinicDoubleNode) valueNode, globalFrameSlot, globalFrame);
                case STRING:
                    return MinicWriteGlobalNodeFactory.MinicStringWriteGlobalNodeGen.create((MinicStringNode) valueNode, globalFrameSlot, globalFrame);
                case STRUCT:
                    System.out.println("MinicNodeFactory.createAssignment structGlobal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(var.type);
            }
        } else {
            // local
            switch (var.type) {
                case CHAR:
                    return MinicWriteNodeFactory.MinicCharWriteNodeGen.create((MinicCharNode) valueNode, frameSlot);
                case INT:
                    return MinicWriteNodeFactory.MinicIntWriteNodeGen.create((MinicIntNode) valueNode, frameSlot);
                case FLOAT:
                    return MinicWriteNodeFactory.MinicFloatWriteNodeGen.create((MinicFloatNode) valueNode, frameSlot);
                case DOUBLE:
                    return MinicWriteNodeFactory.MinicDoubleWriteNodeGen.create((MinicDoubleNode) valueNode, frameSlot);
                case STRING:
                    return MinicWriteNodeFactory.MinicStringWriteNodeGen.create((MinicStringNode) valueNode, frameSlot);
                case STRUCT:
                    System.out.println("MinicNodeFactory.createAssignment structLocal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(var.type);
            }
        }
    }

    public MinicNode createArrayAssignment(Token nameToken, MinicExpressionNode valueNode, List<MinicExpressionNode> arrayPositions) {
        MinicIntNode[] positions = arrayPositions.toArray(new MinicIntNode[arrayPositions.size()]);

        // look for variable
        LexicalVariable var = findVariable(nameToken);
        FrameSlot frameSlot = var.frameSlot;

        if (var.constant) {
            throw new AssertionError("Constant " + nameToken.val + " cannot' be assigned");
        } else if (var.type == MinicBaseType.STRING && var.arraySize == null && positions.length != 1) {
            throw new AssertionError("Strings cannot be accessed with more than one dimension");
        } else if (var.arraySize != null && positions.length != var.arraySize.length) {
            throw new AssertionError("Dimensions not compatible with size of " + nameToken.val);
        }


        if (frameSlot == null) {
            FrameSlot globalFrameSlot = globalFrameDescriptor.findOrAddFrameSlot(nameToken.val);
            // global
            switch (var.type) {
                case CHAR:
                    return MinicWriteGlobalArrayNodeFactory.MinicCharArrayWriteGlobalNodeGen.create(positions, (MinicCharNode) valueNode, globalFrameSlot, globalFrame);
                case INT:
                    return MinicWriteGlobalArrayNodeFactory.MinicIntArrayWriteGlobalNodeGen.create(positions, (MinicIntNode) valueNode, globalFrameSlot, globalFrame);
                case FLOAT:
                    return MinicWriteGlobalArrayNodeFactory.MinicFloatArrayWriteGlobalNodeGen.create(positions, (MinicFloatNode) valueNode, globalFrameSlot, globalFrame);
                case DOUBLE:
                    return MinicWriteGlobalArrayNodeFactory.MinicDoubleArrayWriteGlobalNodeGen.create(positions, (MinicDoubleNode) valueNode, globalFrameSlot, globalFrame);
                case STRING:
                    if (var.arraySize != null) {
                        return MinicWriteGlobalArrayNodeFactory.MinicStringArrayWriteGlobalNodeGen.create(positions, (MinicStringNode) valueNode, globalFrameSlot, globalFrame);
                    } else {
                        return MinicWriteGlobalArrayNodeFactory.MinicStringAsCharArrayWriteGlobalNodeGen.create((MinicCharNode) valueNode, (MinicIntNode) positions[0], globalFrameSlot, globalFrame);
                    }
                case STRUCT:
                    System.out.println("MinicNodeFactory.createArrayAssignment structArrayGlobal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(var.type);
            }
        } else {
            // local
            switch (var.type) {
                case CHAR:
                    return MinicWriteArrayNodeFactory.MinicCharArrayWriteNodeGen.create(positions, (MinicCharNode) valueNode, frameSlot);
                case INT:
                    return MinicWriteArrayNodeFactory.MinicIntArrayWriteNodeGen.create(positions, (MinicIntNode) valueNode, frameSlot);
                case FLOAT:
                    return MinicWriteArrayNodeFactory.MinicFloatArrayWriteNodeGen.create(positions, (MinicFloatNode) valueNode, frameSlot);
                case DOUBLE:
                    return MinicWriteArrayNodeFactory.MinicDoubleArrayWriteNodeGen.create(positions, (MinicDoubleNode) valueNode, frameSlot);
                case STRING:
                    if (var.arraySize != null) {
                        return MinicWriteArrayNodeFactory.MinicStringArrayWriteNodeGen.create(positions, (MinicStringNode) valueNode, frameSlot);
                    } else {
                        return MinicWriteArrayNodeFactory.MinicStringAsCharArrayWriteNodeGen.create((MinicCharNode) valueNode, (MinicIntNode) positions[0], frameSlot);
                    }
                case STRUCT:
                    System.out.println("MinicNodeFactory.createArrayAssignment structArrayLocal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(var.type);
            }
        }
    }

    public MinicNode createIf(MinicExpressionNode condition, MinicNode thenStatement, MinicNode elseStatement) {
        final MinicIfNode ifNode = new MinicIfNode(condition, thenStatement, elseStatement);
        return ifNode;
    }

    public MinicNode createReturn(MinicExpressionNode returnStatement) {
        final MinicReturnNode returnNode = new MinicReturnNode(returnStatement);
        // TODO #8 Techically this only checks if there is a return anywhere. We don't check for unreachable code, and neither if there are control-paths without a return
        functionReturn = returnStatement;
        return returnNode;
    }

    public MinicExpressionNode createUnary(Token opToken, MinicExpressionNode expressionNode) {
        MinicBaseType type = findType(expressionNode);
        switch (opToken.val) {
            case "!":
                return createLogicalNotUnary(expressionNode, type);
            case "+":
                return expressionNode; // Yes this is allowed in C, and a plus essentially does nothing
            case "-":
                return createBinary(opToken, new MinicSimpleLiteralNode.MinicIntLiteralNode(0), expressionNode);
            default:
                throw new RuntimeException("unexpected operation: " + opToken.val);
        }
    }

    private MinicExpressionNode createLogicalNotUnary(MinicExpressionNode value, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharUnaryNodeFactory.MinicCharLogicalNotNodeGen.create((MinicCharNode) value);
            case INT:
                return MinicIntUnaryNodeFactory.MinicIntLogicalNotNodeGen.create((MinicIntNode) value);
            case FLOAT:
                return MinicFloatUnaryNodeFactory.MinicFloatLogicalNotNodeGen.create((MinicFloatNode) value);
            case DOUBLE:
                return MinicDoubleUnaryNodeFactory.MinicDoubleLogicalNotNodeGen.create((MinicDoubleNode) value);
            case STRING:
                return MinicStringUnaryNodeFactory.MinicStringLogicalNotNodeGen.create((MinicStringNode) value);
            default:
                throw new AssertionError(type + " not supported for operator !=");
        }
    }


    public MinicBaseType selectType(Token typeToken) {
        switch (typeToken.val) {
            case "char":
                return MinicBaseType.CHAR;
            case "int":
                return MinicBaseType.INT;
            case "float":
                return MinicBaseType.FLOAT;
            case "double":
                return MinicBaseType.DOUBLE;
            case "string":
                return MinicBaseType.STRING;
            case "array": // TODO #8 this is an ugly cheat as we currently don't support pointers or structs
                return MinicBaseType.ARRAY;
            default:
                throw new AssertionError(typeToken.val);
        }
    }


    public MinicExpressionNode createCall(MinicExpressionNode functionNode, List<MinicExpressionNode> parameterNodes) {
        if (!(functionNode instanceof MinicFunctionLiteralNode)) {
            throw new AssertionError("invocation is only allowed on function literals");
        }
        MinicBaseType type = findFunctionType((MinicFunctionLiteralNode) functionNode);
        switch (type) {
            case CHAR:
                return MinicInvokeNodeFactory.MinicInvokeCharNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case INT:
                return MinicInvokeNodeFactory.MinicInvokeIntNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case FLOAT:
                return MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case DOUBLE:
                return MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case STRING:
                return MinicInvokeNodeFactory.MinicInvokeStringNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case VOID:
                return MinicInvokeNodeFactory.MinicInvokeVoidNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            case ARRAY:
                return MinicInvokeNodeFactory.MinicInvokeArrayNodeGen.create(parameterNodes.toArray(new MinicExpressionNode[parameterNodes.size()]), functionNode);
            default:
                throw new AssertionError("Function type is unknown");
        }
    }

    private MinicBaseType findFunctionType(MinicFunctionLiteralNode function) {
        // TODO: #8 currently we simply assume VOID when the function was not registered before.
        return context.getFunctionRegistry().lookup(function.getName()).getType();
    }

    public MinicExpressionNode createRead(Token nameToken) {
        LexicalVariable variable = findVariable(nameToken);

        MinicBaseType type = variable.type;
        FrameSlot frameSlot = variable.frameSlot;

        if (frameSlot == null) {
            FrameSlot globalFrameSlot = globalFrameDescriptor.findOrAddFrameSlot(nameToken.val);
            // global array without positional access
            if (variable.arraySize != null) {
                return MinicReadGlobalArrayNodeFactory.MinicEntireArrayReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
            }
            // global variable
            switch (type) {
                case CHAR:
                    return MinicReadGlobalNodeFactory.MinicCharReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
                case INT:
                    return MinicReadGlobalNodeFactory.MinicIntReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
                case FLOAT:
                    return MinicReadGlobalNodeFactory.MinicFloatReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
                case DOUBLE:
                    return MinicReadGlobalNodeFactory.MinicDoubleReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
                case STRING:
                    return MinicReadGlobalNodeFactory.MinicStringReadGlobalNodeGen.create(globalFrameSlot, globalFrame);
                case STRUCT:
                    System.out.println("MinicNodeFactory.createRead structGlobal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(nameToken.val);
            }
        } else {
            // local array without positional access
            if (variable.arraySize != null) {
                return MinicReadArrayNodeFactory.MinicEntireArrayReadNodeGen.create(frameSlot);
            }

            // local variable
            switch (type) {
                case CHAR:
                    return MinicReadNodeFactory.MinicCharReadNodeGen.create(frameSlot);
                case INT:
                    return MinicReadNodeFactory.MinicIntReadNodeGen.create(frameSlot);
                case FLOAT:
                    return MinicReadNodeFactory.MinicFloatReadNodeGen.create(frameSlot);
                case DOUBLE:
                    return MinicReadNodeFactory.MinicDoubleReadNodeGen.create(frameSlot);
                case STRING:
                    return MinicReadNodeFactory.MinicStringReadNodeGen.create(frameSlot);
                case STRUCT:
                    System.out.println("MinicNodeFactory.createRead structLocal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(nameToken.val);
            }
        }
    }

    public MinicExpressionNode createArrayRead(Token nameToken, List<MinicExpressionNode> arrayPositions) {
        LexicalVariable variable = findVariable(nameToken);
        MinicIntNode[] positions = arrayPositions.toArray(new MinicIntNode[arrayPositions.size()]);

        MinicBaseType type = variable.type;
        FrameSlot frameSlot = variable.frameSlot;
        if (type != MinicBaseType.STRING && variable.arraySize == null) {
            throw new AssertionError("Variable of name " + nameToken.val + " is not an array!");
        } else if (variable.type == MinicBaseType.STRING && variable.arraySize == null && positions.length != 1) {
            throw new AssertionError("Strings cannot be accessed with more than one dimension");
        } else if (variable.arraySize != null && positions.length != variable.arraySize.length) {
            throw new AssertionError("Dimensions not compatible with size of " + nameToken.val);
        }

        if (frameSlot == null) {
            // global variable
            FrameSlot globalFrameSlot = globalFrameDescriptor.findOrAddFrameSlot(nameToken.val);
            switch (type) {
                case CHAR:
                    return MinicReadGlobalArrayNodeFactory.MinicCharArrayReadGlobalNodeGen.create(positions, globalFrameSlot, globalFrame);
                case INT:
                    return MinicReadGlobalArrayNodeFactory.MinicIntArrayReadGlobalNodeGen.create(positions, globalFrameSlot, globalFrame);
                case FLOAT:
                    return MinicReadGlobalArrayNodeFactory.MinicFloatArrayReadGlobalNodeGen.create(positions, globalFrameSlot, globalFrame);
                case DOUBLE:
                    return MinicReadGlobalArrayNodeFactory.MinicDoubleArrayReadGlobalNodeGen.create(positions, globalFrameSlot, globalFrame);
                case STRING:
                    if (variable.arraySize != null) {
                        return MinicReadGlobalArrayNodeFactory.MinicStringArrayReadGlobalNodeGen.create(positions, globalFrameSlot, globalFrame);
                    } else {
                        return MinicReadGlobalArrayNodeFactory.MinicStringAsCharArrayReadGlobalNodeGen.create(positions[0], globalFrameSlot, globalFrame);
                    }
                case STRUCT:
                    System.out.println("MinicNodeFactory.createArrayRead structArrayGlobal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(nameToken.val);
            }
        } else {
            // local variable
            switch (type) {
                case CHAR:
                    return MinicReadArrayNodeFactory.MinicCharArrayReadNodeGen.create(positions, frameSlot);
                case INT:
                    return MinicReadArrayNodeFactory.MinicIntArrayReadNodeGen.create(positions, frameSlot);
                case FLOAT:
                    return MinicReadArrayNodeFactory.MinicFloatArrayReadNodeGen.create(positions, frameSlot);
                case DOUBLE:
                    return MinicReadArrayNodeFactory.MinicDoubleArrayReadNodeGen.create(positions, frameSlot);
                case STRING:
                    if (variable.arraySize != null) {
                        return MinicReadArrayNodeFactory.MinicStringArrayReadNodeGen.create(positions, frameSlot);
                    } else {
                        return MinicReadArrayNodeFactory.MinicStringAsCharArrayReadNodeGen.create((MinicIntNode) positions[0], frameSlot);
                    }
                case STRUCT:
                    System.out.println("MinicNodeFactory.createArrayRead structArrayLocal -> not yet implemented");
                    return null;
                default:
                    throw new AssertionError(nameToken.val);
            }
        }
    }

    public MinicExpressionNode findFunction(Token nameToken) {
        // All functions are global so no need to look, just return
        return new MinicFunctionLiteralNode(nameToken.val);
    }

    public MinicExpressionNode createBinary(Token opToken, MinicExpressionNode left, MinicExpressionNode right) {
        MinicBaseType type = implicitCast(left, right);
        left = cast(left, type);
        right = cast(right, type);
        switch (opToken.val) {
            case "+":
                return createAdditionBinary(left, right, type);
            case "-":
                return createSubstractionBinary(left, right, type);
            case "*":
                return createMultiplicationBinary(left, right, type);
            case "/":
                return createDivisionBinary(left, right, type);
            case "%":
                return createModuloBinary(left, right, type);
            case "==":
                return createEqualsBinary(left, right, type);
            case "!=":
                return createNotEqualsBinary(left, right, type);
            case ">":
                return createGreaterBinary(left, right, type);
            case ">=":
                return createGreaterOrEqualsBinary(left, right, type);
            case "<":
                return createLesserBinary(left, right, type);
            case "<=":
                return createLesserOrEqualsBinary(left, right, type);
            case "||":
                return createOrBinary(left, right, type);
            case "&&":
                return createAndBinary(left, right, type);
            default:
                throw new RuntimeException("unexpected operation: " + opToken.val);
        }
    }

    private MinicExpressionNode createAdditionBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharArithmeticNodeFactory.MinicCharAddNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntArithmeticNodeFactory.MinicIntAddNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatArithmeticNodeFactory.MinicFloatAddNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleArithmeticNodeFactory.MinicDoubleAddNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringArithmeticNodeFactory.MinicStringAddNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator +");
        }
    }

    private MinicExpressionNode createSubstractionBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharArithmeticNodeFactory.MinicCharSubNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntArithmeticNodeFactory.MinicIntSubNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatArithmeticNodeFactory.MinicFloatSubNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleArithmeticNodeFactory.MinicDoubleSubNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            default:
                throw new AssertionError(type + " not supported for operator -");
        }
    }

    private MinicExpressionNode createMultiplicationBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharArithmeticNodeFactory.MinicCharMulNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntArithmeticNodeFactory.MinicIntMulNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatArithmeticNodeFactory.MinicFloatMulNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleArithmeticNodeFactory.MinicDoubleMulNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            default:
                throw new AssertionError(type + " not supported for operator *");
        }
    }

    private MinicExpressionNode createDivisionBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharArithmeticNodeFactory.MinicCharDivNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntArithmeticNodeFactory.MinicIntDivNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleArithmeticNodeFactory.MinicDoubleDivNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            default:
                throw new AssertionError(type + " not supported for operator /");
        }
    }

    private MinicExpressionNode createModuloBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharArithmeticNodeFactory.MinicCharModNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntArithmeticNodeFactory.MinicIntModNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatArithmeticNodeFactory.MinicFloatModNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleArithmeticNodeFactory.MinicDoubleModNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            default:
                throw new AssertionError(type + " not supported for operator %");
        }
    }

    private MinicExpressionNode createEqualsBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharEqualsNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntEqualsNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatEqualsNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleEqualsNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringEqualsNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator ==");
        }
    }

    private MinicExpressionNode createNotEqualsBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharNotEqualsNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntNotEqualsNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatNotEqualsNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleNotEqualsNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringNotEqualsNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator !=");
        }
    }

    private MinicExpressionNode createGreaterBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharGtNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntGtNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatGtNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleGtNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringGtNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator >");
        }
    }

    private MinicExpressionNode createGreaterOrEqualsBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharGtENodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntGtENodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatGtENodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleGtENodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringGtENodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator >=");
        }
    }

    private MinicExpressionNode createLesserBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharLtNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntLtNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatLtNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleLtNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringLtNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator <");
        }
    }

    private MinicExpressionNode createLesserOrEqualsBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharRelationalNodeFactory.MinicCharLtENodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntRelationalNodeFactory.MinicIntLtENodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatRelationalNodeFactory.MinicFloatLtENodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleRelationalNodeFactory.MinicDoubleLtENodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringRelationalNodeFactory.MinicStringLtENodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator <=");
        }
    }

    private MinicExpressionNode createOrBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharLogicalNodeFactory.MinicCharOrNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntLogicalNodeFactory.MinicIntOrNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatLogicalNodeFactory.MinicFloatOrNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleLogicalNodeFactory.MinicDoubleOrNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringLogicalNodeFactory.MinicStringOrNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator ||");
        }
    }

    private MinicExpressionNode createAndBinary(MinicExpressionNode left, MinicExpressionNode right, MinicBaseType type) {
        switch (type) {
            case CHAR:
                return MinicCharLogicalNodeFactory.MinicCharAndNodeGen.create((MinicCharNode) left, (MinicCharNode) right);
            case INT:
                return MinicIntLogicalNodeFactory.MinicIntAndNodeGen.create((MinicIntNode) left, (MinicIntNode) right);
            case FLOAT:
                return MinicFloatLogicalNodeFactory.MinicFloatAndNodeGen.create((MinicFloatNode) left, (MinicFloatNode) right);
            case DOUBLE:
                return MinicDoubleLogicalNodeFactory.MinicDoubleAndNodeGen.create((MinicDoubleNode) left, (MinicDoubleNode) right);
            case STRING:
                return MinicStringLogicalNodeFactory.MinicStringAndNodeGen.create((MinicStringNode) left, (MinicStringNode) right);
            default:
                throw new AssertionError(type + " not supported for operator ||");
        }
    }

    private MinicBaseType findType(MinicExpressionNode value) {

        if (value instanceof MinicCharNode) {
            return MinicBaseType.CHAR;
        } else if (value instanceof MinicIntNode) {
            return MinicBaseType.INT;
        } else if (value instanceof MinicFloatNode) {
            return MinicBaseType.FLOAT;
        } else if (value instanceof MinicDoubleNode) {
            return MinicBaseType.DOUBLE;
        } else if (value instanceof MinicStringNode) {
            return MinicBaseType.STRING;
        } else if (value instanceof MinicReadArrayNode.MinicEntireArrayReadNode) {
            return MinicBaseType.ARRAY;
        }

        throw new AssertionError("Type exception " + value.getClass() + " unknown");
    }

    private MinicBaseType implicitCast(MinicExpressionNode left, MinicExpressionNode right) {

        if (isNumber(left) && isNumber(right)) {
            // descending cast from char -> int -> float -> double
            if (left instanceof MinicDoubleNode || right instanceof MinicDoubleNode) {
                return MinicBaseType.DOUBLE;
            } else if (left instanceof MinicFloatNode || right instanceof MinicFloatNode) {
                return MinicBaseType.FLOAT;
            } else if (left instanceof MinicIntNode || right instanceof MinicIntNode) {
                return MinicBaseType.INT;
            } else if (left instanceof MinicCharNode || right instanceof MinicCharNode) {
                return MinicBaseType.CHAR;
            }
        }

        if (left instanceof MinicStringNode && right instanceof MinicStringNode) {
            // string is only compatible with string
            return MinicBaseType.STRING;
        }

        throw new AssertionError("Type exception " + left.getClass() + "incompatible with " + right.getClass());
    }

    private boolean isNumber(MinicExpressionNode node) {
        return (node instanceof MinicCharNode || node instanceof MinicIntNode || node instanceof MinicFloatNode || node instanceof MinicDoubleNode);
    }

    public MinicExpressionNode cast(MinicExpressionNode node, MinicBaseType type) {
        switch (type) {
            case CHAR:
                if (node instanceof MinicCharNode) {
                    return node;
                } else if (node instanceof MinicIntNode) {
                    return MinicToCharNodeFactory.MinicIntToCharNodeGen.create((MinicIntNode) node);
                } else if (node instanceof MinicFloatNode) {
                    return MinicToCharNodeFactory.MinicFloatToCharNodeGen.create((MinicFloatNode) node);
                } else if (node instanceof MinicDoubleNode) {
                    return MinicToCharNodeFactory.MinicDoubleToCharNodeGen.create((MinicDoubleNode) node);
                }
            case INT:
                if (node instanceof MinicCharNode) {
                    return MinicToIntNodeFactory.MinicCharToIntNodeGen.create((MinicCharNode) node);
                } else if (node instanceof MinicIntNode) {
                    return node;
                } else if (node instanceof MinicFloatNode) {
                    return MinicToIntNodeFactory.MinicFloatToIntNodeGen.create((MinicFloatNode) node);
                } else if (node instanceof MinicDoubleNode) {
                    return MinicToIntNodeFactory.MinicDoubleToIntNodeGen.create((MinicDoubleNode) node);
                }
                break;
            case FLOAT:
                if (node instanceof MinicCharNode) {
                    return MinicToFloatNodeFactory.MinicCharToFloatNodeGen.create((MinicCharNode) node);
                } else if (node instanceof MinicIntNode) {
                    return MinicToFloatNodeFactory.MinicIntToFloatNodeGen.create((MinicIntNode) node);
                } else if (node instanceof MinicFloatNode) {
                    return node;
                } else if (node instanceof MinicDoubleNode) {
                    return MinicToFloatNodeFactory.MinicDoubleToFloatNodeGen.create((MinicDoubleNode) node);
                }
                break;
            case DOUBLE:
                if (node instanceof MinicCharNode) {
                    return MinicToDoubleNodeFactory.MinicCharToDoubleNodeGen.create((MinicCharNode) node);
                } else if (node instanceof MinicIntNode) {
                    return MinicToDoubleNodeFactory.MinicIntToDoubleNodeGen.create((MinicIntNode) node);
                } else if (node instanceof MinicFloatNode) {
                    return MinicToDoubleNodeFactory.MinicFloatToDoubleNodeGen.create((MinicFloatNode) node);
                } else if (node instanceof MinicDoubleNode) {
                    return node;
                }
                break;
            case STRING:
                if (node instanceof MinicStringNode) {
                    return node;
                }
                if (node instanceof MinicReadArrayNodeFactory.MinicEntireArrayReadNodeGen) {
                    return MinicToStringNodeFactory.MinicCharArrayToStringNodeGen.create(node);
                }
        }

        throw new AssertionError("Type " + node.getClass() + " cannot be cast to " + type);

    }


    public MinicExpressionNode createIntLiteral(Token t) {
        return new MinicSimpleLiteralNode.MinicIntLiteralNode(Integer.parseInt(t.val));
    }

    public MinicExpressionNode createFloatLiteral(Token t) {
        try {
            // try float
            return new MinicSimpleLiteralNode.MinicFloatLiteralNode(Float.valueOf(t.val));
        } catch (NumberFormatException ex) {
            // too big for float, try double
            return new MinicSimpleLiteralNode.MinicDoubleLiteralNode(Double.valueOf(t.val));
        }
    }

    public MinicExpressionNode createCharLiteral(Token t) {
        /* Remove the trailing and ending " */
        String literal = t.val;
        assert literal.length() >= 2 && literal.startsWith("'") && literal.endsWith("'");
        literal = literal.substring(1, literal.length() - 1);
        return new MinicSimpleLiteralNode.MinicCharLiteralNode(literal.charAt(0));
    }

    public MinicExpressionNode createStringLiteral(Token t) {
        /* Remove the trailing and ending " */
        String literal = t.val;
        assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
        literal = literal.substring(1, literal.length() - 1);
        return new MinicSimpleLiteralNode.MinicStringLiteralNode(literal);
    }


}
