/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.algorithm;

import at.fh.hagenberg.aist.gce.optimization.util.NanoProfiler;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import at.fh.hagenberg.aist.gce.pattern.PatternNodeWrapper;
import at.fh.hagenberg.aist.gce.pattern.TrufflePattern;
import at.fh.hagenberg.aist.gce.pattern.TrufflePatternProblem;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.HierarchySupportingNodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.NodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.editor.ValueAbstractingNodeEditor;
import at.fh.hagenberg.aist.gce.pattern.algorithm.labeller.TruffleLanguageLabeller;
import at.fh.hagenberg.aist.gce.pattern.algorithm.labeller.VariableLabeller;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.MaxSupportPerGroupMetric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.Metric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.PatternSizeMetric;
import at.fh.hagenberg.aist.gce.pattern.algorithm.metric.TopNMetric;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.aist.gce.pattern.encoding.TracableBitwisePattern;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.OrderedRelationship;
import at.fh.hagenberg.machinelearning.core.Problem;

import at.fh.hagenberg.machinelearning.core.Solution;
import at.fh.hagenberg.machinelearning.core.SolutionGene;

import org.apache.commons.lang3.ArrayUtils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a rightmost pattern growth algorithm
 *
 * @author Oliver Krauss on 02.12.2020
 */
public class PatternGrowthPatternDetector extends AbstractPatternDetectorAlgorithm<TrufflePattern> {

    /**
     * Solution produced by the algorithm
     */
    protected Solution<TrufflePattern, TrufflePatternProblem> solution;

    /**
     * Editor for manipulating nodes from the DB
     */
    protected NodeEditor<NodeWrapper> editor = new ValueAbstractingNodeEditor(hierarchyFloor == 0);

    /**
     * Labeller extracting variable names out of the nodes
     */
    protected VariableLabeller variableLabeller;

    /**
     * Embedded mining also produces the * wildcard patterns. (Warning! Very expensive operation)
     */
    protected boolean embedded = false;

    protected List<Metric> metrics = new ArrayList<>();

    @Override
    public Solution<TrufflePattern, TrufflePatternProblem> solve(Problem<TrufflePatternProblem> problem, Solution<TrufflePattern, TrufflePatternProblem> solution) {
        // ensure safe reuse
        logInitialized = false;
        logFinalized = false;
        this.solution = null;

        initializeLog(problem);
        this.solution = solution;

        // call IGOR
        Map<Long, NodeWrapper> wrapperMap = new HashMap<>();
        Map<Long, List<OrderedRelationship>> relationshipMap = new HashMap<>();
        Map<Long, String> variableMap = new HashMap<>();
        Map<Long, Map<String, Object>> nodeContentMap = new HashMap<>();
        List<TracableBitwisePattern> finalPatterns = minePatterns(Collections.singletonList(problem.getProblemGenes().get(0).getGene()), wrapperMap, relationshipMap, null, variableMap, nodeContentMap);

        // transform to UI format for patterns
        for (TracableBitwisePattern x : finalPatterns) {
            PatternNodeWrapper transformedNode = transform(x, wrapperMap, relationshipMap, loadMeta(Collections.singletonList(problem.getProblemGenes().get(0).getGene())), variableMap, nodeContentMap);
            TrufflePattern trufflePattern = new TrufflePattern(x, transformedNode);
            trufflePattern.setBitRepresentation(x);
            solution.addGene(new SolutionGene<>(trufflePattern, problem.getProblemGenes()));
        }
        finalizeLog(problem);
        return solution;
    }

