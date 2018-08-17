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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.TransactionContext;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import java.util.logging.Level;

/**
 * Implementation class of {@link com.hazelcast.jca.HazelcastTransaction}
 */
public class HazelcastTransactionImpl extends JcaBase implements HazelcastTransaction {

    /**
     * access to the creator of this {@link #connection}
     */
    private final ManagedConnectionFactoryImpl factory;

    /**
     * access to the creator of this transaction
     */
    private final ManagedConnectionImpl connection;

    /**
     * The hazelcast transaction context itself
     */
    private volatile TransactionContext txContext;

    HazelcastTransactionImpl(ManagedConnectionFactoryImpl factory, ManagedConnectionImpl connection) {
        this.factory = factory;
        this.connection = connection;

        setLogWriter(factory.getLogWriter());
    }

    /**
     * Delegates the hazelcast instance access to the @{link #connection}
     *
     * @see ManagedConnectionImpl#getHazelcastInstance()
     */
    private HazelcastInstance getHazelcastInstance() {
        return connection.getHazelcastInstance();
    }

    /**
     * Delegates the connection event propagation to the @{link #connection}
     *
     * @see ManagedConnectionImpl#fireConnectionEvent(int)
     */
    private void fireConnectionEvent(int event) {
        connection.fireConnectionEvent(event);
    }

    /* (non-Javadoc)
     * @see javax.resource.cci.LocalTransaction#begin()
     */
    public void begin() throws ResourceException {
        if (null == txContext) {
            factory.logHzConnectionEvent(this, HzConnectionEvent.TX_START);

            txContext = getHazelcastInstance().newTransactionContext();

            log(Level.FINEST, "begin");
            txContext.beginTransaction();

            fireConnectionEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED);
        } else {
            log(Level.INFO, "Ignoring duplicate TX begin event");
        }
    }

    /* (non-Javadoc)
     * @see javax.resource.cci.LocalTransaction#commit()
     */
    public void commit() throws ResourceException {
        factory.logHzConnectionEvent(this, HzConnectionEvent.TX_COMPLETE);

        log(Level.FINEST, "commit");
        if (txContext == null) {
            throw new ResourceException("Invalid transaction context; "
                    + "commit operation invoked without an active transaction context");
        }
        txContext.commitTransaction();
        fireConnectionEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        txContext = null;
    }

    /* (non-Javadoc)
     * @see javax.resource.cci.LocalTransaction#rollback()
     */
    public void rollback() throws ResourceException {
        factory.logHzConnectionEvent(this, HzConnectionEvent.TX_COMPLETE);

        log(Level.FINEST, "rollback");
        if (txContext == null) {
            throw new ResourceException("Invalid transaction context;"
                    + " rollback operation invoked without an active transaction context");
        }
        txContext.rollbackTransaction();
        fireConnectionEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        txContext = null;
    }

    public void reset() {
        log(Level.WARNING, "reset");
        txContext = null;

    }

    public TransactionContext getTxContext() {
        return txContext;
    }
}
