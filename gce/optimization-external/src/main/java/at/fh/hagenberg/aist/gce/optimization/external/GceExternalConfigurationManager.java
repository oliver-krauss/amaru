/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.external;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.util.*;
import at.fh.hagenberg.aist.hlc.core.ConfigurationManagementWorker;
import at.fh.hagenberg.aist.hlc.core.messages.*;
import science.aist.neo4j.util.Pair;
import at.fh.hagenberg.machinelearning.analytics.graph.TruffleLanguageInformationRepository;
import at.fh.hagenberg.machinelearning.core.Configurable;
import at.fh.hagenberg.machinelearning.core.options.Descriptor;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides Configurable information on what the workers and languages can support.
 *
 * @author Oliver Krauss on 26.10.2019
 */
public class GceExternalConfigurationManager implements ConfigurationManagementWorker {

    private static final String problemDescriptor = "problem";

    private static final ExternalOptimizationFunctionalityProvider provider = new ExternalOptimizationFunctionalityProvider();

    private static final TruffleLanguageInformationRepository repository = ApplicationContextProvider.getCtx().getBean(TruffleLanguageInformationRepository.class);

    @Override
    public ConfigurationResponse getConfiguration(ConfigurationRequest configurationRequest) {

        List<Language> languages = new ArrayList<>(ExternalOptimizationContextRepository.getRepositories().size());
        ExternalOptimizationContextRepository.getRepositories().forEach((id, lang) -> {
            TruffleLanguageInformation tli = repository.loadOrCreateByLanguageId(lang.getLanguage());

            // prepare builder
            Language.Builder builder = Language.newBuilder()
                .setId(id)
                .setName(tli.getName())
                .setDescription(tli.getHumanReadableName() != null ? tli.getHumanReadableName() : tli.getName());

            // assign groups ids because we usually don't have them
            Map<String, Integer> groupIdList = new HashMap<>();
            // negative indices to never clash with the real symbols of the db
            final int[] index = {-1};


            // collect symbols and groups
            Map<Class, Pair<List<Symbol.Builder>, List<Class>>> hierarchyMap = new HashMap<>();
            tli.getInstantiableNodes().values().forEach(tlc -> {
                // load symbol
                Class symbolClazz = tlc.getClazz();
                Symbol.Builder symbol = makeSymbol(tli, tlc);
                Class groupClazz = symbolClazz.getName().endsWith("Gen") ? symbolClazz.getSuperclass() : symbolClazz;
                Class group = null;
                // go up
                while (groupClazz != null && groupClazz != Object.class) {
                    groupClazz = groupClazz.getEnclosingClass() != null ? groupClazz.getEnclosingClass() : groupClazz.getSuperclass();
                    // skip api groupings
                    while (ExtendedNodeUtil.isAPINode(groupClazz)) {
                        groupClazz = groupClazz.getEnclosingClass() != null ? groupClazz.getEnclosingClass() : groupClazz.getSuperclass();
                    }
                    if (!hierarchyMap.containsKey(groupClazz)) {
                        // add new group
                        hierarchyMap.put(groupClazz, new Pair<>(new LinkedList<>(), new LinkedList<>()));
                        if (!groupIdList.containsKey(groupClazz.getName())) {
                            groupIdList.put(groupClazz.getName(), index[0]--);
                        }
                        // add symbol to new group (if not already added)
                        if (symbolClazz != null) {
                            symbolClazz = null;
                            hierarchyMap.get(groupClazz).getKey().add(symbol);
                        }
                        // add subgroup if existing
                        if (group != null) {
                            hierarchyMap.get(groupClazz).getValue().add(group);
                        }
                        group = groupClazz;
                    } else {
                        // add symbol to existing group (if not already added)
                        if (symbolClazz != null) {
                            hierarchyMap.get(groupClazz).getKey().add(symbol);
                        }
                        // add subgroup if existing
                        if (group != null) {
                            hierarchyMap.get(groupClazz).getValue().add(group);
                        }
                        // no need to go further up in hierarchy
                        return;
                    }
                }
            });

            // compile groups
            Map<Class, GroupSymbol> groupMap = new HashMap<>();
            while (!hierarchyMap.isEmpty()) {
                new HashMap<>(hierarchyMap).forEach((key, value) -> {
                    // load all groups that already have all subgroups built
                    if (groupMap.keySet().containsAll(value.getValue())) {
                        hierarchyMap.remove(key);
                        List<GroupSymbol> groupSymbols = new LinkedList<>();
                        value.getValue().forEach(x -> groupSymbols.add(groupMap.get(x)));
                        groupMap.put(key, makeGroupSymbol(key, groupIdList.get(key.getName())).addAllSymbols(makeAllowedChildSymbols(tli, value.getKey(), groupIdList)).addAllGroups(groupSymbols).build());
                    }
                });
            }

            // we skip object but add all other root classes
            builder.addAllGroups(groupMap.get(Object.class).getGroupsList());
            builder.addAllSymbols(groupMap.get(Object.class).getSymbolsList());

            // publish language
            languages.add(builder.build());
        });

        // creator options
        ConstrainedOption.Builder creator = ConstrainedOption.newBuilder().setName(ExternalOptimizerConstants.OP_CREATOR)
            .setDescription("Available Creators");
        provider.getAvailableCreators().forEach(x -> {
            OptionGroup.Builder group = OptionGroup.newBuilder().setName(x.getClass().getName());
            publishConfigurable(group, x);
            creator.addValidValues(group);
        });


        // crossover options
        ConstrainedOption.Builder crossover = ConstrainedOption.newBuilder().setName(ExternalOptimizerConstants.OP_CROSSOVER)
            .setDescription("Available Crossovers");
        provider.getAvailableCrossovers().forEach(x -> {
            OptionGroup.Builder group = OptionGroup.newBuilder().setName(x.getClass().getName());
            publishConfigurable(group, x);
            crossover.addValidValues(group);
        });


        // mutator options
        ConstrainedOption.Builder mutator = ConstrainedOption.newBuilder().setName(ExternalOptimizerConstants.OP_MUTATOR)
            .setDescription("Available mutators");
        provider.getAvailableMutators().forEach(x -> {
            OptionGroup.Builder group = OptionGroup.newBuilder().setName(x.getClass().getName());
            publishConfigurable(group, x);
            mutator.addValidValues(group);
        });


        // evaluator options
        MultiOption.Builder fitnessFunctions = MultiOption.newBuilder()
            .setName(ExternalOptimizerConstants.FFN)
            .setDescription("Represents a fitness function.");
        provider.getAvailableCachets().forEach(x -> {
            fitnessFunctions.addItems(OptionGroup.newBuilder().setName(x.getName()));
            if (x.getName().contains("Accuracy")) {
                fitnessFunctions.addDefaults(fitnessFunctions.getItemsCount() - 1);
            }

        });
        ConstrainedOption evaluator = ConstrainedOption.newBuilder().setName(ExternalOptimizerConstants.OP_EVALUATOR)
            .setDescription("Evaluator for quality of the ASTs")
            .addValidValues(OptionGroup.newBuilder()
                .setName("TruffleEvaluatorImpl")
                .setDescription("Default evaluator.")
                .addMultiOptions(fitnessFunctions)
                .addOptions(Option.newBuilder().setName("timeout").setType(OptionType.INT).setDescription("maximum runtime of an AST during evaluation").setDefault("15000"))
            )
            .build();


        // problem options
        OptionGroup.Builder truffleProblemOptions = OptionGroup.newBuilder().setName("TruffleOptimizationProblem");
        publishConfigurable(truffleProblemOptions, new TruffleOptimizationProblem());
        ConstrainedOption problem = ConstrainedOption.newBuilder().setName(ExternalOptimizerConstants.PROBLEM)
            .setDescription("Additional values that can be set for the problem")
            .addValidValues(truffleProblemOptions)
            .build();

        OptionGroup options = OptionGroup.newBuilder()
            .addConstrainedOptions(creator)
            .addConstrainedOptions(crossover)
            .addConstrainedOptions(mutator)
            .addConstrainedOptions(evaluator)
            .addConstrainedOptions(problem)
            .build();

        return ConfigurationResponse.newBuilder().addAllLanguages(languages).setOptionGroup(options).build();
    }