    public PatternNodeWrapper transform(TracableBitwisePattern x, Map<Long, NodeWrapper> wrapperMap, Map<Long, List<OrderedRelationship>> relationshipMap, BitwisePatternMeta meta, Map<Long, String> variableMap, Map<Long, Map<String, Object>> nodeContentMap) {
       try {
           PatternNodeWrapper rootNode = new PatternNodeWrapper(new NodeWrapper(processName(meta.unmask(x.getPattern()[0]))), x.getNodeIdsAtPosDisplay(0));
           rootNode.setId(Math.abs(x.getNodeIds()[0][0]));
           rootNode.setValues(new HashMap<>(nodeContentMap.get(x.getNodes()[0])));
           if (x.getVariables()[0] > -1) {
               String variables = Arrays.stream(x.getVariableIdentities()[0]).distinct().filter(y -> y > 0).boxed().map(y -> variableMap.getOrDefault(y, "")).collect(Collectors.joining(";"));
               variableLabeller.inject(rootNode, variables);
           }
           Stack<PatternNodeWrapper> stack = new Stack<>();
           PatternNodeWrapper parent = rootNode;
           stack.push(parent);

           int pos = 1;
           int bitPos = 63;
           int bitPosLong = 0;
           HashMap<PatternNodeWrapper, PatternNodeWrapper> lastAdded = new HashMap<>();

           while (pos < x.getSize()) {
               if (((x.getOpenclosetags()[bitPosLong] >> bitPos) & 1) == 0) {

                   PatternNodeWrapper pnw = new PatternNodeWrapper(new NodeWrapper(processName(meta.unmask(x.getPattern()[pos]))), x.getNodeIdsAtPosDisplay(pos));
                   pnw.setId(Math.abs(x.getNodeIds()[pos][0]));
                   pnw.setValues(new HashMap<>(nodeContentMap.get(x.getNodes()[pos])));
                   if (x.getVariables()[pos] > -1) {
                       String variables = Arrays.stream(x.getVariableIdentities()[pos]).distinct().filter(y -> y > 0).boxed().map(y -> variableMap.getOrDefault(y, "")).collect(Collectors.joining(";"));
                       variableLabeller.inject(pnw, variables);
                   }

                   // add relationship
                   OrderedRelationship relationship = relationshipMap.get(parent.getId()).stream().filter(r -> r.getChild().getId().equals(pnw.getId())).findFirst().orElse(null);
                   if (relationship == null && embedded) {
                       // this was an indirect relationship via the * wildcard
                       if (parent.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                           // path for the root star
                           parent.addChild(pnw, "indirect", parent.getChildren().size());
                       } else {
                           if (lastAdded.containsKey(parent) && lastAdded.get(parent).getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
                               lastAdded.get(parent).addChild(pnw, "indirect", (int) lastAdded.get(parent).getChildren().stream().filter(y -> y.getField().equals("indirect")).count());
                           } else {
                               // stich the wildcard in the correct order of the child nodes for display purposes
                               String relationshipName = "0_indirect";
                               if (lastAdded.containsKey(parent)) {
                                   PatternNodeWrapper finalParent = parent;
                                   OrderedRelationship after_rel = parent.getChildren().stream().filter(y -> y.getChild().equals(lastAdded.get(finalParent))).findFirst().orElse(null);
                                   relationshipName = after_rel.getField();
                               }
                               PatternNodeWrapper starWrapper = new PatternNodeWrapper(new NodeWrapper(Wildcard.WILDCARD_ANYWHERE), meta.mask(Wildcard.WILDCARD_ANYWHERE));
                               starWrapper.addChild(pnw, "indirect", 0);
                               String finalRelationshipName = relationshipName;
                               parent.addChild(starWrapper, relationshipName, (int) parent.getChildren().stream().filter(y -> y.getField().equals(finalRelationshipName)).count());
                               lastAdded.put(parent, starWrapper);
                           }
                       }
                   } else {
                       parent.addChild(pnw, relationship.getField(), relationship.getOrder());
                       lastAdded.put(parent, pnw);
                   }
                   parent = pnw;
                   stack.push(parent);
                   pos++;
               } else {
                   // if close tag move up stack
                   stack.pop();
                   parent = stack.lastElement();
               }

               // move to next bit / long
               bitPos--;
               if (bitPos < 0) {
                   bitPos = 63;
                   bitPosLong++;
               }
           }

           return rootNode;
       } catch (Exception e) {
           return null;
       }
    }

    private String processName(String name) {
        // TODO #63 make this more generic, just hacked in since I was going insane during pattern mining
        // if open tag make new node position
        if (name.contains(".")) {
            name = name.substring(name.lastIndexOf(".") + 1);
        }
        if (name.contains("$")) {
            name = name.substring(name.lastIndexOf("$") + 1);
        }
        name = name.replaceAll("Minic", "").replaceAll("Node", "");
        if (name.endsWith("Gen")) {
            name = name.substring(0, name.length() - 3);
        }
        // NOTE this was only because we combined While and For in EPM Reproduction
//        if (name.equals("For")){
//            name = "Loop";
//        }
        return name;
    }

