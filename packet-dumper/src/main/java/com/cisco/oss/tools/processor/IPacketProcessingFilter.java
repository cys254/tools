package com.cisco.oss.tools.processor;

import java.util.Map;

/**
 * Created by igreenfi on 6/28/2017.
 */
public interface IPacketProcessingFilter {
    boolean filter(Map<String, Object> data);
}
