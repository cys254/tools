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


import com.google.common.collect.Multimap;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.util.List;

public class SimulatorRequest {

	private String method;
	private String path;
	private MultiValueMap<String, String> queryParameters;
	private HttpHeaders requestHeaders;
	private String body;
	
	public SimulatorRequest(String method, String path, MultiValueMap<String, String> queryParameters,
							HttpHeaders requestHeaders, String body) {
		
		this.method = method;
		this.path = path;
		this.body = body;
		this.queryParameters = queryParameters;
		this.requestHeaders = requestHeaders;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public MultiValueMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	public void setQueryParameters(MultiValueMap<String, String> queryParameters) {
		this.queryParameters = queryParameters;
	}

	public HttpHeaders getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(HttpHeaders requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Method: " + method + System.lineSeparator() +
				"Path: " + path + System.lineSeparator() +
				"Body: " + body + System.lineSeparator() + 
				"Query Parameters: " + queryParameters + System.lineSeparator());
		
		return builder.toString();
	}
	
}
