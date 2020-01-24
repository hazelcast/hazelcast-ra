/*
* Copyright 2020 Hazelcast Inc.
*
* Licensed under the Hazelcast Community License (the "License"); you may not use
* this file except in compliance with the License. You may obtain a copy of the
* License at
*
* http://hazelcast.com/hazelcast-community-license
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/

package com.hazelcast.jca;

import com.hazelcast.cardinality.CardinalityEstimator;
import com.hazelcast.config.Config;
import com.hazelcast.client.ClientService;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.cluster.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.collection.IList;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.map.IMap;
import com.hazelcast.collection.IQueue;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionService;
import com.hazelcast.topic.ITopic;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.partition.PartitionService;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class HazelcastConnectionImplTest extends HazelcastTestSupport {

    private HazelcastInstance hz;
    private HazelcastConnectionImpl connection;

    @Before
    public void setup() {
        hz = createHazelcastInstance();
        ManagedConnectionImpl managedConnection = mock(ManagedConnectionImpl.class);
        when(managedConnection.getHazelcastInstance()).thenReturn(hz);
        connection = new HazelcastConnectionImpl(managedConnection, null);
    }

    @Test
    public void getRingbuffer() {
        Ringbuffer rb = connection.getRingbuffer("ringbuffer");
        assertSame(hz.getRingbuffer("ringbuffer"), rb);
    }

    @Test
    public void getReliableTopic() {
        ITopic topic = connection.getReliableTopic("reliableTopic");
        assertSame(hz.getReliableTopic("reliableTopic"), topic);
    }

    @Test
    public void getTopic() {
        ITopic topic = connection.getTopic("reliableTopic");
        assertSame(hz.getTopic("reliableTopic"), topic);
    }

    @Test
    public void getMap() {
        IMap map = connection.getMap("map");
        assertSame(hz.getMap("map"), map);
    }

    @Test
    public void getQueue() {
        IQueue queue = connection.getQueue("queue");
        assertSame(hz.getQueue("queue"), queue);
    }

    @Test
    public void getMultiMap() {
        MultiMap multiMap = connection.getMultiMap("multiMap");
        assertSame(hz.getMultiMap("multiMap"), multiMap);
    }

    @Test
    public void getReplicatedMap() {
        ReplicatedMap replicatedMap = connection.getReplicatedMap("replicatedMap");
        assertSame(hz.getReplicatedMap("replicatedMap"), replicatedMap);
    }

    @Test
    public void getSet() {
        Set set = connection.getSet("set");
        assertSame(hz.getSet("set"), set);
    }

    @Test
    public void getList() {
        IList list = connection.getList("list");
        assertSame(hz.getList("list"), list);
    }

    @Test
    public void getExecutorService() {
        ExecutorService ex = connection.getExecutorService("ex");
        assertSame(hz.getExecutorService("ex"), ex);
    }

    @Test
    public void getDistributedObject() {
        DistributedObject obj = connection.getDistributedObject(MapService.SERVICE_NAME, "id");
        assertSame(hz.getDistributedObject(MapService.SERVICE_NAME, "id"), obj);
    }

    @Test
    public void getName() {
        String name = connection.getName();
        assertSame(hz.getName(), name);
    }

    @Test
    public void testGetConfig() {
        Config config = connection.getConfig();
        assertSame(hz.getConfig(), config);

    }

    @Test
    public void getCluster() {
        Cluster cluster = connection.getCluster();
        assertSame(hz.getCluster(), cluster);
    }

    @Test
    public void getSplitBrainProtectionService() {
        SplitBrainProtectionService splitBrainProtectionService = connection.getSplitBrainProtectionService();
        assertSame(splitBrainProtectionService, hz.getSplitBrainProtectionService());
    }

    @Test
    public void getClientService() {
        ClientService clientService = connection.getClientService();
        assertNotSame(hz.getClientService(), clientService);
    }

    @Test
    public void getLoggingService() {
        LoggingService loggingService = connection.getLoggingService();
        assertSame(hz.getLoggingService(), loggingService);
    }

    @Test
    public void getUserContext() {
        Map userContext = connection.getUserContext();
        assertSame(hz.getUserContext(), userContext);
    }

    @Test
    public void getPartitionService() {
        PartitionService partitionService = connection.getPartitionService();
        assertSame(hz.getPartitionService(), partitionService);
    }

    @Test
    public void getLocalEndpoint() {
        Endpoint endpoint = connection.getLocalEndpoint();
        assertSame(hz.getLocalEndpoint(), endpoint);
    }

    @Test
    public void getXAResource() {
        XAResource resource = connection.getXAResource();
        assertNull(resource);
    }

    @Test
    public void getFlakeIdGenerator() {
        String flakeIdGeneratorName = "flakeIdGenerator";
        FlakeIdGenerator flakeIdGenerator = connection.getFlakeIdGenerator(flakeIdGeneratorName);
        assertSame(hz.getFlakeIdGenerator(flakeIdGeneratorName), flakeIdGenerator);
    }

    @Test
    public void getPNCounter() {
        String pnCounterName = "pnCounter";
        PNCounter pnCounter = connection.getPNCounter(pnCounterName);
        assertSame(hz.getPNCounter(pnCounterName), pnCounter);
    }

    @Test
    public void getCPSubsystem() {
        CPSubsystem cpSubsystem = connection.getCPSubsystem();
        assertSame(hz.getCPSubsystem(), cpSubsystem);
    }

    @Test
    public void getDurableExecutorService() {
        String durableExecutorServiceName = "durableExecutorService";
        DurableExecutorService durableExecutorService = connection.getDurableExecutorService(durableExecutorServiceName);
        assertSame(hz.getDurableExecutorService(durableExecutorServiceName), durableExecutorService);
    }

    @Test
    public void getScheduledExecutorService() {
        String scheduledExecutorServiceName = "scheduledExecutorService";
        IScheduledExecutorService scheduledExecutorService = connection.getScheduledExecutorService(scheduledExecutorServiceName);
        assertSame(hz.getScheduledExecutorService(scheduledExecutorServiceName), scheduledExecutorService);
    }

    @Test
    public void getCardinalityEstimator() {
        String cardinalityEstimatorName = "cardinalityEstimator";
        CardinalityEstimator cardinalityEstimator = connection.getCardinalityEstimator(cardinalityEstimatorName);
        assertSame(hz.getCardinalityEstimator(cardinalityEstimatorName), cardinalityEstimator);
    }

    @Test
    public void getCacheManager() {
        ICacheManager cacheManager = connection.getCacheManager();
        assertSame(hz.getCacheManager(), cacheManager);
    }

}
