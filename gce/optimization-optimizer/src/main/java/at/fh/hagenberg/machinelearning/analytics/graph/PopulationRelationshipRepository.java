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


import science.aist.neo4j.reflective.ReflectiveNeo4JRelationshipRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import at.fh.hagenberg.machinelearning.analytics.graph.nodes.PopulationRelationship;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Values;

public class PopulationRelationshipRepository extends ReflectiveNeo4JRelationshipRepositoryImpl<PopulationRelationship> {

    public PopulationRelationshipRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, PopulationRelationship.class);
    }

    public PopulationRelationship findByStepAndSolution(Long stepId, Long solutionId) {
        return this.execute("MATCH (s)-[r]->(t) WHERE id(s) = $stepId and id(t) = $solutionId RETURN r, s, t",
            Values.parameters("stepId", stepId, "solutionId", solutionId), AccessMode.READ);
    }

}
