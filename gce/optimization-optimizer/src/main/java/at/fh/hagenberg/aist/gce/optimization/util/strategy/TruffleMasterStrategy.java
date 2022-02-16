/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.util.strategy;

import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.DefaultObservableAndObserverStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.RandomArraySizeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.other.SelectorStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveReadArgSubtreeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveSubtreeStrategy;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.ChooseOption;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.DegreeOfFreedomChooser;
import at.fh.hagenberg.aist.gce.optimization.util.strategy.selection.RandomChooser;
import at.fh.hagenberg.aist.gce.pattern.Wildcard;
import at.fh.hagenberg.aist.gce.pattern.encoding.BitwisePatternMeta;
import at.fh.hagenberg.machinelearning.analytics.graph.PatternRepository;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.*;
import at.fh.hagenberg.util.Pair;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.neo4j.ogm.annotation.Transient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The truffle master strategy combines all other strategies to enable creation of a valid subtree.
 * It contains functionality weaving them together, enabling modifications during runtime, and maintaining validity.
 * Note: The truffle master strategy is the ONLY strategy that should be used by other classes as it manages all other strategies
 *
 * @author Oliver Krauss on 07.11.2018
 */

public class TruffleMasterStrategy extends DefaultObservableAndObserverStrategy implements TruffleCombinedStrategy<Node> {

    /**
     * Configuration for subtree creation
     */
    CreationConfiguration configuration;

    /**
     * Data Flow IN and OUT of the function to be mutated
     * (if no restrictions apply anything goes)
     */
    private DataFlowGraph dataFlowGraph;

    /**
     * valid INSTANTIABLE classes handled by the strategy
     */
    private List<Class> classes = new ArrayList<>();

    /**
     * valid classes, INCLUDING their superclasses as a superclass is always instantiable if the class is.
     */
    private List<Class> classesAndSuperclasses = new ArrayList<>();

    /**
     * All strategies contained in the list, any strategy may support a number of classes to be created
     */
    protected List<TruffleHierarchicalStrategy> strategies;

    /**
     * Strategy for selecting one of the values
     */
    protected ChooseOption<Pair<TruffleHierarchicalStrategy, RequirementInformation>> creationChooser = new DegreeOfFreedomChooser();

    /**
     * Strategy for selecting which part of the search space shall be explored
     */
    protected ChooseOption<TruffleHierarchicalStrategy> chooser = new RandomChooser<>();

    /**
     * The language this strategy exists for
     */
    protected TruffleLanguageSearchSpace tss;

