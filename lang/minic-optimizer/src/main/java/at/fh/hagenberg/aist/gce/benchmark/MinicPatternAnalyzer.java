/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.benchmark;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.minic.language.MinicContext;
import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNode;
import at.fh.hagenberg.aist.gce.minic.nodes.builtin.ReadNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToFloatNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToStringNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicIfNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.complex.MinicStringArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicDoubleArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.floating.MinicFloatArithmeticNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicCharArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.optimization.TruffleEvaluatorImpl;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationSolution;
import at.fh.hagenberg.aist.gce.optimization.cachet.AccuracyCachetEvaluator;
import at.fh.hagenberg.aist.gce.optimization.executor.InternalExecutor;
import at.fh.hagenberg.aist.gce.optimization.executor.MessageExecutor;
import at.fh.hagenberg.aist.gce.optimization.language.util.EngineConfig;
import at.fh.hagenberg.aist.gce.optimization.operators.ConfigurableMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.TruffleFaultFixingTreeMutator;
import at.fh.hagenberg.aist.gce.optimization.operators.selection.DepthWidthRestrictedTruffleTreeSelector;
import at.fh.hagenberg.aist.gce.optimization.test.TruffleOptimizationTestResult;
import at.fh.hagenberg.aist.gce.optimization.util.ExtendedNodeUtil;
import at.fh.hagenberg.aist.gce.optimization.util.MinicLanguageLearner;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.initializer.TruffleEntryPointStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicAntiFunctionLiteralStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicCantInvokeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicFunctionLiteralStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.MinicInvokeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.values.IntDefault;
import at.fh.hagenberg.aist.gce.pattern.*;
import at.fh.hagenberg.aist.gce.pattern.constraint.SolutionConstraint;
import at.fh.hagenberg.aist.gce.pattern.constraint.TestResultConstraint;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpace;
import at.fh.hagenberg.aist.gce.pattern.selection.TrufflePatternSearchSpaceDefinition;
import at.fh.hagenberg.aist.gce.science.statistics.data.template.PatternConfidencePrinter;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleProblemGeneRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleTestCaseRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.ProblemGene;
import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;
import at.fh.hagenberg.machinelearning.core.fitness.CachetEvaluator;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.neo4j.driver.Values;
import org.springframework.expression.spel.ast.IntLiteral;
import science.aist.neo4j.transaction.TransactionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Class that exists to analyze patterns found by fault detection or other discovery classes
 *
 * @author Oliver Krauss on 21.07.2020
 */
public class MinicPatternAnalyzer {

    /**
     * Algorithm to compare the different trees
     */
    static TrufflePatternDetector detector = new TrufflePatternDetector();

    static TransactionManager manager;

    static PatternConfidencePrinter printer = new PatternConfidencePrinter();

    /**
     * folder location where the output will be pushed to
     */
    private static final String LOCATION = EngineConfig.ROOT_LOCATION + "/Amaru.LOGS/FAULT/";

    private static final String PROBLEM_FIND = "match (n)<-[:TREE]-()<-[:RWGENE]-()-[:SOLVES]->()-[:RWGENE]->(problem) where id(n) = $ID return problem";

    private static Map<String, ProblemHelperClass> loadedProblems = new HashMap<>();

    private static TruffleProblemGeneRepository truffleOptimizationProblemRepository;
    private static TruffleTestCaseRepository truffleTestCaseRepository;
    private static TruffleMasterStrategy masterStrategy;
    private static TruffleEntryPointStrategy entryPointStrategy;

    private static final int targetCount = 100;

    public enum EvalType {
        POSITIVE,
        NEGATIVE,
        PREVENT,
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting: " + new Date().toString());

        // learn language as some strategies need the additional info
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        MinicLanguageLearner learner = new MinicLanguageLearner(tli);
        learner.setSaveToDB(false);
        learner.setFast(true);
        learner.learn();

        manager = ApplicationContextProvider.getCtx().getBean("transactionManager", TransactionManager.class);

        // Identify the differences in the algorithms with their distinct types
        detector.setHierarchyFloor(0);
        detector.setHierarchyCeil(Integer.MAX_VALUE);
        detector.setMaxPatternSize(100);
        detector.setEmbedded(false);
        detector.setGrouping(SignificanceType.MAX);

        // the count how many ASTs will be checked -> if we don't have that many ASTs for testing we reduce to the maximum available
        // the exception to search (TODO #63 generalize this to injecting the search space definitions instead of hardcoding)
        BitwisePatternMeta meta = new BitwisePatternMeta(tli, true);
        BitwisePatternMeta metaContained = new BitwisePatternMeta(tli, false);

