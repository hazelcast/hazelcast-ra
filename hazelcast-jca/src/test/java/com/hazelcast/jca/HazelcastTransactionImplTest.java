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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import com.hazelcast.internal.util.ExceptionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ManagedConnection;

import java.util.concurrent.Future;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class HazelcastTransactionImplTest extends HazelcastTestSupport {

    private HazelcastTransaction transaction;
    private ManagedConnection managedConnection;
    private ConnectionEventListener mockEventListener;

    @Before
    public void setup() throws ResourceException {
        HazelcastInstance instance = createHazelcastInstance();
        ResourceAdapterImpl resourceAdapter = new ResourceAdapterImpl();
        resourceAdapter.setHazelcastInstance(instance);
        ManagedConnectionFactoryImpl managedConnectionFactory = new ManagedConnectionFactoryImpl();
        managedConnectionFactory.setResourceAdapter(resourceAdapter);
        managedConnectionFactory.setConnectionTracingEvents(null);
        mockEventListener = mock(ConnectionEventListener.class);
        managedConnection = managedConnectionFactory.createManagedConnection(null, null);
        managedConnection.addConnectionEventListener(mockEventListener);
        transaction = new HazelcastTransactionImpl(managedConnectionFactory, (ManagedConnectionImpl) managedConnection);
    }

    @Test
    @Ignore
    public void testTransactionBeginShouldFireEventHandler() throws ResourceException {
        transaction.begin();
        verify(mockEventListener).localTransactionStarted(any(ConnectionEvent.class));
    }

    @Test
    public void testResetFromAnotherThread() throws Exception {
        transaction.begin();
        Future future = spawn(new Runnable() {
            @Override public void run() {
                try {
                    transaction.rollback();
                    fail("Rollback should throw exception");
                } catch (Exception ignored) {
                }
                transaction.reset();
                try {
                    transaction.begin();
                    transaction.commit();
                } catch (ResourceException e) {
                    throw ExceptionUtil.rethrow(e);
                }
            }
        });

        future.get();
    }

    @Test
    public void testTransactionCommitShouldFireEventHandler() throws ResourceException {
        transaction.begin();
        transaction.commit();
        verify(mockEventListener).localTransactionCommitted(any(ConnectionEvent.class));
    }

    @Test
    public void testTransactionRollbackShouldFireEventHandler() throws ResourceException {
        transaction.begin();
        transaction.rollback();
        verify(mockEventListener).localTransactionRolledback(any(ConnectionEvent.class));
        verifyZeroInteractions(mockEventListener);
    }

    @Test
    public void testTransactionCommitShouldNotFireRemovedEventListener() throws ResourceException {
        managedConnection.removeConnectionEventListener(mockEventListener);
        transaction.begin();
        transaction.commit();
        verifyZeroInteractions(mockEventListener);
    }
}
