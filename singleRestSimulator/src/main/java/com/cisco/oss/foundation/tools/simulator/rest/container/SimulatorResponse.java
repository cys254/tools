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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.MultiValueMap;

public class SimulatorResponse {

    private Logger logger = LoggerFactory.getLogger(SimulatorResponse.class);

    private String expectedMethod;
    private Map<String, List<Pattern>> expectedQueryParams;
    private Map<String, String> expectedHeaders;
    private Pattern expectedBodyPattern;
    private int responseCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private long latencyMs;
    private Pattern expectedUrlPattern;

    public Map<String, String> getExpectedHeaders() {
        return expectedHeaders;
    }

    public void setExpectedHeaders(Map<String, String> expectedHeaders) {
        this.expectedHeaders = expectedHeaders;
    }

    public String getExpectedBody() {
        if (expectedBodyPattern != null) {
            return expectedBodyPattern.pattern();
        } else {
            return "";
        }
    }

    public void setExpectedBody(String expectedBody) {

        if (expectedBody == null || expectedBody.isEmpty()) {
            expectedBody = "";
        }

        this.expectedBodyPattern = Pattern.compile(expectedBody);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Pattern getExpectedUrlPattern() {
        return expectedUrlPattern;
    }

    public void setExpectedUrlPattern(Pattern pattern) {
        this.expectedUrlPattern = pattern;
    }

    public void setExpectedUrl(String expectedUrl) {
        expectedUrl = removeFirstAndLastSlashesFromUrl(expectedUrl);

        Pattern urlPattern = Pattern.compile(expectedUrl);
        this.expectedUrlPattern = urlPattern;
    }

    private String removeFirstAndLastSlashesFromUrl(String expectedUrl) {
        // will remove the first and last '/'
        if (expectedUrl.startsWith("/")) {
            expectedUrl = expectedUrl.substring(1);
        }
        if (expectedUrl.endsWith("/")) {
            expectedUrl = expectedUrl.substring(0, expectedUrl.length() - 1);
        }
        return expectedUrl;
    }

    public Map<String, List<Pattern>> getExpectedQueryParams() {
        return expectedQueryParams;
    }

    public void setExpectedQueryParams(Map<String, List<String>> expectedQueryParamsParam) {

        if (expectedQueryParams == null) {
            expectedQueryParams = new HashMap<String, List<Pattern>>();
        }

        if (expectedQueryParamsParam != null)
            for (String key : expectedQueryParamsParam.keySet()) {
                List<String> expectedValuesOfParam = expectedQueryParamsParam.get(key);

                List<Pattern> expectedList = new ArrayList<Pattern>();

                for (String value : expectedValuesOfParam) {
                    Pattern queryParamPattern = Pattern.compile(value);
                    expectedList.add(queryParamPattern);
                }
                this.expectedQueryParams.put(key, expectedList);
            }
    }

    public String getExpectedMethod() {
        return expectedMethod;
    }

    public void setExpectedMethod(String expectedMethod) {
        this.expectedMethod = expectedMethod;
    }

    public boolean isRequestValid(SimulatorRequest simulatorRequest) {

        if (!isValidMethod(simulatorRequest.getMethod())) {
            return false;
        }

        if (!isPathValid(simulatorRequest.getPath())) {
            return false;
        }

        if (!isQueryParamsValid(simulatorRequest.getQueryParameters())) {
            return false;
        }

        if (!isBodyValid(simulatorRequest.getBody())) {
            return false;
        }

        if (!isHeadersValid(simulatorRequest.getRequestHeaders())) {
            return false;
        }

        return true;

    }

    private boolean isValidMethod(String method) {

        if (!method.equals(expectedMethod)) {
            return false;
        }
        return true;
    }

    /**
     * if the path is valid - the simulator will already replace the
     * response with the path params of the url
     */
    private boolean isPathValid(String actualPath) {
        return expectedUrlPattern.matcher(actualPath).matches();
    }

    private boolean isHeadersValid(HttpHeaders actualHeaders) {

        if (expectedHeaders == null || expectedHeaders.isEmpty()) {
            return true;
        }

        return areAllValuesInMultimap(expectedHeaders, actualHeaders);
    }

    private boolean isQueryParamsValid(MultiValueMap<String, String> actualQueryParams) {

        //no params are expected - return true
        if (expectedQueryParams == null || expectedQueryParams.isEmpty()) {
            return true;
        }

        return areAllQueryParamsValid(actualQueryParams);
    }

    private boolean areAllQueryParamsValid(MultiValueMap<String, String> actualQueryParams) {

        //run on all query-params and check if the url has them
        for (String expectedQueryKey : expectedQueryParams.keySet()) {

            List<String> actualQueryValues = actualQueryParams.get(expectedQueryKey);

            if (CollectionUtils.isEmpty(actualQueryValues)) {
                return false;
            }

            boolean exists = isOneQueryParamValid(expectedQueryKey, expectedQueryParams, actualQueryValues);

            if (!exists) {
                return false;
            }
        }
        return true;
    }

    /**
     * if we have more than 1 param for the same key, we treat it as or
     * if the simulator is defined:
     * ...
     * "expectedQueryParams":{"aaa":["2","3"]},
     * ..
     * so www.google.com?a=2 wiil be fine and also www.google.com?a=2
     */
    private boolean isOneQueryParamValid(String expectedQueryKey,
                                         Map<String, List<Pattern>> expectedMap, List<String> actualQueryValues) {

        List<Pattern> expectedParamList = expectedMap.get(expectedQueryKey);

        for (String value : actualQueryValues) {
            for (Pattern expectedParam : expectedParamList) {
                Matcher matcher = expectedParam.matcher(value);
                if (matcher.matches()) {
                    /*String replaceStr = "{@" + expectedQueryKey + "}";
					if (responseBody.contains(replaceStr)) {						
						responseBody = StringUtils.replace(responseBody, replaceStr,  matcher.group(1));
					}*/
                    return true;
                }
            }
        }
        return false;
    }

    private boolean areAllValuesInMultimap(Map<String, String> expectedMap, HttpHeaders multiMap) {

        for (String expectedHeaderKey : expectedMap.keySet()) {
            List<String> actualHeaderValues = multiMap.get(expectedHeaderKey);

            if (CollectionUtils.isEmpty(actualHeaderValues)) {
                return false;
            }

            String expectedKey = expectedMap.get(expectedHeaderKey);

            if (!actualHeaderValues.contains(expectedKey)) {
                return false;
            }

        }
        return true;
    }

    private boolean isBodyValid(String body) {
        if (expectedBodyPattern != null) {
            //remove all the 'new-lines' from the body
            String bodyWithOutNewLines = body.replaceAll("[\\r\\n]+", "");
            return expectedBodyPattern.matcher(bodyWithOutNewLines).matches();
        } else {
            return true;
        }
    }

    public ResponseEntity generateResponse(SimulatorRequest simulatorRequest) {

        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            logger.error("Failed sleeping", e);
        }

        HttpHeaders headers = new HttpHeaders();

        if (responseHeaders != null) {
            for (String headerKey : responseHeaders.keySet()) {
                headers.put(headerKey, Lists.newArrayList(responseHeaders.get(headerKey)));
            }
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(responseCode).headers(headers);
        ResponseEntity re = null;
        if (!StringUtils.isEmpty(responseBody)) {
            String currentResponseBody = responseBody;

            //replace path param
            Matcher matcher = expectedUrlPattern.matcher(simulatorRequest.getPath());
            if (matcher.find()) {
                int matches = matcher.groupCount();
                //begin from 1 (0 is all the string)
                for (int i = 1; i < matches + 1; i++) {
                    currentResponseBody = StringUtils.replace(currentResponseBody, "{$" + i + "}", matcher.group(i));
                }
            }

            MultiValueMap queryParameters = simulatorRequest.getQueryParameters();

            //replace query params
            currentResponseBody = replaceAllQueryParamsInReponse(currentResponseBody, queryParameters);

            re = builder.body(currentResponseBody);
        } else
            re = builder.build();


        return re;
    }

    private String replaceAllQueryParamsInReponse(String currentResponseBody,
                                                  MultiValueMap<String, String> actualQueryParams) {

        if ((actualQueryParams == null || actualQueryParams.isEmpty()) || (expectedQueryParams == null || expectedQueryParams.isEmpty())) {
            return currentResponseBody;
        }

        for (String expectedQueryKey : expectedQueryParams.keySet()) {

            List<String> actualQueryValues = actualQueryParams.get(expectedQueryKey);

            currentResponseBody = replaceOneQueryParam(expectedQueryKey, expectedQueryParams, actualQueryValues, currentResponseBody);
        }

        return currentResponseBody;
    }

    private String replaceOneQueryParam(String expectedQueryKey, Map<String, List<Pattern>> expectedMap, List<String> actualQueryValues, String currentResponseBody) {

        List<Pattern> expectedParamList = expectedMap.get(expectedQueryKey);

        for (String value : actualQueryValues) {
            for (Pattern expectedParam : expectedParamList) {
                Matcher matcher = expectedParam.matcher(value);
                if (matcher.matches()) {
                    String replaceStr = "{@" + expectedQueryKey + "}";
                    if (currentResponseBody.contains(replaceStr)) {
                        currentResponseBody = StringUtils.replace(currentResponseBody, replaceStr, matcher.group(1));
                    }
                }
            }
        }
        return currentResponseBody;
    }

    @Override
    public String toString() {
        return "SimulatorResponse [expectedMethod=" + expectedMethod
                + ", expectedQueryParams=" + expectedQueryParams
                + ", expectedHeaders=" + expectedHeaders
                + ", expectedBodyPattern=" + expectedBodyPattern
                + ", responseCode=" + responseCode + ", responseHeaders="
                + responseHeaders + ", responseBody=" + responseBody
                + ", latencyMs=" + latencyMs + ", expectedUrlPattern="
                + expectedUrlPattern + "]";
    }

}