    /**
     * Helper function for turning configurables into published options
     *
     * @param builder      to add to the group
     * @param configurable to be published
     */
    private void publishConfigurable(OptionGroup.Builder builder, Configurable configurable) {
        configurable.getOptions().forEach((key, value) -> {
            if (value.getValue() instanceof Configurable) {
                // find all instances of that configurable
                Field field = Arrays.stream(configurable.getClass().getDeclaredFields()).filter(x -> x.getName().equals(key)).findFirst().orElse(null);
                if (field == null) {
                    field = Arrays.stream(configurable.getClass().getFields()).filter(x -> x.getName().equals(key)).findFirst().orElse(null);
                }
                if (field != null) {
                    List<Object> classesUntyped = provider.findClassesUntyped(field.getType());
                    ConstrainedOption.Builder possibleClasses = ConstrainedOption.newBuilder().setName(key)
                        .setDescription("Available options for " + key);
                    classesUntyped.forEach(x -> {
                        // collect all possible permutations
                        OptionGroup.Builder group = OptionGroup.newBuilder();
                        group.setName(x.getClass().getName());
                        publishConfigurable(group, (Configurable) value.getValue());
                        possibleClasses.addValidValues(group);
                    });
                    builder.addConstrainedOptions(possibleClasses);
                } else {
                    // do a regular descent
                    OptionGroup.Builder group = OptionGroup.newBuilder();
                    group.setName(value.getValue().getClass().getName());
                    group.setDescription(value.getValue().getClass().getName());
                    publishConfigurable(group, (Configurable) value.getValue());
                    builder.addConstrainedOptions(ConstrainedOption.newBuilder().setName(key).addValidValues(group));
                }
            } else {
                builder.addOptions(Option.newBuilder().setName(key).setType(chooseOptionType(value)).setDefault(String.valueOf(value.getValue())));
            }
        });
    }

