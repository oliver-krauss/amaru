/*
 * Copyright (c) 2022 the original author or authors.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package at.fh.hagenberg.aist.gce.optimization.runtime;

import at.fh.hagenberg.aist.gce.context.ApplicationContextProvider;
import science.aist.neo4j.reflective.ReflectiveNeo4JNodeRepositoryImpl;
import science.aist.neo4j.repository.AbstractNeo4JRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Oliver Krauss on 29.12.2019
 */

public class SystemInformationTest {

    @Test
    public void getSystemInfo() {
        // given

        // when
        SystemInformation systemInformation = new SystemInformation();

        // then
        Assert.assertNotNull(systemInformation);
        System.out.println(systemInformation.toString());
        // we can't test anymore because it's different on every pc we test on
    }

    @Test
    public void persistAndLoadSystemInfo() {
        // given
        ReflectiveNeo4JNodeRepositoryImpl<SystemInformation> runtimeProfileRepository = (ReflectiveNeo4JNodeRepositoryImpl<SystemInformation>) ApplicationContextProvider.getCtx().getBean("systemInformationRepository");

        // when
        runtimeProfileRepository.save(SystemInformation.getCurrentSystem());

        // then
        Assert.assertNotNull(SystemInformation.getCurrentSystem());
        Assert.assertNotNull(SystemInformation.getCurrentSystem().getId());
    }

}
