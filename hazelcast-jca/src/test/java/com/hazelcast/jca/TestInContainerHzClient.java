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

import com.hazelcast.core.DistributedObject;
import com.hazelcast.test.annotation.SlowTest;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.resource.ResourceException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category(SlowTest.class)
@RunWith(Arquillian.class)
public class TestInContainerHzClient extends AbstractDeploymentHzClientTest {

    @Inject
    protected ITestBean testBean;

    @Test
    @OperateOnDeployment("war-deployment")
    public void testTransactionalMap() throws ResourceException {
        String mapname = "txmap";
        String key = "key";
        String value = "value";

        testBean.insertToMap(mapname, key, value);
        assertEquals(testBean.getFromMap(mapname, key), value);
    }

    @Test
    @OperateOnDeployment("war-deployment")
    public void testTransactionalQueue() throws ResourceException {
        String mapname = "txqueue";
        String key = "key";

        testBean.offerToQueue(mapname, key);
        assertEquals(testBean.pollFromQueue(mapname), key);
    }

    @Test
    @OperateOnDeployment("war-deployment")
    public void testTransactionalSet() throws ResourceException {
        String setname = "txset";
        String key = "key";

        testBean.insertToSet(setname, key);
        assertEquals(1, testBean.getSetSize(setname));
        assertTrue(testBean.removeFromSet(setname, key));
        assertEquals(0, testBean.getSetSize(setname));
    }

    @Test
    @OperateOnDeployment("war-deployment")
    public void testTransactionalList() throws ResourceException {
        String listname = "txlist";
        String key = "key";

        testBean.addToList(listname, key);
        assertEquals(1, testBean.getListSize(listname));
        assertTrue(testBean.removeFromList(listname, key));
        assertEquals(0,testBean.getListSize(listname));
    }

    @Test
    @OperateOnDeployment("war-deployment")
    public void testLocalTransactionContext() throws ResourceException {
        String mapname = "txmap";
        String key = "key";
        String value = "value";

        testBean.insertToMap(mapname, key, value);
        testBean.insertToMap("fsdfd", key, value);

        assertNotNull(testBean.getDistributedObjects());
        Collection<DistributedObject> distributedObjectCollection = testBean.getDistributedObjects();

        boolean xaResourceCreated = false;

        for (DistributedObject distributedObject : distributedObjectCollection) {
            if (distributedObject.getName().equals("hz:impl:xaService"))
                xaResourceCreated = true;
        }

        //There should be one XAResource distibuted object in addition to two maps we created here
        assertTrue(xaResourceCreated);
    }
}
