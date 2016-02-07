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

package com.cisco.oss.foundation.tools.simulator.rest.service;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.StringWriter;
import java.io.Writer;

/**
 * this is for helping us converting a SimulatorResponse to json (it's only for testing etc.) 
 *
 */
public class SimulatorResponseToJson {

	public static void main(String args[]) throws Exception {
		
		
//		SimulatorResponse response = new SimulatorResponse();
//
//		Map<String, String> expectedHeaders = new HashMap<String, String>();
//		expectedHeaders.put("Content-Type", "application/json");
//		expectedHeaders.put("Sync-Mode", "true");
//
//		Map<String, String> responseHeaders = new HashMap<String, String>();
//		responseHeaders.put("Location", "www.google.com");
//		responseHeaders.put("Content-Length", "0");
//
//        ListMultimap<String,String> queryParams = ArrayListMultimap.create();
////		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
//
//		List<String> list = new ArrayList<String>();
//		list.add("111");
//		list.add("222");
//		queryParams.put("param1",list);
//		queryParams.putSingle("param2", "222");
//
//		response.setExpectedBody("expectedBodyStr");
//		response.setExpectedHeaders(expectedHeaders);
//
//		response.setLatencyMs(0);
//		response.setResponseBody("responseBodyStr");
//		response.setResponseCode(200);
//		response.setResponseHeaders(responseHeaders);
//
//		response.setExpectedUrl("expected/url");
//		response.setExpectedMethod("GET");
//		response.setExpectedQueryParams(queryParams);
//
//		String jsonStr = getObjectAsJson(response);
//		System.out.println(jsonStr);
	
	}
	

    public static String getObjectAsJson(Object object) throws Exception {
    	
        Writer strWriter = new StringWriter();
        new ObjectMapper().writeValue(strWriter, object);
        
        return strWriter.toString();
       
    }
}
