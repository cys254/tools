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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.sun.xml.internal.ws.client.sei.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.cisco.oss.foundation.tools.simulator.rest.service.SimulatorService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * this is the resource of the simulator itself
 * all the REST calls of the simulator will get here
 */
@RestController
@RequestMapping("/")
@Scope("request")
public class SimulatorResource {

    private static Logger logger = LoggerFactory.getLogger(SimulatorResource.class);

    private static final String subResourcesPath = "{subResources: [.:!a-zA-Z0-9~%_/-]+}";
    private SimulatorService simulatorService;

    public SimulatorResource() {
        simulatorService = SimulatorService.getInstance();
    }

    @RequestMapping(value = subResourcesPath, method = {RequestMethod.GET})
    public ResponseEntity getResource(HttpServletRequest httpServletRequest, HttpHeaders headers,
                                      @PathVariable("subResources") String subResources, @RequestParam MultiValueMap<String, String> queryParams, @RequestBody String body) {

        logMethod(RequestMethod.GET.name(), httpServletRequest.getRequestURI(), httpServletRequest.getQueryString(), body);

        ResponseEntity re = simulatorService.retrieveResponse(RequestMethod.GET.name(), httpServletRequest, headers, queryParams, body);

//		ResponseEntity ResponseEntity = rb.build();
        logResponse(RequestMethod.GET.name(), re);
        return re;

    }

    @RequestMapping(value = subResourcesPath, method = {RequestMethod.PUT})
    public ResponseEntity updateResource(HttpServletRequest httpServletRequest, HttpHeaders headers,
                                         @PathVariable("subResources") String subResources, @RequestParam MultiValueMap<String, String> queryParams, @RequestBody String body) {

        logMethod(RequestMethod.PUT.name(), httpServletRequest.getRequestURI(), httpServletRequest.getQueryString(), body);
        ResponseEntity re = simulatorService.retrieveResponse(RequestMethod.PUT.name(), httpServletRequest, headers, queryParams, body);
        logResponse(RequestMethod.PUT.name(), re);
        return re;
    }

    @RequestMapping(value = subResourcesPath, method = {RequestMethod.POST})
    public ResponseEntity createResource(HttpServletRequest httpServletRequest, HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
                                         @PathVariable("subResources") String subResources, @RequestBody String body) {

        logMethod(RequestMethod.POST.name(), httpServletRequest.getRequestURI(), httpServletRequest.getQueryString(), body);

        ResponseEntity re = simulatorService.retrieveResponse(RequestMethod.POST.name(), httpServletRequest, headers, queryParams, body);

        logResponse(RequestMethod.POST.name(), re);
        return re;
    }

    @RequestMapping(value = subResourcesPath, method = {RequestMethod.DELETE})
    public ResponseEntity deleteResource(HttpServletRequest httpServletRequest, HttpHeaders headers,
                                         @PathVariable("subResources") String subResources, @RequestParam MultiValueMap<String, String> queryParams, @RequestBody String body) {

        logMethod(RequestMethod.DELETE.name(), httpServletRequest.getRequestURI(), httpServletRequest.getQueryString(), body);
        ResponseEntity re = simulatorService.retrieveResponse(RequestMethod.DELETE.name(), httpServletRequest, headers, queryParams, body);

        logResponse(RequestMethod.DELETE.name(), re);
        return re;
    }

    private void logMethod(String method, String uriInfo, String queryParams, String body) {
        try {
//		String queryParams = uriInfo.getQueryParameters() == null ? "" : getQueryParamsStringForLogging(uriInfo.getQueryParameters());
            logger.debug("Simulator recieved a " + method + " request | "
                    + "path: " + uriInfo + " | "
                    + "queryParams: " + queryParams + "| "
                    + "body: " + body);
        } catch (Exception e) {
            logger.error("failed writing to log");
        }

    }

    private void logResponse(String method, ResponseEntity response) {
        logger.debug("ResponseEntity for " + method + " method: " + response.getStatusCode().value());
    }

//	private String getQueryParamsStringForLogging(MultivaluedMap<String, String> multivaluedMap) {
//		
//		StringBuilder sb = new StringBuilder();
//		
//		for (String key : multivaluedMap.keySet()) {
//			List<String> values = multivaluedMap.get(key);
//			sb.append(key).append("=[");
//			
//			for (String value : values) {
//				sb.append(value).append(",");
//			}
//			sb.replace(sb.length()-1, sb.length(), "");
//			sb.append("] ");
//		}
//		
//		return sb.toString();
//	}

}
