# Table of Contents

* [Getting Started](#getting-started)
* [Integrating into Java EE](#integrating-hazelcast-into-java-ee)
* [Sample Code for Java EE Integration](#sample-code-for-java-ee-integration)
* [Configuring Resource Adapter](#configuring-resource-adapter)
* [Configuring a Glassfish v3 Web Application](#configuring-a-glassfish-v3-web-application)
* [Configuring a Wildfly 10.1 Web Application](#configuring-a-wildfly-101-web-application)
* [Configuring a JBoss AS 5 Web Application](#configuring-a-jboss-as-5-web-application)
* [Configuring a JBoss AS 7 or EAP 6 Web App](#configuring-a-jboss-as-7-or-eap-6-web-application)
  * [Starting JBoss](#starting-jboss)
  * [Using the Resource Adapter](#using-the-resource-adapter)
* [Known Issues](#known-issues)


# Getting Started

Hazelcast JCA resource adapter is a system-level software driver used by a Java application to connect to an Hazelcast Cluster.

please see releases prior to v3.7 on [hazelcast main repository](https://github.com/hazelcast/hazelcast). 

As of hazelcast-ra v3.7, hazelcast-ra module has its own release cycle with initial release of [v3.7](https://github.com/hazelcast/hazelcast-ra/releases/tag/v3.7)

# Integrating Hazelcast into Java EE

You can integrate Hazelcast into Java EE containers via the Hazelcast Resource Adapter (`hazelcast-jca-rar-<version>.rar`). After a proper configuration, Hazelcast can participate in standard Java EE transactions.

```java
<%@page import="javax.resource.ResourceException"%>
<%@page import="javax.transaction.*"%>
<%@page import="javax.naming.*"%>
<%@page import="javax.resource.cci.*"%>
<%@page import="java.util.*"%>
<%@page import="com.hazelcast.core.*"%>
<%@page import="com.hazelcast.jca.*"%>

<%
UserTransaction txn = null;
HazelcastConnection conn = null;
HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

try {
  Context context = new InitialContext();
  txn = (UserTransaction) context.lookup( "java:comp/UserTransaction" );
  txn.begin();

  HazelcastConnectionFactory cf = (HazelcastConnectionFactory)
      context.lookup ( "java:comp/env/HazelcastCF" );
        
  conn = cf.getConnection();

  TransactionalMap<String, String> txMap = conn.getTransactionalMap( "default" );
  txMap.put( "key", "value" );

  txn.commit();
    
} catch ( Throwable e ) {
  if ( txn != null ) {
    try {
      txn.rollback();
    } catch ( Exception ix ) {
      ix.printStackTrace();
    };
  }
  e.printStackTrace();
} finally {
  if ( conn != null ) {
    try {
      conn.close();
    } catch (Exception ignored) {};
  }
}
%>
```


Sometimes Hazelcast class loader might not be able to find the classes you provide, i.e. your class loader might be different than that of Hazelcast. In this case, you need to specify the class loader through `Config` to be used internally by Hazelcast.

Assume that Hazelcast is embedded in a container and you want to run your own `Runnable` through `IExecutorService`. Here, Hazelcast class loader and your class loader are different. Therefore, Hazelcast class loader does not know your `Runnable` class.
You need to tell Hazelcast to use a specified class loader to lookup classes internally. A sample code line for this could be `config.setClassLoader(getClass().getClassLoader())`. 


# Sample Code for Java EE Integration

Please see our sample application for <a href="https://github.com/hazelcast/hazelcast-code-samples/tree/master/hazelcast-integration/jca-ra" target="_blank">Java EE Integration</a>.




# Configuring Resource Adapter

Deploying and configuring the Hazelcast resource adapter is no different than configuring any other resource adapter since the Hazelcast resource adapter is a standard JCA one. However, resource adapter installation and configuration is container-specific, so please consult your Java EE vendor documentation for details. The most common steps are:

1. Add the `hazelcast-`*version*`.jar` and `hazelcast-jca-`*version*`.jar` to the container's classpath. Usually there is a lib directory that is loaded automatically by the container on startup.
2. Deploy `hazelcast-jca-rar-`*version*`.rar`. Usually there is some kind of a deploy directory. The name of the directory varies by container.
3. Make container-specific configurations when/after deploying `hazelcast-jca-rar-`*version*`.rar`. In addition to container specific configurations, set the JNDI name for the Hazelcast resource.
4. Configure your application to use the Hazelcast resource. Update `web.xml` and/or `ejb-jar.xml` to let the container know that your application will use the Hazelcast resource, and define the resource reference.
5. Make the container-specific application configuration to specify the JNDI name used for the resource in the application.




# Configuring a Glassfish v3 Web Application

To configure an example Glassfish v3 web application:

1. Place the `hazelcast-`*version*`.jar` and `hazelcast-jca-`*version*`.jar` into the `GLASSFISH_HOME/glassfish/
domains/domain1/lib/ext/` folder.
2. Place the `hazelcast-jca-rar-`*version*`.rar` into `GLASSFISH_HOME/glassfish/domains/domain1/autodeploy/` folder.
3. Add the following lines to the `web.xml` file.

```xml
<resource-ref>
  <res-ref-name>HazelcastCF</res-ref-name>
  <res-type>com.hazelcast.jca.ConnectionFactoryImpl</res-type>
  <res-auth>Container</res-auth>
</resource-ref>
```

Notice that we did not have to put `sun-ra.xml` into the RAR file since it already comes with the `hazelcast-ra-`*version*`.rar` file.

If the Hazelcast resource is used from EJBs, you should configure `ejb-jar.xml` for resource reference and JNDI definitions, just like for the `web.xml` file.


# Configuring a Wildfly 10.1 Web Application

To configure an example Wildfly 10.1 web application: 

- Create the folder `<wildfly_home>/modules/system/layers/base/com/hazelcast/main`.
- Place the `hazelcast-`<*version*>`.jar`and `hazelcast-jca-`<*version*>`.jar` into the folder you created in the previous step.
- Create the file `module.xml` and place it in the same folder. This file should have the following content:

```xml
<module xmlns="urn:jboss:module:1.5" name="com.hazelcast">
    <resources>
        <resource-root path="hazelcast-<version>.jar"/>
        <resource-root path="hazelcast-jca-<version>.jar"/>
    </resources>

    <dependencies>
        <module name="javax.api"/>
        <module name="javax.cache.api"/>
        <module name="javax.resource.api"/>
        <module name="javax.transaction.api"/>
        <module name="org.apache.log4j"/>
        <module name="sun.jdk"/>
    </dependencies>
</module>
```

Here is the simple example of Hazelcast JCA connection with Wildfly in Docker environment:

https://github.com/hazelcast/hazelcast-docker-samples/tree/master/jca-ra


# Configuring a JBoss AS 5 Web Application

To configure a JBoss AS 5 web application:

- Place the `hazelcast-`*version*`.jar` and `hazelcast-jca-`*version*`.jar` into the `JBOSS_HOME/server/deploy/
default/lib` folder.
- Place the `hazelcast-jca-rar-`*version*`.rar` into the `JBOSS_HOME/server/deploy/default/deploy` folder.
- Create a `hazelcast-ds.xml` file containing the following content in the `JBOSS_HOME/server/deploy/default/deploy` folder. Make sure to set the `rar-name` element to `hazelcast-ra-`*version*`.rar`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE connection-factories
  PUBLIC "-//JBoss//DTD JBOSS JCA Config 1.5//EN"
  "http://www.jboss.org/j2ee/dtd/jboss-ds_1_5.dtd">

<connection-factories>
  <tx-connection-factory>
    <local-transaction/>
    <track-connection-by-tx>true</track-connection-by-tx>
    <jndi-name>HazelcastCF</jndi-name>
    <rar-name>hazelcast-jca-rar-<version>.rar</rar-name>
    <connection-definition>
       javax.resource.cci.ConnectionFactory
    </connection-definition>
  </tx-connection-factory>
</connection-factories>
```

- Add the following lines to the `web.xml` file.

```xml
<resource-ref>
  <res-ref-name>HazelcastCF</res-ref-name>
  <res-type>com.hazelcast.jca.ConnectionFactoryImpl</res-type>
  <res-auth>Container</res-auth>
</resource-ref>
```

- Add the following lines to the `jboss-web.xml` file.

```xml
<resource-ref>
  <res-ref-name>HazelcastCF</res-ref-name>
  <jndi-name>java:HazelcastCF</jndi-name>
</resource-ref>
```

If the Hazelcast resource is used from EJBs, you should configure `ejb-jar.xml` and `jboss.xml` for resource reference and JNDI definitions.

<br> </br>




# Configuring a JBoss AS 7 or EAP 6 Web Application

Deploying on JBoss AS 7 or JBoss EAP 6 is a straightforward process. The steps you perform are shown below. The only non-trivial step is the creation of a new JBoss module with Hazelcast libraries.     

- Create the folder `<jboss_home>/modules/system/layers/base/com/hazelcast/main`.
- Place the `hazelcast-`<*version*>`.jar` and `hazelcast-jca-`<*version*>`.jar` into the folder you created in the previous step.
- Create the file `module.xml` and place it in the same folder. This file should have the following content:

```xml
<module xmlns="urn:jboss:module:1.0" name="com.hazelcast">
  <resources>
    <resource-root path="."/>
    <resource-root path="hazelcast-<version>.jar"/>
    <resource-root path="hazelcast-jca-<version>.jar"/>
  </resources>
  <dependencies>
    <module name="sun.jdk"/>
    <module name="javax.api"/>
    <module name="javax.resource.api"/>
    <module name="javax.validation.api"/>
    <module name="org.jboss.ironjacamar.api"/>
  </dependencies>
</module>
```

## Starting JBoss

At this point, you have a new JBoss module with Hazelcast in it. You can now start JBoss and deploy the `hazelcast-jca-rar-`<*version*>`.rar` file via JBoss CLI or Administration Console.

## Using the Resource Adapter

Once the Hazelcast Resource Adapter is deployed, you can start using it. The easiest way is to let a container inject `ConnectionFactory` into your beans. 
    
```java
package com.hazelcast.examples.rar;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.jca.HazelcastConnection;

import javax.annotation.Resource;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

@javax.ejb.Stateless
public class ExampleBean implements ExampleInterface {
    private final static Logger log = Logger.getLogger(ExampleBean.class.getName());

    @Resource(mappedName = "java:/HazelcastCF")
    protected ConnectionFactory connectionFactory;

    public void insert(String key, String value) {
        HazelcastConnection hzConn = null;
        try {
            hzConn = getConnection();
            TransactionalMap<String,String> txmap = hzConn.getTransactionalMap("txmap");
            txmap.put(key, value);
        } finally {
            closeConnection(hzConn);
        }
    }

    private HazelcastConnection getConnection() {
        try {
            return (HazelcastConnection) connectionFactory.getConnection();
        } catch (ResourceException e) {
            throw new RuntimeException("Error while getting Hazelcast connection", e);
        }
    }

    private void closeConnection(HazelcastConnection hzConn) {
        if (hzConn != null) {
            try {
                hzConn.close();
            } catch (ResourceException e) {
                log.log(Level.WARNING, "Error while closing Hazelcast connection.", e);
            }
        }
    }
}
```
### Using a Hazelcast Client Instance

By default the JCA starts a new Hazelcast Server Instance. If this is not necessary, a Hazelcast Client instance can be used instead. 

To use a Hazelcast Client, modify the ```client``` property in the ra.xml file so its set to true. Also please make sure to configure a correct hazelcast client config file.

### Reset for Non-rollbackable Local Transactions

When a thread finishes without calling commit or rollback because of an error, the transaction context is leaked in the 
`LocalTransaction`. Calling `rollback` from another thread is not possible because Hazelcast transactions do not span 
multiple threads. Although resources (obtained locks) will be released automatically after timeout, this 
`LocalTransaction` cannot be used. In such a scenario, `HazelcastTransaction#reset` method can be used to reset the
transaction.

# Known Issues

- There is a regression in JBoss EAP 6.1.0 causing failure during Hazelcast Resource Adapter deployment. The issue is fixed in JBoss EAP 6.1.1. Please see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=976294" target="_blank">this</a> for additional details.  