    /**
     * Creates the master strategy from the given truffle language information.
     * Note that the master strategy will only create nodes, and no terminals or arrays of nodes
     *
     * @param configuration      Information containing every possible configuration required for the creation of a subtree
     * @param tss                search space for the strategy
     * @param strategies         hierarchical strategies that shall be applied, if a class has none,
     *                           the {@link at.fh.hagenberg.aist.gce.optimization.util.strategy.random.RandomReflectiveSubtreeStrategy} will be applied automatically.
     *                           The reflective strategy will NOT be applied if a valid strategy is present
     * @param terminalStrategies strategies for terminal values, they will be used in other strategies to seed terminal values.
     *                           Note that if a terminal strategy isn't provided, NO default value will be created
     * @return master strategy with valid configuration
     */
    public static TruffleMasterStrategy createFromTLI(CreationConfiguration configuration, TruffleLanguageSearchSpace tss, List<TruffleHierarchicalStrategy> strategies, Map<String, TruffleVerifyingStrategy> terminalStrategies) {
        TruffleMasterStrategy masterStrategy = new TruffleMasterStrategy();
        masterStrategy.configuration = configuration;
        masterStrategy.strategies = strategies;
        masterStrategy.tss = tss;

        NodeWrapperWeightUtil weightUtil = configuration.getMaxDepth() < Double.MAX_VALUE ? new NodeWrapperWeightUtil(tss.getInformation()) : null;

        // first pass, collect all classes that actually have a valid instantiation mechanism
        tss.getInstantiableNodes().values().forEach(x -> {
            masterStrategy.addClass(x.getClazz());
        });

        // preprocess strategies that already exist by including the master strategy in them
        Set<Class> providedClasses = new HashSet<>();
        strategies.forEach(x -> {
            x.attach(new RandomArraySizeStrategy(masterStrategy, weightUtil));
            if (x instanceof RandomReflectiveSubtreeStrategy) {
                providedClasses.addAll(x.getManagedClasses());
            }
        });

        // second pass add the reflective subtree strategy for all non-specialized classes
        List<TruffleHierarchicalStrategy> addStrategies = new ArrayList<>();

        tss.getInstantiableNodes().values().forEach(x -> {
            // find all classes that we need to add a reflective subtree strategy for
            // also do NOT add strategies that have an impl already and simply can't create anything
            if (!providedClasses.contains(x.getClazz()) && strategies.stream().noneMatch(strat -> strat.canCreate(new CreationInformation(null, null, new RequirementInformation(null), null, x.getClazz(), 0, configuration)) != null)) {
                List<Class> classes = tss.getOperators().entrySet().stream().filter(set -> set.getValue().contains(x.getClazz())).map(Map.Entry::getKey).collect(Collectors.toList());
                addStrategies.add(decideStrategy(x, classes, terminalStrategies, new RandomArraySizeStrategy(masterStrategy, weightUtil)));
            }
        });
        // add the reflective subtree strategies (outside of loop) so the canCreate goes by faster (as a auto-added strategy will work only for one class anyway, we don't need to check)
        strategies.addAll(addStrategies);

        // tell the strategies about the pattern owner
        strategies.forEach(x -> x.injectRootStrategy(masterStrategy));

        // third pass - revalidate classes
//        masterStrategy.revalidate();

        // add info to TLI which classes are NOT accessible with strategy
        tss.getInstantiableNodes().keySet().stream().filter(x -> !masterStrategy.classes.contains(x)).forEach(x -> tss.addUnreachableClass(x, "No strategy exists to create the class or parameters of it."));

        if (masterStrategy.classes.size() == 0) {
            masterStrategy.disabled = true;
        }


        // subscribe to strategies
        masterStrategy.strategies.forEach(x -> {
            if (x instanceof TruffleObservableStrategy) {
                masterStrategy.subscribe((TruffleObservableStrategy) x);
            }
        });

        return masterStrategy;
    }

    /**
     * Injects the patterns for the language the strategy was created for.
     */
    public void autoLoadPatterns() {
        PatternRepository patternRepository = PatternRepository.loadForLanguage(this.tss.getInformation().getName());
        if (patternRepository != null) {
            Logger.log(Logger.LogLevel.INFO, "Injecting " + patternRepository.loadPatterns().size() + " patterns and " + patternRepository.loadAntipatterns().size() + " antipatterns");
            patternRepository.loadAntipatterns().forEach(this::injectAntiPattern);
            patternRepository.loadPatterns().forEach((pattern, meta) -> injectPattern(pattern, meta, 0.33));
        }

        invalidateCache();
    }

    /**
     * Clears ALL patterns and antipatterns out of the master strategy graph
     */
    public void removePatterns() {
        this.strategies.forEach(x -> antiPatterns.keySet().forEach(a -> x.removeCreateRequirement(new Requirement(Requirement.REQ_ANTIPATTERN).addProperty(Requirement.REQPR_PATTERN, a))));
        this.strategies.forEach(x -> patterns.keySet().forEach(a -> x.removeCreateRequirement(new Requirement(Requirement.REQ_PATTERN).addProperty(Requirement.REQPR_PATTERN, a))));
        antiPatterns.clear();
        rootAntiPatterns.clear();
        patterns.clear();
        rootPatterns.clear();
        invalidateCache();
    }

    /**
     * Clears only the given pattern out of this graph
     *
     * @param pattern to be removed
     */
    public void removePattern(NodeWrapper pattern) {
        this.strategies.forEach(x -> x.removeCreateRequirement(new Requirement(Requirement.REQ_ANTIPATTERN).addProperty(Requirement.REQPR_PATTERN, pattern)));
        this.strategies.forEach(x -> x.removeCreateRequirement(new Requirement(Requirement.REQ_PATTERN).addProperty(Requirement.REQPR_PATTERN, pattern)));
        antiPatterns.remove(pattern);
        rootAntiPatterns.removeIf(x -> x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(pattern));
        patterns.remove(pattern);
        rootPatterns.removeIf(x -> x.getProperty(Requirement.REQPR_PATTERN, NodeWrapper.class).equals(pattern));

        invalidateCache();
    }

