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

import at.fh.hagenberg.aist.gce.optimization.runtime.SystemInformation;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.transaction.TransactionManager;
import science.aist.neo4j.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The SystemInformationRepository exists solely to prevent duplicate creation of SystemInformation.
 * The saves are overriden to save only non-existing profiles, and otherwise return the existing ones
 *
 * @author Oliver Krauss on 13.11.2019
 */
public class SystemInformationRepository extends ReflectiveNeo4JNodeRepositoryImpl<SystemInformation> {

    public SystemInformationRepository(TransactionManager manager) throws NoSuchMethodException, ClassNotFoundException {
        super(manager, SystemInformation.class);
    }

    @Override
    public <T extends SystemInformation> T save(T node) {
        if (node.getId() == null) {
            // try to find the same
            List<Pair<String, Object>> kvset = new ArrayList<>();
            kvset.add(new Pair<>("cpuArchitecture", node.getCpuArchitecture()));
            kvset.add(new Pair<>("cpuCacheL1d", node.getCpuCacheL1d()));
            kvset.add(new Pair<>("cpuCacheL1i", node.getCpuCacheL1i()));
            kvset.add(new Pair<>("cpuCacheL2", node.getCpuCacheL2()));
            kvset.add(new Pair<>("cpuCacheL3", node.getCpuCacheL3()));
            kvset.add(new Pair<>("cpuCores", node.getCpuCores()));
            kvset.add(new Pair<>("cpuName", node.getCpuName()));
            kvset.add(new Pair<>("cpuOperatingFrequency", node.getCpuOperatingFrequency()));
            kvset.add(new Pair<>("cpuPhysicalCores", node.getCpuPhysicalCores()));
            kvset.add(new Pair<>("cpuSockets", node.getCpuSockets()));
            kvset.add(new Pair<>("javaExactVersion", node.getJavaExactVersion()));
            kvset.add(new Pair<>("javaVersion", node.getJavaVersion()));
            kvset.add(new Pair<>("jvmArchitecture", node.getJvmArchitecture()));
            kvset.add(new Pair<>("jvmName", node.getJvmName()));
            kvset.add(new Pair<>("osName", node.getOsName()));
            kvset.add(new Pair<>("ramSize", node.getRamSize()));
            SystemInformation by = this.findBy(kvset);
            if (by != null) {
                node.setId(by.getId());
                return node;
            }
        }

        return super.save(node);
    }

    @Override
    public <T extends SystemInformation> Iterable<T> saveAll(Iterable<T> nodes) {
        List<T> update = StreamSupport
            .stream(nodes.spliterator(), false)
            .filter(x -> getId(x) != null)
            .collect(Collectors.toList());
        super.saveAll(update);

        StreamSupport.stream(nodes.spliterator(), false)
            .filter(x -> getId(x) == null).forEach(
            x -> update.add((T) save(x))
        );

        return update;
    }
}
