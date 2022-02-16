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

import at.fh.hagenberg.aist.gce.optimization.util.TruffleClassInformation;
import at.fh.hagenberg.aist.gce.optimization.util.TruffleLanguageInformation;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.repository.AbstractNeo4JRepository;
import science.aist.neo4j.transaction.TransactionManager;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Values;

/**
 * @author Oliver Krauss on 13.11.2019
 */

public class TruffleLanguageInformationRepository extends ReflectiveNeo4JNodeRepositoryImpl<TruffleLanguageInformation> {

    public TruffleLanguageInformationRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, TruffleLanguageInformation.class);
    }

    @Override
    public <T extends TruffleLanguageInformation> T save(T node) {
        T save = super.save(node);
        node.getInstantiableNodes().values().forEach(x -> {
            AbstractNeo4JRepository.getProvidedRepository(this.getTransactionManager(), TruffleClassInformation.class.getName()).save(x);
        });
        return save;
    }

    /**
     * Loads a truffle language from the database if it exists.
     * @param languageId to be loaded
     * @return truffle language
     */
    public TruffleLanguageInformation loadByLanguageId(String languageId) {
        return execute("MATCH (n:TruffleLanguageInformation) WHERE n.name = $name OPTIONAL MATCH (n)-[r*..]->(c) UNWIND r as row RETURN {root: n, relationships: collect(distinct row), nodes: collect(distinct c)}", Values.parameters(new Object[]{"name", languageId}), AccessMode.READ);
    }

    /**
     * Loads a truffle language from the database, or alternatively creates and stores it in the db, if it didn't exist already.
     * @param languageId to be loaded
     * @return truffle language with DB ids set
     */
    public TruffleLanguageInformation loadOrCreateByLanguageId(String languageId) {
        TruffleLanguageInformation information = this.loadByLanguageId(languageId);
        if (information == null) {
            information = this.save(TruffleLanguageInformation.getLanguageInformation(languageId));
        } else {
            information.setSelf();
        }
        return information;
    }
}
