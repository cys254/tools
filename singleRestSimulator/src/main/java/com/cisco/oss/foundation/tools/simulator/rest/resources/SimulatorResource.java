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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;

/**
 * this is the resource of the simulator itself
 * all the REST calls of the simulator will get here
 *
 */
@Component
@Path("/")
@Scope("request")
public class SimulatorResource {

	private static Logger logger = LoggerFactory.getLogger(SimulatorResource.class);
	
	private static final String subResourcesPath = "{subResources: [.:!a-zA-Z0-9~%_/-]+}";
	private SimulatorService simulatorService;

	public SimulatorResource() {
		simulatorService = SimulatorService.getInstance();
	}

	@GET
	@Path(subResourcesPath)
	public Response getResource(@Context HttpServletRequest httpServletRequest, @Context final UriInfo uriInfo, @Context HttpHeaders headers,
			@PathParam("subResources") String subResources, String body) {
		
		logMethod(HttpMethod.GET, uriInfo, body);
		
		ResponseBuilder rb = simulatorService.retrieveResponse(HttpMethod.GET, httpServletRequest, uriInfo, headers, body);

		Response response = rb.build();
		logResponse(HttpMethod.GET, response);
		return response;
	
	}

	@PUT
	@Path(subResourcesPath)
	public Response updateResource(@Context HttpServletRequest httpServletRequest, @Context final UriInfo uriInfo, @Context HttpHeaders headers,
			@PathParam("subResources") String subResources, String body) {
		
		logMethod(HttpMethod.PUT, uriInfo, body);
		ResponseBuilder rb = simulatorService.retrieveResponse(HttpMethod.PUT, httpServletRequest, uriInfo, headers, body);
		Response response = rb.build();
		logResponse(HttpMethod.PUT, response);
		return response;
	}

	@POST
	@Path(subResourcesPath)
	public Response createResource(@Context HttpServletRequest httpServletRequest, @Context final UriInfo uriInfo, @Context HttpHeaders headers,
			@PathParam("subResources") String subResources, String body) {
		
		logMethod(HttpMethod.POST, uriInfo, body);
		
		ResponseBuilder rb = simulatorService.retrieveResponse(HttpMethod.POST, httpServletRequest, uriInfo, headers, body);

		Response response = rb.build();
		logResponse(HttpMethod.POST, response);
		return response;
	}

	@DELETE
	@Path(subResourcesPath)
	public Response deleteResource(@Context HttpServletRequest httpServletRequest, @Context final UriInfo uriInfo, @Context HttpHeaders headers,
			@PathParam("subResources") String subResources, String body) {
		
		logMethod(HttpMethod.DELETE, uriInfo, body);
		ResponseBuilder rb = simulatorService.retrieveResponse(HttpMethod.DELETE, httpServletRequest, uriInfo, headers, body);

		Response response = rb.build();
		logResponse(HttpMethod.DELETE, response);
		return response;
	}
	
	private void logMethod(String method, UriInfo uriInfo, String body) {
		try {
		String queryParams = uriInfo.getQueryParameters() == null ? "" : getQueryParamsStringForLogging(uriInfo.getQueryParameters());
		logger.debug("Simulator recieved a " + method + " request | "
				+ "path: " + uriInfo.getPath() + " | "
				+ "queryParams: " +  queryParams + "| "
				+ "body: " +  body);
		} catch(Exception e) {
			logger.error("failed writing to log");
		}
		
	}

	private void logResponse(String method, Response response) {
		logger.debug("response for " + method + " method: " + response.getStatus());	
	}
	
	private String getQueryParamsStringForLogging(MultivaluedMap<String, String> multivaluedMap) {
		
		StringBuilder sb = new StringBuilder();
		
		for (String key : multivaluedMap.keySet()) {
			List<String> values = multivaluedMap.get(key);
			sb.append(key).append("=[");
			
			for (String value : values) {
				sb.append(value).append(",");
			}
			sb.replace(sb.length()-1, sb.length(), "");
			sb.append("] ");
		}
		
		return sb.toString();
	}
	
}