    /**
     * Core Implementation of the IGOR algorithm can be used for regular Significant Mining as well as Cluster Mining
     *
     * @param clusters        to be considered
     * @param wrapperMap      optional wrapper map (EMPTY!) that will be filled with the NodeWrappers for post-transformations
     * @param relationshipMap optional relation ship map (EMPTY!) that will be filled with OrderedRelationships for post-transformations
     * @param clusterMap      optional map that will be (EMPTY!) filled with the cluster IDs used for post-transformations
     * @param variableMap     optional map that will be (EMPTY!) filled with the variable names that are replaced in the patterns
     * @return all bitwise patterns from the search space
     */
    public List<TracableBitwisePattern> minePatterns(List<TrufflePatternProblem> clusters, Map<Long, NodeWrapper> wrapperMap, Map<Long, List<OrderedRelationship>> relationshipMap, Map<Integer, TrufflePatternProblem> clusterMap, Map<Long, String> variableMap, Map<Long, Map<String, Object>> nodeContentMap) {
        List<Metric> metrics = new ArrayList<>(this.metrics);

        // TODO #252 add all metrics from the individual problems

        // Growth Map is the map of patterns that we must grow
        List<TracableBitwisePattern> growthList = Collections.synchronizedList(new ArrayList<>(50000));

        // Base Map is a cache of all permutations a given node can take
        Map<Long, ArrayList<TracableBitwisePattern>> baseMap = new HashMap<>();

        // Init maps for UI transformations
        if (wrapperMap == null) {
            wrapperMap = new HashMap<>();
        }
        if (relationshipMap == null) {
            relationshipMap = new HashMap<>();
        }
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
        }
        if (variableMap == null) {
            variableMap = new HashMap<>();
        }
        if (nodeContentMap == null) {
            nodeContentMap = new HashMap<>();
        }


        // load the hierarchy
        // NOTE we only support one hierarchy that must be the same for all clusters for now
        BitwisePatternMeta meta = loadMeta(clusters);

        // build the finals for lamba ops
        final BitwisePatternMeta finalMeta = meta;
        final Map<Long, List<OrderedRelationship>> finalRelationshipMap = relationshipMap;
        final Map<Long, NodeWrapper> finalWrapperMap = wrapperMap;
        final Map<Integer, TrufflePatternProblem> finalClusterMap = clusterMap;
        final Map<Long, String> finalVariableMap = variableMap;
        final Map<Long, Map<String, Object>> finalNodeContentMap = nodeContentMap;

        // Inject the star wildcard into the search
        NodeWrapper starWildcard = new NodeWrapper(Wildcard.WILDCARD_ANYWHERE);
        if (embedded) {
            starWildcard.setId(meta.mask(Wildcard.WILDCARD_ANYWHERE));
            nodeContentMap.put(0L, new HashMap<>());
            relationshipMap.put(starWildcard.getId(), new LinkedList<>());
        }

