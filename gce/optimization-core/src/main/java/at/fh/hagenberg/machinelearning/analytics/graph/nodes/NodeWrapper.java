/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.machinelearning.analytics.graph.nodes;

import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.util.SerializationUtil;
import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.neo4j.driver.internal.value.FloatValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NodeEntity(label = "Node")
public class NodeWrapper implements Serializable {

    /**
     * Id generated by database
     */
    @Id
    protected Long id;

    /**
     * The class name of the wrapped node (Class.getTitle())
     */
    protected String type;

    /**
     * hash code that is (hopefully) uniqe for this (sub)tree
     */
    protected String hash;

    /**
     * Terminal Values of the node that are NOT a Node
     */
    protected Map<String, Object> values = new HashMap<>();

    /**
     * Links to all genes that make up this problem
     */
    @Relationship(type = "CHILD", direction = Relationship.OUTGOING)
    protected Set<OrderedRelationship> children = new TreeSet<>();

    public NodeWrapper() {
    }

    public NodeWrapper(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String simplifiedType() {
        String name = type;
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
        if (this.values.containsKey("slot:com.oracle.truffle.api.frame.FrameSlot")) {
            name += " " + this.values.get("slot:com.oracle.truffle.api.frame.FrameSlot");
        }
        if (this.values.containsKey("index:int")) {
            name += " " + this.values.get("index:int");
        }
        if (this.type.contains("SimpleLiteral") && !this.values.isEmpty()) {
            name += " " + this.values.entrySet().stream().filter(x -> x.getKey().startsWith("value:")).findFirst().get().getValue();
        }
        if (this.type.contains("FunctionLiteral")) {
            name += " " + this.values.get("name:java.lang.String");
        }
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addChild(OrderedRelationship childNode) {
        children.add(childNode);
    }

    public void addChild(NodeWrapper childNode, String fieldName, int pos) {
        children.add(new OrderedRelationship(this, childNode, fieldName, pos));
    }

    public Set<OrderedRelationship> getChildren() {
        return children;
    }

    public Collection<OrderedRelationship> getChildren(String fieldName) {
        return children.stream().filter(x -> x.getField().equals(fieldName)).collect(Collectors.toList());
    }

    public void setChildren(Set<OrderedRelationship> children) {
        this.children = children;
    }


    @Transient
    private static Cache<Integer, NodeWrapper> microCache = init();

    private static Cache<Integer, NodeWrapper> init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("NodeWrapperCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, NodeWrapper.class, ResourcePoolsBuilder.heap(5)))
                .build();
        cacheManager.init();
        return cacheManager.getCache("NodeWrapperCache", Integer.class, NodeWrapper.class);
    }

    /**
     * Helper function that transforms a Truffle node into this class for logging to a graph Database
     *
     * @param node to be logged
     * @return graph of nodes with the given node as parent, including all children.
     */
    public static NodeWrapper wrap(Node node) {
        // little helper that makes sure we don't repeatedly wrap the same node
        if (microCache.containsKey(node.hashCode())) {
            return microCache.get(node.hashCode());
        }
        // Caching only works top-level (as subtrees are NEVER the same objects!)
        NodeWrapper w = new NodeWrapper(node.getClass().getName());
        microCache.put(node.hashCode(), w);
        return wrapChildren(w, node);
    }

    /**
     * Clears cache in case we modified a given node
     */
    public static void clearCache() {
        microCache.clear();
    }

    /**
     * Helper function that transforms a graph node into a Truffle node
     *
     * @param wrapper to be turned into a truffle node
     * @return Truffle Node (entire graph)
     */
    public static Node unwrap(NodeWrapper wrapper, FrameDescriptor localContext, MaterializedFrame globalFrame, String language) {
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformationMinimal(language);
        if (tli == null) {
            throw new RuntimeException("Language " + language + " unknown. Cannot parse node.");
        }


        // create node
        try {
            // get creation information
            TruffleClassInformation tci = tli.getNodes().get(Class.forName(wrapper.getType()));
            tci.getInitializersForProxying().get(0);
            TruffleClassInitializer initializer = tci.getInitializersForProxying().get(0);
            TruffleParameterInformation[] parameters = initializer.getParameters();
            Map<String, Object> children = new HashMap<>();

            // we need to force skip in case of API nodes
            if (!wrapper.children.isEmpty()) {
                new LinkedList<>(wrapper.children).forEach(x -> {
                    if (ExtendedNodeUtil.isAPINode(x.getChild()) && x.getChild().getType().contains("LoopNode")) {
                        wrapper.children.remove(x);
                        wrapper.children.addAll(x.getChild().getChildren().iterator().next().getChild().getChildren());
                    } else if (ExtendedNodeUtil.isAPINode(x.getChild())) {
                        // remove unknown API nodes
                        wrapper.children.remove(x);
                    }
                });
            }

            // make the graph bottom to top
            if (!wrapper.children.isEmpty()) {
                wrapper.children.forEach(x -> {
                    TruffleParameterInformation tpi = Arrays.stream(parameters).filter(param -> param.getName().startsWith(x.getField()) || (param.getField() != null && param.getField().getName().startsWith(x.getField()))).findFirst().orElse(null);
                    if (tpi == null) {
                        tpi = Arrays.stream(parameters).filter(param -> x.getField().startsWith(param.getName()) || (param.getField() != null && x.getField().startsWith(param.getField().getName()))).findFirst().orElse(null);
                    }
                    if (tpi != null) {
                        Node child = unwrap(x.getChild(), localContext, globalFrame, language);

                        if (tpi.isArray()) {
                            if (children.containsKey(x.getField())) {
                                Object[] o = (Object[]) children.get(x.getField());
                                Object[] oNew = Arrays.copyOf(o, o.length + 1);
                                oNew[o.length] = child;
                                children.put(x.getField(), oNew);
                            } else {
                                Object[] o = (Object[]) Array.newInstance(tpi.getType().getComponentType(), 1);
                                o[0] = child;
                                children.put(x.getField(), o);
                            }
                        } else {
                            children.put(x.getField(), child);
                        }
                    } else {
                        Logger.log(Logger.LogLevel.WARN, "Unknown relationship " + x.getField() + " parsing may be incorrect with " + wrapper.getType());
                    }
                });
            }


            Object[] params = Arrays.stream(parameters).map(x -> {
                String identifier = x.getName() + ":" + x.getType().getName();
                if (wrapper.getValues().containsKey(identifier)) {
                    Object value = wrapper.getValues().get(identifier);
                    switch (x.getType().getName()) {
                        case "com.oracle.truffle.api.frame.FrameSlot":
                            if (localContext.findFrameSlot(value) != null) {
                                return localContext.findFrameSlot(value);
                            } else if (globalFrame != null && globalFrame.getFrameDescriptor().findFrameSlot(value) != null) {
                                return globalFrame.getFrameDescriptor().findFrameSlot(value);
                            } else {
                                throw new RuntimeException("Context for parsing is not available");
                            }
                        case "com.oracle.truffle.api.frame.MaterializedFrame":
                            return globalFrame;
                        case "com.oracle.truffle.api.TruffleLanguage$ContextReference":
                            throw new RuntimeException("Language context loading not yet implemented");
                        case "char":
                            if (value instanceof String) {
                                return ((String) value).charAt(0);
                            }
                        case "float":
                            if (value instanceof Double) {
                                return ((Double) value).floatValue();
                            } else if (value instanceof FloatValue) {
                                return (float) ((FloatValue) value).asDouble(); // cannot use .asFloat() as this throws an exception
                            }
                        case "double":
                            if (value instanceof Double) {
                                return ((Double) value).doubleValue();
                            } else if (value instanceof FloatValue) {
                                return ((FloatValue) value).asDouble();
                            }
                        case "int":
                            if (value instanceof Long) {
                                return ((Long) value).intValue();
                            }
                        default:
                            if (Enum.class.isAssignableFrom(x.getType())) {
                                try {
                                    return x.getType().getDeclaredField((String) value);
                                } catch (NoSuchFieldException e) {
                                    throw new RuntimeException("Enum instatiation failed for " + x.getType().getName() + " with " + value);
                                }
                            }
                    }
                    return value;
                } else if (children.containsKey(x.getName())) {
                    return children.get(x.getName());
                } else if (children.containsKey(x.getName() + "_")) {
                    return children.get(x.getName() + "_");
                } else {
                    // final try match starts with
                    String key = children.keySet().stream().filter(c -> c.startsWith(x.getName()) || x.getName().startsWith(c)).findFirst().orElse(null);
                    if (key != null) {
                        return children.get(key);
                    }
                }
                return null;
            }).toArray();
            Node node = (Node) initializer.instantiate(params);
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Class " + wrapper.getType() + " unknown. Cannot parse node.", e);
        }
    }

    @Transient
    private static List<String> packageExcludes;

    private static List<String> getPackageExcludes() {
        if (packageExcludes != null) {
            return packageExcludes;
        }
        packageExcludes = new ArrayList<>();
        packageExcludes.add("com.oracle.truffle.api.profiles"); // we want no truffle specific profiles
        packageExcludes.add("com.oracle.truffle.api.source"); // we want no source fields
        packageExcludes.add("com.oracle.truffle.js.runtime"); // we want no js runtime specifics
        return packageExcludes;
    }

    @Transient
    private static List<String> fieldExcludes;

    private static List<String> getFieldExcludes() {
        if (fieldExcludes != null) {
            return fieldExcludes;
        }
        fieldExcludes = new ArrayList<>();
        fieldExcludes.add("state_"); // we don't wan't to log the state field. It is a truffle specific field in generated nodes
        fieldExcludes.add("state_0_"); // newer graal versions renamed the state field
        fieldExcludes.add("INLINE_CACHE_SIZE"); // we don't want to log INLINE_CACHE_SIZE as caches don't interest us
        fieldExcludes.add("source"); // we seriously do NOT wan't the source
        fieldExcludes.add("charIndex");
        fieldExcludes.add("charLength");
        fieldExcludes.add("STATEMENT_TAG_BIT"); // TODO #74 -> These are finals in JavaScript node. I guess we don't wan't to log constants
        fieldExcludes.add("CALL_TAG_BIT");
        fieldExcludes.add("CHAR_LENGTH_MASK");
        fieldExcludes.add("ROOT_TAG_BIT");
        fieldExcludes.add("EXPRESSION_TAG_BIT");
        fieldExcludes.add("CHAR_INDEX_MASK");
        return fieldExcludes;
    }

    @Transient
    private static List<String> valueExcludes;

    private static List<String> getValueExcludes() {
        if (valueExcludes != null) {
            return valueExcludes;
        }
        valueExcludes = new ArrayList<>();
        valueExcludes.add("com.oracle.truffle.api.source");
        valueExcludes.add("com.oracle.truffle.object");
        valueExcludes.add("com.oracle.truffle.api.object");
        return valueExcludes;
    }

    /**
     * Helper function that transforms a Truffle node into this class for logging to a graph Database
     *
     * @param w    node to be wrapped in
     * @param node to be logged
     * @return graph of nodes with the given node as parent, including all children.
     */
    private static NodeWrapper wrapChildren(NodeWrapper w, Node node) {
        // search through the class and all it's parent classes for fields
        Class fieldClass = node.getClass();
        while (fieldClass != null && fieldClass != Node.class) { // we don't wan't to log the truffle specific fields in Node.class
            // search trough current fields
            for (Field field : fieldClass.getDeclaredFields()) {
                Object value = JavaAssistUtil.safeFieldAccess(field, node);

                // regular child -> wrap
                if (value instanceof Node && Arrays.stream(field.getDeclaredAnnotations()).anyMatch(x -> x.annotationType().equals(Node.Child.class))) {
                    NodeWrapper childNode = NodeWrapper.wrapChildren(new NodeWrapper(value.getClass().getName()), (Node) value);
                    w.addChild(childNode, field.getName(), 0);
                }
                // group of children -> wrap all
                else if (field.getType().isArray() && Arrays.stream(field.getDeclaredAnnotations()).anyMatch(x -> x.annotationType().equals(Node.Children.class))) {
                    Object[] castValue = ((Object[]) value);
                    if (castValue != null) {
                        for (int i = 0; i < castValue.length; i++) {
                            if (castValue[i] != null) {
                                NodeWrapper childNode = NodeWrapper.wrapChildren(new NodeWrapper(castValue[i].getClass().getName()), (Node) castValue[i]);
                                w.addChild(childNode, field.getName(), i);
                            }
                        }
                    }
                }
                // fields
                else if (getPackageExcludes().stream().noneMatch(x -> field.getName().startsWith(x))
                        && (value == null || getValueExcludes().stream().noneMatch(x -> value.getClass().getName().startsWith(x))) // some source fields are "Object" thus we check the value as well.
                        && !field.isSynthetic() // we want no synthetic fields (added by compiler, or framework etc.).
                        && !Node.class.isAssignableFrom(field.getType()) // we do NOT wan't to log nodes that aren't children. They are most likely cache values
                        && !Assumption.class.isAssignableFrom(field.getType()) // we don't want to log assumptions
                        && !getFieldExcludes().contains(field.getName()) // exclude fieldnames
                ) {
                    String fieldName = field.getName() + ":" + field.getType().getName();
                    switch (field.getType().getName()) {
                        case "com.oracle.truffle.api.frame.FrameSlot":
                            w.getValues().put(fieldName, ((FrameSlot) value).getIdentifier());
                            break;
                        case "com.oracle.truffle.api.frame.MaterializedFrame":
                            w.getValues().put(fieldName, ((MaterializedFrame) value).hashCode()); // TODO #74: We actually just wan't to check if there is >1 mat frame. Number observed frames and order (reduces amount of different hashes!)
                            break;
                        case "com.oracle.truffle.api.TruffleLanguage$ContextReference":
                            break; // Don't log the language context -> this is the correct and generic access, some languages may incorrectly try to access a fixed context though!
                        case "char":
                            w.getValues().put(fieldName, String.valueOf(((char) value))); // special char treatment, as Neo4J has no char type
                            break;
                        case "com.oracle.truffle.js.nodes.function.JSBuiltin": // TODO #74 we wan't to enable generic NodeWrapping -> No JS Specifics
                            w.getValues().put(fieldName, JavaAssistUtil.safeFieldAccess("name", value));
                        default:
                            if (Enum.class.isAssignableFrom(field.getType())) {
                                w.getValues().put(fieldName, ((Enum) value).name());
                            } else if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                                w.getValues().put(fieldName, value);
                            } else {
                                if (!nonLogged.contains(field.getType())) {
                                    nonLogged.add(field.getType());
                                }
                                if (value != null && !nonLogged.contains(value.getClass())) {
                                    nonLogged.add(field.getType());
                                }
                                Logger.log(Logger.LogLevel.TRACE, "Not Logging field of type " + field.getType().getName());
                                Logger.log(Logger.LogLevel.TRACE, "Value is of type " + (value == null ? "NULL" : value.getClass().getName()));
                            }
                    }
                }
            }
            fieldClass = fieldClass.getSuperclass();
        }
        // hash and return
        return hash(w);
    }

    @Transient
    private static List<Class> nonLogged = new ArrayList<>();

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    protected byte[][] selfHash() {
        // hash this node
        byte[][] selfHash = new byte[this.getValues().keySet().size() * 2 + 1][];
        selfHash[0] = HashUtil.hash(this.getType().getBytes());
        int i = 1;

        // hash all values
        Iterator<String> vIt = this.getValues().keySet().stream().sorted(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        }).iterator();
        while (vIt.hasNext()) {
            String key = vIt.next();
            selfHash[i] = key.getBytes();
            i++;
            Object value = this.getValues().get(key);
            // ensure that we correctly encode null values
            if (value != null) {
                selfHash[i] = value.toString().getBytes();
            } else {
                selfHash[i] = "".getBytes();
            }
            i++;
        }

        return selfHash;
    }

