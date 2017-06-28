package com.cisco.oss.tools.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Created by Yair Ogen (yaogen) on 28/06/2017.
 */
public class PodData {

    private final String nodeName;
    private final String podIP;
    private final List<Integer> ports;

    public PodData(String nodeName, String podIP, List<Integer> ports) {
        this.nodeName = nodeName;
        this.podIP = podIP;
        this.ports = ports;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getPodIP() {
        return podIP;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    @Override
    public String toString() {
//            return ToStringBuilder.reflectionToString(this);
        return new ToStringBuilder(this)
                .append("nodeName", nodeName)
                .append("podIP", podIP)
                .append("ports", ports)
                .toString();
    }

}
