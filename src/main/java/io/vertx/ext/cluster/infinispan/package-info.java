/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * = Infinispan Cluster Manager
 *
 * This is a cluster manager implementation for Vert.x that uses http://infinispan.org/[Infinispan].
 *
 * This implementation is packaged inside:
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * ----
 *
 * In Vert.x a cluster manager is used for various functions including:
 *
 * * Discovery and group membership of Vert.x nodes in a cluster
 * * Maintaining cluster wide topic subscriber lists (so we know which nodes are interested in which event bus addresses)
 * * Distributed Map support
 * * Distributed Locks
 * * Distributed Counters
 *
 * Cluster managers *do not* handle the event bus inter-node transport, this is done directly by Vert.x with TCP connections.
 *
 * == Using this cluster manager
 *
 * If you are using Vert.x from the command line, the jar corresponding to this cluster manager (it will be named `${maven.artifactId}-${maven.version}.jar`
 * should be in the `lib` directory of the Vert.x installation.
 *
 * If you want clustering with this cluster manager in your Vert.x Maven or Gradle project then just add a dependency to
 * the artifact: `${maven.groupId}:${maven.artifactId}:${maven.version}` in your project.
 *
 * If the jar is on your classpath as above then Vert.x will automatically detect this and use it as the cluster manager.
 * Please make sure you don't have any other cluster managers on your classpath or Vert.x might
 * choose the wrong one.
 *
 * You can also specify the cluster manager programmatically if you are embedding Vert.x by specifying it on the options
 * when you are creating your Vert.x instance, for example:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#createClusterManagerProgramatically()}
 * ----
 *
 * == Configuring this cluster manager
 *
 * The default cluster manager configuration can be modified with `infinispan.xml` and/or `jgroups.xml` files.
 * The former configures the data grid, the latter group management and member discovery.
 *
 * You can place one or both of them on your classpath.
 * If you want to embed your custom file in a fat jar, it must be located at the root of the fat jar.
 * If it's an external file, the **directory** containing the file must be added to the classpath. For
 * example, if you are using the _launcher_ class from Vert.x, the classpath enhancement can be done as follows:
 *
 * [source,shell]
 * ----
 * # If infinispan.xml and/or jgroups.xml files are in the current directory:
 * java -jar my-app.jar -cp . -cluster
 *
 * # If infinispan.xml and/or jgroups.xml files are in the conf directory:
 * java -jar my-app.jar -cp conf -cluster
 * ----
 *
 * Another way to override the configuration is by providing the file locations via system properties:
 * `vertx.infinispan.config` and/or `vertx.jgroups.config`.
 *
 * [source,shell]
 * ----
 * # Use a cluster configuration located in an external file
 * java -Dvertx.infinispan.config=./config/my-infinispan.xml -jar ... -cluster
 *
 * # Or use a custom configuration from the classpath
 * java -Dvertx.infinispan.config=my/package/config/my-infinispan.xml -jar ... -cluster
 * ----
 *
 * The cluster manager will search for the file in classpath first, and fallback to the filesystem.
 *
 * The system properties, when present, override any `infinispan.xml` or `jgroups.xml` on the classpath.
 *
 * The xml files are Infinispan and JGroups configuration files and are described in detail in the documentation on the Infinispan and JGroups web-sites.
 *
 * IMPORTANT: if a `jgroups.xml` file is on the classpath or if you set the `vertx.jgroups.config` system property,
 * it will override any JGroups `stack-file` path defined in the Infinispan configuration file.
 *
 * The default JGroups configuration uses multicast for discovery and TCP for group management.
 * Make sure multicast is enabled on your network for this to work.
 *
 * For full documentation on how to configure the transport differently or use a different transport please consult the
 * Infinispan / JGroups documentations.
 *
 * == Using an existing Infinispan Cache Manager
 *
 * You can pass an existing `DefaultCacheManager` in the cluster manager to reuse an existing cache manager:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#useExistingCacheManager(org.infinispan.manager.DefaultCacheManager)}
 * ----
 *
 * In this case, vert.x is not the cache manager owner and so do not shut it down on close.
 *
 * Notice that the custom Infinispan instance need to be configured with:
 *
 * [source,xml]
 * ----
 * <cache-container default-cache="__vertx.distributed.cache">
 *
 *   <replicated-cache name="__vertx.subs">
 *     <expiration interval="-1"/>
 *   </replicated-cache>
 *
 *   <replicated-cache name="__vertx.haInfo">
 *     <expiration interval="-1"/>
 *   </replicated-cache>
 *
 *   <distributed-cache name="__vertx.distributed.cache">
 *     <expiration interval="-1"/>
 *   </distributed-cache>
 *
 * </cache-container>
 * ----
 *
 * == Configuring for Openshift 3
 *
 * On Openshift 3, JGroups should be configured to use the `KUBE_PING` protocol.
 *
 * First, add the `org.infinispan:infinispan-cloud:${infinispan.version}` dependency to your project.
 * With Maven it looks like:
 *
 * [source,xml]
 * ----
 * <dependency>
 *   <groupId>org.infinispan</groupId>
 *   <artifactId>infinispan-cloud</artifactId>
 *   <version>${infinispan.version}</version>
 *   <exclusions>
 *     <exclusion>
 *       <groupId>org.jgroups</groupId>                <1>
 *       <artifactId>jgroups</artifactId>
 *     </exclusion>
 *     <exclusion>
 *       <artifactId>undertow-core</artifactId>        <2>
 *       <groupId>io.undertow</groupId>
 *     </exclusion>
 *   </exclusions>
 * </dependency>
 * ----
 * <1> make sure to use the `infinispan-core` JGroups version
 * <2> avoid extra dependencies, `KUBE_PING` works fine with the JDK's Http server
 *
 * Then, set the `vertx.jgroups.config` system property to `default-configs/default-jgroups-kubernetes.xml`.
 *
 * [source,shell]
 * ----
 * -Dvertx.jgroups.config=default-configs/default-jgroups-kubernetes.xml
 * ----
 *
 * This JGroups stack file is located in the `infinispan-cloud` JAR and preconfigured for Kubernetes/Openshift3.
 *
 * `KUBE_PING` listens to requests on port `8888` by default, so make sure to declare it when building the container image.
 *
 * [source,Dockerfile]
 * ----
 * EXPOSE 8888
 * ----
 *
 * Also, set the project namespace as the scope for discovery.
 *
 * [source,Dockerfile]
 * ----
 * ENV OPENSHIFT_KUBE_PING_NAMESPACE my-openshift3-project
 * ----
 *
 * Then, force usage of IPv4 in the JVM with a system property.
 *
 * [source,shell]
 * ----
 * -Djava.net.preferIPv4Stack=true
 * ----
 *
 * Eventually, the setup needs a service account.
 *
 * [source,shell]
 * ----
 * oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
 * ----
 *
 * Further configuration details are available in the
 * https://github.com/jgroups-extras/jgroups-kubernetes[Kubernetes discovery protocol for JGroups] README.
 *
 * == Configuring for Docker Compose
 *
 * Make sure to start the Java Virtual Machines with those system properties:
 *
 * [source,shell]
 * ----
 * -Djava.net.preferIPv4Stack=true -Djgroups.tcp.address=NON_LOOPBACK
 * ----
 *
 * This will make JGroups pick the interface of the virtual private network created by Docker.
 *
 * == Trouble shooting clustering
 *
 * If the default multicast discovery configuration is not working here are some common causes:
 *
 * === Multicast not enabled on the machine.
 *
 * It is quite common in particular on OSX machines for multicast to be disabled by default. Please google for
 * information on how to enable this.
 *
 * === Using wrong network interface
 *
 * If you have more than one network interface on your machine (and this can also be the case if you are running
 * VPN software on your machine), then JGroups may be using the wrong one.
 *
 * To tell JGroups to use a specific interface you can provide the IP address of the interface in the `bind_addr`
 * element of the configuration. For example:
 *
 * [source,xml]
 * ----
 * <TCP bind_addr="192.168.1.20"
 *      ...
 *      />
 * <MPING bind_addr="192.168.1.20"
 *      ...
 *      />
 * ----
 *
 * Alternatively, if you want to stick with the bundled `jgroups.xml` file, you can set the `jgroups.tcp.address` system property:
 *
 * ----
 * -Djgroups.tcp.address=192.168.1.20
 * ----
 *
 * When running Vert.x is in clustered mode, you should also make sure that Vert.x knows about the correct interface.
 * When running at the command line this is done by specifying the `cluster-host` option:
 *
 * ----
 * vertx run myverticle.js -cluster -cluster-host your-ip-address
 * ----
 *
 * Where `your-ip-address` is the same IP address you specified in the JGroups configuration.
 *
 * If using Vert.x programmatically you can specify this using
 * {@link io.vertx.core.VertxOptions#setClusterHost(java.lang.String)}.
 *
 * === Using a VPN
 *
 * This is a variation of the above case. VPN software often works by creating a virtual network interface which often
 * doesn't support multicast. If you have a VPN running and you do not specify the correct interface to use in both the
 * JGroups configuration and to Vert.x then the VPN interface may be chosen instead of the correct interface.
 *
 * So, if you have a VPN running you may have to configure both JGroups and Vert.x to use the correct interface as
 * described in the previous section.
 *
 * === When multicast is not available
 *
 * In some cases you may not be able to use multicast discovery as it might not be available in your environment. In that case
 * you should configure another protocol, e.g. `TCPPING` to use TCP sockets, or `S3_PING` when running on Amazon EC2.
 *
 * For more information on available JGroups discovery protocols and how to configure them
 * please consult the http://www.jgroups.org/manual/index.html#Discovery[JGroups documentation].
 *
 * === Problems with IPv6
 *
 * If you have troubles configuring an IPv6 host, force the use of IPv4 with the `java.net.preferIPv4Stack` system property.
 *
 * ----
 * -Djava.net.preferIPv4Stack=true
 * ----
 *
 * === Enabling logging
 *
 * When trouble-shooting clustering issues with it's often useful to get some logging output from Infinispan and JGroups
 * to see if it's forming a cluster properly. You can do this (when using the default JUL logging) by adding a file
 * called `vertx-default-jul-logging.properties` on your classpath. This is a standard java.util.logging (JUL)
 * configuration file. Inside it set:
 *
 * ----
 * org.infinispan.level=INFO
 * org.jgroups.level=INFO
 * ----
 *
 * and also
 *
 * ----
 * java.util.logging.ConsoleHandler.level=INFO
 * java.util.logging.FileHandler.level=INFO
 * ----
 *
 * == Infinispan logging
 *
 * Infinispan relies on JBoss logging. JBoss Logging is a logging bridge providing integration with numerous logging frameworks.
 *
 * Add the logging JARs of you choice to the classpath and JBoss Logging will pick them up automatically.
 *
 * If you have multiple logging backends on your classpath, you can force selection with the `org.jboss.logging.provider` system property.
 * For exeample:
 *
 * ----
 * -Dorg.jboss.logging.provider=log4j2
 * ----
 *
 * See this http://docs.jboss.org/hibernate/orm/4.3/topical/html/logging/Logging.html[JBoss Logging guide] for more details.
 *
 * == JGroups logging
 *
 * JGroups uses JDK logging by default. log4j and log4j2 are supported if the corresponding JARs are found on the classpath.
 *
 * Please refer to the http://www.jgroups.org/manual/index.html#Logging[JGroups logging documentation] if you need
 * more details or want to implement your own logging backend implementation.
 *
 * == SharedData extensions
 *
 * === AsyncMap content streams
 *
 * The `InfinispanAsyncMap` API allows to retrieve keys, values and entries as streams.
 * This can be useful if you need to go through the content of a large map for bulk processing.
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#asyncMapStreams(io.vertx.core.shareddata.AsyncMap)}
 * ----

 *
 */
@Document(fileName = "index.adoc")
@ModuleGen(name = "vertx-infinispan", groupPackage = "io.vertx")
package io.vertx.ext.cluster.infinispan;

import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.docgen.Document;