    /**
     * Checks if a pattern adheres to the definitions that makes them executable.
     * In short this is:
     * - Every relationship must be sortable:
     * For every node: ALL or NONE of the children have a fieldname (not mixable!)
     * a->b->c is valid
     * a-[body]->b->c is valid
     * b<-a-[body]->c is NOT valid as a points to B without fieldname
     * For all relationships Each ORDER must be unique and without skip
     * [0, 1, 2] is valid
     * [body.0, condition.0] is also valid
     * [body.0, body.0] is NOT valid as .0 is repeated
     * [0, 2, 3] is NOT valid as 1 is missing
     *
     * @param pattern
     */
    public static void validatePattern(NodeWrapper pattern) {
        NodeWrapper.flatten(pattern).forEach(x -> {
            // Check if all fieldnames are empty, or if none are
            if (!x.getChildren().stream().allMatch(c -> c.getField() == null || c.getField().isEmpty()) && !x.getChildren().stream().allMatch(c -> c.getField() != null && !c.getField().isEmpty())) {
                throw new RuntimeException("Relationships of a single node must either ALL be qualifying or NONE must be. -> " + x.getType());
            }
            // check if every field (or empty field) has a valid order!
            List<String> fieldNames = x.getChildren().stream().map(OrderedRelationship::getField).distinct().collect(Collectors.toList());
            fieldNames.forEach(f -> {
                List<OrderedRelationship> subset = x.getChildren().stream().filter(c -> Objects.equals(f, c.getField())).collect(Collectors.toList());
                boolean[] vals = new boolean[Integer.max(subset.size(), subset.stream().mapToInt(OrderedRelationship::getOrder).max().orElse(-1) + 1)];
                subset.forEach(s -> vals[s.getOrder()] = true);
                StringBuilder nodes = new StringBuilder();
                boolean failed = false;
                for (int i = 0; i < vals.length; i++) {
                    if (!vals[i]) {
                        failed = true;
                        nodes.append(i).append(" ");
                    }
                }
                if (failed) {
                    throw new RuntimeException("Relationship order must be from 0..n with no breaks in between. -> " + x.getType() + " " + nodes + " is missing");
                }
            });
        });
    }

    /**
     * Injects an anti-pattern that must not be in the tree into the Master Strategy.
     *
     * @param pattern to be injected
     * @param meta    Meta the pattern adheres to (to resolve explicit implementations)
     */
    public void injectAntiPattern(NodeWrapper pattern, BitwisePatternMeta meta) {
        validatePattern(pattern);
        // add to registered anti patterns
        antiPatterns.put(pattern, meta);

        // specialization for small patterns where we can remove relationships
        if (pattern.treeSize() == 2 && !pattern.getType().startsWith(Wildcard.WILDCARD_NOT)) {
            // Specialization for TRAILING nodes (performance only!)
            // Patterns of size 2 happen so often that we take the optimization potential.
            OrderedRelationship onlyRelation = pattern.getChildren().iterator().next();
            NodeWrapper child = onlyRelation.getChild();

            List<String> parentTypes = loadTypes(pattern, meta);
            List<String> childTypes = loadTypes(child, meta);

            // if only the value is forbidden we can't restrict the datatype
            // we also can't restrict if only a subset of fields is restricted
            if (child.getValues().isEmpty()) {
                // modify relationships
                parentTypes.forEach(x -> {
                    TruffleHierarchicalStrategy strat = loadStrat(x);
                    // we can only remove the relationship if this is the only relationship or the restriction applies to all relationships
                    if (strat instanceof RandomReflectiveSubtreeStrategy && (onlyRelation.getField().isEmpty() || ((RandomReflectiveSubtreeStrategy<?>) strat).getInitializer().getParameters().length == 1)) {
                        TruffleHierarchicalStrategy nonTerminalStrategy = ((RandomReflectiveSubtreeStrategy<?>) strat).getNonTerminalStrategy();
                        if (nonTerminalStrategy instanceof RandomArraySizeStrategy) {
                            // evict master strategy and add reduced set of strats via the SelectorStrategy
                            List<TruffleHierarchicalStrategy> strats = new ArrayList<>();
                            if (child.getType().startsWith(Wildcard.WILDCARD_NOT)) {
                                // remove all relationships BUT that one.
                                childTypes.forEach(cT -> strats.add(loadStrat(cT)));
                            } else {
                                // remove exactly this relationship
                                strats.addAll(this.strategies);
                                childTypes.forEach(cT -> strats.remove(loadStrat(cT)));
                            }
                            SelectorStrategy selector = new SelectorStrategy(strats);
                            ((RandomArraySizeStrategy) nonTerminalStrategy).setNonTerminalStrategy(selector);
                            Logger.log(Logger.LogLevel.INFO, "restricted pattern relationships");
                        }
                    }
                });
            }
        }

        // Inject Create Checks for every position in the anti-pattern.
        NodeWrapper.flatten(pattern).forEach(current -> {
            // Anywhere and Not Wildcards are irrelevant. Only real valued nodes must check their identity
            if (!current.getType().equals(Wildcard.WILDCARD_ANYWHERE) && !current.getType().startsWith(Wildcard.WILDCARD_NOT)) {
                List<String> types = loadTypes(current, meta);
                types.forEach(x -> {
                    TruffleHierarchicalStrategy strat = loadStrat(x);
                    if (strat != null) {
                        strat.addCreateRequirement(new Requirement(Requirement.REQ_ANTIPATTERN).addProperty(Requirement.REQPR_PATTERN, pattern).addProperty(Requirement.REQPR_PATTERN_POS, current));
                    }
                });
            }
        });

        // Root patterns are injected into the master strategy
        if (pattern.getType().startsWith(Wildcard.WILDCARD_NOT) || pattern.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
            this.rootAntiPatterns.add(new Requirement(Requirement.REQ_ANTIPATTERN).addProperty(Requirement.REQPR_PATTERN, pattern).addProperty(Requirement.REQPR_PATTERN_POS, pattern));
        }

        invalidateCache();
    }

