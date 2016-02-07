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

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.io.Writer;

/**
 * this is the resource of the next-response of the simulator
 */
@RestController
@RequestMapping("/simulator/{port}/nextResponse")
@Scope("request")
public class SimulatorNextResponseResource {

	private static ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger  = LoggerFactory.getLogger(SimulatorNextResponseResource.class);
	
	private SimulatorService simulatorService;

	public SimulatorNextResponseResource() {		
		simulatorService = SimulatorService.getInstance();
	}
	
	@RequestMapping(method = {RequestMethod.POST})
	public ResponseEntity updateNextResponseForSimulator(@PathVariable("port") final int port, @RequestBody final SimulatorResponse simulatorNextResponse) {
		
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not set next response for simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
//		SimulatorResponse simulatorNextResponse;
//		try {
//			simulatorNextResponse = objectMapper.readValue(simulatorResponseStr, SimulatorResponse.class);
//		} catch (Exception e) {
//			logger.error("failed parsing json request", e);
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
		boolean added = simulatorService.addNextResponseToSimulator(port, simulatorNextResponse);
		
		ResponseEntity re;
		if (added) {
			re = ResponseEntity.ok().build();
		} else {
			re = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		return re;
	}

	@RequestMapping(method = {RequestMethod.DELETE})
	public ResponseEntity<String> deleteSimulator(@PathVariable("port") final int port) {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not delete next response for simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}
		
		try {
			simulatorService.deleteNextResponseForSimulator(port);
		} catch (Exception e) {
			logger.error("failed deleting next response for simulator", e);
		}
		return ResponseEntity.ok("next response for simulator on port " + port + " was deleted");
	}
	
	@RequestMapping(method = {RequestMethod.GET})
	public ResponseEntity<String> getSimulator(@PathVariable("port") final int port) throws Exception {
		if (!simulatorService.simulatorExists(port)) {
			String msg = "can not retrieve simulator. simulator on port " + port +  " doesn't exist";
			logger.error(msg);
			return ResponseEntity.badRequest().body(msg);
		}

		SimulatorResponse simulatorNextResponse = simulatorService.getSimulator(port).getSimulatorNextResponse();
		
		String simulatorNextResponseStr = "";
		
		if (simulatorNextResponse != null) {	
			Writer strWriter = new StringWriter();
			objectMapper.writeValue(strWriter, simulatorNextResponse);
			
			simulatorNextResponseStr = strWriter.toString();
		}

		return ResponseEntity.ok(simulatorNextResponseStr);
	}
}
