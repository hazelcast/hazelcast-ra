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
import com.hazelcast.transaction.TransactionalList;
import com.hazelcast.transaction.TransactionalMap;
import com.hazelcast.transaction.TransactionalMultiMap;
import com.hazelcast.transaction.TransactionalQueue;
import com.hazelcast.transaction.TransactionalSet;

import javax.resource.cci.Connection;

/**
 * Hazelcast specific connection which allows
 * access to this hazelcast instance to acquire
 * the maps, lists etc.
 */
public interface HazelcastConnection extends Connection, HazelcastInstance {

    //Transactionals

    /**
     * @see com.hazelcast.transaction.TransactionalTaskContext#getMap(String)
     */
    <K, V> TransactionalMap<K, V> getTransactionalMap(String name);

    /**
     * @see com.hazelcast.transaction.TransactionalTaskContext#getQueue(String)
     */
    <E> TransactionalQueue<E> getTransactionalQueue(String name);

    /**
     * @see com.hazelcast.transaction.TransactionalTaskContext#getMultiMap(String)
     */
    <K, V> TransactionalMultiMap<K, V> getTransactionalMultiMap(String name);

    /**
     * @see com.hazelcast.transaction.TransactionalTaskContext#getList(String)
     */
    <E> TransactionalList<E> getTransactionalList(String name);

    /**
     * @see com.hazelcast.transaction.TransactionalTaskContext#getSet(String)
     */
    <E> TransactionalSet<E> getTransactionalSet(String name);
}
