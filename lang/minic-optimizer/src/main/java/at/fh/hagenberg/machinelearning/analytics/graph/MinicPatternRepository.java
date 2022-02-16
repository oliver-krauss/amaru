/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph;

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicCastNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToFloatNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.cast.MinicToStringNodeFactory;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.control.MinicWhileNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicFunctionLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.function.MinicInvokeNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.literals.MinicSimpleLiteralNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.op.arith.integer.signed.MinicIntArithmeticNode;
import at.fh.hagenberg.aist.gce.minic.nodes.impl.vars.*;
import at.fh.hagenberg.aist.gce.minic.types.complex.MinicStringNode;
import at.fh.hagenberg.aist.gce.minic.types.floating.MinicFloatNode;
import at.fh.hagenberg.aist.gce.minic.types.integer.signed.MinicCharNode;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.Requirement;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMetaLoader;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Repository for patterns and anti-patterns
 * <p>
 * TODO #258 move the given patterns into a DB
 *
 * @author Oliver Krauss on 3.6.2021
 */
public class MinicPatternRepository extends PatternRepository {

    HashMap<NodeWrapper, BitwisePatternMeta> antipatterns;

    HashMap<NodeWrapper, BitwisePatternMeta> patterns;

    public MinicPatternRepository() {
        // prepare antipatterns
        antipatterns = new HashMap<>();
        patterns = new HashMap<>();

        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(MinicLanguage.ID);
        BitwisePatternMeta superclassMeta = new BitwisePatternMeta(tli, true);
        BitwisePatternMeta enclosingClassMeta = new BitwisePatternMeta(tli, false);
        List<String> datatypes = new ArrayList<>();
        datatypes.add("Char");
        datatypes.add("Int");
        datatypes.add("Float");
        datatypes.add("Double");
        datatypes.add("String");
        BitwisePatternMeta datatypeIndependentMeta = BitwisePatternMetaLoader.loadDatatypeIndependent(MinicLanguage.ID, datatypes, false);

        antipatterns.put(charArrayToStringAntipattern(), superclassMeta);
        antipatterns.put(stringAndCharAntipattern(), superclassMeta);
        antipatterns.put(genericToFloatAntipattern(), superclassMeta);
        antipatterns.put(invokeFNLiteralAntipattern(), enclosingClassMeta);
        antipatterns.put(divByZeroDirect(), datatypeIndependentMeta);
        antipatterns.put(modByZeroDirect(), datatypeIndependentMeta);
        antipatterns.put(divByZeroWithCast(), datatypeIndependentMeta);
        antipatterns.put(modByZeroWithCast(), datatypeIndependentMeta);
        antipatterns.put(endlessWhileAntipattern(), enclosingClassMeta);

        patterns.put(allocPatternA(), enclosingClassMeta);
        patterns.put(allocPatternB(), enclosingClassMeta);
        patterns.put(allocPatternC(), enclosingClassMeta);
        patterns.put(readArrayPatternA(), enclosingClassMeta);
        patterns.put(readArrayPatternB(), enclosingClassMeta);
        patterns.put(readArrayPatternC(), enclosingClassMeta);
        patterns.put(readArrayPatternD(), enclosingClassMeta);
        patterns.put(writeArrayPatternA(), enclosingClassMeta);
        patterns.put(writeArrayPatternB(), enclosingClassMeta);
        patterns.put(writeArrayPatternC(), enclosingClassMeta);
        patterns.put(writeArrayPatternD(), enclosingClassMeta);
    }

    private NodeWrapper genericToFloatAntipattern() {
        NodeWrapper antipattern = new NodeWrapper(MinicToFloatNode.MinicGenericToFloatNode.class.getName());
        antipattern.addChild(new NodeWrapper(Wildcard.WILDCARD_NOT + MinicFloatNode.class.getName()), "", 0);
        return antipattern;
    }

    private NodeWrapper writeArrayPatternA() {
        //  Pattern A) write[int-read]
        NodeWrapper writeArrayNode = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper wintRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNode.addChild(wintRead, "", 0);
        wintRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        return writeArrayNode;
    }

    private NodeWrapper writeArrayPatternB() {
        //  Pattern B) write[int-read, int-read]
        NodeWrapper writeArrayNod2 = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper wintRead1 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNod2.addChild(wintRead1, "", 0);
        wintRead1.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        NodeWrapper wintRead2 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        writeArrayNod2.addChild(wintRead2, "", 1);
        wintRead2.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        return writeArrayNod2;
    }