        // Seed the original size 1 points
        clusters.forEach(cluster -> {
            int clusterId = finalClusterMap.size() + 1;
            finalClusterMap.put(clusterId, cluster);
            cluster.getSearchSpace().getSearchSpace().forEach(x -> {

                // enforce relationship order to guarantee rightmost extension in pattern growth
                Stream<OrderedRelationship> sorted = Arrays.stream(x.getValue()).sorted((o1, o2) -> {
                    int sort = o1.getParent().getId().compareTo(o2.getParent().getId());
                    if (sort == 0) {
                        return Integer.compare(o1.getOrder(), o2.getOrder());
                    }
                    return sort;
                });

                // collect relationship map from -> list<to>
                // TODO #249 We might want to use the "field" of ordered relationship in the mining!
                Map<Long, LinkedList<Long>> relationships = new HashMap<>();
                Map<Long, long[]> arrRelationships = new HashMap<>();
                if (!embedded) {
                    // growth can happen only on direct relationships
                    sorted.forEach(relationship -> {
                        if (!relationships.containsKey(relationship.getParent().getId())) {
                            relationships.put(relationship.getParent().getId(), new LinkedList<>());
                            finalRelationshipMap.put(relationship.getParent().getId(), new LinkedList<>());
                        }
                        relationships.get(relationship.getParent().getId()).add(relationship.getChild().getId());
                        finalRelationshipMap.get(relationship.getParent().getId()).add(relationship);
                    });
                } else {
                    // growth happens on ANY location
                    List<OrderedRelationship> collect = sorted.collect(Collectors.toList());
                    List<Long> parents = collect.stream().map(y -> y.getParent().getId()).distinct().collect(Collectors.toList());
                    parents.forEach(relationship -> {
                        if (!relationships.containsKey(relationship)) {
                            relationships.put(relationship, new LinkedList<>());
                            finalRelationshipMap.put(relationship, new LinkedList<>());
                        }

                        // collect all relationships in DFS order (ensuring the rightmost add still works!)
                        relationships.get(relationship).addAll(dfsCollect(relationship, collect, true));
                        finalRelationshipMap.get(relationship).addAll(collect.stream().filter(z -> z.getParent().getId().equals(relationship)).collect(Collectors.toList()));
                    });
                }

                // Map relationships to primitive to not have to do it later
                relationships.forEach((k, v) -> {
                    Long[] vl = new Long[v.size()];
                    v.toArray(vl);
                    arrRelationships.put(k, ArrayUtils.toPrimitive(vl));
                });

                long root = x.getKey()[0].getId();

                if (embedded) {
                    arrRelationships.put(finalMeta.mask(Wildcard.WILDCARD_ANYWHERE), arrRelationships.getOrDefault(root, new long[0]));
                    // add * root node for growing any children from wildcard root
                    TracableBitwisePattern encoded = new TracableBitwisePattern(clusterId, root, starWildcard, finalMeta, arrRelationships, null, 0L);
                    TracableBitwisePattern existingPattern = growthList.stream().filter(y -> y.equals(encoded)).findAny().orElse(null);
                    if (existingPattern != null) {
                        existingPattern.addLocation(encoded);
                    } else {
                        growthList.add(encoded);
                    }
                }

                for (NodeWrapper nodeWrapper : x.getKey()) {
                    String var = variableLabeller != null ? variableLabeller.label(nodeWrapper) : null;
                    Long variable = null;
                    if (var != null) {
                        Map.Entry<Long, String> existingEntry = finalVariableMap.entrySet().stream().filter(y -> y.getValue().equals(var)).findFirst().orElse(null);
                        if (existingEntry == null) {
                            variable = (long) finalVariableMap.size() + 1;
                            finalVariableMap.put(variable, var);
                        } else {
                            variable = existingEntry.getKey();
                        }
                    }
                    editor.edit(nodeWrapper);
                    // init base map with an overextension of what it could possibly take
                    baseMap.put(nodeWrapper.getId(), new ArrayList<>(finalMeta.maxHeight() + 10));
                    finalWrapperMap.put(nodeWrapper.getId(), nodeWrapper);
                    while (editor.hasNext()) {
                        nodeWrapper = editor.next();
                        Map<String, Object> values = nodeWrapper.getValues();
                        Map.Entry<Long, Map<String, Object>> nodeContentEntry = finalNodeContentMap.entrySet().stream().filter(y -> y.getValue().equals(values)).findFirst().orElse(null);
                        long nodeContent;
                        if (nodeContentEntry == null) {
                            nodeContent = (long) finalNodeContentMap.size();
                            finalNodeContentMap.put(nodeContent, values);
                        } else {
                            nodeContent = nodeContentEntry.getKey();
                        }
                        TracableBitwisePattern encoded = new TracableBitwisePattern(clusterId, root, nodeWrapper, finalMeta, arrRelationships, variable, nodeContent);
                        baseMap.get(nodeWrapper.getId()).add(encoded.copy());

                        TracableBitwisePattern existingPattern = growthList.stream().filter(y -> y.equals(encoded)).findAny().orElse(null);
                        if (existingPattern != null) {
                            existingPattern.addLocation(encoded);
                        } else {
                            growthList.add(encoded);
                        }
                    }
                }
            });
        });
        // reduce the overextended base map (pays off if we have hundreds of thousands of nodes)
        baseMap.values().parallelStream().forEach(ArrayList::trimToSize);

