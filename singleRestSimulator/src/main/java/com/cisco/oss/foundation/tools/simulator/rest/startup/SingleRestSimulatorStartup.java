/*
 * Copyright 2016 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.tools.simulator.rest.startup;

import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.log4j.Logger;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.*;

public final class SingleRestSimulatorStartup {
	
    public static final String SINGLE_REST_SIMULATOR = "restSimulator";
    private static Logger logger = Logger.getLogger(SingleRestSimulatorStartup.class.getName());

    public static void main(final String[] args) {
        startServer();
    }

    public static void startServer() {
        try {

            ListMultimap<String, Servlet> servlets = ArrayListMultimap.create();
            ListMultimap<String, Filter> filters = ArrayListMultimap.create();

            XmlWebApplicationContext webConfig = new XmlWebApplicationContext();
            webConfig.setConfigLocation("classpath:META-INF/restSimulatorContext.xml");
            webConfig.registerShutdownHook();

            // Create the servlet
            servlets.put("/", new DispatcherServlet(webConfig));


            List<EventListener> eventListeners = new ArrayList<EventListener>();
            eventListeners.add(new ContextLoaderListener(webConfig));
            eventListeners.add(new RequestContextListener());

            JettyHttpServerFactory.INSTANCE.startHttpServer(SINGLE_REST_SIMULATOR, servlets, filters, eventListeners);
        
//			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
//					"META-INF/applicationContext.xml");
//			server = (Server) applicationContext.getBean("restSimJettyServer");
//
//			server.start();
        } catch (Exception ex) {
            logger.error("Error Starting REST Simulator" + ex);
        }
    }

    public static void stopServer() throws Exception {
//		server.stop();
        JettyHttpServerFactory.INSTANCE.stopHttpServer(SINGLE_REST_SIMULATOR);
    }
}