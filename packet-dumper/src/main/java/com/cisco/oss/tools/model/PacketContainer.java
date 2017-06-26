package com.cisco.oss.tools.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pcap4j.packet.Packet;

/**
 * Created by igreenfi on 6/26/2017.
 */
public class PacketContainer {
    private final Long timestamp;
    private final String ip;
    private final Packet packet;

    public PacketContainer(Long timestamp, String ip, Packet packet) {
        this.timestamp = timestamp;
        this.ip = ip;
        this.packet = packet;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getIp() {
        return ip;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("timestamp", timestamp)
                .append("packet", packet)
                .toString();
    }
}
