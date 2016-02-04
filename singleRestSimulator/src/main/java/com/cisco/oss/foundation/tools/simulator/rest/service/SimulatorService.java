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

package com.cisco.oss.foundation.tools.simulator.rest.service;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorEntity;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorRequest;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.cisco.oss.foundation.tools.simulator.rest.startup.SingleRestSimulatorStartup;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sun.xml.internal.ws.client.sei.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
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
		String requestValidityEnabledConfigPreffix = ".http.requestValidityFilter.isEnabled";
		ConfigurationFactory.getConfiguration().setProperty(serverName + ".http.port", port);
		
		boolean isRequestValidityFilterEnabled = 
				ConfigurationFactory.getConfiguration().getBoolean(SingleRestSimulatorStartup.SINGLE_REST_SIMULATOR + requestValidityEnabledConfigPreffix);
		ConfigurationFactory.getConfiguration().setProperty(serverName + requestValidityEnabledConfigPreffix, isRequestValidityFilterEnabled);

		if (simulatorExists(port)) {
			logger.error("simulator on port " + port + " already exists");
			return false;
		}


		ListMultimap<String, Servlet> servlets = ArrayListMultimap.create();
		XmlWebApplicationContext webConfig = new XmlWebApplicationContext();
		webConfig.setConfigLocation("classpath:META-INF/restSimulatorContext.xml");
		webConfig.registerShutdownHook();

		// Create the servlet
		servlets.put("/", new DispatcherServlet(webConfig));

		JettyHttpServerFactory.INSTANCE.startHttpServer(serverName, servlets);
		
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

	public void clearAllRequests(int port) throws Exception {
		
		if (!simulatorExists(port)) {
			logger.debug("there is no simulator on port " + port);
			return;
		}
		
		simulators.get(port).clearSimulatorResponses();
		
		logger.debug("All requests for simulator on port " + port + " were cleared");
	}

	
	public void deleteSimulator(int port) throws Exception {
		
		if (!simulatorExists(port)) {
			logger.debug("there is no simulator on port " + port);
			return;
		}

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

	public ResponseEntity retrieveResponse(String method, HttpServletRequest httpServletRequest, HttpHeaders headers, MultiValueMap<String, String> queryParams, String body) {

        int port = httpServletRequest.getLocalPort();
		
		if (!simulatorExists(port)) {
			logger.error("there is no simulator on port " + port);
			ResponseEntity.status(0).build();
		}
		
		SimulatorEntity simulator = simulators.get(port);
		String path = httpServletRequest.getPathInfo();
		path = removeFirstAndLastSlashesFromUrl(path);
		SimulatorRequest simulatorRequest = new SimulatorRequest(method, path, queryParams,
				headers, body);
		
		if (ConfigurationFactory.getConfiguration().getBoolean("restSimulator.queue.enable", true)) {	
			if (simulatorRequest != null) {
				simulator.addRequestToQueue(simulatorRequest);
			}
		}
		
		return simulator.generateResponse(simulatorRequest);
		
	}
	
	private String removeFirstAndLastSlashesFromUrl(String expectedUrl) {
		// will remove the first and last '/'
		if (expectedUrl.startsWith("/")) {
			expectedUrl = expectedUrl.substring(1);
		}
		if (expectedUrl.endsWith("/")) {
			expectedUrl = expectedUrl.substring(0, expectedUrl.length() - 1);
		}
		return expectedUrl;
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

	public SimulatorRequest removeLastRequestOfSimulator(int port) {
		SimulatorEntity simulator = getSimulator(port);

		if (simulator != null) {
			SimulatorRequest removedLastRequest = simulator.removeLastRequest();
			logger.debug("last request was removed for simulator on port:" + port);
			return removedLastRequest;
		}
		
		return null;
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
