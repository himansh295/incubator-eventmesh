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

package org.apache.eventmesh.http.demo.sub.service;

import static org.apache.eventmesh.common.ExampleConstants.ENV;
import static org.apache.eventmesh.common.ExampleConstants.IDC;
import static org.apache.eventmesh.common.ExampleConstants.SERVER_PORT;
import static org.apache.eventmesh.common.ExampleConstants.SUB_SYS;
import static org.apache.eventmesh.util.Utils.getURL;

import org.apache.eventmesh.client.http.conf.EventMeshHttpClientConfig;
import org.apache.eventmesh.client.http.consumer.EventMeshHttpConsumer;
import org.apache.eventmesh.common.ExampleConstants;
import org.apache.eventmesh.common.protocol.SubscriptionItem;
import org.apache.eventmesh.common.protocol.SubscriptionMode;
import org.apache.eventmesh.common.protocol.SubscriptionType;
import org.apache.eventmesh.common.utils.IPUtils;
import org.apache.eventmesh.common.utils.ThreadUtils;
import org.apache.eventmesh.http.demo.pub.eventmeshmessage.AsyncPublishInstance;
import org.apache.eventmesh.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SubService implements InitializingBean {

    private transient EventMeshHttpConsumer eventMeshHttpConsumer;

    private transient Properties properties;

    private final transient List<SubscriptionItem> topicList = Lists.newArrayList(
        new SubscriptionItem(ExampleConstants.EVENTMESH_HTTP_ASYNC_TEST_TOPIC, SubscriptionMode.CLUSTERING, SubscriptionType.ASYNC)
    );
    private transient String testURL;

    // CountDownLatch size is the same as messageSize in AsyncPublishInstance.java (Publisher)
    private transient CountDownLatch countDownLatch = new CountDownLatch(AsyncPublishInstance.MESSAGE_SIZE);

    @Override
    public void afterPropertiesSet() throws Exception {
        properties = Utils.readPropertiesFile(ExampleConstants.CONFIG_FILE_NAME);
        final String localPort = properties.getProperty(SERVER_PORT);
        final String eventmeshIP = properties.getProperty(ExampleConstants.EVENTMESH_IP);
        final String eventmeshHttpPort = properties.getProperty(ExampleConstants.EVENTMESH_HTTP_PORT);
        final String testURL = getURL(localPort, "/sub/test");

        final String eventMeshIPPort = eventmeshIP + ":" + eventmeshHttpPort;
        final EventMeshHttpClientConfig eventMeshClientConfig = EventMeshHttpClientConfig.builder()
            .liteEventMeshAddr(eventMeshIPPort)
            .consumerGroup(ExampleConstants.DEFAULT_EVENTMESH_TEST_CONSUMER_GROUP)
            .env(ENV)
            .idc(IDC)
            .ip(IPUtils.getLocalAddress())
            .sys(SUB_SYS)
            .pid(String.valueOf(ThreadUtils.getPID())).build();

        eventMeshHttpConsumer = new EventMeshHttpConsumer(eventMeshClientConfig);
        eventMeshHttpConsumer.heartBeat(topicList, testURL);
        eventMeshHttpConsumer.subscribe(topicList, testURL);

        // Wait for all messaged to be consumed
        final Thread stopThread = new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                log.error("interrupted exception", e);
            }
            if (log.isInfoEnabled()) {
                log.info("stopThread start....");
            }
        });
        stopThread.start();
    }

    @PreDestroy
    public void cleanup() {
        if (log.isInfoEnabled()) {
            log.info("start destroy....");
        }

        try {
            final List<String> unSubList = new ArrayList<>();
            for (final SubscriptionItem item : topicList) {
                unSubList.add(item.getTopic());
            }
            eventMeshHttpConsumer.unsubscribe(unSubList, testURL);
        } catch (Exception e) {
            log.error("unsubscribe exception", e);
        }

        if (eventMeshHttpConsumer != null) {
            eventMeshHttpConsumer.close();
        }

        if (log.isInfoEnabled()) {
            log.info("end destroy....");
        }
    }

    /**
     * Count the message already consumed
     */
    public void consumeMessage(final String msg) {
        if (log.isInfoEnabled()) {
            log.info("consume message: {}", msg);
        }
        countDownLatch.countDown();
        if (log.isInfoEnabled()) {
            log.info("remaining number: {} of messages to be consumed", countDownLatch.getCount());
        }
    }
}