    private Symbol.Builder makeSymbol(TruffleLanguageInformation tli, TruffleClassInformation information) {

        // create smybol and set most important values
        Symbol.Builder symbol = Symbol.newBuilder();
        symbol.setId(information.getId());
        String clazzName = information.getClazz().getName().endsWith("Gen") ? information.getClazz().getSuperclass().getName() : information.getClazz().getName();
        symbol.setName(information.getShortName() != null ? information.getShortName() : clazzName);
        symbol.setDescription(information.getDescription() != null ? information.getDescription() : clazzName);

        // set other values
        symbol.setInitialFrequency(1.0);
        symbol.setEnabled(true);

        // map arities
        if (tli.getOperators().get(information.getClazz()) == null) {
            // only terminals for this one
            symbol.setMaximumArity(0);
            symbol.setMinimumArity(0);
        } else if (tli.getOperands().get(information.getClazz()) == null) {
            // only non-terminals exist
            symbol.setMinimumArity(tli.getOperators().get(information.getClazz()).stream().mapToInt(x ->
                tli.getNodes().get(x).getInitializers().stream().mapToInt(init -> init.getParameters().length).min().orElse(0)).min().orElse(0));
            symbol.setMaximumArity(-1);
        } else {
            // both exist
            symbol.setMinimumArity(0);
            symbol.setMaximumArity(-1);
        }
        if (symbol.getMaximumArity() == -1) {
            // if we have any array type the arity becomes infinite
            if (tli.getOperators().get(information.getClazz()).stream().anyMatch(x -> tli.getNodes().get(x).getInitializers().stream().anyMatch(
                init -> Arrays.stream(init.getParameters()).anyMatch(TruffleParameterInformation::isArray)))) {
                symbol.setMaximumArity(Integer.MAX_VALUE);
            } else {
                symbol.setMaximumArity(tli.getOperators().get(information.getClazz()).stream().mapToInt(x ->
                    tli.getNodes().get(x).getInitializers().stream().mapToInt(init -> init.getParameters().length).max().orElse(0)).max().orElse(0));
            }
        }
        return symbol;
    }

    private List<Symbol> makeAllowedChildSymbols(TruffleLanguageInformation tli, List<Symbol.Builder> symbols, Map<String, Integer> groupIdList) {
        return symbols.stream().map(x -> makeAllowedChildSymbol(tli, x, groupIdList)).collect(Collectors.toList());
    }

