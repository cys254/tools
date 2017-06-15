package com.cisco.oss.tools;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by igreenfi on 6/14/2017.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pcap")
public class PcapConfiguration {
    private List<String> devicePrefix;
    /**
     * <h1>Sample filter</h1>
     * <h2>GET</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420
     * <h2>POST</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x504f5354 && tcp[((tcp[12:1] & 0xf0) >> 2) + 4:1] = 0x20
     * <h2>PUT</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x50555420
     * <h2>DELETE</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x44454c45 && tcp[((tcp[12:1] & 0xf0) >> 2) + 4:2] = 0x5445 && tcp[((tcp[12:1] & 0xf0) >> 2) + 6:1] = 0x20
     * <h2>HEAD</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x48454144 && tcp[((tcp[12:1] & 0xf0) >> 2) + 4:1] = 0x20
     * <h2>HTTP RESPONSE</h2>
     * tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x48545450 && tcp[((tcp[12:1] & 0xf0) >> 2) + 4:2] = 0x2f31 && tcp[((tcp[12:1] & 0xf0) >> 2) + 6:1] = 0x2e
     */
    private String filter;

}