        // get the problem loader and executor
        truffleOptimizationProblemRepository = ((TruffleProblemGeneRepository) ApplicationContextProvider.getCtx().getBean("truffleOptimizationProblemRepository"));
        truffleTestCaseRepository = ((TruffleTestCaseRepository) ApplicationContextProvider.getCtx().getBean("truffleTestCaseRepository"));

        // BLOCK -> FAILED TO SERIALIZE MESSAGE (0) ------------------------------
        String exception = "FAILED TO SERIALIZE MESSAGE";
        // evaluate the positive space
        NodeWrapper allocate = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        allocate.addChild(litNodeForbiddenVal, "", 0);
        // TODO #63 this should in the future be modified to allow before / after nodes or restrict them
        litNodeForbiddenVal.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        // NOTE: THE 0 value is done via the strategies as we can't force inject values via the patterns yet. We just definitely want an intlit

        evaluatePattern(exception, Collections.singletonList(allocate), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, AllocateArrayNode.class);

        // create fix patterns
        // Pattern A) int[int-read]
        NodeWrapper allocArray = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper intRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        allocArray.addChild(intRead, "", 0);
        intRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        // Pattern B) int[int-read][int-read]
        NodeWrapper allocArray2 = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper intRead2A = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        NodeWrapper intRead2B = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        allocArray2.addChild(intRead2A, "", 0);
        allocArray2.addChild(intRead2B, "", 1);
        intRead2A.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        // Pattern C) int[(int-read - int-read) + int-lit)]
        NodeWrapper allocArray3 = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper intAdd = new NodeWrapper(MinicIntArithmeticNode.MinicIntAddNode.class.getName());
        intAdd.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        allocArray3.addChild(intAdd, "", 0);
        NodeWrapper intSub = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName());
        intSub.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        intSub.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "right", 0);
        intAdd.addChild(intSub, "left", 0);
        intAdd.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);

        ArrayList<NodeWrapper> patterns = new ArrayList<>();
        patterns.add(allocArray);
        patterns.add(allocArray2);
        patterns.add(allocArray3);

        // evaluate the negative space
        evaluatePattern(exception, patterns, new ArrayList<>(), meta, exception, EvalType.NEGATIVE, AllocateArrayNode.class);


        // evaluate prevention of bug in future runs
        evaluatePattern(exception, patterns, new ArrayList<>(), meta, exception, EvalType.PREVENT, AllocateArrayNode.class);
        // END BLOCK  -> FAILED TO SERIALIZE MESSAGE (0)  --------------------------------

        // BLOCK -> IllegalStateException (1) ------------------------------
        exception = "java.lang.IllegalStateException";
        // evaluate the positive space
        NodeWrapper pattern = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        NodeWrapper writeVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        pattern.addChild(writeVarNode, "", 0);
        NodeWrapper readVarNode = new NodeWrapper(MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        pattern.addChild(readVarNode, "", 1);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.NEGATIVE, MinicReadNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicReadNode.class);
        // END BLOCK -> IllegalStateException (1) ------------------------------

        // BLOCK -> An Illegal Reflective Access Operation has occured (2) ALSO THE WORKER CRASHED(3) ------------------------------
        exception = "An illegal reflective access operation has occurred";
        // evaluate the positive space -> Di it with ReadNode and ReadArrayNode as those are the most common
        pattern = new NodeWrapper(MinicReadNode.class.getName());
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.NEGATIVE, MinicReadArrayNode.class);

       // evaluate prevention of bug in future runs
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicReadArrayNode.class);
        // END BLOCK -> An Illegal Reflective Access Operation has occured (2) ALSO THE WORKER CRASHED(3) ------------------------------

        // BLOCK -> UnsupportedSpecializationException (4) ------------------------------
        exception = "com.oracle.truffle.api.dsl.UnsupportedSpecializationException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicInvokeNode.class.getName());
        NodeWrapper firstArg = new NodeWrapper(MinicSimpleLiteralNode.MinicCharLiteralNode.class.getName());
        pattern.addChild(firstArg, "functionNode", 0);
        firstArg.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicInvokeNode.class);

        // evaluate the negative space
        NodeWrapper antipattern = new NodeWrapper(MinicInvokeNode.class.getName());
        NodeWrapper functionNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicFunctionLiteralNode.class.getName());
        antipattern.addChild(functionNode, "functionNode", 0);
        evaluatePattern(exception, new ArrayList<>(), Collections.singletonList(antipattern), meta, exception, EvalType.NEGATIVE, MinicInvokeNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, new ArrayList<>(), Collections.singletonList(antipattern), meta, exception, EvalType.PREVENT, MinicInvokeNode.class);
        // END BLOCK -> UnsupportedSpecializationException (4)  ------------------------------

        // BLOCK AND END BLOCK -> TimeoutException(5) has no proveable patterns

        // BLOCK -> java.lang.ArithmeticException (6) DIV VERSION ------------------------------
        exception = "java.lang.ArithmeticException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicIntArithmeticNode.MinicIntDivNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "leftNode", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "rightNode", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicIntArithmeticNode.MinicIntDivNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        NodeWrapper antiPattern = new NodeWrapper(MinicIntArithmeticNode.MinicIntDivNode.class.getName() + "|" +
                MinicCharArithmeticNode.MinicCharDivNode.class.getName() + "|" +
                MinicFloatArithmeticNodeFactory.MinicFloatDivNodeGen.class.getName() + "|" +
                MinicDoubleArithmeticNode.MinicDoubleDivNode.class.getName());
        litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:int", 0);
        antiPattern.addChild(litNodeForbiddenVal, "rightNode", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), Collections.singletonList(antiPattern), meta, exception, EvalType.NEGATIVE, MinicIntArithmeticNode.MinicIntDivNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, Collections.singletonList(pattern), Collections.singletonList(antiPattern), meta, exception, EvalType.PREVENT, MinicIntArithmeticNode.MinicIntDivNode.class);
        // BLOCK -> java.lang.ArithmeticException (6) MOD VERSION ------------------------------
        exception = "java.lang.ArithmeticException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicIntArithmeticNode.MinicIntModNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "leftNode", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "rightNode", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicIntArithmeticNode.MinicIntModNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        antiPattern = new NodeWrapper(MinicIntArithmeticNode.MinicIntModNode.class.getName() + "|" +
                MinicCharArithmeticNode.MinicCharModNode.class.getName() + "|" +
                MinicFloatArithmeticNodeFactory.MinicFloatModNodeGen.class.getName() + "|" +
                MinicDoubleArithmeticNode.MinicDoubleModNode.class.getName());
        litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:int", 0);
        antiPattern.addChild(litNodeForbiddenVal, "rightNode", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), Collections.singletonList(antiPattern), meta, exception, EvalType.NEGATIVE, MinicIntArithmeticNode.MinicIntModNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, Collections.singletonList(pattern), Collections.singletonList(antiPattern), meta, exception, EvalType.PREVENT, MinicIntArithmeticNode.MinicIntModNode.class);
        // END BLOCK -> java.lang.ArithmeticException (6) ------------------------------

        // BLOCK -> RuntimeException (7) -----------------------------
        exception = "java.lang.RuntimeException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicInvokeNode.class.getName());
        NodeWrapper fnName = new NodeWrapper(MinicFunctionLiteralNode.class.getName());
        pattern.addChild(fnName, "functionNode", 0);
        firstArg = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        pattern.addChild(firstArg, "argumentNodes", 0);
        firstArg.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicInvokeNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), metaContained, exception, EvalType.NEGATIVE, MinicInvokeNode.class);
