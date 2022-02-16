/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.pattern.encoding;

import at.fh.hagenberg.machinelearning.analytics.graph.nodes.NodeWrapper;
import org.ehcache.sizeof.SizeOf;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a Truffle pattern in a bit array.
 * Also contains information on which trees and nodes correspond to this pattern.
 * <p>
 * Note that we use long[] instead of ArrayList as the current bottleneck is MEMORY and not run-time performance
 *
 * @author Oliver Krauss on 11.04.2019R
 */
public class TracableBitwisePattern extends BitwisePattern {

    protected static final int INCREMENT_SIZE = 20;

    /**
     * Helper field to track what pattern created THIS pattern.
     */
    protected TracableBitwisePattern origin;

    /**
     * At what position the origin was extended, for easy finding of parent hierarchy
     */
    protected int originExt;

    /**
     * The hashes of the nodes that represent this pattern
     */
    private long[] nodes;

    /**
     * The variable identities in the nodes
     */
    private long[]
            variables;

    /**
     * Matches of the identities of the variables to find out if we need renaming for the growth points
     */
    private long[][] variableIdentities;

    /**
     * All trees corresponding to this pattern
     */
    private long[] treeId;

    /**
     * Mapping which tree belongs to which cluster
     */
    private int[] clusterId;

    /**
     * All node-ids corresponding to this pattern
     * [pos][loc] meaning position in pattern and loc meaning location in trees
     */
    private long[][] nodeIds;

    /**
     * Amount of times this pattern matches (yes it can match more than once per tree!)
     */
    private int count;

    /**
     * Sizes of the growthPoints per location the offset defines the BEGINNING in the map
     * Is size + 1 so you can always use [pos * size + loc + 1] to determine the END of the offset
     * the long[loc * count + pos] corresponds exactly to nodeid[pos][loc]
     */
    private int[] growthPointSizeMap;

    /**
     * List of Growth Points per location, the amount of positions can be read from growthPointSizeMap
     * the long[loc * count + pos + ITEM] corresponds to nodeid[pos][loc]
     */
    private long[] growthPoints;

    /**
     * amount of nodes that can still grow in this pattern
     */
    private int growthOpportunities;

    protected TracableBitwisePattern() {
        // Constructor for copy purposes
    }

    public TracableBitwisePattern(int cluster, long root, NodeWrapper node, BitwisePatternMeta meta) {
        this(cluster, root, node, meta, null, null, 1L);
    }

    public TracableBitwisePattern(int cluster, long root, NodeWrapper node, BitwisePatternMeta meta, Map<Long, long[]> growthList, Long variable, Long nodeLookup) {
        super(node, meta);
        nodes[nodePos] = nodeLookup;

        if (variable != null) {
            variables[0] = 0;
            variableIdentities[0][0] = variable;
        } else {
            variables[0] = -1;
            variableIdentities[0] = null;
        }

        treeId[count] = root;
        clusterId[count++] = cluster;
        if (growthList != null) {
            // init growth point size map
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < size; j++) {
                    int pos = i * size + j;
                    if (growthList.containsKey(nodeIds[j][i])) {
                        // allocate pos + prev pos
                        growthPointSizeMap[pos + 1] = growthList.get(nodeIds[j][i]).length + growthPointSizeMap[pos];
                    } else {
                        // allocate 0 size
                        growthPointSizeMap[pos + 1] = growthPointSizeMap[pos];
                    }
                }
            }