        List<TracableBitwisePattern> finalPatterns = Collections.synchronizedList(new ArrayList<>(5000));
        final int[] expanded = {0};
        final int[] pruned = {0};

        // Special handling for top N metrics to take control of the growth list
        metrics.forEach(m -> m.init(finalClusterMap));
        new ArrayList<>(metrics).forEach(x -> {
            if (x instanceof TopNMetric) {
                // we only let the top n metric live and inject all other metrics into top n
                ((TopNMetric) x).setOtherMetrics(new LinkedList<>(metrics));
                ((TopNMetric) x).getOtherMetrics().remove(x);
                metrics.clear();
                metrics.add(x);
                ((TopNMetric) x).injectLists(growthList, finalPatterns);
            }
            if (x instanceof MaxSupportPerGroupMetric) {
                // we only let the top n metric live and inject all other metrics into top n
                ((MaxSupportPerGroupMetric) x).setOtherMetrics(new LinkedList<>(metrics));
                ((MaxSupportPerGroupMetric) x).getOtherMetrics().remove(x);
                metrics.clear();
                metrics.add(x);
                ((MaxSupportPerGroupMetric) x).injectLists(growthList, finalPatterns);
            }
        });

        // restrict to most specialized if we deal with hierarchies
        // TODO #252 with the SpecializationType this changes! + there is a code duplicate below
        if (hierarchyCeil > 1) {
            ArrayList<TracableBitwisePattern> specialRestrict = new ArrayList<>(growthList);
            while (!specialRestrict.isEmpty()) {
                TracableBitwisePattern x = specialRestrict.remove(0);
                if (specialRestrict.stream().anyMatch(y -> x.getCount() == y.getCount() && x.generalizesEqContent(y))) {
                    growthList.remove(x);
                    pruned[0]++;
                }
                List<TracableBitwisePattern> collect = specialRestrict.parallelStream().filter(y -> x.getCount() == y.getCount() && y.generalizesEqContent(x)).collect(Collectors.toList());
                collect.forEach(y -> {
                    growthList.remove(y);
                    specialRestrict.remove(y);
                    pruned[0]++;
                });
            }
        }

        // pre prune size 1 patterns
        new ArrayList<>(growthList).forEach(pattern -> {
            if (metrics.stream().allMatch(m -> m.applicable(pattern))) {
                finalPatterns.add(pattern);
            }
            if (!metrics.stream().allMatch(m -> m.expand(pattern))) {
                growthList.remove(pattern);
                pruned[0]++;
            }
        });

        NanoProfiler nanoProfiler = new NanoProfiler();

