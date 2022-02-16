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

import at.fh.hagenberg.aist.hlc.core.ExternalOptimizationWorker;
import at.fh.hagenberg.aist.hlc.core.messages.*;
import com.google.protobuf.Message;

import java.io.*;

/**
 * @author Oliver Krauss on 28.11.2019
 */

public class MessageCollectingWorker implements ExternalOptimizationWorker {

    @Override
    public StartAlgorithmResponse configure(String aLong, StartAlgorithmRequest startAlgorithmRequest) {
        try {
            FileOutputStream f = new FileOutputStream(new File("./src/test/resources/sampleStartAlgorithmRequest.msg"));
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(startAlgorithmRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Message operate(String aLong, Message message) {
        return null;
    }

    @Override
    public void shutdown(String aLong, StopAlgorithmRequest stopAlgorithmRequest) {
    }
}
