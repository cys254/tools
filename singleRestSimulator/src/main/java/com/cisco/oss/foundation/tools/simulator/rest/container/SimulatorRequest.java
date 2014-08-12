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

package com.cisco.oss.foundation.tools.simulator.rest.container;

import javax.ws.rs.core.MultivaluedMap;

public class SimulatorRequest {

	private String method;
	private String path;
	private MultivaluedMap<String, String> queryParameters;
	private MultivaluedMap<String, String> requestHeaders;
	private String body;
	
	public SimulatorRequest(String method, String path, MultivaluedMap<String, String> queryParameters,
			MultivaluedMap<String, String> requestHeaders, String body) {
		
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

	public MultivaluedMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	public void setQueryParameters(MultivaluedMap<String, String> queryParameters) {
		this.queryParameters = queryParameters;
	}

	public MultivaluedMap<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	
}
