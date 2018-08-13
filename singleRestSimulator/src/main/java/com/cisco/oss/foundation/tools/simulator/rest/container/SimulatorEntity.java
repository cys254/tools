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

package com.cisco.oss.foundation.tools.simulator.rest.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.oss.foundation.configuration.ConfigurationFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

public class SimulatorEntity {

	private static Logger logger = LoggerFactory.getLogger(SimulatorEntity.class);
	
	private int port;
	private List<SimulatorResponse> simulatorResponses;
	private List<SimulatorRequest> allRequests;
	
	private SimulatorResponse simulatorNextResponse;
	
	public SimulatorEntity(int port) {
		this.port = port;
		simulatorResponses = new ArrayList<SimulatorResponse>();
		allRequests = Collections.synchronizedList(new ArrayList<SimulatorRequest>());
	}

	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public List<SimulatorResponse> getSimulatorResponses() {
		return simulatorResponses;
	}

	public void setSimulatorResponses(List<SimulatorResponse> simulatorResponses) {
		this.simulatorResponses = simulatorResponses;
	}

	public void clearSimulatorResponses() {
		simulatorResponses.clear();
	}
	
	public void addRequestToQueue(SimulatorRequest simulatorRequest) {
		
		int maxQueueSize = ConfigurationFactory.getConfiguration().getInteger("restSimulator.queue.maxSize", 100);
		
		//remove the last one if we are out of space...
		if (maxQueueSize <= allRequests.size()) {		
			allRequests.remove(allRequests.size() -1);
		}
		allRequests.add(0, simulatorRequest);
	}

	public List<SimulatorRequest> getAllRequests() {
		return allRequests;
	}
	
	public List<SimulatorRequest> getLastRequests(int numOfMsg) {
		
		if (CollectionUtils.isEmpty(allRequests)) {
			return allRequests;
		}
		
		if (allRequests.size() >= numOfMsg) {
			return allRequests.subList(0, numOfMsg);
		}
		
		return allRequests;
	}

	public void removeAllRequests() {
		allRequests = new ArrayList<SimulatorRequest>();
	}
	
	public SimulatorRequest removeLastRequest() {
		if (allRequests.size() > 0) {
			SimulatorRequest deletedSimulatorRequest = allRequests.get(0);
			allRequests.remove(0);
			return deletedSimulatorRequest;
		}
		
		return null;
	}

	public void setAllRequests(List<SimulatorRequest> allRequests) {
		this.allRequests = allRequests;
	}

	public ResponseEntity generateResponse(SimulatorRequest simulatorRequest) {
		ResponseEntity re  = null;
		if (simulatorNextResponse != null) {
			re = simulatorNextResponse.generateResponse(simulatorRequest);
			simulatorNextResponse = null;
		} else {
			
			List<SimulatorResponse> simulatorResponses = getSimulatorResponses();
			
			if (CollectionUtils.isEmpty(simulatorResponses)) {
				return ResponseEntity.notFound().build();
			}
			
			long start = System.currentTimeMillis();
			for (SimulatorResponse simulatorResponse : simulatorResponses) {		
				if (simulatorResponse.isRequestValid(simulatorRequest)) {
					re = simulatorResponse.generateResponse(simulatorRequest);
					break;
				}
			}
			long end = System.currentTimeMillis();
			int numOfResponses = simulatorResponses.size();
			
			if (logger.isDebugEnabled()) {
				logger.debug("found response | time for finding the response = " + (end -start) + 
						" milli | number of responses in simulator = " + numOfResponses);
			}
		}
		
		if (re == null) {
			re = ResponseEntity.notFound().build();
		}
		return re;
	}

	public SimulatorResponse getSimulatorNextResponse() {
		return simulatorNextResponse;
	}

	public void setSimulatorNextResponse(SimulatorResponse simulatorNextResponse) {
		this.simulatorNextResponse = simulatorNextResponse;
	}

	
}
