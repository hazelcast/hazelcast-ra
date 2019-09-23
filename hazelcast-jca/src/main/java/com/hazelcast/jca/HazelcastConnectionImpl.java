/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jca;

import com.hazelcast.cardinality.CardinalityEstimator;
import com.hazelcast.config.Config;
import com.hazelcast.client.ClientService;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.cluster.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.IAtomicReference;
import com.hazelcast.core.ICacheManager;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.collection.IList;
import com.hazelcast.cp.lock.ILock;
import com.hazelcast.map.IMap;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.splitbrainprotection.SplitBrainProtectionService;
import com.hazelcast.topic.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.partition.PartitionService;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.transaction.TransactionalList;
import com.hazelcast.transaction.TransactionalMap;
import com.hazelcast.transaction.TransactionalMultiMap;
import com.hazelcast.transaction.TransactionalQueue;
import com.hazelcast.transaction.TransactionalSet;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.crdt.pncounter.PNCounter;
import com.hazelcast.durableexecutor.DurableExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionException;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalTask;
import com.hazelcast.internal.util.ExceptionUtil;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;
import javax.security.auth.Subject;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Implementation class of {@link com.hazelcast.jca.HazelcastConnectionImpl}
 */
@SuppressWarnings({"checkstyle:methodcount", "checkstyle:classfanoutcomplexity"})
public class HazelcastConnectionImpl implements HazelcastConnection {

    /**
     * Identity generator
     */
    private static AtomicInteger idGen = new AtomicInteger();

    /**
     * Reference to this creator and access to container infrastructure
     */
    final ManagedConnectionImpl managedConnection;

    /**
     * this identity
     */
    private final int id;

    public HazelcastConnectionImpl(ManagedConnectionImpl managedConnectionImpl, Subject subject) {
        this.managedConnection = managedConnectionImpl;
        id = idGen.incrementAndGet();
    }

    @Override
    public void close() throws ResourceException {
        managedConnection.log(Level.FINEST, "close");
        // important: inform the container!
        managedConnection.fireConnectionEvent(ConnectionEvent.CONNECTION_CLOSED, this);
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        // TODO
        return null;
    }

    /**
     * @throws NotSupportedException as this is not supported by this resource adapter
     */
    @Override
    public ResultSetInfo getResultSetInfo() throws NotSupportedException {
        throw new NotSupportedException("getResultSetInfo() is not supported by this resource adapter as per spec 15.11.3");
    }

    @Override
    public HazelcastTransaction getLocalTransaction() throws ResourceException {
        managedConnection.log(Level.FINEST, "getLocalTransaction");
        return managedConnection.getLocalTransaction();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return managedConnection.getMetaData();
    }

    @Override
    public FlakeIdGenerator getFlakeIdGenerator(String name) {
        return getHazelcastInstance().getFlakeIdGenerator(name);
    }

    @Override
    public PNCounter getPNCounter(String name) {
        return getHazelcastInstance().getPNCounter(name);
    }

    @Override
    public CPSubsystem getCPSubsystem() {
        return getHazelcastInstance().getCPSubsystem();
    }

    @Override
    public String toString() {
        return "hazelcast.ConnectionImpl [" + id + "]";
    }