    private TruffleHierarchicalStrategy loadStrat(String clazz) {
        return this.strategies.stream().filter(y -> y.getManagedClasses().stream().anyMatch(c -> ((Class) c).getName().equals(clazz))).findFirst().orElse(null);
    }

    /**
     * Known antipatterns (unique to one master strategy)
     */
    private Map<NodeWrapper, BitwisePatternMeta> antiPatterns = new HashMap<>();

    /**
     * Patterns that must be checked every time instead of being assigned to an entry-strategy
     */
    private List<Requirement> rootAntiPatterns = new ArrayList<>();

    public List<Requirement> getRootAntiPatterns() {
        return rootAntiPatterns;
    }

    /**
     * Known antipatterns (unique to one master strategy)
     */
    private Map<NodeWrapper, BitwisePatternMeta> patterns = new HashMap<>();

    /**
     * Patterns that must be checked every time instead of being assigned to an entry-strategy
     */
    private List<Requirement> rootPatterns = new ArrayList<>();

    public List<Requirement> getRootPatterns() {
        return rootPatterns;
    }

    public BitwisePatternMeta metaForPattern(NodeWrapper pattern, String patternType) {
        return Requirement.REQ_ANTIPATTERN.equals(patternType) ? antiPatterns.get(pattern) : patterns.get(pattern);
    }

    /**
     * Loads all types out of a pattern node
     *
     * @param pattern to be loaded
     * @return
     * @oaram meta    Meta that resolves the hierarhy layers
     */
    public static List<String> loadTypes(NodeWrapper pattern, BitwisePatternMeta meta) {
        String resolve = pattern.getType();
        if (resolve.startsWith(Wildcard.WILDCARD_NOT)) {
            // ignore leading not wildcard
            resolve = resolve.substring(1);
        }
        // remove encapsulations
        resolve = resolve.replace("(", "").replace(")", "");

        // load all explicit classes of all options
        if (resolve.contains("|")) {
            List<String> clazzes = new ArrayList<>();
            Arrays.stream(resolve.split(Pattern.quote("|"))).map(meta::instantiables).forEach(clazzes::addAll);
            return clazzes;
        } else {
            return meta.instantiables(resolve);
        }
    }

