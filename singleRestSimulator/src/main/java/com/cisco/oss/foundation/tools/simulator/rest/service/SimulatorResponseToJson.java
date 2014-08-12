package com.cisco.oss.foundation.tools.simulator.rest.service;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;

import com.cisco.oss.foundation.tools.simulator.rest.container.SimulatorResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * this is for helping us converting a SimulatorResponse to json (it's only for testing etc.) 
 *
 */
public class SimulatorResponseToJson {

	public static void main(String args[]) throws Exception {
		
		
		SimulatorResponse response = new SimulatorResponse();
		
		Map<String, String> expectedHeaders = new HashMap<String, String>();
		expectedHeaders.put("Content-Type", "application/json");
		expectedHeaders.put("Sync-Mode", "true");
		
		Map<String, String> responseHeaders = new HashMap<String, String>();
		responseHeaders.put("Location", "www.google.com");
		responseHeaders.put("Content-Length", "0");
		
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		
		List<String> list = new ArrayList<String>();
		list.add("111");
		list.add("222");
		queryParams.put("param1",list);
		queryParams.putSingle("param2", "222");
		
		response.setExpectedBody("expectedBodyStr");
		response.setExpectedHeaders(expectedHeaders);
		
		response.setLatencyMs(0);
		response.setResponseBody("responseBodyStr");
		response.setResponseCode(200);
		response.setResponseHeaders(responseHeaders);
		
		response.setExpectedUrl("expected/url");
		response.setExpectedMethod("GET");
		response.setExpectedQueryParams(queryParams);
		
		String jsonStr = getObjectAsJson(response);
		System.out.println(jsonStr);
	
	}
	

    public static String getObjectAsJson(Object object) throws Exception {
    	
        Writer strWriter = new StringWriter();
        new ObjectMapper().writeValue(strWriter, object);
        
        return strWriter.toString();
       
    }
}
