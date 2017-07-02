package com.cisco.oss.tools.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.cisco.oss.tools.processor.Constants.*;

/**
 * Created by igreenfi on 6/28/2017.
 */
@Component
@Profile("vm")
@Slf4j
public class VMPacketProcessingFilter implements IPacketProcessingFilter {

    public VMPacketProcessingFilter() {
        log.info("VMPacketProcessingFilter in use.");
    }

    @Override
    public boolean filter(Map<String, Object> data) {

        Object captureInterfaceIp = data.get(INTERFACE_IP);

        Object destHost = data.get(DST_ADDR);
        Object srcHost = data.get(SRC_ADDR);

        String txType = (String) data.get(TYPE);

        log.debug("txType: {}, captureInterfaceIp: {},srcHost: {} destHost: {}", txType, captureInterfaceIp, srcHost, destHost);

        if (txType == null)
            return false;

        switch (txType) {
            case REQUEST: {
                return captureInterfaceIp.equals(destHost);
            }
            case RESPONSE: {
                return captureInterfaceIp.equals(srcHost) ;
            }
            default: {
                log.error("Got unexpected transaction type. Packet data: {}", data);
                return false;
            }

        }
    }
}