            // init growth points
            growthPoints = new long[Math.max(growthPointSizeMap.length * growthPointSizeMap.length, growthPointSizeMap[count * size] + INCREMENT_SIZE)];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < size; j++) {
                    if (growthList.containsKey(nodeIds[j][i])) {
                        System.arraycopy(growthList.get(nodeIds[j][i]), 0, growthPoints, growthPointSizeMap[i * size + j], growthList.get(nodeIds[j][i]).length);
                    }
                }
            }
        } else {
            growthPoints = null;
        }
    }

    @Override
    protected void initDataStructures(int size) {
        super.initDataStructures(size);
        int targetSize = INCREMENT_SIZE;
        while (targetSize < size) {
            targetSize += INCREMENT_SIZE;
        }
        nodes = new long[targetSize];
        treeId = new long[targetSize];
        clusterId = new int[targetSize];
        nodeIds = new long[size][targetSize];
        variables = new long[targetSize];
        variableIdentities = new long[size][targetSize];

        // this allocates as if all points could grow. so we over-allocate
        growthPointSizeMap = new int[targetSize + 1];
    }

    protected void initDataStructures(int size, int locations) {
        super.initDataStructures(size);
        nodes = new long[size];
        treeId = new long[locations];
        clusterId = new int[locations];
        nodeIds = new long[size][locations];
        variables = new long[size];
        variableIdentities = new long[size][locations];
    }

    @Override
    protected long[] dfs(long[] pattern, NodeWrapper node) {
        // add the node
        nodeIds[nodePos][count] = node.getId();
        return super.dfs(pattern, node);
    }

    public long[] getNodes() {
        return nodes;
    }

    public long[] getVariables() {
        return variables;
    }

    public long[][] getVariableIdentities() {
        return variableIdentities;
    }

    public long[] getTreeId() {
        return treeId;
    }

    public int[] getClusterId() {
        return clusterId;
    }

    public long[][] getNodeIds() {
        return nodeIds;
    }

    public int getTreeCount() {
        return (int) Arrays.stream(treeId).filter(x -> x != 0).distinct().count();
    }

    public int getClusterCount() {
        return (int) Arrays.stream(clusterId).filter(x -> x != 0).distinct().count();
    }

    public int getCount() {
        return count;
    }

    public long[] getGrowthPoints() {
        return growthPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TracableBitwisePattern that = (TracableBitwisePattern) o;
        return Arrays.equals(openclosetags, that.openclosetags) && Arrays.equals(nodes, that.nodes) && Arrays.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(nodes);
        return result;
    }

    public void addLocation(TracableBitwisePattern encoded) {
        // check and resize arrays
        if (this.count + encoded.count > this.nodeIds[0].length) {
            int newLen = this.nodeIds[0].length + INCREMENT_SIZE;
            while (newLen - this.nodeIds[0].length < encoded.count) {
                newLen += INCREMENT_SIZE;
            }
            long[] newTreeId = new long[newLen];
            System.arraycopy(this.treeId, 0, newTreeId, 0, this.count);
            this.treeId = newTreeId;
            int[] newClusterId = new int[newLen];
            System.arraycopy(this.clusterId, 0, newClusterId, 0, this.count);
            this.clusterId = newClusterId;

            long[][] newNodeId = new long[nodeIds.length][newLen];
            for (int i = 0; i < this.size; i++) {
                System.arraycopy(nodeIds[i], 0, newNodeId[i], 0, this.count);
            }
            this.nodeIds = newNodeId;

            long[][] newVariableIdentities = new long[nodeIds.length][];
            for (int i = 0; i < this.size; i++) {
                if (variableIdentities[i] != null) {
                    newVariableIdentities[i] = new long[newLen];
                    System.arraycopy(variableIdentities[i], 0, newVariableIdentities[i], 0, this.count);
                }
            }
            this.variableIdentities = newVariableIdentities;

            // increase growth point map size
            int[] newGrowthPointSizeMap = new int[newLen + 1];
            System.arraycopy(growthPointSizeMap, 0, newGrowthPointSizeMap, 0, growthPointSizeMap.length);
            this.growthPointSizeMap = newGrowthPointSizeMap;
        }

        // update growth point size
        int targetSize = this.growthPointSizeMap[count * size] + encoded.growthPointSizeMap[encoded.count * encoded.size];
        if (targetSize > this.growthPoints.length) {
            long[] newGrowthPoints = new long[targetSize * 2];
            System.arraycopy(growthPoints, 0, newGrowthPoints, 0, growthPoints.length);
            this.growthPoints = newGrowthPoints;
        }

        // copy over new growth points
        System.arraycopy(encoded.growthPoints, 0, growthPoints, growthPointSizeMap[count * size], encoded.growthPointSizeMap[encoded.count * encoded.size]);

        // move over tree ids, node ids and growth opportunities
        for (int i = 0; i < encoded.getCount(); i++) {
            this.treeId[this.count] = encoded.treeId[i];
            this.clusterId[this.count] = encoded.clusterId[i];
            for (int j = 0; j < encoded.nodeIds.length; j++) {
                this.nodeIds[j][this.count] = encoded.nodeIds[j][i];
                if (this.variableIdentities[j] != null && encoded.variableIdentities[j] != null) {
                    this.variableIdentities[j][this.count] = encoded.variableIdentities[j][i];
                }
                // adapt positions of new growth points
                growthPointSizeMap[count * size + j + 1] = encoded.growthPointSizeMap[i * encoded.size + j + 1] + growthPointSizeMap[count * size + j];
            }
            count++;
        }
    }

    public TracableBitwisePattern copy() {
        TracableBitwisePattern copy = new TracableBitwisePattern();

        // from root
        copy.size = this.size;
        copy.meta = this.meta;
        copy.pattern = new long[this.pattern.length];
        System.arraycopy(this.pattern, 0, copy.pattern, 0, this.pattern.length);
        copy.openclosetags = new long[this.openclosetags.length];
        System.arraycopy(this.openclosetags, 0, copy.openclosetags, 0, this.openclosetags.length);
        copy.nodePos = this.nodePos;
        copy.openClosePos = this.openClosePos;
        copy.openCloseLong = this.openCloseLong;

        // from trace
        copy.count = this.count;
        copy.treeId = new long[this.treeId.length];
        System.arraycopy(this.treeId, 0, copy.treeId, 0, this.count);
        copy.clusterId = new int[this.clusterId.length];
        System.arraycopy(this.clusterId, 0, copy.clusterId, 0, this.count);
        copy.nodeIds = new long[this.nodeIds.length][this.nodeIds[0].length];
        copy.variableIdentities = new long[this.variableIdentities.length][];
        copy.growthOpportunities = this.growthOpportunities;
        copy.growthPointSizeMap = new int[this.growthPointSizeMap.length];
        System.arraycopy(this.growthPointSizeMap, 0, copy.growthPointSizeMap, 0, this.growthPointSizeMap.length);
        copy.growthPoints = new long[this.growthPoints.length];
        System.arraycopy(this.growthPoints, 0, copy.growthPoints, 0, this.growthPointSizeMap[this.count * this.size]);
        for (int i = 0; i < this.size; i++) {
            System.arraycopy(this.nodeIds[i], 0, copy.nodeIds[i], 0, this.count);
            if (this.variableIdentities[i] != null) {
                copy.variableIdentities[i] = new long[this.variableIdentities[i].length];
                System.arraycopy(this.variableIdentities[i], 0, copy.variableIdentities[i], 0, this.count);
            }
        }
        copy.nodes = new long[this.nodes.length];
        System.arraycopy(this.nodes, 0, copy.nodes, 0, this.size);
        copy.variables = new long[this.nodes.length];
        System.arraycopy(this.variables, 0, copy.variables, 0, this.size);

        return copy;
    }

    /**
     * Grow a pattern by ONE node
     *
     * @param pos        position of the node that will attach this new node (not the new position!)
     * @param ext_tuples tuples of source/target node ids to update the locations
     * @param target     the target pattern (to extract real-valued information)
     * @param embedded   embedded growth requires us to deal with the growth points differently
     * @return the grown pattern
     */
    public List<TracableBitwisePattern> grow(int pos, long[][] ext_tuples, TracableBitwisePattern target, boolean embedded) {
        // checking length is cheaper than over / re allocating arrays
        int newCount = ext_tuples.length - 1;
        while (newCount > 0) {
            // if null we know length
            if (ext_tuples[newCount] == null) {
                break;
            }
            newCount--;
        }

        // find out if we have a new variable increment
        long[] replacementComposition = new long[newCount];
        Arrays.fill(replacementComposition, -1);
        if (target.variables[0] > -1) {
            // set to target in case this pattern has no variables
            boolean hasVariables = false;
            for (int i = 0; i < this.size; i++) {// all the same replacement
                if (variables[i] > -1) {
                    hasVariables = true;
                    boolean found = true;
                    for (int j = 0; j < newCount; j++) {
                        // if all variable names are the same this is the same label - if not we have a problem
                        if (variableIdentities[i][(int) ext_tuples[j][0]] != target.variableIdentities[0][j]) {
                            found = false;
                        } else {
                            replacementComposition[j] = variables[i];
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
            if (!hasVariables || Arrays.stream(replacementComposition).allMatch(x -> x == -1)) {
                // all the same replacement
                Arrays.fill(replacementComposition, target.variables[0]);
            }
        }

        List<Long> distinctLabels = Arrays.stream(replacementComposition).distinct().boxed().collect(Collectors.toList());
        if (distinctLabels.size() > 1) {
            List<TracableBitwisePattern> patterns = new ArrayList<>();
            long[][] ext_tuples_var = new long[newCount][];
            distinctLabels.forEach(x -> {
                long variableLabel = x != -1 ? x : Arrays.stream(this.variables).max().orElse(0) + 1;
                int variableVersionCount = (int) Arrays.stream(replacementComposition).filter(y -> x == y).count();
                int duplPos = 0;
                for (int i = 0; i < replacementComposition.length; i++) {
                    if (replacementComposition[i] == x) {
                        ext_tuples_var[duplPos++] = ext_tuples[i];
                    }
                }
                patterns.add(growFromDuplicationMap(pos, ext_tuples_var, target, variableVersionCount, variableLabel, embedded));
            });
            return patterns;
        }
        return Collections.singletonList(growFromDuplicationMap(pos, ext_tuples, target, newCount, replacementComposition[0], embedded));
    }

    private TracableBitwisePattern growFromDuplicationMap(int pos, long[][] ext_tuples, TracableBitwisePattern target, int newCount, long variableLabel, boolean embedded) {
        TracableBitwisePattern pattern = new TracableBitwisePattern();
        pattern.origin = this;
        pattern.originExt = pos;
        pattern.meta = this.meta;
        // set correct sizes of data structures
        pattern.size = this.size + 1;
        // as we know that the amount of locations can only be <= than before we don't need to over-allocate anymore
        pattern.initDataStructures(pattern.size, newCount);
        pattern.pattern = new long[pattern.size];


        int inject_pos = this.size;
        Set<Integer> parentPositions = new HashSet<>();
        if (pos == 0) {
            // if new child of root just add at the end
            if (this.openclosetags.length > 0) {
                System.arraycopy(this.openclosetags, 0, pattern.openclosetags, 0, this.openclosetags.length);
                pattern.nodePos = this.nodePos;
                pattern.openClosePos = this.openClosePos;
                pattern.openCloseLong = this.openCloseLong;
            }
            pattern.nodePos++;
            pattern.openClosePos--;
            if (pattern.openClosePos < 0) {
                pattern.openClosePos = reverseStartingPos;
                pattern.openCloseLong++;
            }
            pattern.openclosetags[openCloseLong] |= 1L << pattern.openClosePos;
            pattern.openClosePos--;
            if (pattern.openClosePos < 0) {
                pattern.openClosePos = reverseStartingPos;
                pattern.openCloseLong++;
            }
        } else {
            // we go right to left in the origin stack to find after which closing 1 we must inject
            int ocp = 1;
            int ocl = this.openCloseLong;
            int parentPos = pos;
            TracableBitwisePattern parent_trace = this;
            while (parentPos != 0) {
                if (parent_trace.size - 1 == parentPos) {
                    ocp++;
                    if (ocp == reverseStartingPos) {
                        ocp = 0;
                        ocl--;
                    }
                    parentPos = parent_trace.originExt;
                    parentPositions.add(parentPos);
                }
                parent_trace = parent_trace.origin;
            }
            ocp = this.openClosePos + ocp;

            // we are exactly at the injection point
            // copy beginning
            System.arraycopy(this.openclosetags, 0, pattern.openclosetags, 0, ocl + 1);
            // inject 01
            pattern.openclosetags[ocl] = (
                    // Select left side by shifting right (cutoff right part of bitmap) and then shifting two positions (new 01, the +1 is the 1 part) then shifting back to the original positions of the bits
                    (this.openclosetags[ocl] >>> ocp << 2) + 1 << ocp - 2) +
                    // add right side by shifting left (=cutoff left part of bitmap) and shifting back two additional positions
                    (this.openclosetags[ocl] << (reverseStartingPos - ocp + 1) >>> (reverseStartingPos - ocp + 3));

            // copy and stitch everything that follows
            while (ocl + 1 < pattern.openclosetags.length) {
                // add the two missing bit from previous long (luckily at least the bit-mask size can only be a multiple of 2)
                pattern.openclosetags[ocl + 1] = (this.openclosetags[ocl] << reverseStartingPos - 1);
                if (ocl + 1 < this.openclosetags.length) {
                    pattern.openclosetags[ocl + 1] |= (this.openclosetags[ocl + 1] << 2) >>> 2;
                }
                ocl++;
            }
            // correctly set pos and long
            pattern.nodePos = this.nodePos + 1;
            pattern.openClosePos = this.openClosePos;
            pattern.openCloseLong = this.openCloseLong;
            pattern.openClosePos--;
            if (pattern.openClosePos < 0) {
                pattern.openClosePos = reverseStartingPos;
                pattern.openCloseLong++;
            }
            pattern.openClosePos--;
            if (pattern.openClosePos < 0) {
                pattern.openClosePos = reverseStartingPos;
                pattern.openCloseLong++;
            }
        }

        // update pattern
        System.arraycopy(this.pattern, 0, pattern.pattern, 0, inject_pos);
        pattern.pattern[inject_pos] = target.pattern[0];

        // update pattern count
        pattern.count = newCount;

        // update the nodes
        System.arraycopy(this.nodes, 0, pattern.nodes, 0, inject_pos);
        pattern.nodes[inject_pos] = target.nodes[0];

        // update the variables with the correct label
        System.arraycopy(this.variables, 0, pattern.variables, 0, inject_pos);
        pattern.variables[inject_pos] = variableLabel;

        // allcate the growth points
        pattern.growthPointSizeMap = new int[pattern.count * pattern.size + 1];
        int estimate = 0;
        // see how many positions we need per extension - slight over-allocation as likely some extensions will be deleted!
        for (int i = 0; i < newCount; i++) {
            for (int j = 0; j < this.size; j++) {
                estimate += this.growthPointSizeMap[(int) (ext_tuples[i][0] * this.size + j + 1)] - this.growthPointSizeMap[(int) (ext_tuples[i][0] * this.size + j)];
            }
        }
        pattern.growthPoints = new long[estimate + target.growthPoints.length];

        // update treeId and nodeId and also growth points
        for (int i = 0; i < newCount; i++) {
            pattern.treeId[i] = this.treeId[(int) ext_tuples[i][0]];
            pattern.clusterId[i] = this.clusterId[(int) ext_tuples[i][0]];
            for (int j = 0; j < pattern.size; j++) {
                // copy over starting position of growth map
                pattern.growthPointSizeMap[i * pattern.size + j + 1] = pattern.growthPointSizeMap[i * pattern.size + j];

                if (j == inject_pos) {
                    pattern.nodeIds[j][i] = ext_tuples[i][1];
                    if (target.variableIdentities[0] != null) {
                        long search = Math.abs(ext_tuples[i][1]);
                        for (int k = 0; k < target.count; k++) {
                            if (target.nodeIds[0][k] == search) {
                                // inject the variable identity of the correct node
                                pattern.variableIdentities[j][i] = target.variableIdentities[0][k];
                                break;
                            }
                        }
                    }
                    for (int k = 0; k < target.count; k++) {
                        if (target.nodeIds[0][k] == pattern.nodeIds[j][i] && target.growthPointSizeMap[k + 1] > target.growthPointSizeMap[k]) {
                            // note: no need to copy as we throw the target away
                            int growLen = (target.growthPointSizeMap[k + 1] - target.growthPointSizeMap[k]);
                            int growPos = target.growthPointSizeMap[k];
                            pattern.growthPointSizeMap[i * pattern.size + j + 1] = pattern.growthPointSizeMap[i * pattern.size + j] + growLen;
                            System.arraycopy(target.growthPoints, growPos, pattern.growthPoints, pattern.growthPointSizeMap[i * pattern.size + j], growLen);
                        }
                    }
                } else {
                    pattern.nodeIds[j][i] = this.nodeIds[j][(int) ext_tuples[i][0]];
                    if (this.variableIdentities[j] != null) {
                        pattern.variableIdentities[j][i] = this.variableIdentities[j][(int) ext_tuples[i][0]];
                    }

                    int position = (int) ext_tuples[i][0] * this.size + j;
                    int growPos = this.growthPointSizeMap[position];
                    int growLen = (this.growthPointSizeMap[position + 1] - growPos);
                    if (j == pos) {
                        // For direct parent do pruning -> search for the "rightmost-prune-positions"
                        int rmPos = -1;
                        for (int rm = 0; rm < growLen; rm++) {
                            if (this.growthPoints[rm + growPos] == ext_tuples[i][1]) {
                                rmPos = rm;
                                break;
                            }
                        }
                        if (embedded) {
                            // for embedded values we must skip all growth points that are now in the new child instead of the parent
                            rmPos += target.growthPointSizeMap[i + 1] - target.growthPointSizeMap[i];
                        }
                        if (growLen - rmPos - 1 > 0) {
                            pattern.growthPointSizeMap[i * pattern.size + j + 1] = pattern.growthPointSizeMap[i * pattern.size + j] + growLen - rmPos - 1;
                            System.arraycopy(this.growthPoints, growPos + rmPos + 1, pattern.growthPoints, pattern.growthPointSizeMap[i * pattern.size + j], growLen - rmPos - 1);
                        }
                    } else if (parentPositions.contains(j)) {
                        // for parents of parents add growth points
                        pattern.growthPointSizeMap[i * pattern.size + j + 1] = pattern.growthPointSizeMap[i * pattern.size + j] + growLen;
                        System.arraycopy(this.growthPoints, growPos, pattern.growthPoints, pattern.growthPointSizeMap[i * pattern.size + j], growLen);
                    }
                    // for everyone else do NOT add the growth points.
                }
            }
        }

        return pattern;
    }

    /**
     * Returns the growth points only for a specific position. It is a single size array with tuples (e.g. *2 is the pos)
     * value[i * pos + 0] = source
     * value[i * pos + 1] = target
     *
     * @param pos position in the pattern that shall be explored
     */
    public long[] getGrowthPoints(int pos) {
        if (this.growthPoints == null) {
            return new long[0];
        }

        // Pre-Checking what size we must allocate is actually FASTER than increasing the array size
        int ggpPos = 0;
        for (int i = 0; i < this.count; i++) {
            ggpPos += this.growthPointSizeMap[i * size + pos + 1] - this.growthPointSizeMap[i * size + pos];
        }

        if (ggpPos == 0) {
            return new long[0];
        }

        long[] ggp = new long[ggpPos * 2];
        ggpPos = 0;
        for (int i = 0; i < this.count; i++) {
            long sizeG = this.growthPointSizeMap[i * size + pos + 1] - this.growthPointSizeMap[i * size + pos];
            int posG = this.growthPointSizeMap[i * size + pos];
            for (int j = 0; j < sizeG; j++) {
                ggp[ggpPos * 2] = i;
                ggp[ggpPos++ * 2 + 1] = this.growthPoints[posG + j];
            }
        }
        return ggp;
    }

    public void finish() {
        this.growthOpportunities = 0;
        this.growthPoints = null;
    }

    public Collection<Long> getNodeIdsAtPos(int position) {
        List<Long> ids = new ArrayList<>(this.count);
        for (int i = 0; i < count; i++) {
            ids.add(nodeIds[position][i]);
        }
        return ids;
    }

    public Collection<Long> getNodeIdsAtPosDisplay(int position) {
        List<Long> ids = new ArrayList<>(this.count);
        for (int i = 0; i < count; i++) {
            ids.add(Math.abs(nodeIds[position][i]));
        }
        return ids;
    }

    public Collection<Long> getTreeIds() {
        Set<Long> ids = new HashSet<>(this.count);
        for (int i = 0; i < count; i++) {
            ids.add(treeId[i]);
        }
        return ids;
    }

    public Collection<Integer> getClusterIds() {
        Set<Integer> ids = new HashSet<>(this.count);
        for (int i = 0; i < count; i++) {
            ids.add(clusterId[i]);
        }
        return ids;
    }

    public long getClusterCount(Integer cluster) {
        return Arrays.stream(clusterId).filter(x -> x == cluster).count();
    }

    public long getClusterTreeCount(Integer cluster) {
        Set<Long> ids = new HashSet<>(this.count);
        for (int i = 0; i < count; i++) {
            if (this.clusterId[i] == cluster) {
                ids.add(treeId[i]);
            }
        }
        return ids.size();
    }

    public TracableBitwisePattern getOrigin() {
        return origin;
    }

    public void sizeCheck() {
        String debugEstimate = "";
        debugEstimate += System.lineSeparator() + "  META    " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.meta));
        debugEstimate += System.lineSeparator() + "  PATTERN " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.pattern));
        debugEstimate += System.lineSeparator() + "  OCTAGS  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.openclosetags));
        debugEstimate += System.lineSeparator() + "  SIZE    " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.size));
        debugEstimate += System.lineSeparator() + "  ORIGIN  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.origin));
        debugEstimate += System.lineSeparator() + "  ORIG_E  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.originExt));
        debugEstimate += System.lineSeparator() + "  NODES   " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.nodes));
        debugEstimate += System.lineSeparator() + "  VARS    " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.variables));
        debugEstimate += System.lineSeparator() + "  VARID   " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.variableIdentities));
        debugEstimate += System.lineSeparator() + "  TREEID  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.treeId));
        debugEstimate += System.lineSeparator() + "  CLUSTID " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.clusterId));
        debugEstimate += System.lineSeparator() + "  NODEID  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.nodeIds));
        debugEstimate += System.lineSeparator() + "  CNT     " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.count));
        debugEstimate += System.lineSeparator() + "  GROWPOI " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.growthPoints));
        debugEstimate += System.lineSeparator() + "  GROWOP  " + DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.growthOpportunities));

        long size = SizeOf.newInstance().deepSizeOf(this) - (origin != null ? SizeOf.newInstance().deepSizeOf(origin) : SizeOf.newInstance().deepSizeOf(this.meta));

        System.out.println("Pattern size: " + DecimalFormat.getNumberInstance().format(size) +
                " Prune estimate: " +
                DecimalFormat.getNumberInstance().format(SizeOf.newInstance().deepSizeOf(this.growthPoints)) + debugEstimate);

    }


    // conducts an INDUCED, EMBEDDED contains operation, so the dfs order is important, but not the actual tree structure
    // TODO #246 expand this into the diversity algorithm / auto match alg
    // Hint We have the originExt locations which identify the parents!
    public boolean contains(TracableBitwisePattern pattern) {
        if (this.size < pattern.size) {
            return false;
        } else if (this.size == pattern.size) {
            return equals(pattern);
        }

        // left to right node search
        int pos = 0;
        int searchPos = 0;
        while (searchPos < pattern.size) {
            while (pos < size && this.pattern[pos] != pattern.pattern[searchPos]) {
                pos++;
            }
            if (pos < size) {
                searchPos++;
            } else {
                break;
            }
        }

        // not all positions matched
        if (searchPos < pattern.size) {
            return false;
        }
        // all positions matched
        return true;
    }

    /**
     * Checks generalization INCLUDING special values and variables
     * @param otherPattern that is generalized by this pattern
     * @return if equal or not
     */
    public boolean generalizesEqContent(TracableBitwisePattern otherPattern) {
        if (pattern.length != otherPattern.pattern.length || !Arrays.equals(openclosetags, otherPattern.openclosetags)
            || !Arrays.equals(nodes, otherPattern.nodes) || !Arrays.equals(variables, otherPattern.variables)) {
            // not same size -> not structurally equal       // not structurally equal structure
            return false;
        }

        // check masks
        for (int i = 0; i < pattern.length; i++) {
            long size = meta.maskSize(pattern[i]);
            if (pattern[i] >>> (64 - size) != otherPattern.pattern[i] >>> (64 - size)) {
                return false;
            }
        }

        return true;
    }
}