    /**
     * Helper function that creates a (mostly) unique hash code for this subtree
     * ALL sub-nodes should already have a hash!
     *
     * @param node -> Node to be hashed
     * @return node with a hash set (dependent on children!)
     */
    public static NodeWrapper hash(NodeWrapper node) {
        if (node.getHash() != null) {
            return node;
        }

        byte[][] hashes = Arrays.copyOf(node.selfHash(), node.selfHash().length + node.getChildren().size());
        int i = 1 + node.getValues().keySet().size() * 2;

        // hash all children
        // TODO: #67 When we log commutativity of children (so order irrelevant) we also need to make sure that a->b|c and a->c|b get the same hash
        Iterator<OrderedRelationship> it = node.getChildren().iterator();
        while (it.hasNext()) {
            hashes[i] = HashUtil.decodeHash(it.next().getChild().getHash());
            i++;
        }

        // set the hash
        node.setHash(HashUtil.hashAndEncode(hashes));

        return node;
    }

    /**
     * Helper function that creates a (mostly) unique hash code for this subtree
     * It overrides any hash related to CHILD nodes that already exists (child-nodes don't have to have hash)
     *
     * @param node -> Node to be hashed
     * @return node with a hash set (dependent on children!)
     */
    public static NodeWrapper reHashChildren(NodeWrapper node) {
        byte[][] hashes = Arrays.copyOf(node.selfHash(), node.selfHash().length + node.getChildren().size());
        int i = 1 + node.getValues().keySet().size() * 2;


        // hash all children
        // TODO: #67 When we log commutativity of children (so order irrelevant) we also need to make sure that a->b|c and a->c|b get the same hash
        Iterator<OrderedRelationship> it = node.getChildren().iterator();
        while (it.hasNext()) {
            hashes[i] = HashUtil.decodeHash(reHashChildren(it.next().getChild()).getHash());
            i++;
        }

        // set the hash
        node.setHash(HashUtil.hashAndEncode(hashes));

        return node;
    }