    /**
     * Method is not exposed to force all clients to go through this connection object and its
     * methods from {@link HazelcastConnection}
     *
     * @return the local hazelcast instance
     */
    private HazelcastInstance getHazelcastInstance() {
        return managedConnection.getHazelcastInstance();
    }

    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        return getHazelcastInstance().getMap(name);
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        return getHazelcastInstance().getQueue(name);
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        return getHazelcastInstance().getTopic(name);
    }

    @Override
    public <E> ITopic<E> getReliableTopic(String name) {
        return getHazelcastInstance().getReliableTopic(name);
    }

    @Override
    public <E> ISet<E> getSet(String name) {
        return getHazelcastInstance().getSet(name);
    }

    @Override
    public <E> IList<E> getList(String name) {
        return getHazelcastInstance().getList(name);
    }

    @Override
    public <K, V> MultiMap<K, V> getMultiMap(String name) {
        return getHazelcastInstance().getMultiMap(name);
    }

    @Override
    public IExecutorService getExecutorService(String name) {
        return getHazelcastInstance().getExecutorService(name);
    }

    @Override
    public IAtomicLong getAtomicLong(String name) {
        return getHazelcastInstance().getAtomicLong(name);
    }

    @Override
    public Collection<DistributedObject> getDistributedObjects() {
        return getHazelcastInstance().getDistributedObjects();
    }

    @Override
    public String addDistributedObjectListener(DistributedObjectListener distributedObjectListener) {
        return getHazelcastInstance().addDistributedObjectListener(distributedObjectListener);
    }

    @Override
    public boolean removeDistributedObjectListener(String registrationId) {
        return getHazelcastInstance().removeDistributedObjectListener(registrationId);
    }

    @Override
    public Config getConfig() {
        return getHazelcastInstance().getConfig();
    }

    @Override
    public PartitionService getPartitionService() {
        return getHazelcastInstance().getPartitionService();
    }

    @Override
    public SplitBrainProtectionService getSplitBrainProtectionService() {
        return getHazelcastInstance().getSplitBrainProtectionService();
    }

    @Override
    public ClientService getClientService() {
        return getHazelcastInstance().getClientService();
    }

    @Override
    public LoggingService getLoggingService() {
        return getHazelcastInstance().getLoggingService();
    }

    @Override
    public <T extends DistributedObject> T getDistributedObject(String serviceName, String name) {
        return getHazelcastInstance().getDistributedObject(serviceName, name);
    }

    @Override
    public ConcurrentMap<String, Object> getUserContext() {
        return getHazelcastInstance().getUserContext();
    }

    @Override
    public <K, V> TransactionalMap<K, V> getTransactionalMap(String name) {
        TransactionContext txContext = getTransactionContext();
        return txContext.getMap(name);
    }

    @Override
    public <E> TransactionalQueue<E> getTransactionalQueue(String name) {
        TransactionContext txContext = getTransactionContext();
        return txContext.getQueue(name);
    }

    @Override
    public <K, V> TransactionalMultiMap<K, V> getTransactionalMultiMap(String name) {
        TransactionContext txContext = getTransactionContext();
        return txContext.getMultiMap(name);
    }

    @Override
    public <E> TransactionalList<E> getTransactionalList(String name) {
        TransactionContext txContext = getTransactionContext();
        return txContext.getList(name);
    }

    @Override
    public <E> TransactionalSet<E> getTransactionalSet(String name) {
        TransactionContext txContext = getTransactionContext();
        return txContext.getSet(name);
    }

    private TransactionContext getTransactionContext() {
        TransactionContext transactionContext = managedConnection.getTransactionContext();
        if (transactionContext != null) {
            return transactionContext;
        }
        HazelcastXAResource xaResource = getXAResource();
        return xaResource.getTransactionContext();
    }

    @Override
    public IdGenerator getIdGenerator(String name) {
        return getHazelcastInstance().getIdGenerator(name);
    }

    @Override
    public <E> IAtomicReference<E> getAtomicReference(String name) {
        return getHazelcastInstance().getAtomicReference(name);
    }

    @Override
    public <K, V> ReplicatedMap<K, V> getReplicatedMap(String name) {
        return getHazelcastInstance().getReplicatedMap(name);
    }

    @Override
    public <E> Ringbuffer<E> getRingbuffer(String name) {
        return getHazelcastInstance().getRingbuffer(name);
    }

    @Override
    public String getName() {
        return getHazelcastInstance().getName();
    }


    @Override
    public ILock getLock(String key) {
        return getHazelcastInstance().getLock(key);
    }

    @Override
    public Cluster getCluster() {
        return getHazelcastInstance().getCluster();
    }

    @Override
    public Endpoint getLocalEndpoint() {
        return getHazelcastInstance().getLocalEndpoint();
    }

    @Override
    public HazelcastXAResource getXAResource() {
        try {
            return (HazelcastXAResource) managedConnection.getXAResource();
        } catch (ResourceException e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    @Override
    public DurableExecutorService getDurableExecutorService(final String s) {
        return getHazelcastInstance().getDurableExecutorService(s);
    }

    @Override
    public ICacheManager getCacheManager() {
        return getHazelcastInstance().getCacheManager();
    }

    @Override
    public CardinalityEstimator getCardinalityEstimator(final String s) {
        return getHazelcastInstance().getCardinalityEstimator(s);
    }

    @Override
    public IScheduledExecutorService getScheduledExecutorService(final String s) {
        return getHazelcastInstance().getScheduledExecutorService(s);
    }

    // unsupported operations

    @Override
    public LifecycleService getLifecycleService() {
        throw new UnsupportedOperationException("Hazelcast Lifecycle is only managed by JCA Container");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Hazelcast Lifecycle is only managed by JCA Container");
    }

    @Override
    public <T> T executeTransaction(TransactionalTask<T> task) throws TransactionException {
        throw new UnsupportedOperationException("getTransactional*() methods are"
                + " only methods allowed for transactional operations!");
    }

    @Override
    public <T> T executeTransaction(TransactionOptions options, TransactionalTask<T> task) throws TransactionException {
        throw new UnsupportedOperationException("getTransactional*() methods are"
                + " only methods allowed for transactional operations!");
    }

    @Override
    public TransactionContext newTransactionContext() {
        throw new UnsupportedOperationException("getTransactional*() methods are"
                + " only methods allowed for transactional operations!");
    }

    @Override
    public TransactionContext newTransactionContext(TransactionOptions options) {
        throw new UnsupportedOperationException("getTransactional*() methods are"
                + " only methods allowed for transactional operations!");
    }
}
