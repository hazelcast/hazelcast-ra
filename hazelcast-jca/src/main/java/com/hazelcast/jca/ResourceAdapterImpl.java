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

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.ConfigBuilder;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the starting point of the whole resource adapter for hazelcast.
 * The hazelcast instance is created/fetched in this class
 */
@Connector(
        description = "Hazelcast JCA Connection",
        displayName = "Hazelcast",
        vendorName = "Hazelcast.com",
        eisType = "Hazelcast",
        licenseDescription = "Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.",
        licenseRequired = true,
        transactionSupport = TransactionSupport.TransactionSupportLevel.XATransaction,
        version = "4.0")
public class ResourceAdapterImpl implements ResourceAdapter, Referenceable, Serializable {

    /**
     * Identity generator
     */
    private static final AtomicInteger ID_GEN = new AtomicInteger();

    private static final long serialVersionUID = -1727994229521767306L;

    /**
     * The hazelcast instance itself
     */
    private transient volatile HazelcastInstance hazelcastInstance;

    /**
     * The Reference instance provided to support Referenceable interface.
     */
    private Reference reference;

    /**
     * The configured hazelcast configuration location
     */
    @ConfigProperty(description = "Location of the hazelcast.xml file (Client or Server)")
    private String configurationLocation;

    /**
     * Indicates whether to create a Hazelcast Client Instance
     */
    @ConfigProperty(description = "Create a Hazelcast Client Instance? Defaults to false (Server)")
    private Boolean client = Boolean.FALSE;

    /**
     * Identity
     */
    private transient int id;

    public ResourceAdapterImpl() {
        setId(ID_GEN.incrementAndGet());
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.ResourceAdapter
     * #endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
            throws ResourceException {
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.ResourceAdapter
     * #endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.ResourceAdapter
     * #getXAResources(javax.resource.spi.ActivationSpec[])
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        //JBoss is fine with null, weblogic requires an empty array
        return new XAResource[0];
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
     */
    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        if (client != null && client) {
            // Creates the hazelcast client instance
            XmlClientConfigBuilder configBuilder = buildClientConfiguration();
            hazelcastInstance = HazelcastClient.newHazelcastClient(configBuilder.build());
        } else {
            // Gets/creates the hazelcast instance
            ConfigBuilder config = buildConfiguration();
            hazelcastInstance = Hazelcast.newHazelcastInstance(config.build());
        }
    }

    /**
     * Creates a hazelcast configuration based on the {@link #getConfigurationLocation()}
     *
     * @return the created hazelcast configuration
     * @throws ResourceAdapterInternalException If there was a problem with the configuration creation
     */
    private ConfigBuilder buildConfiguration() throws ResourceAdapterInternalException {
        XmlConfigBuilder config;
        if (configurationLocation == null || configurationLocation.length() == 0) {
            config = new XmlConfigBuilder();
        } else {
            try {
                config = new XmlConfigBuilder(configurationLocation);
            } catch (FileNotFoundException e) {
                throw new ResourceAdapterInternalException(e.getMessage(), e);
            }
        }
        return config;
    }

    /**
     * Creates a hazelcast client configuration based on the {@link #getConfigurationLocation()}
     *
     * @return the created hazelcast client configuration
     * @throws ResourceAdapterInternalException If there was a problem with the configuration creation
     */
    private XmlClientConfigBuilder buildClientConfiguration()
            throws ResourceAdapterInternalException {
        XmlClientConfigBuilder configBuilder;
        if (configurationLocation == null || configurationLocation.length() == 0) {
            configBuilder = new XmlClientConfigBuilder();
        } else {
            try {
                configBuilder = new XmlClientConfigBuilder(configurationLocation);
            } catch (IOException e) {
                throw new ResourceAdapterInternalException(e.getMessage(), e);
            }
        }
        return configBuilder;
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.ResourceAdapter#stop()
     */
    @Override
    public void stop() {
        HazelcastInstance instance = hazelcastInstance;
        if (instance != null) {
            instance.getLifecycleService().shutdown();
        }
    }

    /**
     * Provides access to the underlying hazelcast instance
     */
    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    /**
     * Sets the underlying hazelcast instance
     * Used only for testing purposes
     */
    public void setHazelcastInstance(HazelcastInstance hazelcast) {
        this.hazelcastInstance = hazelcast;
    }

    /**
     * @see javax.resource.Referenceable
     */
    @Override
    public Reference getReference() throws NamingException {
        // API contract says we can not return null
        if (reference == null) {
            throw new NamingException("reference has not been set");
        }
        return reference;
    }

    /**
     * @see javax.resource.Referenceable
     */
    @Override
    public void setReference(/*@Nonnull*/ Reference reference) {
        this.reference = reference;
    }

    /**
     * @return The configured hazelcast configuration location via RAR deployment descriptor
     */
    public String getConfigurationLocation() {
        return configurationLocation;
    }

    /**
     * Called by the container
     *
     * @param configurationLocation Hazelcast's configuration location
     */
    public void setConfigurationLocation(String configurationLocation) {
        this.configurationLocation = configurationLocation;
    }

    /**
     * @return True if client mode is enabled.
     */
    public Boolean isClient() {
        return client;
    }

    /**
     * Called by the container. Sets whether client mode is enabled.
     *
     * @param client True if client mode is enabled.
     */
    public void setClient(Boolean client) {
        this.client = client;
    }

    @Deprecated
    public String getConfigLocation() {
        return configurationLocation;
    }

    @Deprecated
    public void setConfigLocation(String configLocation) {
        this.configurationLocation = configLocation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceAdapterImpl other = (ResourceAdapterImpl) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    public void setId(int id) {
        this.id = id;
    }
}
