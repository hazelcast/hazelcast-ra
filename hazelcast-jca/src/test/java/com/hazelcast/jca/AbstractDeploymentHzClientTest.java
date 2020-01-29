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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
* base class for jca tests that rely on client connections to hazelcast
*
*/
public abstract class AbstractDeploymentHzClientTest {
    private static HazelcastInstance serverInstance;

    @AfterClass
    public static void stopServerInstance() {
        serverInstance.shutdown();
        serverInstance = null;
    }

    @Deployment(testable = false, name = "rar-deployment", order = 1)
    public static ResourceAdapterArchive createResourceAdapterDeployment() {
        startServerInstanceIfNecessary();

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, "raa.rar");

        raa.addAsResource("ra-client.xml", "META-INF/ra.xml");
        raa.addAsResource("ironjacamar.xml", "META-INF/ironjacamar.xml");
        raa.addAsResource("sun-ra.xml", "META-INF/sun-ra.xml");

        return raa;
    }

    @Deployment(name = "war-deployment", order = 2)
    public static WebArchive createWebDeployment() {
        WebArchive wa = ShrinkWrap.create(WebArchive.class, "waa.war");
        wa.addAsResource("h2-xa-ds.xml","h2-xa-ds.xml");
        wa.addClass(TestBean.class);
        wa.addClass(ITestBean.class);
        wa.addClass(TestInContainer.class);
        wa.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");

        return wa;
    }

    protected static void startServerInstanceIfNecessary() {
        if (serverInstance == null)
            serverInstance = Hazelcast.newHazelcastInstance();
    }
}