    /**
     * Injects a pattern into the tree.
     *
     * @param pattern          to be injected
     * @param meta             Meta the pattern adheres to (to resolve explicit implementations)
     * @param activationChance between 0 and 1, representing a random chance the pattern activates (1 = always activates)
     */
    public void injectPattern(NodeWrapper pattern, BitwisePatternMeta meta, double activationChance) {
        validatePattern(pattern);
        if (activationChance <= 0) {
            System.out.println("Not injecting a pattern that will never be injected");
            return;
        }

        // add to registered anti patterns
        this.patterns.put(pattern, meta);

        // Inject Create Checks for every position in the pattern.
        NodeWrapper.flatten(pattern).forEach(current -> {
            // Anywhere and Not Wildcards are irrelevant. Only real valued nodes must check their identity
            if (!current.getType().equals(Wildcard.WILDCARD_ANYWHERE) && !current.getType().startsWith(Wildcard.WILDCARD_NOT)) {
                List<String> types = loadTypes(current, meta);
                types.forEach(x -> {
                    TruffleHierarchicalStrategy strat = loadStrat(x);
                    if (strat != null) {
                        Requirement patternReq = new Requirement(Requirement.REQ_PATTERN).addProperty(Requirement.REQPR_PATTERN, pattern).addProperty(Requirement.REQPR_PATTERN_POS, current);
                        if (current.equals(pattern)) {
                            patternReq.addProperty(Requirement.REQ_PATTERN_ACTIVATION_CHANCE, activationChance);
                        }
                        strat.addCreateRequirement(patternReq);
                    }
                });
            }
        });

        // Root patterns are injected into the master strategy
        if (pattern.getType().startsWith(Wildcard.WILDCARD_NOT) || pattern.getType().equals(Wildcard.WILDCARD_ANYWHERE)) {
            Requirement patternReq = new Requirement(Requirement.REQ_PATTERN).addProperty(Requirement.REQPR_PATTERN, pattern).addProperty(Requirement.REQPR_PATTERN_POS, pattern).addProperty(Requirement.REQ_PATTERN_ACTIVATION_CHANCE, activationChance);
            this.rootPatterns.add(patternReq);
        }

        invalidateCache();
    }

// TODO #231 THIS IS DEBUG Info for performance measurement
//    public static HashMap<String, Long> traceMap = new HashMap<>();
//    public int cancreates = 0;
//    public int creates = 0;
//    public int minweights = 0;

    /**
     * Helper Function that
     *
     * @param x                       class strategy is for
     * @param classes                 classes this class can handle
     * @param terminalStrategies      strategies for terminal values
     * @param randomArraySizeStrategy strategy for delegating non-terminals
     * @return best strategy for class
     */
    private static TruffleHierarchicalStrategy decideStrategy(TruffleClassInformation x, List<Class> classes, Map<String, TruffleVerifyingStrategy> terminalStrategies, RandomArraySizeStrategy randomArraySizeStrategy) {
        if (x.hasProperty(TruffleClassProperty.STATE_READ_ARGUMENT)) {
            return new RandomReflectiveReadArgSubtreeStrategy(x, classes, terminalStrategies, randomArraySizeStrategy);
        }

        return new RandomReflectiveSubtreeStrategy(x, classes, terminalStrategies, randomArraySizeStrategy);
    }

    private void revalidate() {
        // remove all classes where no reflective subtree strategy exists (do this as long as there was a removal!)
        AtomicBoolean removal = new AtomicBoolean(true);
        CreationInformation information = new CreationInformation(null, null, new RequirementInformation(null), null, null, 0, configuration);
        while (removal.get()) {
            removal.set(false);
            new ArrayList<>(this.classes).forEach(x -> {
                information.setClazz(x);
                if (!strategies.stream().filter(strategy -> strategy.canCreateVerbose(information) != null).findAny().isPresent()) {
                    removal.set(true);
                    this.removeClass(x);
                }
            });
        }
    }

    @Override
    public Node create(CreationInformation information) {
        // TODO #231 creates++;
        List<Pair<TruffleHierarchicalStrategy, RequirementInformation>> collect = strategies.stream().map(x -> new Pair<>(x, x.canCreateVerbose(information.copy()))).filter(x -> x.getValue() != null).collect(Collectors.toList());
        if (collect.size() > 0) {
            Pair<TruffleHierarchicalStrategy, RequirementInformation> choose = creationChooser.choose(collect);
//            // TODO #63 THIS IS DEBUG CODE IF WE EVER CHANGE THE LOGIC ON HOW PATTERNS WORK
//            String name = choose.getKey() instanceof RandomReflectiveSubtreeStrategy ? ((RandomReflectiveSubtreeStrategy<?>) choose.getKey()).getInformation().getShortName() : "";
//            System.out.println("  ".repeat(Math.max(0, information.getCurrentDepth())) + "CHOSE " + choose.getValue().getDegreesOfFreedom() + " " + name);
            return (Node) choose.getKey().create(information);
        }
        throw new RuntimeException(new InstantiationException("No valid strategy to create " + information.clazz));
    }

