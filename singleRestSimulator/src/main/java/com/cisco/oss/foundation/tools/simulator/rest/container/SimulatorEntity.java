package com.cisco.oss.foundation.tools.simulator.rest.container;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.CollectionUtils;

public class SimulatorEntity {

	private int port;
	private List<SimulatorResponse> simulatorResponses;
//	private Server server;
	private List<SimulatorRequest> allRequests;
	
	private SimulatorResponse simulatorNextResponse;
	
	public SimulatorEntity(int port/*, Server server*/) {
		this.port = port;
//		this.server = server;
		simulatorResponses = new ArrayList<SimulatorResponse>();
		allRequests = new ArrayList<SimulatorRequest>();
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

//	public Server getServer() {
//		return server;
//	}
//
//	public void setServer(Server server) {
//		this.server = server;
//	}

	public void addRequestToQueue(SimulatorRequest simulatorRequest) {
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

	public void setAllRequests(List<SimulatorRequest> allRequests) {
		this.allRequests = allRequests;
	}

	public ResponseBuilder generateResponse(SimulatorRequest simulatorRequest) {
		ResponseBuilder rb = null;
		
		if (simulatorNextResponse != null) {
			rb = simulatorNextResponse.generateResponse(simulatorRequest);
			simulatorNextResponse = null;
		} else {
			
			List<SimulatorResponse> urls = getSimulatorResponses();
			
			if (CollectionUtils.isEmpty(urls)) {
				return Response.status(Status.NOT_FOUND);
			}
			
			for (SimulatorResponse simulatorResponse : urls) {		
				if (simulatorResponse.isRequestValid(simulatorRequest)) {
					rb = simulatorResponse.generateResponse(simulatorRequest);
					break;
				}
			}
		}
		
		if (rb == null) {
			rb = Response.status(Status.NOT_FOUND);
		}
		return rb;
	}

	public SimulatorResponse getSimulatorNextResponse() {
		return simulatorNextResponse;
	}

	public void setSimulatorNextResponse(SimulatorResponse simulatorNextResponse) {
		this.simulatorNextResponse = simulatorNextResponse;
	}

	
}
