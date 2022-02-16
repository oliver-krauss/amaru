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

import at.fh.hagenberg.aist.gce.minic.language.MinicLanguage;
import at.fh.hagenberg.aist.gce.optimization.TruffleOptimizationProblem;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageSearchSpace;
import at.fh.hagenberg.aist.hlc.core.messages.ConfigurationResponse;
import at.fh.hagenberg.aist.hlc.core.messages.GroupSymbol;
import at.fh.hagenberg.aist.hlc.core.messages.OptionType;
import at.fh.hagenberg.aist.hlc.core.messages.Symbol;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Oliver Krauss on 27.10.2019
 */
public class GceExternalConfigurationManagerTest {

    private GceExternalConfigurationManager manager = new GceExternalConfigurationManager();

    @BeforeClass
    public void setUp() {
        ExternalOptimizationContextRepository.registerRepository(0L, new ExternalOptimizationContextRepository() {

            @Override
            public TruffleOptimizationProblem getProblem(TruffleLanguageSearchSpace space, String file, String function, String input, String output, String evaluationIdentity) {
                return null;
            }

            @Override
            public String getLanguage() {
                return MinicLanguage.ID;
            }
        });
    }

    @Test
    public void testGceExternalConfigurationManager() {
        // given
        // everything is in the ExternalOptimizationContextRepository

        // when
        ConfigurationResponse configuration = manager.getConfiguration(null);

        // then
        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getLanguagesCount(), 1);
        Assert.assertEquals(configuration.getLanguages(0).getId(), 0);
        Assert.assertEquals(configuration.getLanguages(0).getName(), MinicLanguage.ID);
        Assert.assertEquals(configuration.getLanguages(0).getDescription(), "Mini ANSI C11");
        Assert.assertEquals(configuration.getLanguages(0).getSymbolsCount(), 3);
        Assert.assertNotEquals(configuration.getLanguages(0).getSymbols(0).getId(), 0);
        Symbol symbol = configuration.getLanguages(0).getSymbolsList().stream().filter(x -> x.getName().equals("dispatch")).findFirst().get();
        Assert.assertEquals(symbol.getMinimumArity(), 0);
        Assert.assertEquals(symbol.getMinimumArity(), 0);
        Assert.assertNotNull(symbol.getName());
        Assert.assertEquals(symbol.getInitialFrequency(), 1.0);
        Assert.assertEquals(symbol.getEnabled(), true);
        Assert.assertEquals(symbol.getDescription(), "Dispatches a function call from an invoke to the appropriate function");
        Assert.assertEquals(configuration.getLanguages(0).getGroupsCount(), 4);
        GroupSymbol groupSymbol = configuration.getLanguages(0).getGroupsList().stream().filter(x -> x.getName().equals("C")).findFirst().get();
        Assert.assertEquals(groupSymbol.getGroupsCount(), 8);
        Assert.assertEquals(groupSymbol.getSymbolsCount(), 6);
        Assert.assertTrue(groupSymbol.getSymbolsList().stream().anyMatch(x -> x.getAllowedChildSymbolsList() != null && x.getAllowedChildSymbolsList().size() > 0));

        Assert.assertNotNull(configuration.getOptionGroup());
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptionsCount(), 5);

        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(0).getName(), "SolutionCreator");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(0).getValidValuesCount(), 2);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(0).getValidValues(0).getName(), "at.fh.hagenberg.aist.gce.optimization.operators.MutatingTruffleTreeCreator");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(0).getValidValues(0).getConstrainedOptionsCount(), 0);

        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(1).getName(), "Crossover");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(1).getValidValuesCount(), 2);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(1).getValidValues(0).getName(), "at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeCrossover");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(1).getValidValues(0).getConstrainedOptionsCount(), 1);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(1).getValidValues(0).getConstrainedOptions(0).getValidValuesCount(), 5);

        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(2).getName(), "Mutator");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(2).getValidValuesCount(), 2);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(2).getValidValues(0).getName(), "at.fh.hagenberg.aist.gce.optimization.operators.TruffleTreeMutator");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(2).getValidValues(0).getConstrainedOptionsCount(), 2);

        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(3).getName(), "Evaluator");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(3).getValidValues(0).getName(), "TruffleEvaluatorImpl");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(3).getValidValues(0).getMultiOptionsCount(), 1);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(3).getValidValues(0).getMultiOptions(0).getItemsCount(), 6);

        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getName(), "Problem");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValuesCount(), 1);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getName(), "TruffleOptimizationProblem");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getOptionsCount(), 2);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getOptions(0).getName(), "repeats");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getOptions(0).getType(), OptionType.INT);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getOptions(0).getDefault(), "1");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getConstrainedOptionsCount(), 1);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getConstrainedOptions(0).getName(), "configuration");
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getConstrainedOptions(0).getValidValuesCount(), 1);
        Assert.assertEquals(configuration.getOptionGroup().getConstrainedOptions(4).getValidValues(0).getConstrainedOptions(0).getValidValues(0).getOptionsCount(), 3);
    }
}
