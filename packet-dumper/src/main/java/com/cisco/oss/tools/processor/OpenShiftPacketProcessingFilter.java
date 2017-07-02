package com.cisco.oss.tools.processor;

import com.cisco.oss.tools.model.PodDatas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.cisco.oss.tools.processor.Constants.*;

/**
 * Created by igreenfi on 6/28/2017.
 */
@Component
@Profile("openshift")
@Slf4j
public class OpenShiftPacketProcessingFilter implements IPacketProcessingFilter {

    public OpenShiftPacketProcessingFilter() {
        log.info("OpenShiftPacketProcessingFilter in use.");
    }

    @Autowired
    private PodDatas podDatas;

    @Override
    public boolean filter(Map<String, Object> data) {

        Object destHost = data.get(DST_ADDR);
        Object srcHost = data.get(SRC_ADDR);

        Object destPort = data.get(DST_PORT);
        Object srcPort = data.get(SRC_PORT);

        String txType = (String) data.get(TYPE);
        switch (txType) {
            case REQUEST: {
                return podDatas.getPodByPodIp().containsKey(destHost) && podDatas.getPodByPodIp().get(destHost).getPorts().contains(destPort);
            }
            case RESPONSE: {
                return podDatas.getPodByPodIp().containsKey(srcHost) && podDatas.getPodByPodIp().get(srcHost).getPorts().contains(srcPort);
            }
            default: {
                log.error("Got unexpected transaction type. Packet data: {}", data);
                return false;
            }

        }
    }
}
