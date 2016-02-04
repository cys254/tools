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

package com.cisco.oss.foundation.tools.simulator.rest.resources;

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorRequest;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * this is the resource of the Queue of the simulator
 * this will manage the messages the were sent to the simulator
 *
 */
@RestController
@RequestMapping("/simulator/{port}/queue")
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
	@RequestMapping(value = "/{numOfMessages}", method = {RequestMethod.GET})
	public ResponseEntity getAllMessagesInQueue(@PathVariable("port") final int port,
												@PathVariable("numOfMessages") final String numOfMessages) throws Exception {
		
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulators queue. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}

		String body = "[]";
		if ("all".equalsIgnoreCase(numOfMessages)) {
			List<SimulatorRequest> allQueueOfSimulator = simulatorService.getAllQueueOfSimulator(port);
			
			if (!CollectionUtils.isEmpty(allQueueOfSimulator)) {
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
				return ResponseEntity.badRequest().body("");
			}
		}
		
		return ResponseEntity.ok(body);
	}

	/**
	 * this function will delete the queue of the requests on this port
	 */
	@RequestMapping(method = {RequestMethod.DELETE})
	public ResponseEntity deleteQueue(@PathVariable("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete queue of simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		
		simulatorService.removeQueueOfSimulator(port);
		return ResponseEntity.ok("queue of simulator on port " + port + " is empty");
	}

	/**
	 * this function will delete the last request from the queue on this port
	 */
	@RequestMapping(value = "/lastRequest", method = {RequestMethod.DELETE})
	public ResponseEntity deleteLastRequestFromQueue(@PathVariable("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete last request from queue of simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		
		SimulatorRequest removeLastRequestOfSimulator = simulatorService.removeLastRequestOfSimulator(port);
		if (removeLastRequestOfSimulator == null) {
			return ResponseEntity.ok("queue is empty. No request of simulator on port " + port + " was removed");
		} else {
			return ResponseEntity.ok("last request of simulator on port " + port + " was removed: " +
					System.lineSeparator() + removeLastRequestOfSimulator.toString());
		}
	}
}
