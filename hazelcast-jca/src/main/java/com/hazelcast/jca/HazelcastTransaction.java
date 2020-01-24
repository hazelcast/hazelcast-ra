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


/**
 * Wrapper interface to bundle {@link javax.resource.cci.LocalTransaction} and
 * {@link javax.resource.spi.LocalTransaction} into one interface
 */
public interface HazelcastTransaction extends javax.resource.cci.LocalTransaction, javax.resource.spi.LocalTransaction {

    /**
     * Resets the transaction so that it can be re-used
     * Useful when a thread finishes without calling commit or rollback
     */
    void reset();
}
