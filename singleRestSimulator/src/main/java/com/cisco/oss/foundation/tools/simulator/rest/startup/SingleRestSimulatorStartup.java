package com.cisco.oss.foundation.tools.simulator.rest.startup;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public final class SingleRestSimulatorStartup {
	private static final String SINGLE_REST_SIMULATOR = "restSimulator";
	private static Logger logger = Logger.getLogger(SingleRestSimulatorStartup.class.getName());
//	private static Server server = null;

	public static void main(final String[] args) {
		startServer();
	}

	public static void startServer() {
		try {
			
			ListMultimap<String, Servlet> servlets = ArrayListMultimap.create();
			ListMultimap<String, Filter> filters = ArrayListMultimap.create();
			
			Map<String, String> initParams = new HashMap<>();
			initParams.put("contextConfigLocation", "classpath:META-INF/restSimulatorContext.xml");
			
			servlets.put("/*", new SpringServlet());
			
//			XmlWebApplicationContext webConfig = new XmlWebApplicationContext();
//			webConfig.setConfigLocation("classpath:META-INF/restSimulatorContext.xml");
//			webConfig.registerShutdownHook();
			
			List<EventListener> eventListeners = new ArrayList<EventListener>();
			eventListeners.add(new ContextLoaderListener());
			eventListeners.add(new RequestContextListener());
			
			JettyHttpServerFactory.INSTANCE.startHttpServer(SINGLE_REST_SIMULATOR, servlets, filters, eventListeners, initParams);

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