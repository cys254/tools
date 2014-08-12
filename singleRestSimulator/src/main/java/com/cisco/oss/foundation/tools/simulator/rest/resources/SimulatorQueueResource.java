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
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorRequest;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;

/**
 * this is the resource of the Queue of the simulator
 * this will manage the messages the were sent to the simulator
 *
 */
@Component
@Path("/simulator/{port}/queue")
@Scope("request")
public class SimulatorQueueResource {

	private static Logger logger  = LoggerFactory.getLogger(SimulatorConfigurationResource.class);
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	private SimulatorService simulatorService;

	public SimulatorQueueResource() {		
		simulatorService = SimulatorService.getInstance();
	}

	/**
	 * for the last request call /simulator/8888/queue/1
	 * for the last 6 requests call /simulator/8888/queue/6
	 * for all the requests call /simulator/8888/queue/all
	 */
	@GET
	@Path("/{numOfMessages}")
	public Response getAllMessagesInQueue(@PathParam("port") final int port, 
			@PathParam("numOfMessages") final String numOfMessages) throws Exception {
		
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulators queue. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}

		String body = "[]";
		if ("all".equalsIgnoreCase(numOfMessages)) {
			List<SimulatorRequest> allQueueOfSimulator = simulatorService.getAllQueueOfSimulator(port);
			
			if (CollectionUtils.isNotEmpty(allQueueOfSimulator)) {	
				Writer strWriter = new StringWriter();
				
				objectMapper.writeValue(strWriter, allQueueOfSimulator);
				body = strWriter.toString();
			}
			
			
		} else {
			if (NumberUtils.isNumber(numOfMessages)) {
				int numOfMsg = Integer.valueOf(numOfMessages);
				List<SimulatorRequest> queueOfSimulator = simulatorService.getQueueOfSimulator(port, numOfMsg);
				
				Writer strWriter = new StringWriter();

				objectMapper.writeValue(strWriter, queueOfSimulator);
				body = strWriter.toString();
			} else {
				String msg = "number of messages (" + numOfMessages + ") isn't a valid number";
				logger.error(msg);
				return Response.status(Status.BAD_REQUEST).entity("").build();
			}
		}
		
		return Response.status(Status.OK).entity(body).build();
	}

	/**
	 * this function will delete the queue of the requests on this port
	 */
	@DELETE
	public Response deleteQueue(@PathParam("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete queue of simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return Response.status(Status.BAD_REQUEST).entity(msg).build();
		}
		
		simulatorService.removeQueueOfSimulator(port);
		return Response.status(Status.OK).entity("queue of simulator on port " + port + " is empty").build();
	}

}