    private NodeWrapper writeArrayPatternC() {
        // Pattern C) write[(-|+)(int-read,int-read|int-lit)]
        NodeWrapper writeArrayNod3 = new NodeWrapper(MinicWriteArrayNode.class.getName());
        NodeWrapper warithNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName() + "|" + MinicIntArithmeticNode.MinicIntAddNode.class.getName());
        writeArrayNod3.addChild(warithNode, "", 0);
        warithNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        warithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        warithNode.addChild(new NodeWrapper(MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        return writeArrayNod3;
    }

    private NodeWrapper writeArrayPatternD() {
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
        return writeArrayNod4;
    }

    private NodeWrapper readArrayPatternA() {
        //  Pattern A) read[int-read]
        NodeWrapper readArrayNode = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper intRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode.addChild(intRead, "", 0);
        intRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        return readArrayNode;
    }

    private NodeWrapper readArrayPatternB() {
        //  Pattern B) read[int-read, int-read]
        NodeWrapper readArrayNode2 = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper intRead1 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode2.addChild(intRead1, "", 0);
        intRead1.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        NodeWrapper intRead2 = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        readArrayNode2.addChild(intRead2, "", 1);
        intRead2.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        return readArrayNode2;
    }

    private NodeWrapper readArrayPatternC() {
        //         Pattern C) read[(-|+)(int-read,int-read|int-lit)]
        NodeWrapper readArrayNode3 = new NodeWrapper(MinicReadArrayNode.class.getName());
        NodeWrapper arithNode = new NodeWrapper(MinicIntArithmeticNode.MinicIntSubNode.class.getName() + "|" + MinicIntArithmeticNode.MinicIntAddNode.class.getName());
        readArrayNode3.addChild(arithNode, "", 0);
        arithNode.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        arithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName()), "left", 0);
        arithNode.addChild(new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName() + "|" + MinicSimpleLiteralNode.MinicIntLiteralNode.class.getName()), "right", 0);
        return readArrayNode3;
    }

    private NodeWrapper readArrayPatternD() {
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
        return readArrayNode4;
    }

    private NodeWrapper allocPatternA() {
        // Pattern A) int[int-read]
        NodeWrapper allocArray = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper intRead = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        allocArray.addChild(intRead, "", 0);
        intRead.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 1);
        return allocArray;
    }

    private NodeWrapper allocPatternB() {
        // Pattern B) int[int-read][int-read]
        NodeWrapper allocArray2 = new NodeWrapper(AllocateArrayNode.class.getName());
        NodeWrapper intRead2A = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        NodeWrapper intRead2B = new NodeWrapper(MinicReadNode.MinicIntReadNode.class.getName());
        allocArray2.addChild(intRead2A, "", 0);
        allocArray2.addChild(intRead2B, "", 1);
        intRead2A.getValues().put("PATTERN:" + Requirement.REQPR_MAX_WIDTH, 2);
        return allocArray2;
    }

    private NodeWrapper allocPatternC() {
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
        return allocArray3;
    }

    private NodeWrapper endlessWhileAntipattern() {
        NodeWrapper endlessWhileAntipattern = new NodeWrapper(MinicWhileNode.class.getName());
        NodeWrapper condStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        OrderedRelationship condRel = new OrderedRelationship(endlessWhileAntipattern, condStarWildcard, "conditionNode", 0);
        endlessWhileAntipattern.addChild(condRel);
        NodeWrapper readVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicReadNode.class.getName() + "|" + MinicReadGlobalNode.class.getName());
        readVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        OrderedRelationship condRel2 = new OrderedRelationship(condStarWildcard, readVarNode, "", 0);
        condStarWildcard.addChild(condRel2);
        NodeWrapper bodyStarWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        endlessWhileAntipattern.addChild(new OrderedRelationship(endlessWhileAntipattern, bodyStarWildcard, "bodyNode", 0));
        NodeWrapper writeVarNode = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicWriteNode.class.getName() + "|" + MinicWriteGlobalNode.class.getName());
        writeVarNode.getValues().put("slot:com.oracle.truffle.api.frame.FrameSlot", "0");
        bodyStarWildcard.addChild(writeVarNode, "", 0);
        return endlessWhileAntipattern;
    }

    public NodeWrapper divByZeroWithCast() {
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
        NodeWrapper cast = new NodeWrapper(MinicCastNode.class.getName());
        OrderedRelationship relationshipDos = new OrderedRelationship(antiPattern, cast, "rightNode", 0);
        antiPattern.addChild(relationshipDos);
        NodeWrapper litNodeForbiddenValDos = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenValDos.getValues().put("value:double", 0.0);
        litNodeForbiddenValDos.getValues().put("value:int", 0);
        litNodeForbiddenValDos.getValues().put("value:char", "\0");
        litNodeForbiddenValDos.getValues().put("value:float", 0.0f);
        OrderedRelationship relationshipCast = new OrderedRelationship(cast, litNodeForbiddenValDos, "", 0);
        cast.addChild(relationshipCast);
        return antiPattern;
    }

    public NodeWrapper modByZeroWithCast() {
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTModNode");
        NodeWrapper cast = new NodeWrapper(MinicCastNode.class.getName());
        OrderedRelationship relationshipDos = new OrderedRelationship(antiPattern, cast, "rightNode", 0);
        antiPattern.addChild(relationshipDos);
        NodeWrapper litNodeForbiddenValDos = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenValDos.getValues().put("value:double", 0.0);
        litNodeForbiddenValDos.getValues().put("value:int", 0);
        litNodeForbiddenValDos.getValues().put("value:char", "\0");
        litNodeForbiddenValDos.getValues().put("value:float", 0.0f);
        OrderedRelationship relationshipCast = new OrderedRelationship(cast, litNodeForbiddenValDos, "", 0);
        cast.addChild(relationshipCast);
        return antiPattern;
    }

    /**
     * x / 0 -> causes Arithmetic exception
     *
     * @return fix for problem
     */
    public NodeWrapper divByZeroDirect() {
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTDivNode");
        NodeWrapper litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:double", 0.0);
        litNodeForbiddenVal.getValues().put("value:int", 0);
        litNodeForbiddenVal.getValues().put("value:char", "\0");
        litNodeForbiddenVal.getValues().put("value:float", 0.0f);
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, litNodeForbiddenVal, "rightNode", 0);
        antiPattern.addChild(relationship);
        return antiPattern;
    }

    /**
     * x / 0 -> causes Arithmetic exception
     *
     * @return fix for problem
     */
    public NodeWrapper modByZeroDirect() {
        NodeWrapper antiPattern = new NodeWrapper("MinicDTArithmeticNode$MinicDTModNode");
        NodeWrapper litNodeForbiddenVal = new NodeWrapper(MinicSimpleLiteralNode.class.getName());
        litNodeForbiddenVal.getValues().put("value:double", 0.0);
        litNodeForbiddenVal.getValues().put("value:int", 0);
        litNodeForbiddenVal.getValues().put("value:char", "\0");
        litNodeForbiddenVal.getValues().put("value:float", 0.0f);
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, litNodeForbiddenVal, "rightNode", 0);
        antiPattern.addChild(relationship);
        return antiPattern;
    }


    /**
     * invoke("XXX") -> fails often because the invoke node takes Object as parameter for what is being invoked. The pattern injects only function literals, so FUNCTIONS are called
     *
     * @return fix for problem
     */
    public NodeWrapper invokeFNLiteralAntipattern() {
        NodeWrapper antiPattern = new NodeWrapper(MinicInvokeNode.class.getName());
        NodeWrapper fnLiteralChild = new NodeWrapper(Wildcard.WILDCARD_NOT + MinicFunctionLiteralNode.class.getName());
        OrderedRelationship relationship = new OrderedRelationship(antiPattern, fnLiteralChild, "functionNode", 0);
        antiPattern.addChild(relationship);
        return antiPattern;
    }

    /**
     * (String) charArray -> fails often because the cast takes Object as parameter. The pattern injects only arrays (not yet checking if the array is hiding a char array!)
     *
     * @return fix for problem
     */
    public NodeWrapper charArrayToStringAntipattern() {
        NodeWrapper antiPattern = new NodeWrapper(MinicToStringNodeFactory.MinicCharArrayToStringNodeGen.class.getName());
        // NOTE: We just forbid char array completely
//        NodeWrapper entireArrayNode = new NodeWrapper(Wildcard.WILDCARD_NOT + "(" + MinicReadArrayNode.MinicEntireArrayReadNode.class.getName() + "|" + MinicReadGlobalArrayNode.MinicEntireArrayReadGlobalNode.class.getName() + ")");
//        OrderedRelationship relationship = new OrderedRelationship(antiPattern, entireArrayNode, "", 0);
//        antiPattern.addChild(relationship);
        return antiPattern;
    }

    public NodeWrapper stringAndCharAntipattern() {
        NodeWrapper antiPattern = new NodeWrapper(MinicStringNode.class.getName() + "|" + MinicCharNode.class.getName());
        return antiPattern;
    }

    /**
     * Returns a hardcoded list of currently mined antipatterns that are valid in any MiniC code
     *
     * @return all known antipatterns
     */
    @Override
    public HashMap<NodeWrapper, BitwisePatternMeta> loadAntipatterns() {
        return antipatterns;
    }

    /**
     * Returns a hardcoded list of currently mined patterns that are valid in any MiniC code
     *
     * @return all known patterns
     */
    @Override
    public HashMap<NodeWrapper, BitwisePatternMeta> loadPatterns() {
        return patterns;
    }
}