    /**
     * Helper function that creates a (mostly) unique hash code for this subtree
     * It overrides ALL hashes in the tree;
     *
     * @param node -> Node to be hashed
     * @return node with a hash set (dependent on children!)
     */
    public static <T extends NodeWrapper> T reHash(T node) {
        byte[][] hashes = Arrays.copyOf(node.selfHash(), node.selfHash().length + node.getChildren().size());
        int i = 1 + node.getValues().keySet().size() * 2;


        // hash all children
        // TODO: #67 When we log commutativity of children (so order irrelevant) we also need to make sure that a->b|c and a->c|b get the same hash
        Iterator<OrderedRelationship> it = node.getChildren().iterator();
        while (it.hasNext()) {
            hashes[i] = HashUtil.decodeHash(reHash(it.next().getChild()).getHash());
            i++;
        }

        // set the hash
        node.setHash(HashUtil.hashAndEncode(hashes));

        return node;
    }

    /**
     * Helper function to save heap space. We create the hash, but delete it from ALL children
     * DO NOT USE THIS WHEN PERSISTING TO DB!
     *
     * @param node to only contain a hash at the parent node
     * @return tree with hash only in parent.
     */
    public static NodeWrapper reHashAndPurge(NodeWrapper node) {
        node = reHash(node);
        node.getChildren().stream().map(OrderedRelationship::getChild).forEach(NodeWrapper::purge);
        return node;
    }