    // TODO #231 The Cache can be MASSIVELY improved by checking sizes in the containment (e.g. We can always use >= as soon as we have a truth value)
    @Transient
    private static Cache<CreationInformation, RequirementInformation> microCache = init();
    private static RequirementInformation nullPlaceholder = new RequirementInformation(null);

    private static Cache<CreationInformation, RequirementInformation> init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("RequirementInfoCache",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(CreationInformation.class, RequirementInformation.class, ResourcePoolsBuilder.heap(1000000)))
                .build();
        cacheManager.init();
        return cacheManager.getCache("RequirementInfoCache", CreationInformation.class, RequirementInformation.class);
    }

    @Override
    public RequirementInformation canCreate(CreationInformation information) {
        if (microCache.containsKey(information)) {
            RequirementInformation requirementInformation = microCache.get(information);
            // prevent the cache to be modified after it has been created -> return copy
            return requirementInformation == nullPlaceholder ? null : requirementInformation.copy();
        }

//        TODO #231 THIS IS DEBUG INFO FOR PERFORMANCE UPGRADES
//        Arrays.stream(new RuntimeException().getStackTrace()).forEach(x -> {
//            if (x.getClassName().startsWith("at.fh")) {
//                String s = x.getClassName() + "." + x.getMethodName() + ":" + x.getLineNumber();
//                if (!traceMap.containsKey(s)) {
//                    traceMap.put(s, 1L);
//                } else {
//                    traceMap.put(s, traceMap.get(s) + 1);
//                }
//            }
//        });
        // TODO #231 cancreates++;
        RequirementInformation requirementInformation = !disabled && // if disabled can create nothing
                (classesAndSuperclasses.contains(information.clazz) || // the class can be created
                        (information.getClazz().isArray() && classesAndSuperclasses.contains(information.getClazz().getComponentType())) // or an array version can be created
                ) ?
                satisfy(information) :
                null;// Master strategy depends entirely on children

        if (requirementInformation == null) {
            microCache.put(information, nullPlaceholder);
        } else {
            // Put copy in cache as the returned value might be modified later
            microCache.put(information, requirementInformation.copy());
        }
        return requirementInformation;
    }

    private RequirementInformation satisfy(CreationInformation information) {
        // search until we can satisfy requirement
        List<TruffleHierarchicalStrategy> query = new ArrayList<>(strategies);

        RequirementInformation backup = null;
        while (!query.isEmpty()) {
            TruffleHierarchicalStrategy choice = chooser.choose(query);
            query.remove(choice);
            RequirementInformation rqI = choice.canCreateVerbose(information.copy());
            if (rqI != null) {
                if (rqI.fullfillsAll()) {
                    return rqI;
                } else {
                    backup = rqI;
                }
            }
        }
        return backup;
    }

    @Override
    public RequirementInformation canCreateVerbose(CreationInformation information) {
        return canCreate(information);
    }

    @Override
    public void addCreateRequirement(Requirement requirement) {
        // NOTHING - The Master strategy does not deal with creation checks
    }

    @Override
    public void removeCreateRequirement(Requirement requirement) {
        // NOTHING - The Master strategy does not deal with creation checks
    }

    @Override
    public void attach(TruffleHierarchicalStrategy strategy) {
        // Don't do anything. the master strategy doesn't attach itself anywhere
    }

    @Override
    public TruffleHierarchicalStrategy<Node> injectRootStrategy(TruffleMasterStrategy rootStrategy) {
        return this;
    }

    /**
     * last node that was created
     */
    Node current = null;

    @Override
    public Node current() {
        if (current == null) {
            return next();
        }
        return current;
    }

    @Override
    public Node next() {
        // TODO #231
//        minweights = 0;
//        cancreates = 0;
//        creates = 0;
        Node node = create(new CreationInformation(null, null, new RequirementInformation(null), dataFlowGraph, classes.get(RandomUtil.random.nextInt(classes.size())), 0, configuration));
        // TODO #231 System.out.println("Can Creates: " + cancreates + " creates " + creates + "minweights " + minweights);
        return node;
    }

    public void setCreationChooser(ChooseOption<Pair<TruffleHierarchicalStrategy, RequirementInformation>> creationChooser) {
        this.creationChooser = creationChooser;
    }

    public void setChooser(ChooseOption<TruffleHierarchicalStrategy> chooser) {
        this.chooser = chooser;
    }

    @Override
    public void disabled(TruffleObservableStrategy strategy) {
        // when no choice is left, disable all, when not all are observable,
        if (observing.size() == strategies.size() && observing.stream().allMatch(x -> x.isDisabled())) {
            notifyDisable();
        }
        revalidate();
    }

    @Override
    public void enabled(TruffleObservableStrategy strategy) {
        // one single valid strategy is enough to re-enable the selector
        strategy.getManagedClasses().forEach(this::addClass);
        notifyEnable();
        revalidate();
    }

    @Override
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast, RequirementInformation parentInformation, Map<Node, LoadedRequirementInformation> requirementMap) {
        // fast forward API nodes
        if (ExtendedNodeUtil.isAPINode(ast)) {
            // TODO #229 we currently don't consider multi child api nodes
            ast.getChildren().forEach(x -> loadRequirements(x, parentInformation, requirementMap));
            return requirementMap;
        }

        // forward to real strat
        List<TruffleHierarchicalStrategy> collect = strategies.stream().filter(x -> x.getManagedClasses().contains(ast.getClass())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            // alwayys work with strat 0, there should only be one anyways
            DefaultObservableAndObserverStrategy strategy = (DefaultObservableAndObserverStrategy) collect.get(0);
            return strategy.loadRequirements(ast, parentInformation, requirementMap);
        }

        throw new RuntimeException("Can't load requirements for class " + ast.getClass());
    }

    /**
     * Loads ALL requirements in the entire ast
     *
     * @param ast to be loaded
     * @return requirements per node in ast
     */
    public Map<Node, LoadedRequirementInformation> loadRequirements(Node ast) {
        List<TruffleHierarchicalStrategy> collect = strategies.stream().filter(x -> x.getManagedClasses().contains(ast.getClass())).collect(Collectors.toList());
        if (!collect.isEmpty()) {

            // alwayys work with strat 0, there should only be one anyways
            DefaultObservableAndObserverStrategy strategy = (DefaultObservableAndObserverStrategy) collect.get(0);
            Map<Node, LoadedRequirementInformation> requirements = strategy.loadRequirements(ast, new RequirementInformation(null), new HashMap<>());

            requirements.forEach((k, v) -> modifyRQIforVariables(requirements, ast, k));

            return requirements;
        }

        throw new RuntimeException("Can't load requirements for class " + ast.getClass());
    }

    public void modifyRQIforVariables(Map<Node, LoadedRequirementInformation> requirements, Node ast, Node node) {
        LoadedRequirementInformation loadedReqs = requirements.get(node);
        if (loadedReqs == null) {
            return;
        }

        Collection<Node> nodes = ExtendedNodeUtil.rightContext(node);

        // Modify the laoded REQ for missing WRITE reqs (TODO #216 will become unnecessary when patterns are expressive enough)
        DataFlowUtil.findUnsatisfiedDataItems(this.tss.getInformation(), ast, node).forEach((k, v) -> {
            v.forEach(dfNode -> {
                loadedReqs.getRequirementInformation().addRequirement(new Requirement(Requirement.REQ_DATA_WRITE).addProperty(Requirement.REQPR_SLOT, dfNode));
                // as soon as we got a write req this is a failure
                loadedReqs.setFailed(true);
            });
        });

        // Modify the loadedReq for consideration of variable requirements introduced at a later date
        List<Requirement> varOptions = requirements.entrySet().stream().filter(x ->
                nodes.contains(x.getKey())).flatMap(x -> x.getValue().getRequirements().stream()).filter(x -> x.containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER)).collect(Collectors.toList());
        if (!varOptions.isEmpty()) {
            // RM duplicates
            Set<Integer> ids = new HashSet<>();
            varOptions.removeIf(x ->
                    !ids.add(x.getName().equals(Requirement.REQ_PATTERN_VAR_PLACEHOLDER) ? x.getProperty(Requirement.REQ_REF, Requirement.class).getProperty("ID", Integer.class) : x.getProperty("ID", Integer.class))
            );
            List<Requirement> varPointers = varOptions.stream().filter(x -> x.getName().equals(Requirement.REQ_PATTERN_VAR_PLACEHOLDER)).collect(Collectors.toList());
            loadedReqs.getRequirementInformation().getRequirements().keySet().forEach(x -> {
                if (!x.containsProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER)) {
                    Requirement requirement = Requirement.loadMatch(x, varOptions);
                    if (requirement != null) {
                        x.addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, requirement.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class));
                    } else {
                        varPointers.stream().filter(var -> var.containsProperty(Requirement.REQ_REF) && Requirement.isMatch(var.getProperty(Requirement.REQ_REF, Requirement.class), x)).findFirst().ifPresent(match -> x.addProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, match.getProperty(Requirement.REQ_PATTERN_VAR_PLACEHOLDER, Map.class)));
                    }
                }
            });
        }
    }

    /**
     * Loads requirements for the given node in the ast
     *
     * @param ast  to be loaded
     * @param node that we need requirements for
     * @return requirements for node
     */
    public LoadedRequirementInformation loadRequirements(Node ast, Node node) {
        List<TruffleHierarchicalStrategy> collect = strategies.stream().filter(x -> x.getManagedClasses().contains(ast.getClass())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            while (ExtendedNodeUtil.isAPINode(node)) {
                // TODO #229 we currently don't consider multi child api nodes
                node = node.getChildren().iterator().next();
            }

            // alwayys work with strat 0, there should only be one anyways
            DefaultObservableAndObserverStrategy strategy = (DefaultObservableAndObserverStrategy) collect.get(0);
            Map<Node, LoadedRequirementInformation> requirements = strategy.loadRequirements(ast, new RequirementInformation(null), new HashMap<>());
            LoadedRequirementInformation loadedReqs = requirements.get(node);

            modifyRQIforVariables(requirements, ast, node);

            return loadedReqs;
        }

        throw new RuntimeException("Can't load requirements for class " + ast.getClass());
    }

    private boolean distinctReq(Requirement property) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return seen.add(property.getProperty("ID", Integer.class));
    }

    protected void addClass(Class clazz) {
        if (!classes.contains(clazz)) {
            classes.add(clazz);
            while (clazz != null) {
                if (!classesAndSuperclasses.contains(clazz)) {
                    classesAndSuperclasses.add(clazz);
                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    protected void removeClass(Class clazz) {
        if (classes.contains(clazz)) {
            classes.remove(clazz);
            classesAndSuperclasses.clear();
            classes.forEach(x -> {
                while (x != null) {
                    if (!classesAndSuperclasses.contains(x)) {
                        classesAndSuperclasses.add(x);
                    }
                    x = x.getSuperclass();
                }
            });
        }
    }

    @Override
    public Collection<Class> getManagedClasses() {
        return classes;
    }

    @Override
    public double minWeight(CreationInformation information) {
        // TODO #231 minweights++;
        return strategies.stream().filter(x -> x.canCreate(information) != null).mapToDouble(x -> x.minWeight(information)).min().orElse(Double.MAX_VALUE);
    }

    public List<TruffleHierarchicalStrategy> getStrategies() {
        return strategies;
    }

    public DataFlowGraph getDataFlowGraph() {
        return dataFlowGraph;
    }

    public void setDataFlowGraph(DataFlowGraph dataFlowGraph) {
        this.dataFlowGraph = dataFlowGraph;

        if (dataFlowGraph != null && dataFlowGraph.getSignature() != null) {
            TruffleFunctionSignature signature = dataFlowGraph.getSignature();
            new ArrayList<>(strategies).forEach(x -> {
                if (x instanceof RandomReflectiveReadArgSubtreeStrategy) {
                    // restrict the strategy to only create on valid slots
                    boolean stillWorks = ((RandomReflectiveReadArgSubtreeStrategy<?>) x).restrictBySignature(signature);
                    // performance optimization -> remove all strategies that can't do anything anyway
                    if (!stillWorks) {
                        strategies.remove(x);
                    }
                }
            });
        }
    }

    /**
     * When you make changes to the graph the Cache must be cleared!
     */
    public void invalidateCache() {
        microCache.clear();
    }
}
