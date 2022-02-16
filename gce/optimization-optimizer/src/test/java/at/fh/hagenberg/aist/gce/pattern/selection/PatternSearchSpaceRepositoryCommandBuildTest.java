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

import at.fh.hagenberg.aist.gce.pattern.constraint.*;
import at.fh.hagenberg.aist.gce.pattern.selection.PatternSearchSpaceRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * 1) Test the Search commands used for the current DB
 * @author Oliver Krauss on 05.12.2018
 */
public class PatternSearchSpaceRepositoryCommandBuildTest {

    private PatternSearchSpaceRepository repository = new PatternSearchSpaceRepository(null);

    public PatternSearchSpaceRepositoryCommandBuildTest() throws NoSuchMethodException, ClassNotFoundException {
    }

    @Test
    public void buildCachetEmpty() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetNull() {
        // given

        // when
        String constraint = repository.buildRequest(true, null, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetNameOnly() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3"));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetExactly() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", 0.0D));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WHERE c0.quality = 0.0 WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetUpper() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", 1.0, null));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WHERE 1.0 >= c0.quality WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetLower() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", null, -1.0));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WHERE c0.quality >= -1.0 WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetBetween() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", 1.0, -1.0));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WHERE 1.0 >= c0.quality >= -1.0 WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildCachetMulti() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", 0.0D));
        cachets.add(new CachetConstraint("Performance-0.3", 1.0, -1.0));
        cachets.add(new CachetConstraint("Something-0.3", 1.0, null));
        cachets.add(new CachetConstraint("Whatever-0.3", null, -1.0));

        // when
        String constraint = repository.buildRequest(true, cachets, null, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) MATCH (c1:Cachet{name:'Performance-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) MATCH (c2:Cachet{name:'Something-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) MATCH (c3:Cachet{name:'Whatever-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) WHERE c0.quality = 0.0 AND 1.0 >= c1.quality >= -1.0 AND 1.0 >= c2.quality AND c3.quality >= -1.0 WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildSolution() {
        // given
        SolutionConstraint solutionConstraint = new SolutionConstraint(1.0, -1.0);

        // when
        String constraint = repository.buildRequest(true, null, solutionConstraint, null, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (s:Solution)-[:GENE]->(sg) WHERE 1.0 >= s.quality >= -1.0 WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildProblem() {
        // given
        List<ProblemConstraint> problemConstraints = new LinkedList<>();
        problemConstraints.add(new ProblemConstraint("timesTwo"));
        problemConstraints.add(new ProblemConstraint("fibonacci"));

        // when
        String constraint = repository.buildRequest(true, null, null, problemConstraints, null, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (sg)-[:SOLVES]->()-[:RWGENE]->(p:TruffleOptimizationProblem) WHERE p.description in ['timesTwo','fibonacci'] WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildExperiment() {
        // given
        List<ExperimentConstraint> experimentConstraints = new LinkedList<>();
        experimentConstraints.add(new ExperimentConstraint("EXPERIMENT_2019-02-06 10:38:14"));
        experimentConstraints.add(new ExperimentConstraint("EXPERIMENT_2019-02-06 10:38:20"));

        // when
        String constraint = repository.buildRequest(true, null, null, null, experimentConstraints, null, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (sg)-[:SOLVES]->()<-[:GENE]-()<-[:PROBLEM]-(e:AnalyticsNode) WHERE e.title in ['EXPERIMENT_2019-02-06 10:38:14','EXPERIMENT_2019-02-06 10:38:20'] WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildTestValues() {
        // given
        List<TestValueConstraint> testValueConstraints = new LinkedList<>();
        testValueConstraints.add(new TestValueConstraint(null, null, 0.0D));
        testValueConstraints.add(new TestValueConstraint(null, true, 1.0, -1.0));
        testValueConstraints.add(new TestValueConstraint("int", null, 1.0, null));
        testValueConstraints.add(new TestValueConstraint("int", false, null, -1.0));

        // when
        String constraint = repository.buildRequest(true, null, null, null, null, testValueConstraints, null);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[]->(t0) MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[:TEST_INPUT]->(t1) MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[]->(t2) MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[:TEST_OUTPUT]->(t3) WHERE t0.quality = 0.0 AND 1.0 >= t1.quality >= -1.0 AND 1.0 >= t2.quality AND t2.type = 'int' AND t3.quality >= -1.0 AND t3.type = 'int' WITH DISTINCT n as dn CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildTestException() {
        // given
        List<TestResultConstraint> testResultConstraints = new LinkedList<>();
        testResultConstraints.add(new TestResultConstraint("TimeoutException"));
        testResultConstraints.add(new TestResultConstraint("StackOverflowError"));
        testResultConstraints.add(new TestResultConstraint(null));

        // when
        String constraint = repository.buildRequest(true, null, null, null, null, null, testResultConstraints);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:RWGENE]->()-[:TREE]->(n:Node) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr0) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr1) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr2) " +
            "WHERE tr0.exception CONTAINS 'TimeoutException' " +
            "AND tr1.exception CONTAINS 'StackOverflowError' " +
            "AND NOT exists(tr2.exception) " +
            "WITH DISTINCT n as dn " +
            "CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

    @Test
    public void buildAll() {
        // given
        List<CachetConstraint> cachets = new LinkedList<>();
        cachets.add(new CachetConstraint("Accuracy-0.3", 0.0D));
        cachets.add(new CachetConstraint("Performance-0.3", 1.0, -1.0));
        cachets.add(new CachetConstraint("Something-0.3", 1.0, null));
        cachets.add(new CachetConstraint("Whatever-0.3", null, -1.0));
        SolutionConstraint solutionConstraint = new SolutionConstraint(1.0, -1.0);
        List<ProblemConstraint> problemConstraints = new LinkedList<>();
        problemConstraints.add(new ProblemConstraint("timesTwo"));
        problemConstraints.add(new ProblemConstraint("fibonacci"));
        List<ExperimentConstraint> experimentConstraints = new LinkedList<>();
        experimentConstraints.add(new ExperimentConstraint("EXPERIMENT_2019-02-06 10:38:14"));
        experimentConstraints.add(new ExperimentConstraint("EXPERIMENT_2019-02-06 10:38:20"));
        List<TestValueConstraint> testValueConstraints = new LinkedList<>();
        testValueConstraints.add(new TestValueConstraint(null, null, 0.0D));
        testValueConstraints.add(new TestValueConstraint(null, true, 1.0, -1.0));
        testValueConstraints.add(new TestValueConstraint("int", null, 1.0, null));
        testValueConstraints.add(new TestValueConstraint("int", false, null, -1.0));
        List<TestResultConstraint> testResultConstraints = new LinkedList<>();
        testResultConstraints.add(new TestResultConstraint("TimeoutException"));
        testResultConstraints.add(new TestResultConstraint("StackOverflowError"));
        testResultConstraints.add(new TestResultConstraint(null));

        // when
        String constraint = repository.buildRequest(false, cachets, solutionConstraint, problemConstraints, experimentConstraints, testValueConstraints, testResultConstraints);

        // then
        Assert.assertEquals(constraint, "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:ORIGIN]->(n:Node) " +
            "MATCH (s:Solution)-[:GENE]->(sg) MATCH (c0:Cachet{name:'Accuracy-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) " +
            "MATCH (c1:Cachet{name:'Performance-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) " +
            "MATCH (c2:Cachet{name:'Something-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) " +
            "MATCH (c3:Cachet{name:'Whatever-0.3'})<-[:QUALITY]-()-[:GENE]->(sg) " +
            "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->(p:TruffleOptimizationProblem) " +
            "MATCH (sg)-[:SOLVES]->()<-[:GENE]-()<-[:PROBLEM]-(e:AnalyticsNode) " +
            "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[]->(t0) " +
            "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[:TEST_INPUT]->(t1) " +
            "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[]->(t2) " +
            "MATCH (sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[:TEST_OUTPUT]->(t3) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr0) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr1) " +
            "MATCH (sg)-[:RWGENE]->()-[:TEST_RESULT]->(tr2) " +
            "WHERE 1.0 >= s.quality >= -1.0 " +
            "AND c0.quality = 0.0 " +
            "AND 1.0 >= c1.quality >= -1.0 " +
            "AND 1.0 >= c2.quality " +
            "AND c3.quality >= -1.0 " +
            "AND p.description in ['timesTwo','fibonacci'] " +
            "AND e.title in ['EXPERIMENT_2019-02-06 10:38:14','EXPERIMENT_2019-02-06 10:38:20'] " +
            "AND t0.quality = 0.0 " +
            "AND 1.0 >= t1.quality >= -1.0 " +
            "AND 1.0 >= t2.quality " +
            "AND t2.type = 'int' " +
            "AND t3.quality >= -1.0 " +
            "AND t3.type = 'int' " +
            "AND tr0.exception CONTAINS 'TimeoutException' " +
            "AND tr1.exception CONTAINS 'StackOverflowError' " +
            "AND NOT exists(tr2.exception) " +
            "WITH DISTINCT n as dn " +
            "CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships RETURN nodes, relationships");
    }

}