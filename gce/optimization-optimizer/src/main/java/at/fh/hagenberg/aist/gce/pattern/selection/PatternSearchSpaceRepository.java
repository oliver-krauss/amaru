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
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.DbHelper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import junit.framework.TestResult;
import org.neo4j.driver.Result;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Repository that help with finding the TREES that we want to analyze for PATTERNS
 * Also turns a TrufflePatternSearchSpaceDefinition into a TrufflePatternSearchSpace
 *
 * @author Oliver Krauss on 28.11.2018
 */
public class PatternSearchSpaceRepository extends ReflectiveNeo4JNodeRepositoryImpl<NodeWrapper> {

    /**
     * Query to load without any filters. This loads ALL trees from the db and is quite expensive
     */
    private static String NO_FILTER = "MATCH (n:Node) WHERE NOT ()-[:CHILD]->(n) call apoc.path.subgraphAll(n, {relationshipFilter:'CHILD>'}) yield nodes, relationships return nodes, relationships";

    /**
     * Query to load specific trees not defined through any other filters
     */
    private static String DIRECT_TREE_LOAD = "MATCH (n:Node) WHERE id(n) in [__ID_CONSTRAINT__] AND NOT ()-[:CHILD]->(n) call apoc.path.subgraphAll(n, {relationshipFilter:'CHILD>'}) yield nodes, relationships return nodes, relationships";

    /**
     * Merge option on ID_CONSTRAINT
     */
    private static String ID_CONSTRAINT_MERGER = ",";

    /**
     * Neo4j Query for all nodes and relationships. One record per graph (record.0 -> nodes, record.1 -> relationships)
     */
    private static String SUBGRAPH_FIND = "MATCH __SEARCH_SPACE__" +
            "__PATH_CONSTRAINT__ " +
            "WHERE __WHERE_CONSTRAINT__ " +
            "WITH DISTINCT n as dn " +
            "CALL apoc.path.subgraphAll(dn, {relationshipFilter:'CHILD>'}) yield nodes, relationships " +
            "RETURN nodes, relationships";

    private static String SOLUTION_SPACE = "(sg)-[:RWGENE]->()-[:TREE]->(n:Node)";

    private static String TARGET_SPACE = "(sg)-[:SOLVES]->()-[:RWGENE]->()-[:ORIGIN]->(n:Node)";

    /**
     * Merge option on path constraints (nodes that must exist)
     */
    private static String PATH_CONSTRAINT_MERGER = " MATCH ";

    /**
     * Merge option on where constraints (values of fields)
     */
    private static String WHERE_CONSTRAINT_MERGER = " AND ";

    /**
     * Variable prefix that Cachet constraints must use.
     */
    private static String CACHET_VARIABLE = "c";

    /**
     * Variable prefix that Solution constraints must use.
     */
    private static String SOLUTION_VARIABLE = "s";

    /**
     * Variable prefix that test value constraints must use.
     */
    private static String TEST_VALUE_VARIABLE = "t";

    /**
     * Variable prefix that test result constraints must use.
     */
    private static String TEST_RESULT_VARIABLE = "tr";

    /**
     * Path constraint for cachets the graphs should fit
     */
    private static String PROBLEM_PATH_CONSTRAINT = "(sg)-[:SOLVES]->()-[:RWGENE]->(p:TruffleOptimizationProblem)";

    /**
     * Experiment constraint for cachets the graphs should fit
     */
    private static String EXPERIMENT_PATH_CONSTRAINT = "(sg)-[:SOLVES]->()<-[:GENE]-()<-[:PROBLEM]-(e:AnalyticsNode)";

    /**
     * Path constraint for cachets the graphs should fit
     */
    private static String CACHET_PATH_CONSTRAINT = "(__CACHET_VARIABLE__:Cachet{name:'__CACHET_NAME__'})<-[:QUALITY]-()-[:GENE]->(sg)";

    /**
     * Path constraint to find the test result values in the target space
     */
    private static String TEST_VALUE_PATH_CONSTRAINT = "(sg)-[:SOLVES]->()-[:RWGENE]->()-[:TEST_CASE]->()-[__DIRECTION_TYPE__]->(__TESTVALUE_VARIABLE__)";

    /**
     * Path constraint to find the test results in the solution space
     */
    private static String TEST_RESULT_PATH_CONSTRAINT = "(sg)-[:RWGENE]->()-[:TEST_RESULT]->(__TESTRESULT_VARIABLE__)";

    private static String IN_TYPE = ":TEST_INPUT";

    private static String OUT_TYPE = ":TEST_OUTPUT";

    /**
     * Where constraint for cachets the graph should fit. Can't exist without Path constraint!
     */
    private static String QUALITY_CONSTRAINT = ".quality";

