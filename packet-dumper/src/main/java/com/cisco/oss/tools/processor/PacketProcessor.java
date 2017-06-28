package com.cisco.oss.tools.processor;

import com.cisco.oss.tools.model.PacketContainer;
import com.cisco.oss.tools.model.PodDatas;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by igreenfi on 6/26/2017.
 */
@Component
@Slf4j
public class PacketProcessor extends Thread {

    private static final String TIMESTAMP = "ts";
    private static final String DST_ADDR = "dst";
    private static final String SRC_ADDR = "src";
    private static final String DST_PORT = "dstPort";
    private static final String SRC_PORT = "srcPort";
    private static final String R_LINE = "l";
    private static final String INTERFACE_IP = "ipv4";
    private static final String LENGTH = "len";
    private static final String TYPE = "t";
    private static final String SEQUENCE_NUMBER = "seqNum";
    private static final String ACKNOWLEDGMENT_NUMBER = "ackNum";

    private static final String REQUEST = "REQUEST";
    private static final String RESPONSE = "RESPONSE";

    private static final String HTTP_1 = "HTTP/1.";
    private static final String HTTP_2 = "HTTP/2.";
    private static final String COLLECTED_DATA_MESSAGE = "collected data: {}";
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();


    @Autowired
    private BlockingQueue<PacketContainer> packetQueue;

    @Autowired
    private BlockingQueue<Map<String, Object>> dataQueue;

    @Autowired(required = false)
    private PodDatas podDatas;

    private boolean stop = false;

    public PacketProcessor() {
        super("packetProcessor");
    }

    @PostConstruct
    public void init() {
        log.info("Start PacketProcessor.");
        this.setDaemon(true);
        this.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stop PacketProcessor.");
        stop = true;
    }

    @Override
    public void run() {

        while (!stop && !this.isInterrupted()) {
            try {
                final PacketContainer container = packetQueue.take();

                final Map<String, Object> data = new HashMap<>();
                data.put(TIMESTAMP, container.getTimestamp());
                data.put(INTERFACE_IP, container.getIp());
                final Packet packet = container.getPacket();

                if (packet instanceof EthernetPacket) {
                    final IpPacket ipPacket = (IpPacket) packet.getPayload();

                    final InetAddress dstAddr = ipPacket.getHeader().getDstAddr();
                    final InetAddress srcAddr = ipPacket.getHeader().getSrcAddr();
                    data.put(DST_ADDR, dstAddr.getHostAddress());
                    data.put(SRC_ADDR, srcAddr.getHostAddress());

                    final TcpPacket tcpPacket = (TcpPacket) ipPacket.getPayload();
                    final TcpPort dstPort = tcpPacket.getHeader().getDstPort();
                    final TcpPort srcPort = tcpPacket.getHeader().getSrcPort();

                    final int tcpPayloadLength = ipPacket.length() - (20 + tcpPacket.getHeader().getDataOffsetAsInt() * 4);
                    data.put(LENGTH, tcpPayloadLength);

                    data.put(DST_PORT, dstPort.value().intValue());
                    data.put(SRC_PORT, srcPort.value().intValue());

                    try {
                        String type = REQUEST;
                        final byte[] rawData = tcpPacket.getPayload().getRawData();

                        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), DEFAULT_CHARSET));
                        final String firstLine = bufferedReader.readLine();

                        if (firstLine.startsWith(HTTP_1)) {
                            type = RESPONSE;
                        }

                        data.put(R_LINE, firstLine);

                        List<String> headers = new ArrayList<>();
                        String line = bufferedReader.readLine();
                        while (StringUtils.isNoneBlank(line)) {
                            headers.add(line);
                            line = bufferedReader.readLine();
                        }

                        data.put("headers", headers);

                        data.put(TYPE, type);
                    } catch (IOException e) {
                        log.error(e.toString(), e);
                    }

                    data.put(SEQUENCE_NUMBER, tcpPacket.getHeader().getSequenceNumberAsLong());
                    data.put(ACKNOWLEDGMENT_NUMBER, tcpPacket.getHeader().getAcknowledgmentNumberAsLong());

                    log.debug(COLLECTED_DATA_MESSAGE, data);

                    if (shouldProcess(data)) {
                        dataQueue.add(data);
                    }


                }
            } catch (InterruptedException e) {
                log.trace(e.toString());
            }
        }

    }

    private boolean shouldProcess(Map<String, Object> data) {
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
