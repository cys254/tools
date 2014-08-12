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

import java.io.StringWriter;
import java.io.Writer;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;

/**
 * this is the resource of the next-response of the simulator
 *
 */
@Component
@Path("/simulator/{port}/nextResponse")
@Scope("request")
public class SimulatorNextResponseResource {

	private static ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger  = LoggerFactory.getLogger(SimulatorNextResponseResource.class);
	
	private SimulatorService simulatorService;

	public SimulatorNextResponseResource() {		
		simulatorService = SimulatorService.getInstance();
	}
	
	@POST
	public Response updateNextResponseForSimulator(@PathParam("port") final int port, final String simulatorResponseStr) {
		
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not set next response for simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		SimulatorResponse simulatorNextResponse;
		try {
			simulatorNextResponse = objectMapper.readValue(simulatorResponseStr, SimulatorResponse.class);
		} catch (Exception e) {
			logger.error("failed parsing json request", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
		boolean added = simulatorService.addNextResponseToSimulator(port, simulatorNextResponse);
		
		ResponseBuilder rb;
		if (added) {
			rb = Response.ok();
		} else {
			rb = Response.status(Status.INTERNAL_SERVER_ERROR);
		}
		return rb.build();
	}
	
	@DELETE
	public Response deleteSimulator(@PathParam("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete next response for simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		
		try {
			simulatorService.deleteNextResponseForSimulator(port);
		} catch (Exception e) {
			logger.error("failed deleting next response for simulator", e);
		}
		return Response.status(Status.OK).entity("next response for simulator on port " + port + " was deleted").build();
	}
	
	@GET
	public Response getSimulator(@PathParam("port") final int port) throws Exception {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}

		SimulatorResponse simulatorNextResponse = simulatorService.getSimulator(port).getSimulatorNextResponse();
		
		String simulatorNextResponseStr = "";
		
		if (simulatorNextResponse != null) {	
			Writer strWriter = new StringWriter();
			objectMapper.writeValue(strWriter, simulatorNextResponse);
			
			simulatorNextResponseStr = strWriter.toString();
		}

		return Response.status(Status.OK).entity(simulatorNextResponseStr).build();
	}
}