    /**
     * Path constraint for solutions the graph should fit
     */
    private static String SOLUTION_PATH_CONSTRAINT = "(__SOLUTION_VARIABLE__:Solution)-[:GENE]->(sg)";

    public PatternSearchSpaceRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, NodeWrapper.class);
    }

    /**
     * searches for all possible permutations of all subgraphs in all graphs in the database
     * Danger! Use at your own risk. This loads ALL trees from the database
     *
     * @return all found trees
     */
    public TrufflePatternSearchSpace findTrees() {
        return loadTrees(NO_FILTER);
    }

    /**
     * Searches for all possible permutations of all subgraphs in all graphs in the database.
     * All constraints are nullable
     *
     * @param ssd Constraint(s) on which trees we actually want to group into one search space
     * @return
     */
    public TrufflePatternSearchSpace findTrees(TrufflePatternSearchSpaceDefinition ssd) {
        TrufflePatternSearchSpace trees;
        String constraint = buildRequest(ssd.isSolutionSpace(), ssd.getCachets(), ssd.getSolution(), ssd.getProblems(), ssd.getExperiments(), ssd.getTestValues(), ssd.getTestResult());
        if (!constraint.isEmpty()) {
            trees = loadTrees(constraint, ssd.getIncludedTreeIds() != null ? ssd.getIncludedTreeIds() : new HashSet<>(),
                    ssd.getIncludedNodeIds() != null ? ssd.getIncludedNodeIds() : new HashSet<>(),
                    ssd.getIncludedTypes() != null ? ssd.getIncludedTypes() : new HashSet<>(),
                    ssd.getExcludedTreeIds() != null ? ssd.getExcludedTreeIds() : new HashSet<>(),
                    ssd.getExcludedNodeIds() != null ? ssd.getExcludedNodeIds() : new HashSet<>(),
                    ssd.getExcludedTypes() != null ? ssd.getExcludedTypes() : new HashSet<>());
        } else {
            trees = findTrees();
        }
        // filter for given patterns
        trees.filterPatterns(ssd.getPatterns());
        return trees;
    }

    protected TrufflePatternSearchSpace loadTrees(String dbRequest) {
        return loadTrees(dbRequest, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * Helper class used to find a fixed ordering for how the trees are created
     */
    private class SortingHelperClass implements Comparable<SortingHelperClass> {
        public long start;
        public long end;
        public String field;
        public int order;

        public SortingHelperClass(long start, long end, String field, int order) {
            this.start = start;
            this.end = end;
            this.field = field;
            this.order = order;
        }

        @Override
        public int compareTo(SortingHelperClass o) {
            if (o == null) {
                return 1;
            }
            int comp = 0;
            if (field == null) {
                if (o.field != null) {
                    comp = -1;
                }
            } else {
                comp = field.compareTo(o.field);
            }
            return comp == 0 ? Integer.compare(order, o.order) : comp;
        }
    }

    protected TrufflePatternSearchSpace loadTrees(String dbRequest, Set<Long> includedTrees, Set<Long> includedNodes, Set<String> includedTypes, Set<Long> excludedTrees, Set<Long> excludedNodes, Set<String> excludedTypes) {
        TrufflePatternSearchSpace searchSpace = new TrufflePatternSearchSpace();
        Result result = getTransactionManager().executeRead((transaction) -> transaction.run(dbRequest));

        // load all the records
        result.forEachRemaining(record -> {

            long treeId = record.get(0).get(0).asNode().id();
            includedTrees.remove(treeId);

            // make sure the tree is not excluded
            if (!excludedTrees.contains(treeId)) {
                // we need to know HOW to sort the nodes. Thus we need to sort by relationships
                List<SortingHelperClass> unordered = record.get(1).asList(x -> {
                    Relationship r = x.asRelationship();
                    if (r.type().equals("CHILD")) {
                        return new SortingHelperClass(r.startNodeId(), r.endNodeId(), r.get("field").asString(), r.get("order").asInt());
                    } else {
                        return null;
                    }
                }).stream().filter(Objects::nonNull).sorted(SortingHelperClass::compareTo).collect(Collectors.toList());

                // find root
                LinkedList<SortingHelperClass> ordered = new LinkedList<>();
                Map<Long, Integer> order = new HashMap<>();
                SortingHelperClass root = unordered.stream().filter(x -> unordered.stream().noneMatch(y -> y.end == x.start)).findFirst().orElse(null);

                if (root != null) {
                    // in case we have relationships do something
                    unordered.remove(root);
                    ordered.add(root);
                    order.put(root.start, 0);
                    order.put(root.end, 1);
                    int pos = 2;

                    // order relationships BFS
                    LinkedList<Long> bfsPoint = new LinkedList<>();
                    Long current = root.start;
                    bfsPoint.add(root.end);

                    while (current != null) {
                        Long finalCurrent = current;
                        SortingHelperClass target = unordered.stream().filter(x -> x.start == finalCurrent).findFirst().orElse(null);
                        if (target == null) {
                            // step up
                            if (!bfsPoint.isEmpty() && !unordered.isEmpty()) {
                                current = bfsPoint.removeFirst();
                            } else {
                                current = null;
                            }
                        } else {
                            // step down
                            unordered.remove(target);
                            ordered.addLast(target);
                            bfsPoint.addLast(target.end);
                            order.put(target.end, pos++);
                        }
                    }
                }

                // load all nodes
                AtomicBoolean orderRebuild = new AtomicBoolean(false);
                NodeWrapper[] nodes = new NodeWrapper[0];
                nodes = record.get(0).asList(value -> {
                    Node n = value.asNode();
                    long id = n.id();
                    // manage includes
                    if (includedNodes.contains(id) || includedTypes.contains(n.get("type").asString())) {
                        return DbHelper.cast(n);
                    } else if (excludedNodes.contains(id) || excludedTypes.contains(n.get("type").asString())) {
                        // clean relationship away
                        orderRebuild.set(true);
                        ordered.removeIf(x -> x.start == id || x.end == id);
                        return null;
                    }
                    // manage excludes
                    return DbHelper.cast(n);
                }).stream().filter(Objects::nonNull).sorted(Comparator.comparing(x -> order.get(x.getId()))).collect(Collectors.toList()).toArray(nodes);

                if (nodes.length > 0) {
                    OrderedRelationship[] relationships = new OrderedRelationship[0];
                    NodeWrapper[] finalNodes = nodes;

                    if (orderRebuild.get()) {
                        order.clear();
                        for (int i = 0; i < nodes.length; i++) {
                            order.put(nodes[i].getId(), i);
                        }
                    }
                    relationships = ordered.stream().map(x -> new OrderedRelationship(finalNodes[order.get(x.start)], finalNodes[order.get(x.end)], x.field, x.order)).collect(Collectors.toList()).toArray(relationships);
                    searchSpace.addTree(nodes, relationships);
                }
            }
        });


        if (!includedTrees.isEmpty()) {
            String idConstraint = includedTrees.stream().map(String::valueOf).collect(Collectors.joining(ID_CONSTRAINT_MERGER));
            String directLoad = DIRECT_TREE_LOAD.replace("__ID_CONSTRAINT__", idConstraint);
            TrufflePatternSearchSpace includeSearchSpace = loadTrees(directLoad, new HashSet<>(), includedNodes, includedTypes, new HashSet<>(), excludedNodes, excludedTypes);

            if (searchSpace.searchSpace.isEmpty()) {
                return includeSearchSpace;
            } else {
                searchSpace.searchSpace.addAll(includeSearchSpace.searchSpace);
            }
        }

        return searchSpace;
    }

    private String buildRangeConstraint(String variable, String qualifier, RangeConstraint<Double> constraint) {
        String whereConstraint = "";
        // build where constraints
        if (constraint.getExactly() != null) {
            whereConstraint += WHERE_CONSTRAINT_MERGER + variable + qualifier + " = " + constraint.getExactly();
        } else if (constraint.getLowerLimit() != null && constraint.getUpperLimit() != null) {
            whereConstraint += WHERE_CONSTRAINT_MERGER + constraint.getUpperLimit() + " >= " + variable + qualifier + " >= " + constraint.getLowerLimit();
        } else if (constraint.getLowerLimit() != null) {
            whereConstraint += WHERE_CONSTRAINT_MERGER + variable + qualifier + " >= " + constraint.getLowerLimit();
        } else if (constraint.getUpperLimit() != null) {
            whereConstraint += WHERE_CONSTRAINT_MERGER + constraint.getUpperLimit() + " >= " + variable + qualifier;
        }
        return whereConstraint;
    }

    /**
     * Helper method that builds the constraint string
     *
     * @return valid string for db request, containing ALL given constraints
     */
    protected String buildRequest(boolean solutionSpace, List<CachetConstraint> cachets, SolutionConstraint solutionConstraint, List<ProblemConstraint> problemConstraints, List<ExperimentConstraint> experimentConstraints, List<TestValueConstraint> testValueConstraints, List<TestResultConstraint> testResultConstraints) {
        String pathConstraint = "";
        String whereConstraint = "";

        // build solution constraint (distance from SG 1)
        if (solutionConstraint != null) {
            // select variable name for where constraints
            String variable = SOLUTION_VARIABLE;
            String where = buildRangeConstraint(variable, QUALITY_CONSTRAINT, solutionConstraint);
            whereConstraint += where;
            if (where.isEmpty()) {
                variable = "";
            }
            pathConstraint += PATH_CONSTRAINT_MERGER + SOLUTION_PATH_CONSTRAINT.replace("__SOLUTION_VARIABLE__", variable);
        }

        // build cachet constraints (distance from SG 2)
        if (cachets != null && cachets.size() > 0) {
            int i = 0;
            for (CachetConstraint cachet : cachets) {
                // select variable name for where constraints
                String variable = CACHET_VARIABLE + i++;
                String where = buildRangeConstraint(variable, QUALITY_CONSTRAINT, cachet);
                whereConstraint += where;
                if (where.isEmpty()) {
                    variable = "";
                }
                pathConstraint += PATH_CONSTRAINT_MERGER + CACHET_PATH_CONSTRAINT.replace("__CACHET_NAME__", cachet.getName()).replace("__CACHET_VARIABLE__", variable);
            }
        }

        // build TruffleProblem (distance from SG 2)
        if (problemConstraints != null && problemConstraints.size() > 0) {
            pathConstraint += PATH_CONSTRAINT_MERGER + PROBLEM_PATH_CONSTRAINT;
            whereConstraint += WHERE_CONSTRAINT_MERGER + "p.description in [";
            for (ProblemConstraint constraint : problemConstraints) {
                whereConstraint += "'" + constraint.getName() + "',";
            }
            whereConstraint = whereConstraint.substring(0, whereConstraint.length() - 1);
            whereConstraint += "]";
        }

        // build Experiment (distance from SG 3)
        if (experimentConstraints != null && experimentConstraints.size() > 0) {
            pathConstraint += PATH_CONSTRAINT_MERGER + EXPERIMENT_PATH_CONSTRAINT;
            whereConstraint += WHERE_CONSTRAINT_MERGER + "e.title in [";
            for (ExperimentConstraint constraint : experimentConstraints) {
                whereConstraint += "'" + constraint.getTitle() + "',";
            }
            whereConstraint = whereConstraint.substring(0, whereConstraint.length() - 1);
            whereConstraint += "]";
        }

        // build TestValue-in out (distance from SG 4)
        if (testValueConstraints != null && testValueConstraints.size() > 0) {
            int i = 0;
            for (TestValueConstraint testValue : testValueConstraints) {
                // select variable name for where constraints
                String variable = TEST_VALUE_VARIABLE + i++;
                String where = buildRangeConstraint(variable, QUALITY_CONSTRAINT, testValue);
                if (testValue.getType() != null) {
                    if (!where.isEmpty()) {
                        where += WHERE_CONSTRAINT_MERGER;
                    }
                    where += variable + ".type = '" + testValue.getType() + "'";
                }
                whereConstraint += where;
                if (where.isEmpty()) {
                    variable = "";
                }
                String direction = testValue.getInput() == null ? "" : (testValue.getInput() ? IN_TYPE : OUT_TYPE);
                pathConstraint += PATH_CONSTRAINT_MERGER + TEST_VALUE_PATH_CONSTRAINT.replace("__TESTVALUE_VARIABLE__", variable).replace("__DIRECTION_TYPE__", direction);
            }
        }

        // build Test Result (distance from SG 2)
        if (testResultConstraints != null && testResultConstraints.size() > 0) {
            int i = 0;
            for (TestResultConstraint testResult : testResultConstraints) {
                // select variable name for where constraints
                String variable = TEST_RESULT_VARIABLE + i++;

                // create where
                String where = testResult.getException() == null ? (
                        // empty
                        "NOT exists(" + variable + ".exception)"
                ) : (
                        // contians
                        variable + ".exception CONTAINS '" + testResult.getException() + "'"
                );
                whereConstraint += WHERE_CONSTRAINT_MERGER + where;
                pathConstraint += PATH_CONSTRAINT_MERGER + TEST_RESULT_PATH_CONSTRAINT.replace("__TESTRESULT_VARIABLE__", variable);
            }
        }

        // cleanup request
        String space = solutionSpace ? SOLUTION_SPACE : TARGET_SPACE;
        String request = SUBGRAPH_FIND.replace("__SEARCH_SPACE__", space);
        if (!pathConstraint.isEmpty()) {
            request = request.replace("__PATH_CONSTRAINT__", pathConstraint);
            if (!whereConstraint.isEmpty()) {
                whereConstraint = whereConstraint.substring(WHERE_CONSTRAINT_MERGER.length());
                request = request.replace("__WHERE_CONSTRAINT__", whereConstraint);
            } else {
                request = request.replace("WHERE __WHERE_CONSTRAINT__ ", "");
            }
        } else {
            request = request.replace("__PATH_CONSTRAINT__ WHERE __WHERE_CONSTRAINT__", "");
        }

        return request;
    }


}