        while (!growthList.isEmpty()) {
            System.out.println("Evaluated " + expanded[0] + " Found " + finalPatterns.size() + " Remaining " + growthList.size() + " Pruned " + pruned[0] + " Heap " +
                    DecimalFormat.getNumberInstance().format(Runtime.getRuntime().freeMemory()));
            expanded[0] += growthList.size();
            nanoProfiler.report();

            // move to parallelizable list (growth list collects for next thread split)
            List<TracableBitwisePattern> processList = new ArrayList<>(growthList);
            growthList.clear();

            processList.parallelStream().forEach(pattern -> {
                // check growth points at every position
                int pos = 0;
                while (pos < pattern.getSize()) {
                    final long[] np = {nanoProfiler.start()};

                    long[] growthPoints = pattern.getGrowthPoints(pos);
                    int growth = 0;
                    HashMap<TracableBitwisePattern, ExtensionMapHelperClass> ext_map = new HashMap<>();
                    //HashSet<Long> extPointAlkreadyDone = new HashSet<>(growthPoints.length);
                    while (growth < growthPoints.length) {
                        int finalGrowth = growth;
                        long extPoint = growthPoints[growth + 1];
                        extPoint = extPoint < 0 ? extPoint * -1 : extPoint;
                        growth += 2;

                        // NOTE: I had the genious idea to skip ext points which is almost unrecognizable but introduces a bug (only shown while labelling, but applies everywhere)
                        // Simple fact is WE CAN'T SKIP EXT POINTS
//                        if (extPointAlkreadyDone.contains(extPoint)) {
//                            // the same node can be duplicate over multiple locations. Skip duplicates
//                            continue;
//                        }
//                        extPointAlkreadyDone.add(extPoint);

                        ArrayList<TracableBitwisePattern> patternOps = baseMap.get(extPoint);
                        patternOps.forEach(x -> {
                            if (ext_map.containsKey(x)) {
                                ExtensionMapHelperClass emhc = ext_map.get(x);
                                // TODO #252 THis is the only line worthy of improvement here
                                emhc.pattern.addLocation(x);
                                emhc.appendExtMap(growthPoints[finalGrowth], growthPoints[finalGrowth + 1]);
                            } else {
                                TracableBitwisePattern copy = x.copy();
                                // TODO #252 THis is the only line worthy of improvement here
                                ext_map.put(copy, new ExtensionMapHelperClass(copy, growthPoints[finalGrowth], growthPoints[finalGrowth + 1], growthPoints.length));
                            }
                        });
                    }

                    np[0] = nanoProfiler.profile("growthPoints", np[0]);

                    long start = System.currentTimeMillis();
                    // restrict ext map to most specialized if we deal with hierarchies
                    if (hierarchyCeil > 1) {
                        ArrayList<TracableBitwisePattern> specialRestrict = new ArrayList<>(ext_map.keySet());
                        while (!specialRestrict.isEmpty()) {
                            TracableBitwisePattern x = specialRestrict.remove(0);
                            if (specialRestrict.stream().anyMatch(y -> x.getCount() == y.getCount() && x.generalizesEqContent(y))) {
                                ext_map.remove(x);
                                pruned[0]++;
                            }
                            List<TracableBitwisePattern> collect = specialRestrict.parallelStream().filter(y -> x.getCount() == y.getCount() && y.generalizesEqContent(x)).collect(Collectors.toList());
                            collect.forEach(y -> {
                                ext_map.remove(y);
                                specialRestrict.remove(y);
                                pruned[0]++;
                            });
                        }
                    }

                    int finalPos = pos;
                    ext_map.forEach((k, v) -> {
                        // check and add patterns that are applicable
                        pattern.grow(finalPos, v.getExtMap(), k, embedded).forEach(x -> {
                            // check if this should be added to the final results
                            if (metrics.stream().allMatch(m -> m.applicable(x))) {
                                finalPatterns.add(x);
                            }
                            // check if pattern shall still be grown
                            if (metrics.stream().allMatch(m -> m.expand(x))) {
                                growthList.add(x);
                            } else {
                                pruned[0]++;
                                if (pruned[0] % 200 == 0) {
                                    System.out.println(+pruned[0] + " (pruned) " + growthList.size() + " (remaining) " + finalPatterns.size() + " (found) " + LocalDateTime.now() + " " + DecimalFormat.getNumberInstance().format(Runtime.getRuntime().freeMemory()));
                                    nanoProfiler.report();
                                }
                            }
                        });
                    });
                    pos++;

                    nanoProfiler.profile("growAndMetrics", np[0]);
                }

                // remove growth opportunities from pattern as this has now been thoroughly evaluated (save heap space)
                pattern.finish();
            });
        }

        MaxSupportPerGroupMetric metric = (MaxSupportPerGroupMetric) metrics.get(0);

        System.out.println("Finished expanded " + expanded[0] + " patterns of which " + finalPatterns.size() + " are significant and " + pruned[0] + " were pruned");
        nanoProfiler.report();

