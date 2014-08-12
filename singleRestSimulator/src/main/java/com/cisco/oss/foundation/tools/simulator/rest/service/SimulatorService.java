/*
 * Copyright 2014 Cisco Systems, Inc.
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

package com.cisco.oss.foundation.tools.simulator.rest.service;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorEntity;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorRequest;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorService {

	private static Logger logger = LoggerFactory.getLogger(SimulatorService.class);

	private Map<Integer, SimulatorEntity> simulators = new HashMap<Integer, SimulatorEntity>();

	private static SimulatorService simulatorService;

	private SimulatorService() {

	}

	public static SimulatorService getInstance() {

		if (simulatorService == null) {
			simulatorService = new SimulatorService();
		}
		return simulatorService;
	}

	public boolean addSimulator(int port) throws Exception {
		
		String serverName = "ServerSim_" + port;
		
		ConfigurationFactory.getConfiguration().setProperty(serverName + ".http.port", port);

		if (simulatorExists(port)) {
			logger.error("simulator on port " + port + " already exists");
			return false;
		}

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("com.cisco.oss.foundation.tools");
		
		ServletContainer servletContainer = new ServletContainer(resourceConfig);
		
		ListMultimap<String, Servlet> servlets = ArrayListMultimap.create();
		servlets.put("/*", servletContainer);
		
		JettyHttpServerFactory.INSTANCE.startHttpServer(serverName, servlets);
		
//		Server server = new Server(port);
//
//		Context root = new Context(server, "/", Context.SESSIONS);
//        ServletHolder holder = new ServletHolder(servletContainer);
//		root.addServlet(holder, "/");
//		try {
//			server.start();
//		} catch (BindException e) {
//			logger.error("can't create simulator", e);
//			return false;
//		}
//        holder.setInitParameter("port", Integer.toString(port));

		logger.debug("simulator was added on port:" + port);

		SimulatorEntity simulatorEntity = new SimulatorEntity(port);
		
		simulators.put(port, simulatorEntity);
	
		return true;
	}

	public boolean addResponsesToSimulator(int port, List<SimulatorResponse> defaultResponses) {
		
		boolean oneOrMordeFailed = false;
		
		for (SimulatorResponse response : defaultResponses) {
			boolean added = addResponseToSimulator(port, response);
			
			if (!added) {
				logger.error("adding simulatorResponses to simulator on port " + port + ", failed loading response. " + response.toString());
				oneOrMordeFailed = true;
			}
		}
		
		return !oneOrMordeFailed;
	}
	
	public boolean addResponseToSimulator(int port, SimulatorResponse response) {

		if (!simulatorExists(port)) {
			logger.error("there is no simulator on port " + port);
			return false;
		}
	
		List<SimulatorResponse> responses = simulators.get(port).getSimulatorResponses();
		
		responses.add(response);

		logger.debug("response was added to simulator on port:" + port);
		return true;
	}

	public void deleteSimulator(int port) throws Exception {
		
		if (!simulatorExists(port)) {
			logger.debug("there is no simulator on port " + port);
			return;
		}
//		SimulatorEntity simulatorEntity = simulators.get(port);
		
		String serverName = "ServerSim_" + port;
		
		JettyHttpServerFactory.INSTANCE.stopHttpServer(serverName);
		
		logger.debug("simulator on port " + port + " was stoped");
		
		simulators.remove(port);
	}

	public SimulatorEntity getSimulator(int port) {
		return simulators.get(port);
	}

	public boolean simulatorExists(int port) {
		return simulators.containsKey(port);
	}

	public ResponseBuilder retrieveResponse(String method, ServletConfig servletConfig, UriInfo uriInfo, HttpHeaders headers, String body) {

        int port = Integer.parseInt(servletConfig.getInitParameter("port"));
		
		if (!simulatorExists(port)) {
			logger.error("there is no simulator on port " + port);
			Response.status(0).build();
		}
		
		SimulatorEntity simulator = simulators.get(port);
		
		SimulatorRequest simulatorRequest = new SimulatorRequest(method, uriInfo.getPath(), uriInfo.getQueryParameters(),
				headers.getRequestHeaders(), body);
		
		simulator.addRequestToQueue(simulatorRequest);
		
		return simulator.generateResponse(simulatorRequest);
		
	}

	public List<SimulatorRequest> getAllQueueOfSimulator(int port) {

		SimulatorEntity simulator = getSimulator(port);
		
		if (simulator != null) {
			return simulator.getAllRequests();
		}
		return null;
	}

	public List<SimulatorRequest> getQueueOfSimulator(int port, int numOfMsg) {
		
		SimulatorEntity simulator = getSimulator(port);

		if (simulator != null) {
			return simulator.getLastRequests(numOfMsg);
		}
		return null;
	}

	public void removeQueueOfSimulator(int port) {
		SimulatorEntity simulator = getSimulator(port);

		if (simulator != null) {
			simulator.removeAllRequests();
			logger.debug("queue of requests was removed for simulator on port:" + port);
		}
	}

	public boolean addNextResponseToSimulator(int port,	SimulatorResponse simulatorNextResponse) {
		if (!simulatorExists(port)) {
			logger.error("there is no simulator on port " + port);
			return false;
		}
	
		SimulatorEntity simulator = simulators.get(port);
		simulator.setSimulatorNextResponse(simulatorNextResponse);
		
		logger.debug("next response was added to simulator on port:" + port);
		
		return true;
	}

	public boolean deleteNextResponseForSimulator(int port) {
		if (!simulatorExists(port)) {
			logger.error("there is no simulator on port " + port);
			return false;
		}
	
		SimulatorEntity simulator = simulators.get(port);
		simulator.setSimulatorNextResponse(null);
		
		logger.debug("next response was deleted for simulator on port:" + port);
		
		return true;
		
	}

}