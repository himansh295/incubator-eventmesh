/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.eventmesh.storage.rocketmq.cloudevent;

import org.apache.eventmesh.storage.rocketmq.cloudevent.impl.RocketMQHeaders;

import org.apache.rocketmq.common.message.Message;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.cloudevents.SpecVersion;

public class RocketMQMessageFactoryTest {

    private Message message;

    @Before
    public void setUp() {
        message = new Message();
        message.putUserProperty(RocketMQHeaders.SPEC_VERSION, SpecVersion.V03.toString());
        message.setBody("BODY".getBytes());
    }

    @Test
    public void testCreateReader() {
        Assert.assertNotNull(RocketMQMessageFactory.createReader(message));
    }
}