        return metric.getPatterns();
    }

    /**
     * in a dfs way collects all children of the given id
     *
     * @param id      to collect
     * @param collect the relationships to search
     * @param root    if this is a root node - indirect relationships are added as negative values
     * @return all relationships dfs sorted
     */
    private Collection<? extends Long> dfsCollect(Long id, List<OrderedRelationship> collect, boolean root) {
        List<Long> dfsIds = new LinkedList<>();
        collect.stream().filter(x -> x.getParent().getId().equals(id)).forEach(x -> {
            dfsIds.add(root ? x.getChild().getId() : x.getChild().getId() * -1);
            dfsIds.addAll(dfsCollect(x.getChild().getId(), collect, false));
        });
        return dfsIds;
    }

    public BitwisePatternMeta loadMeta(List<TrufflePatternProblem> clusters) {
        BitwisePatternMeta meta = clusters.get(0).getHierarchy();
        if (meta == null) {
            // if no hierarchy given, build from language
            TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(clusters.get(0).getLanguage());
            if (tli != null) {
                meta = new BitwisePatternMeta(tli, false);
                if (variableLabeller == null) {
                    variableLabeller = new TruffleLanguageLabeller(tli);
                }
            } else {
                // if language doesn't exist create a dummy hierarchy
                Set<String> classes = new HashSet<>();
                clusters.forEach(y -> y.getSearchSpace().getSearchSpace().forEach(x -> {
                    for (NodeWrapper nodeWrapper : x.getKey()) {
                        classes.add(nodeWrapper.getType());
                    }
                }));
                meta = new BitwisePatternMeta(new LinkedList<>(classes));
            }
        }
        if (editor == null) {
            editor = new HierarchySupportingNodeEditor(meta, hierarchyFloor, hierarchyCeil);
        }
        return meta;
    }

    @Override
    protected Solution<TrufflePattern, TrufflePatternProblem> getSolution() {
        return solution;
    }

    public NodeEditor<NodeWrapper> getEditor() {
        return editor;
    }

    public void setEditor(NodeEditor<NodeWrapper> editor) {
        this.editor = editor;
    }

    public VariableLabeller getVariableLabeller() {
        return variableLabeller;
    }

    public void setVariableLabeller(VariableLabeller variableLabeller) {
        this.variableLabeller = variableLabeller;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    @Override
    public void setHierarchyFloor(int hierarchyFloor) {
        // this function auto-configures, if you want more explicit use, just use the set-editor functio
        super.setHierarchyFloor(hierarchyFloor);
        if (editor == null || editor instanceof ValueAbstractingNodeEditor) {
            if (hierarchyCeil < 2 && hierarchyFloor == hierarchyCeil) {
                // if only exlicit or not explicit asked for -> go for more efficient editor
                editor = new ValueAbstractingNodeEditor(hierarchyFloor == 0);
            } else {
                // otherwise remove the editor as it must be created with a specific hierarchy in mind
                editor = null;
            }
        }
    }

    @Override
    public void setHierarchyCeil(int hierarchyCeil) {
        // this function auto-configures, if you want more explicit use, just use the set-editor function
        super.setHierarchyCeil(hierarchyCeil);
        if (editor == null || editor instanceof ValueAbstractingNodeEditor) {
            if (hierarchyCeil < 2 && hierarchyFloor == hierarchyCeil) {
                // if only exlicit or not explicit asked for -> go for more efficient editor
                editor = new ValueAbstractingNodeEditor(hierarchyFloor == 0);
            } else {
                // otherwise remove the editor as it must be created with a specific hierarchy in mind
                editor = null;
            }
        }
    }

    /**
     * Simple helper class for managing the extension map
     */
    private class ExtensionMapHelperClass {
        public TracableBitwisePattern pattern;
        public int len;
        // TODO #252 we want the ext tuples to be single-dimensional
        private long[][] extMap;

        public ExtensionMapHelperClass(TracableBitwisePattern pattern, long initialLocK, long initialLocV, int size) {
            this.pattern = pattern;
            this.len = 1;
            this.extMap = new long[size / 2 + 1][2];
            this.extMap[0][0] = initialLocK;
            this.extMap[0][1] = initialLocV;
        }

        public long[][] getExtMap() {
            extMap[len] = null;
            return extMap;
        }

        public void setExtMap(long[][] extMap) {
            this.extMap = extMap;
        }

        public void appendExtMap(long k, long v) {
            extMap[len][0] = k;
            extMap[len++][1] = v;
        }
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public void setMaxPatternSize(int maxPatternSize) {
        super.setMaxPatternSize(maxPatternSize);
        PatternSizeMetric metric = (PatternSizeMetric) this.metrics.stream().filter(x -> x instanceof PatternSizeMetric).findAny().orElse(null);
        if (metric != null) {
            metric.setPatternSize(maxPatternSize);
        } else {
            metrics.add(new PatternSizeMetric(maxPatternSize));
        }
    }
}