    private static void purge(NodeWrapper node) {
        node.hash = null;
        node.getChildren().stream().map(OrderedRelationship::getChild).forEach(NodeWrapper::purge);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * Copies the node fully, without any children
     *
     * @return copy of node without children
     */
    public NodeWrapper copy() {
        NodeWrapper copy = new NodeWrapper(this.type);
        copy.values = new HashMap<>(this.values);
        copy.id = this.id;
        copy.hash = this.hash;

        return copy;
    }

    /**
     * Copies the node fully, WITH children
     *
     * @return copy of node with children
     */
    public NodeWrapper deepCopy() {
        NodeWrapper copy = new NodeWrapper(this.type);
        copy.values = new HashMap<>(this.values);
        copy.id = this.id;
        copy.hash = this.hash;
        this.children.forEach(x -> copy.addChild(x.getChild().deepCopy(), x.getField(), x.getOrder()));
        return copy;
    }

    /**
     * Database based trace. Much more performant than hashes
     *
     * @return a uniquely identifying trace
     */
    public String trace() {
        String print = this.getId().toString();
        if (!this.children.isEmpty()) {
            print += ", " + this.children.stream().map(x -> String.valueOf(x.getChild().getId())).collect(Collectors.joining(", "));
        }
        return print;
    }

    /**
     * Calculates the amount of the node and all its children down to the last descendant.
     *
     * @return amount of all nodes in subtree
     */
    public int treeSize() {
        return 1 + this.children.stream().mapToInt(x -> x.getChild().treeSize()).sum();
    }

    /**
     * Gets the ids of all nodes in the graph in DFS
     *
     * @return all node ids in graph
     */
    public Set<Long> getNodeIds() {
        Set<Long> nodes = new HashSet<>();
        return getNodeIds(nodes);
    }

    /**
     * Adds all ids to a given list
     *
     * @param ids all node ids in graph
     * @return all node ids in graph
     */
    protected Set<Long> getNodeIds(Set<Long> ids) {
        ids.add(this.getId());
        children.forEach(x -> x.getChild().getNodeIds(ids));
        return ids;
    }

    /**
     * HumanReadable trace.
     *
     * @return a uniquely identifying trace
     */
    public String humanReadable() {
        String print = this.getType() + " " + (this.getValues().containsKey("value") ? this.getValues().get("value").toString() : "");
        if (this.children.size() > 0) {
            print += "(" + children.stream().map(x -> x.getChild().humanReadable()).collect(Collectors.joining(", ")) + ")";
        }
        return print;
    }

    /**
     * Human readable trace in tree form (new line with indents for children)
     *
     * @return string representation of tree
     */
    public String humanReadableTree() {
        final String[] print = {this.getType().substring(this.getType().lastIndexOf('.') + 1)};
        String key;
        if ((key = this.values.keySet().stream().filter(x -> x.contains("frame.FrameSlot")).findFirst().orElse(null)) != null) {
            print[0] += " " + this.values.get(key);
        }
        this.children.forEach(x -> print[0] += this.humanReadableTree(x.getChild(), 1));
        return print[0];
    }

    private String humanReadableTree(NodeWrapper node, int depth) {
        final String[] print = {System.lineSeparator() + "  ".repeat(depth) + node.getType().substring(node.getType().lastIndexOf('.') + 1)};
        String key;
        if ((key = node.values.keySet().stream().filter(x -> x.contains("frame.FrameSlot")).findFirst().orElse(null)) != null) {
            print[0] += " " + node.values.get(key);
        }
        node.children.forEach(x -> print[0] += this.humanReadableTree(x.getChild(), 1 + depth));
        return print[0];
    }

    public static String serialize(NodeWrapper wrapper) {
        return SerializationUtil.serialize(wrapper);
    }

    public static NodeWrapper deserialize(String wrapper) {
        return (NodeWrapper) SerializationUtil.deserialize(wrapper);
    }

    public static double cyclomaticComplexity(NodeWrapper tree, String language) {
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(language);
        if (tli == null) {
            throw new RuntimeException("Language " + language + " unknown. Cannot calculate cyclomatic complexity.");
        }
        return cyclomaticComplexity(tree, tli);
    }

    private static double cyclomaticComplexity(NodeWrapper tree, TruffleLanguageInformation tli) {
        double complexity = 0.0;
        if (tli.getTci(tree).hasProperty(TruffleClassProperty.CONTROL_FLOW)) {
            complexity += 1;
        }
        complexity += tree.getChildren().stream().mapToDouble(x -> cyclomaticComplexity(x.getChild(), tli)).sum();
        return complexity;
    }

    public static double cyclomaticComplexity(Node tree, String language) {
        TruffleLanguageInformation tli = TruffleLanguageInformation.getLanguageInformation(language);
        if (tli == null) {
            throw new RuntimeException("Language " + language + " unknown. Cannot calculate cyclomatic complexity.");
        }
        return cyclomaticComplexity(tree, tli);
    }

    private static double cyclomaticComplexity(Node tree, TruffleLanguageInformation tli) {
        double complexity = 0.0;
        TruffleClassInformation tci = tli.getTci(tree.getClass());
        if (tci != null && tci.hasProperty(TruffleClassProperty.CONTROL_FLOW)) {
            // tci can be nullable!
            complexity += 1;
        }
        complexity += StreamSupport.stream(tree.getChildren().spliterator(), true).mapToDouble(x -> cyclomaticComplexity(x, tli)).sum();
        return complexity;
    }

    /**
     * Calculates the amout of nodes in a tree
     *
     * @param tree to be evaluated
     * @return amount of nodes in tree (inclusive!)
     */
    public static int size(NodeWrapper tree) {
        return 1 + StreamSupport.stream(tree.getChildren().spliterator(), true).map(OrderedRelationship::getChild).mapToInt(NodeWrapper::size).sum();
    }

    public static Stream<NodeWrapper> flatten(NodeWrapper nodes) {
        return flatten(Stream.of(nodes));
    }

    public static Stream<NodeWrapper> flatten(Stream<NodeWrapper> nodes) {
        return nodes.flatMap(x -> Stream.concat(Stream.of(x), flatten(x.children.stream().map(OrderedRelationship::getChild))));
    }

    /**
     * Checks if ast is contained anywhere in this
     *
     * @param ast possible sub-ast of this
     * @return true if ast occurs anywhere in this
     */
    public boolean contains(NodeWrapper ast) {
        if (Arrays.deepEquals(this.selfHash(), ast.selfHash())) {
            return ltrMatch(ast.getChildren(), this.getChildren());
        } else {
            return this.getChildren().stream().anyMatch(x -> x.getChild().contains(ast));
        }
    }

    /**
     * left to right match of children in an AST. they must occur in the given order, but in may have children in between
     *
     * @param find children to be found
     * @param in   where they are supposed to be found in
     */
    private boolean ltrMatch(Set<OrderedRelationship> find, Set<OrderedRelationship> in) {
        if (find == null || find.isEmpty()) {
            return true;
        }
        Iterator<OrderedRelationship> findIt = find.iterator();
        Iterator<OrderedRelationship> inIt = in.iterator();

        while (findIt.hasNext() && inIt.hasNext()) {
            NodeWrapper currFind = findIt.next().getChild();
            NodeWrapper currIn = inIt.next().getChild();

            boolean found = false;
            while (!(found = directContains(currFind, currIn)) && inIt.hasNext()) {
                currIn = inIt.next().getChild();
            }

            if (found && !findIt.hasNext()) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the find sub-ast is contained in "in". No skipping of nodes allowed
     *
     * @param find node to be found
     * @param in   where it is to be found
     * @return if the sub ast is contained
     */
    private boolean directContains(NodeWrapper find, NodeWrapper in) {
        if (!Arrays.deepEquals(find.selfHash(), in.selfHash())) {
            return false;
        }
        return ltrMatch(find.getChildren(), in.getChildren());
    }

    /**
     * Find the parent of of the given target in this subtree
     *
     * @param target to be found
     * @return parent or NULL (if not in subtree)
     */
    public NodeWrapper getParentOf(NodeWrapper target) {
        Optional<OrderedRelationship> found = this.children.stream().filter(x -> x.getChild().equals(target)).findAny();
        if (found.isEmpty()) {
            return this.children.stream().map(x -> x.getChild().getParentOf(target)).filter(Objects::nonNull).findAny().orElse(null);
        } else {
            return this;
        }
    }
}
