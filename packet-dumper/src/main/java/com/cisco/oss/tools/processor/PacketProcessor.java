package com.cisco.oss.tools.processor;

import com.cisco.oss.tools.hardware.CpuSampler;
import com.cisco.oss.tools.model.PacketContainer;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("queueImpl")
@Slf4j
public class PacketProcessor extends Thread {

    private static final String HTTP_1 = "HTTP/1.";
    private static final String HTTP_2 = "HTTP/2.";
    private static final String COLLECTED_DATA_MESSAGE = "collected data: {}";
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    private static final Splitter SPLITTER = Splitter.onPattern("\\s+|:").omitEmptyStrings();

    @Autowired
    private BlockingQueue<PacketContainer> packetQueue;

    @Autowired
    private BlockingQueue<Map<String, Object>> dataQueue;

    @Autowired
    private IPacketProcessingFilter processingFilter;

    @Autowired
    private CpuSampler cpuSampler;

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
                data.put(Constants.TIMESTAMP, container.getTimestamp());
                data.put(Constants.INTERFACE_IP, container.getIp());
                final Packet packet = container.getPacket();

                if (log.isTraceEnabled()) {
                    log.trace(packet.toString());
                }
                if (packet instanceof EthernetPacket) {
                    final IpPacket ipPacket = (IpPacket) packet.getPayload();

                    final InetAddress dstAddr = ipPacket.getHeader().getDstAddr();
                    final InetAddress srcAddr = ipPacket.getHeader().getSrcAddr();
                    data.put(Constants.DST_ADDR, dstAddr.getHostAddress());
                    data.put(Constants.SRC_ADDR, srcAddr.getHostAddress());

                    final TcpPacket tcpPacket = (TcpPacket) ipPacket.getPayload();
                    final TcpPort dstPort = tcpPacket.getHeader().getDstPort();
                    final TcpPort srcPort = tcpPacket.getHeader().getSrcPort();

                    final int tcpPayloadLength = ipPacket.length() - (20 + tcpPacket.getHeader().getDataOffsetAsInt() * 4);
                    data.put(Constants.LENGTH, tcpPayloadLength);

                    data.put(Constants.DST_PORT, dstPort.value().intValue());
                    data.put(Constants.SRC_PORT, srcPort.value().intValue());

                    try {
                        String type = Constants.REQUEST;
                        final byte[] rawData = tcpPacket.getPayload().getRawData();

                        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), DEFAULT_CHARSET));
                        final String firstLine = bufferedReader.readLine();

                        if (firstLine.startsWith(HTTP_1)) {
                            type = Constants.RESPONSE;
                        }

                        data.put(Constants.R_LINE, firstLine);

                        List<String> headers = new ArrayList<>();
                        String line = bufferedReader.readLine();
                        while (StringUtils.isNoneBlank(line)) {
                            if (line.startsWith(Constants.CONTENT_LENGTH)){
                                final int contentLength = Integer.parseInt(SPLITTER.splitToList(line).get(1));
                                data.put(Constants.LENGTH, contentLength);
                            } else {
                                headers.add(line);
                            }
                            line = bufferedReader.readLine();
                        }

                        data.put(Constants.HEADERS, headers);

                        data.put(Constants.CPU, cpuSampler.getCpuUsage());

                        data.put(Constants.TYPE, type);
                    } catch (IOException e) {
                        log.error(e.toString(), e);
                    } catch (NullPointerException e) {
                        log.error(e.toString(), e);
                    }

                    if (processingFilter.filter(data)) {
                        log.debug(COLLECTED_DATA_MESSAGE, data);
                        dataQueue.add(data);
                    } else {
                        log.debug("Skip packet: {}", data);
                    }
                }
            } catch (InterruptedException e) {
                log.trace(e.toString());
            }
        }

    }

}