//
//        // evaluate prevention of bug in future runs
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), metaContained, exception, EvalType.PREVENT, MinicInvokeNode.class);
        // END BLOCK -> RuntimeException (7) ------------------------------

        // BLOCK -> ArrayIndexException (8) READ ------------------------------
        exception = "java.lang.ArrayIndexOutOfBoundsException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper position = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        pattern.addChild(position, "", 0);
        position.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);

        // Create FIX Patterns:
        //  Pattern A) read[int-read]
        NodeWrapper readArrayNode = new NodeWrapper(MinicReadArrayNode.class.getName());
        intRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode.addChild(intRead, "", 0);
        intRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        //  Pattern B) read[int-read, int-read]
        NodeWrapper readArrayNode2 = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper intRead1 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode2.addChild(intRead1, "", 0);
        intRead1.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        NodeWrapper intRead2 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode2.addChild(intRead2, "", 1);
        intRead2.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
//         Pattern C) read[(-|+)(int-read,int-read|int-lit)]
        NodeWrapper readArrayNode3 = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper arithNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName() + "|" + MinicIntArithmeticNode.MinicIntAddNode.class.getName());
        readArrayNode3.addChild(arithNode, "", 0);
        arithNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        arithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        arithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName() + "|" + MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        // Pattern D) read[/(-(read,lit),lit)]
        NodeWrapper readArrayNode4 = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper readDivNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntDivNode.class.getName());
        readArrayNode4.addChild(readDivNode, "", 0);
        readDivNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        NodeWrapper divLeftNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName());
        readDivNode.addChild(divLeftNode, "left", 0);
        readDivNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        divLeftNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        divLeftNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);


        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadArrayNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, Arrays.asList(readArrayNode, readArrayNode2, readArrayNode3, readArrayNode4), new ArrayList<>(), metaContained, exception, EvalType.NEGATIVE, MinicReadArrayNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, Arrays.asList(readArrayNode, readArrayNode2, readArrayNode3, readArrayNode4), new ArrayList<>(), metaContained, exception, EvalType.PREVENT, MinicReadArrayNode.class);

        // BLOCK -> ArrayIndexException (8) WRITE ------------------------------
        exception = "java.lang.ArrayIndexOutOfBoundsException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicWriteArrayNode.class.getName());
        position = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        pattern.addChild(position, "arrayPosition", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.class.getName()), "value", 0);
        position.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);

        // Create FIX Patterns:
        //  Pattern A) write[int-read]
        NodeWrapper writeArrayNode = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper wintRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNode.addChild(wintRead, "", 0);
        wintRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        //  Pattern B) write[int-read, int-read]
        NodeWrapper writeArrayNod2 = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper wintRead1 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNod2.addChild(wintRead1, "", 0);
        wintRead1.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        NodeWrapper wintRead2 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNod2.addChild(wintRead2, "", 1);
        wintRead2.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
