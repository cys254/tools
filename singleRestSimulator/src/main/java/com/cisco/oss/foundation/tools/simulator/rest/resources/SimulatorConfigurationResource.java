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

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorEntity;
import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * this resource will handle the configuration of the rest-simulator. 
 * from this resource we'll manage all the simulators we want to run.
 * 
 * regular-expressions in path example:
 * 
 * request:
 * ...
 * "expectedUrl":"pps/households/(\\w*)/catalog/(\\w*)",
 * "responseBody":"this is the ResponseEntity of for household {$1} catalog-item {$2}. (hh={$1})",
 * ...
 * 
 * 
 * when you'll call the simulator with:
 * 
 * http://host:8888/pps/households/1234/catalog/abcde
 * 
 * you'll get:
 * 
 * this is the ResponseEntity of for household 1234 catalog-item abcde. (hh=1234)
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
@RestController
@RequestMapping("/simulator/{port}")
@Scope("request")
public class SimulatorConfigurationResource {

	private static ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger  = LoggerFactory.getLogger(SimulatorConfigurationResource.class);
	
	private SimulatorService simulatorService;

	public SimulatorConfigurationResource() {		
		simulatorService = SimulatorService.getInstance();
	}
	
	@RequestMapping(method = {RequestMethod.POST})
	public ResponseEntity postRequest(@PathVariable("port") final int port) {
		boolean addSimulator = false;
		try {
			addSimulator = simulatorService.addSimulator(port);
		} catch (Exception e) {
			logger.error("failed adding simulator.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
		
		if (addSimulator) {	
			return ResponseEntity.ok("simulator was added on port " + port);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("failed adding simulator on port " + port);
		}
	}

	
	@RequestMapping(method = {RequestMethod.PUT}, consumes = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity updateSimulator(@PathVariable("port") final int port, @RequestBody final SimulatorResponse simulatorResponse) {
	
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not update simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		boolean added = false;;
		 
		if (simulatorResponse.getExpectedMethod() == null) {
			String msg = "When adding a ResponseEntity to an existing simulator you need to specify the ResponseEntity json in the body." +
					" if you want to load the simulator from a file just write 'file' in the body and we'll take it from the default location which is at: " +
					" src/main/resources/responses/defaultResponses.json - sababa ?";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
			
		}
		
//		if (simulatorResponseStr.trim().equalsIgnoreCase("file")) {
//			added = addResponsesFromDefaultFile(port);
//		} else if (simulatorResponseStr.trim().startsWith("file:") && simulatorResponseStr.trim().endsWith(".json")) {
//			String fileName = StringUtils.right(simulatorResponseStr, simulatorResponseStr.length() - 5);
//			added = addResponsesFromSpecificFile(port, fileName);
//		} else {
			try {				
//				simulatorResponse = objectMapper.readValue(simulatorResponseStr, SimulatorResponse.class);
				logger.debug("Request added to simulator:\n" + simulatorResponse);
			} catch (Exception e) {
				logger.error("failed parsing json request", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			} 
			added = simulatorService.addResponseToSimulator(port, simulatorResponse);
//		}
			
		ResponseEntity re;
		
		if (added) {
			re = ResponseEntity.ok().build();
		} else {
			re = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		return re;
	}
	
	@RequestMapping(method = {RequestMethod.PUT}, consumes = {MediaType.TEXT_PLAIN_VALUE})
	public ResponseEntity clearSimulator(@PathVariable("port") final int port, @RequestBody final String operation) {
	
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not clear requests of simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		
		if (StringUtils.isEmpty(operation)) {
			return ResponseEntity.badRequest().body("request must contain a body. valid body: 'clear'");
		}

		ResponseEntity re;
		if (operation.equalsIgnoreCase("clear")) {
			try {
				simulatorService.clearAllRequests(port);
			} catch (Exception e) {
				logger.error("failed to clear all request for simulator on port " + port, e);
			}
			re = ResponseEntity.ok("All requests for simulator on port " + port + " were reset");
		} else if (operation.trim().equalsIgnoreCase("file")) {
			addResponsesFromDefaultFile(port);
			re = ResponseEntity.ok().build();
		} else if (operation.trim().startsWith("file:") && operation.trim().endsWith(".json")) {
			String fileName = StringUtils.right(operation, operation.length() - 5);
			addResponsesFromSpecificFile(port, fileName);
			re = ResponseEntity.ok().build();
		}else {
			re = ResponseEntity.badRequest().body("valid body is 'clear' of 'file'");
		}
	
		return re;
	}

	@RequestMapping(method = {RequestMethod.GET})
	public ResponseEntity getSimulator(@PathVariable("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}

		SimulatorEntity simulator = simulatorService.getSimulator(port);
		
		Writer strWriter = new StringWriter();
		
		try {
			objectMapper.writeValue(strWriter, simulator.getSimulatorResponses());
		} catch (Exception e) {
			logger.error("failed to write simulator to json", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("failed to write simulator to json");
		}

		String simulatorStr = strWriter.toString();

		return ResponseEntity.ok(simulatorStr);
	}

	@RequestMapping(method = {RequestMethod.DELETE})
	public ResponseEntity deleteSimulator(@PathVariable("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		
		try {
			simulatorService.deleteSimulator(port);
		} catch (Exception e) {
			logger.error("failed stopping simulator", e);
		}
		return ResponseEntity.ok("simulator on port " + port + " was deleted");
	}
	
	@RequestMapping(value = "isAlive", method = {RequestMethod.GET})
	public ResponseEntity isAlive(@PathVariable("port") final int port) {
		boolean isAlive;
		
		if (!simulatorService.simulatorExists(port)) {
			isAlive = false;
		} else {	
			isAlive = true;
		}
		String isAliveStr = String.valueOf(isAlive);
		String msg = "simulator on port " + port +  " |  is alive=" + isAliveStr;
		logger.info(msg);
		return ResponseEntity.ok(isAliveStr);

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
		List<SimulatorResponse> responses = null;
		String defaultLocation = "/responses/" + fileName;
		InputStream is = getClass().getResourceAsStream(defaultLocation);
		
		try {
			responses = objectMapper.readValue(is, new TypeReference<List<SimulatorResponse>>() {});
		} catch (IOException e) {
			logger.error("failed loading simulatorResponses from default file",	e);
		}

		return responses;
	}


}

	
