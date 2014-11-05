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

package com.cisco.oss.foundation.tools.simulator.rest.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorEntity;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;

/**
 * this resource will handle the configuration of the rest-simulator. 
 * from this resource we'll manage all the simulators we want to run.
 * 
 * regular-expressions in path example:
 * 
 * request:
 * ...
 * "expectedUrl":"pps/households/(\\w*)/catalog/(\\w*)",
 * "responseBody":"this is the response of for household {$1} catalog-item {$2}. (hh={$1})",
 * ...
 * 
 * 
 * when you'll call the simulator with:
 * 
 * http://host:8888/pps/households/1234/catalog/abcde
 * 
 * you'll get:
 * 
 * this is the response of for household 1234 catalog-item abcde. (hh=1234)
 * 
 * ******************************************************
 * regular-expressions in query params example:
 * "expectedUrl":"pps/households?howAreYou=(\\w*)&areYouSure=(\\w*)"
 * "responseBody":"I am {@howAreYou}. {@areYouSure}, I am sure",
 * 
 * when you'll call the simulator with:
 * 
 * http://host:8888/pps/households?howAreYou=Sababa&areYouSure=Yes
 * 
 * you'll get:
 * 
 * I am Sababa. Yes, I am sure
 */
@Component
@Path("/simulator/{port}")
@Scope("request")
public class SimulatorConfigurationResource {

	private static ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger  = LoggerFactory.getLogger(SimulatorConfigurationResource.class);
	
	private SimulatorService simulatorService;

	public SimulatorConfigurationResource() {		
		simulatorService = SimulatorService.getInstance();
	}
	
	@POST
	public Response postRequest(@PathParam("port") final int port) {
		boolean addSimulator = false;
		try {
			addSimulator = simulatorService.addSimulator(port);
		} catch (Exception e) {
			logger.error("failed adding simulator.", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
		if (addSimulator) {	
			return Response.ok().entity("simulator was added on port " + port).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("failed adding simulator on port " + port).build();
		}
	}

	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response updateSimulator(@PathParam("port") final int port, final String simulatorResponseStr) {
	
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not update simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		SimulatorResponse simulatorResponse;
		boolean added = false;;
		 
		if (StringUtils.isEmpty(simulatorResponseStr)) {
			String msg = "When adding a response to an existing simulator you need to specify the response json in the body." +
					" if you want to load the simulator from a file just write 'file' in the body and we'll take it from the default location which is at: " +
					" src/main/resources/responses/defaultResponses.json - sababa ?";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		
		if (simulatorResponseStr.trim().equalsIgnoreCase("file")) {	
			added = addResponsesFromDefaultFile(port);
		} else if (simulatorResponseStr.trim().startsWith("file:") && simulatorResponseStr.trim().endsWith(".json")) {
			String fileName = StringUtils.right(simulatorResponseStr, simulatorResponseStr.length() - 5);
			added = addResponsesFromSpecificFile(port, fileName);
		} else {	
			try {				
				simulatorResponse = objectMapper.readValue(simulatorResponseStr, SimulatorResponse.class);
				logger.debug("Request added to simulator:\n" + simulatorResponseStr);
			} catch (Exception e) {
				logger.error("failed parsing json request", e);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} 
			added = simulatorService.addResponseToSimulator(port, simulatorResponse);
		}
			
		ResponseBuilder rb;
		
		if (added) {
			rb = Response.ok();
		} else {
			rb = Response.status(Status.INTERNAL_SERVER_ERROR);
		}
		return rb.build();
	}
	
	@PUT
	@Consumes({ MediaType.TEXT_PLAIN })
	public Response clearSimulator(@PathParam("port") final int port, final String operation) {
	
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not clear requests of simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		
		if (StringUtils.isEmpty(operation)) {
			return Response.status(Status.BAD_REQUEST).entity("request must contain a body. valid body: 'clear'").build();
		}
		
		ResponseBuilder rb;
		if (operation.equalsIgnoreCase("clear")) {
			try {
				simulatorService.clearAllRequests(port);
			} catch (Exception e) {
				logger.error("failed to clear all request for simulator on port " + port, e);
			}
			rb = Response.status(Status.OK).entity("All requests for simulator on port " + port + " were reset");
		} else {
			rb = Response.status(Status.BAD_REQUEST).entity("valid body is 'clear'");
		}
	
		return rb.build();
	}

	@GET
	public Response getSimulator(@PathParam("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}

		SimulatorEntity simulator = simulatorService.getSimulator(port);
		
		Writer strWriter = new StringWriter();
		
		try {
			objectMapper.writeValue(strWriter, simulator.getSimulatorResponses());
		} catch (Exception e) {
			logger.error("failed to write simulator to json", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("failed to write simulator to json").build();
		}

		String simulatorStr = strWriter.toString();

		return Response.status(Status.OK).entity(simulatorStr).build();
	}

	@DELETE
	public Response deleteSimulator(@PathParam("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		
		try {
			simulatorService.deleteSimulator(port);
		} catch (Exception e) {
			logger.error("failed stopping simulator", e);
		}
		return Response.status(Status.OK).entity("simulator on port " + port + " was deleted").build();
	}
	
	@GET
	@Path("isAlive")
	public Response isAlive(@PathParam("port") final int port) {
		boolean isAlive;
		
		if (!simulatorService.simulatorExists(port)) {
			isAlive = false;
		} else {	
			isAlive = true;
		}
		String isAliveStr = String.valueOf(isAlive);
		String msg = "simulator on port " + port +  " |  is alive=" + isAliveStr;
		logger.info(msg);
		return Response.status(Status.OK).entity(isAliveStr).build();

	}

	private boolean addResponsesFromDefaultFile(int port) {
		List<SimulatorResponse> defaultResponses = loadResponsesFromFile("defaultResponses.json");
		return simulatorService.addResponsesToSimulator(port, defaultResponses);
	}

	private boolean addResponsesFromSpecificFile(int port, String fileName) {
		List<SimulatorResponse> defaultResponses = loadResponsesFromFile(fileName);
		return simulatorService.addResponsesToSimulator(port, defaultResponses);
	}
	
	private List<SimulatorResponse> loadResponsesFromFile(String fileName) {
		List<SimulatorResponse> response = null;
		String defaultLocation = "/responses/" + fileName;
		InputStream is = getClass().getResourceAsStream(defaultLocation);
		
		try {
			response = objectMapper.readValue(is, new TypeReference<List<SimulatorResponse>>() {});
		} catch (IOException e) {
			logger.error("failed loading simulatorResponses from default file",	e);
		}

		return response;	
	}


}

	