//         Pattern C) write[(-|+)(int-read,int-read|int-lit)]
        NodeWrapper writeArrayNod3 = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper warithNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName() + "|" + MinicIntArithmeticNode.MinicIntAddNode.class.getName());
        writeArrayNod3.addChild(warithNode, "", 0);
        warithNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        warithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        warithNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        // Pattern D) write[/(-(read,lit),lit)]
        NodeWrapper writeArrayNod4 = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper writeDivNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntDivNode.class.getName());
        writeArrayNod4.addChild(writeDivNode, "", 0);
        writeDivNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        NodeWrapper wdivLeftNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName());
        writeDivNode.addChild(wdivLeftNode, "left", 0);
        writeDivNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        wdivLeftNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        wdivLeftNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);


        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicWriteArrayNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, Arrays.asList(writeArrayNode, writeArrayNod2, writeArrayNod3, writeArrayNod4), new ArrayList<>(), metaContained, exception, EvalType.NEGATIVE, MinicWriteArrayNode.class);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, Arrays.asList(writeArrayNode, writeArrayNod2, writeArrayNod3, writeArrayNod4), new ArrayList<>(), metaContained, exception, EvalType.PREVENT, MinicWriteArrayNode.class);
        // END BLOCK -> ArrayIndexException (8)  -----------------------------

        // BLOCK -> Illegal Argument Exception (9) ------------------------------
        exception = "java.lang.IllegalArgumentException";
        // evaluate the positive space
        // WE FORCE INJECT A READ GLOBAL HERE
        pattern = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        pattern.addChild(new NodeWrapper(MinicReadGlobalNode.class.getName()), "", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadGlobalNode.class);

        // evaluate the negative space
        // WE DON'T FORCE INJECT
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.NEGATIVE, null);

        // evaluate prevention of bug in future runs
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, null);
        // END BLOCK -> Illegal Argument Exception (9) ------------------------------


        // BLOCK -> Class Cast Exception - Array but wrong type (10) ------------------------------
        exception = "java.lang.ClassCastException";
        // evaluate the positive space
        pattern = new NodeWrapper(MinicReadArrayNode.class.getName());
        position = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        pattern.addChild(position, "", 0);
        position.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadArrayNode.class);
        // FIX for read array
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicReadArrayNode.class);

        // BLOCK -> Class Cast Exception -String / Char read
        pattern = new NodeWrapper(MinicReadNode.MinicStringReadNode.class.getName());
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadNode.MinicStringReadNode.class);
        // FIX for string read
        // WARNING: DO NOT COMMENT THIS IN! THE PATTERN IS UNPROVABLE BECAUSE NO TEST HAS A CHAR
        // evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicReadNode.MinicStringReadNode.class);

        pattern = new NodeWrapper(MinicReadNode.MinicCharReadNode.class.getName());
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicReadNode.MinicCharReadNode.class);
        // FIX for string read
        // WARNING: DO NOT COMMENT THIS IN! THE PATTERN IS UNPROVABLE BECAUSE NO TEST HAS A CHAR
        // evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicReadNode.MinicCharReadNode.class);

        pattern = new NodeWrapper(MinicToFloatNode.MinicGenericToFloatNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicToFloatNode.MinicGenericToFloatNode.class);
        // FIX for Generic To float
        antipattern = new NodeWrapper(MinicToFloatNode.MinicGenericToFloatNode.class.getName());
        antipattern.addChild(new NodeWrapper(Wildcard.WILDCARD_NOT + MinicFloatNode.class.getName()), "", 0);
        evaluatePattern(exception, new ArrayList<>(), Collections.singletonList(antipattern), meta, exception, EvalType.PREVENT, MinicToFloatNode.MinicGenericToFloatNode.class);


        pattern = new NodeWrapper(MinicToStringNode.MinicCharArrayToStringNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "", 0);
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicToStringNode.MinicCharArrayToStringNode.class);
        antipattern = new NodeWrapper(MinicToStringNode.MinicCharArrayToStringNode.class.getName());
        NodeWrapper entireArrayNode = new NodeWrapper(Wildcard.WILDCARD_NOT + "(" + MinicReadArrayNode.MinicEntireArrayReadNode.class.getName() + "|" + MinicReadGlobalArrayNode.MinicEntireArrayReadGlobalNode.class.getName() + ")");
        antipattern.addChild(entireArrayNode, "", 0);
        // NOTE: THIS PATTERN IS NOT PROVABLE. WE HAVE NO CHAR ARRAY
        evaluatePattern(exception, new ArrayList<>(), Collections.singletonList(antipattern), meta, exception, EvalType.PREVENT, MinicToStringNode.MinicCharArrayToStringNode.class);

        // END BLOCK -> Class Cast Exception - Array but wrong type (10)  ------------------------------

        // BLOCK -> NullPointerException (11) -----------------------------
        exception = "java.lang.NullPointerException";

        // BLOCK -> String Read
        pattern = new NodeWrapper(MinicIfNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicReadNode.MinicStringReadNode.class.getName()), "condition", 0);

        // NOTE: Neither String nor Char can be proven since no example uses those two datatypes
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicIfNode.class);

        // BLOCK -> Char Read
        pattern = new NodeWrapper(MinicIfNode.class.getName());
        pattern.addChild(new NodeWrapper(MinicReadNode.MinicCharReadNode.class.getName()), "condition", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "thenPath", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "elsePath", 0);

        // NOTE: Neither String nor Char can be proven since no example uses those two datatypes
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicIfNode.class);

        // BLOCK -> ArrayRead
        pattern = new NodeWrapper(MinicIfNode.class.getName());
        NodeWrapper readArray = new NodeWrapper(MinicReadArrayNode.class.getName());
        pattern.addChild(readArray, "condition", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "thenPath", 0);
        pattern.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "elsePath", 0);
        position = new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName());
        readArray.addChild(position, "", 0);
        position.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);

        // NOTE: Neither String nor Char can be proven since no example uses those two datatypes
        evaluatePattern(exception, Collections.singletonList(pattern), new ArrayList<>(), metaContained, exception, EvalType.POSITIVE, MinicIfNode.class);
        evaluatePattern(exception, new ArrayList<>(), new ArrayList<>(), meta, exception, EvalType.PREVENT, MinicIfNode.class);
        // END BLOCK -> NullPointerException (11) ------------------------------

        System.out.println("Ending: " + new Date().toString());
        ApplicationContextProvider.getCtx().close();
    }

    private static void evaluatePattern(String name, List<NodeWrapper> patterns, List<NodeWrapper> antiPatterns, BitwisePatternMeta meta, String exception, EvalType evalType, Class checkClass) {
        TrufflePatternSearchSpaceDefinition definition = new TrufflePatternSearchSpaceDefinition();
        Random random = new Random();
        if (evalType == EvalType.POSITIVE) {
            System.out.println("Testing if this pattern is responsible for the given bug");
            // find the positive space
            definition = new TrufflePatternSearchSpaceDefinition();
            definition.setSolutionSpace(false);
            // we want all solutions that do not throw exceptions in any test, and also do not return the wrong datatype
//            definition.setSolution(new SolutionConstraint(0.0, -1.0));
        } else if (evalType == EvalType.NEGATIVE) {
            // find the negative/exception space
            System.out.println("Testing if using this pattern repairs the given bug");
            definition.setSolutionSpace(true);
            List<TestResultConstraint> testResults = new LinkedList<>();
            testResults.add(new TestResultConstraint(exception));
            definition.setTestResult(testResults);
        } else {
            // find the repair space
            System.out.println("Testing if using this pattern prevents the given bug");
            // find the positive space
            definition = new TrufflePatternSearchSpaceDefinition();
            definition.setSolutionSpace(true);
            // we want all solutions that do not throw exceptions in any test, and also do not return the wrong datatype
            definition.setSolution(new SolutionConstraint(0.0, -1.0));
        }

        TrufflePatternSearchSpace solutions = detector.findSearchSpace(definition);
        TruffleEvaluatorImpl eval = createTruffleEvaluator();
        System.out.println("Examples space is " + solutions.getSearchSpace().size() + " ASTs");

        double confidence = 0;
        double softConfidence = 0;

        int count = targetCount;
//        if (solutions.getSearchSpace().
//                size() < targetCount) {
//            count = solutions.getSearchSpace().size();
//        }

        HashMap<String, Integer> exceptions = new HashMap<>();

        // load problem and execute it again
        ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator;
        MessageExecutor executor;
        for (int i = 0; i < count; i++) {
            int pos = random.nextInt(solutions.getSearchSpace().size());
            NodeWrapper rootWrapper = solutions.getSearchSpace().get(pos).getKey()[0];
            TruffleOptimizationProblem problem = truffleOptimizationProblemRepository.queryTyped(PROBLEM_FIND, Values.parameters("ID", rootWrapper.getId()));
            // force load the tests
            String e = evalType.equals(EvalType.POSITIVE) ? "p" : "n";
            String key = e + problem.getId();
            if (loadedProblems.containsKey(key)) {
                ProblemHelperClass phc = loadedProblems.get(key);
                problem = phc.problem;
                mutator = phc.mutator;
            } else {
                eval.verifyExecutor(problem, false);
                executor = (MessageExecutor) eval.getExecutor();
                problem = repair(problem, executor.getOrigin());
                if (evalType.equals(EvalType.POSITIVE)) {
                    CreationConfiguration config = problem.getConfiguration();
                    masterStrategy = TruffleMasterStrategy.createFromTLI(config, getTruffleLanguageSearchSpace(), getPostitiveStrategies(exception), getPositiveTerminalStrategies(problem, exception));
                    entryPointStrategy = new TruffleEntryPointStrategy(getTruffleLanguageSearchSpace(), problem.getNode(), problem.getNode(), masterStrategy, config);
                    patterns.forEach(pattern -> {
                        masterStrategy.injectPattern(pattern, meta, 1.0);
                    });
                    antiPatterns.forEach(antipattern -> {
                        masterStrategy.injectAntiPattern(antipattern, meta);
                    });
                    mutator = createFaultFixingMutator();
                } else {
                    masterStrategy = TruffleMasterStrategy.createFromTLI(problem.getConfiguration(), getTruffleLanguageSearchSpace(), getStrategies(), getTerminalStrategies(problem));
                    entryPointStrategy = new TruffleEntryPointStrategy(getTruffleLanguageSearchSpace(), problem.getNode(), problem.getNode(), masterStrategy, problem.getConfiguration());
                    patterns.forEach(pattern -> {
                        masterStrategy.injectPattern(pattern, meta, 1.0);
                    });
                    antiPatterns.forEach(antipattern -> {
                        masterStrategy.injectAntiPattern(antipattern, meta);
                    });
                    mutator = createFaultFixingMutator();
                }
                ((TruffleFaultFixingTreeMutator) mutator).setCheck(checkClass);
                loadedProblems.put(key, new ProblemHelperClass(problem, mutator));
            }
            eval.verifyExecutor(problem, false);

            // mutate before evaluation and then execute
            executor = (MessageExecutor) eval.getExecutor();
            Node node = NodeWrapper.unwrap(solutions.toAst(solutions.getSearchSpace().get(pos)), executor.getRoot().getFrameDescriptor(), executor.getGlobalScope(), MinicLanguage.ID);
            boolean prebug = false;
            if (prebug) {
                new InternalExecutor(problem.getLanguage(), problem.getCode(), problem.getEntryPoint(), problem.getFunction()).conductTest(node, problem.getTests().iterator().next().getTest().getInputArguments());
            }
            Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = createSolution(node, problem);
            try {
                solution = mutator.mutate(solution);
                eval.evaluateQuality(solution);

                // evaluate the confidence in the pattern
                Set<TruffleOptimizationTestResult> results = solution.getSolutionGenes().get(0).getGene().getTestResults();
                if (results != null) {
                    TruffleOptimizationProblem finalProblem = problem;
                    Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> finalSolution = solution;
                    results.forEach(x -> {
                        addException(x.getException(), exceptions);
                        boolean debug = false;
                        if (debug) {
                            new InternalExecutor(finalProblem.getLanguage(), finalProblem.getCode(), finalProblem.getEntryPoint(), finalProblem.getFunction()).conductTest(finalSolution.getSolutionGenes().get(0).getGene().getNode(), finalProblem.getTests().iterator().next().getTest().getInputArguments());
                        }
                    });
                }
            } catch (Exception ex) {
                System.out.println("RETRYING BECAUSE OF FAIL");
                i--;
            }
        }

        int sum = exceptions.values().stream().mapToInt(x -> x).sum();
        if (evalType.equals(EvalType.POSITIVE)) {
            confidence = exceptions.getOrDefault(exception, 0) / (double) sum;
            softConfidence = (sum - exceptions.getOrDefault("Successful", 0)) / (double) sum;
        } else {
            confidence = (exceptions.getOrDefault("Successful", 0)) / (double) sum;
            softConfidence = (sum - (exceptions.getOrDefault(exception, 0))) / (double) sum;
        }

        if (evalType.equals(EvalType.POSITIVE)) {
            System.out.println("Evaluated confidence pattern being responsible for bug on " + count + " trees");
            System.out.println("Confidence: " + confidence);
            System.out.println("Soft Confidence: " + softConfidence);
            System.out.println("Soft Confidence: " + softConfidence);
            print(name + "_INJECTED_BUG", patterns, antiPatterns, exceptions, confidence, softConfidence);
        } else if (evalType.equals(EvalType.NEGATIVE)) {
            System.out.println("Evaluated confidence of bugfix pattern on " + count + " trees");
            System.out.println("Confidence: " + confidence);
            System.out.println("Soft Confidence: " + softConfidence);
            print(name + "_FIXED_BUG", patterns, antiPatterns, exceptions, confidence, softConfidence);
        } else {
            System.out.println("Evaluated confidence of pattern preventing bug on " + count + " trees");
            System.out.println("Confidence: " + confidence);
            System.out.println("Soft Confidence: " + softConfidence);
            print(name + "_PREVENTED_BUG", patterns, antiPatterns, exceptions, confidence, softConfidence);
        }
    }

    private static void addException(String exception, HashMap<String, Integer> exceptions) {
        String shortened = "Successful";
        if (exception != null) {
            // trim to first line
            String ex = exception.replace("'", "\\'");
            if (ex.contains("\n")) {
                ex = ex.substring(0, ex.indexOf("\n")).trim();
            }

            // special handling for the weirder messages
            if (ex.contains("An illegal reflective access operation has occurred")) {
                shortened = "An illegal reflective access operation has occurred";
            } else if (ex.contains("The worker crashed")) {
                shortened = "The worker crashed";
            }
            // reduce to text before : and @
            else if (ex.contains(":")) {
                shortened = ex.substring(0, ex.indexOf(":"));
            } else if (ex.contains("@")) {
                shortened = ex.substring(0, ex.indexOf("@"));
            } else {
                shortened = ex;
            }
        }

        if (!exceptions.containsKey(shortened)) {
            exceptions.put(shortened, 0);
        }
        exceptions.put(shortened, exceptions.get(shortened) + 1);
    }

    private static void print(String name, List<NodeWrapper> patterns, List<NodeWrapper> antiPatterns, HashMap<String, Integer> exceptions, double confidence, double softConfidence) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(LOCATION + name + ".html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        printer.setWriter(writer);
        printer.printConfidence(name, patterns, antiPatterns, exceptions, confidence, softConfidence);
    }

    private static TruffleOptimizationProblem repair(TruffleOptimizationProblem problem, Node nodeToOptimize) {
        // force load the tests
        TruffleOptimizationProblem loadedProblem = truffleOptimizationProblemRepository.findById(problem.getId());
        // force load the test values
        loadedProblem.getTests().forEach(x -> {
            x.setTest(truffleTestCaseRepository.findById(x.getTest().getId()));
        });

        // Set node
        loadedProblem.setOption("node", new Descriptor(nodeToOptimize));

        // set config
        loadedProblem.setOption("configuration", new Descriptor(new CreationConfiguration(ExtendedNodeUtil.maxDepth(nodeToOptimize) + 1, ExtendedNodeUtil.maxWidth(nodeToOptimize) + 1, Double.MAX_VALUE)));

        // set search space
        loadedProblem.setOption("searchSpace", new Descriptor(getTruffleLanguageSearchSpace()));

        return loadedProblem;
    }

    protected static Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> createSolution(Node node, TruffleOptimizationProblem problem) {
        Solution<TruffleOptimizationSolution, TruffleOptimizationProblem> solution = new Solution<>();
        TruffleOptimizationSolution solutionGene = new TruffleOptimizationSolution(node, problem, null);
        SolutionGene<TruffleOptimizationSolution, TruffleOptimizationProblem> sg = new SolutionGene<>(solutionGene);
        sg.addProblemGene(new ProblemGene<>(problem));
        solution.addGene(sg);
        return solution;
    }

    public static TruffleEvaluatorImpl createTruffleEvaluator() {
        TruffleEvaluatorImpl evaluator = new TruffleEvaluatorImpl();
        evaluator.setTimeout(3000);
        // WE EXPLICITLY DO NOT WANT TO CONTAMINATE THE DB HERE -> evaluator.setAnalyticsService(getAnalytics());
        Map<CachetEvaluator<TruffleOptimizationSolution, TruffleOptimizationProblem>, Double> cachetMap = new HashMap<>();
        cachetMap.put(new AccuracyCachetEvaluator(), 1.0);
        evaluator.setCachetEvaluators(cachetMap);
        return evaluator;
    }

    public static ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> createFaultFixingMutator() {
        TruffleFaultFixingTreeMutator truffleTreeMutator = new TruffleFaultFixingTreeMutator();
        DepthWidthRestrictedTruffleTreeSelector selector = new DepthWidthRestrictedTruffleTreeSelector();
        selector.setMaxDepth(3);
        selector.setMaxWidth(3);
        truffleTreeMutator.setSelector(selector);
        truffleTreeMutator.setSubtreeStrategy(masterStrategy);
        truffleTreeMutator.setFullTreeStrategy(entryPointStrategy);
        return truffleTreeMutator;
    }


    // TODO # 63 all of the below is copy paste from the MINIC TESTFILE OPTIMIZER
    protected static TruffleLanguageSearchSpace getTruffleLanguageSearchSpace() {
        List<Class> excludes = new ArrayList<>();
        excludes.add(ReadNodeFactory.ReadNodeGen.class);
        excludes.add(ReadNode.class);
        TruffleLanguageInformation information = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        return new TruffleLanguageSearchSpace(information, excludes);
    }

    protected static List<TruffleHierarchicalStrategy> getStrategies() {
        MinicContext ctx = MinicLanguage.INSTANCE.getContextReference().get();
        List<TruffleHierarchicalStrategy> strategies = new ArrayList<>();
        strategies.add(new MinicFunctionLiteralStrategy(ctx));
        Set<Class> invokeClasses = new HashSet<>();
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeIntNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeFloatNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeVoidNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeArrayNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeCharNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeStringNodeGen.class);
        invokeClasses.add(MinicInvokeNodeFactory.MinicInvokeDoubleNodeGen.class);

        // create dedicated invoke strategies for each method
        ctx.getFunctionRegistry().getFunctions().stream().filter(x -> !x.getName().equals("main") && !x.getName().equals("read") && !x.getName().endsWith("_benchmark") && !x.getName().endsWith("_entry")).forEach(x -> {
            MinicInvokeStrategy invoke = new MinicInvokeStrategy(x);
            strategies.add(invoke);
            invokeClasses.remove(invoke.getInvokeClazz());
        });
        // prevent other invokes if they have no valid impl:
        invokeClasses.forEach(x -> {
            strategies.add(new MinicCantInvokeStrategy(ctx, x));
        });

        return strategies;
    }

    protected static List<TruffleHierarchicalStrategy> getPostitiveStrategies(String exception) {
        MinicContext ctx = MinicLanguage.INSTANCE.getContextReference().get();
        List<TruffleHierarchicalStrategy> strategies = new ArrayList<>();
        if (exception.equals("java.lang.RuntimeException")) {
            strategies.add(new MinicAntiFunctionLiteralStrategy(ctx));
        } else {
            strategies.add(new MinicFunctionLiteralStrategy(ctx));
        }
        Set<Class> invokeClasses = new HashSet<>();
        return strategies;
    }

    protected static Map<String, TruffleVerifyingStrategy> getPositiveTerminalStrategies(TruffleOptimizationProblem problem, String exception) {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        strategies.putAll(DefaultStrategyUtil.defaultStrategies());
        if (exception.equals("java.lang.IllegalArgumentException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new IllegalArgumentExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.IllegalStateException") || exception.equals("java.lang.NullPointerException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new IllegalStateExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("FAILED TO SERIALIZE MESSAGE")) {
            // Allowing ONLY 0, since the patterns don't have a "I require this literal" option yet
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(-1000))));
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.ArithmeticException")) {
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(0))));
        } else if (exception.equals("java.lang.ArrayIndexOutOfBoundsException")) {
            strategies.put("int", new KnownValueStrategy<>(new LinkedList<>(Arrays.asList(-1000))));
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else if (exception.equals("java.lang.ClassCastException")) {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new ClassCastExceptionFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        } else {
            strategies.put("com.oracle.truffle.api.frame.FrameSlot", new NonCheckingFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        }
        return strategies;
    }

    protected static Map<String, TruffleVerifyingStrategy> getTerminalStrategies(TruffleOptimizationProblem problem) {
        Map<String, TruffleVerifyingStrategy> strategies = new HashMap<>();
        if (MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage() != null) {
            strategies.put("com.oracle.truffle.api.frame.MaterializedFrame", new StaticObjectStrategy<MaterializedFrame>(MinicLanguage.INSTANCE.getContextReference().get().getGlobalStorage()));
        }
        strategies.put("at.fh.hagenberg.aist.gce.minic.language.MinicContext", new StaticObjectStrategy<MinicContext>(MinicLanguage.INSTANCE.getContextReference().get()));
        strategies.putAll(DefaultStrategyUtil.defaultStrategies());
        strategies.put("com.oracle.truffle.api.frame.FrameSlot", new DefaultFrameSlotStrategy(problem.getNode().getRootNode().getFrameDescriptor()));
        return strategies;
    }

    private static class ProblemHelperClass {
        public TruffleOptimizationProblem problem;
        public ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator;

        public ProblemHelperClass(TruffleOptimizationProblem problem, ConfigurableMutator<TruffleOptimizationSolution, TruffleOptimizationProblem> mutator) {
            this.problem = problem;
            this.mutator = mutator;
        }
    }
}