    private Symbol makeAllowedChildSymbol(TruffleLanguageInformation tli, Symbol.Builder symbol, Map<String, Integer> groupIdList) {
        // set the allowed child symbols
        TruffleClassInformation information = tli.getTci(symbol.getId());
        TruffleClassInitializer initializer = information.getInitializersForProxying().stream().findFirst().orElse(null);
        if (initializer != null) {
            // map to add repeated symbols
            Map<Long, ChildSymbolConfiguration.Builder> childSymbolMap = new HashMap<>();

            for (int i = 0; i < initializer.getParameters().length; i++) {
                TruffleParameterInformation parameter = initializer.getParameters()[i];
                TruffleClassInformation paramClass = tli.getTci(parameter.getType());

                if (paramClass != null) {
                    // this is a specific class (rarely happens)
                    if (childSymbolMap.containsKey(paramClass.getId())) {
                        childSymbolMap.get(paramClass.getId()).addArgumentIndices(i);
                    } else {
                        childSymbolMap.put(paramClass.getId(), ChildSymbolConfiguration.newBuilder().addArgumentIndices(i).setChildId(paramClass.getId()));
                    }
                } else if (tli.getOperators().get(parameter.getType()) != null && !Object.class.equals(parameter.getType())) {
                    // this is a group class
                    if (groupIdList.get(parameter.getType().getName()) != null) {
                        long groupId = groupIdList.get(parameter.getType().getName());
                        // add found group
                        if (childSymbolMap.containsKey(groupId)) {
                            childSymbolMap.get(groupId).addArgumentIndices(i);
                        } else {
                            childSymbolMap.put(groupId, ChildSymbolConfiguration.newBuilder().addArgumentIndices(i).setChildId(groupId));
                        }
                    } else {
                        // groupings may miss from nodes that have too few implementations -> Ignore for now and warn users
                        System.out.println("Warning missing group: "+ parameter.getType().getName());
                        Class missingGroup = parameter.getType();
                        while (missingGroup != null && !groupIdList.containsKey(missingGroup.getName())) {
                            missingGroup = missingGroup.getSuperclass();
                        }
                        if (missingGroup != null && !Object.class.equals(parameter.getType()) && groupIdList.containsKey(missingGroup.getName())) {
                            // add higher order group
                            long groupId = groupIdList.get(missingGroup.getName());
                            if (childSymbolMap.containsKey(groupId)) {
                                childSymbolMap.get(groupId).addArgumentIndices(i);
                            } else {
                                childSymbolMap.put(groupId, ChildSymbolConfiguration.newBuilder().addArgumentIndices(i).setChildId(groupId));
                            }
                        } else {
                            System.out.println("Warning missing group - did not find alternative");
                        }
                    }
                }
                // we are not interested in others as they are terminals that we won't represent
            }

            // add the collected symbols
            childSymbolMap.values().forEach(symbol::addAllowedChildSymbols);
        }
        return symbol.build();
    }

    private GroupSymbol.Builder makeGroupSymbol(Class clazz, long id) {
        // create new group
        GroupSymbol.Builder newG = GroupSymbol.newBuilder();
        // set id
        newG.setId(id);
        // find name
        String name = clazz.getName();
        NodeInfo info = (NodeInfo) clazz.getDeclaredAnnotation(NodeInfo.class);
        if (info != null) {
            if (!info.shortName().isEmpty()) {
                name = info.shortName();
            }
        }
        newG.setName(name);
        return newG;
    }

    private OptionType chooseOptionType(Descriptor descriptor) {
        Object value = descriptor.getValue();
        if (value != null) {
            switch (value.getClass().getName()) {
                case "int":
                case "java.lang.Integer":
                    return OptionType.INT;
                case "java.lang.String":
                    return OptionType.STRING;
                case "double":
                case "java.lang.Double":
                    return OptionType.DOUBLE;
                case "boolean":
                case "java.lang.Boolean":
                    return OptionType.BOOL;
                case "java.time.LocalDateTime":
                    return OptionType.DATE_TIME;
                case "java.time.Duration":
                    return OptionType.TIME_SPAN;
                default:
                    return OptionType.STRING;
            }
        }
        return OptionType.STRING;
    }

}
