package com.cisco.oss.tools;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.TcpPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class PacketDumper {
    private static final ExecutorService pool = Executors.newCachedThreadPool(new CustomizableThreadFactory("pkt-rdr-"));

    private static final String TIMESTAMP = "ts";
    private static final String DST_ADDR = "dst";
    private static final String SRC_ADDR = "src";
    private static final String DST_PORT = "dstPort";
    private static final String SRC_PORT = "srcPort";
    private static final String R_LINE = "l";
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

    private final BlockingQueue<Map<String, Object>> queue;
    private final PcapConfiguration configuration;

    @Autowired
    public PacketDumper(BlockingQueue<Map<String, Object>> queue, PcapConfiguration configuration) {
        this.queue = queue;
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() {
        try {
            Pcaps.findAllDevs().stream()
                    .filter(item -> configuration.getDevicePrefix().stream().anyMatch(prefix -> item.getName().startsWith(prefix)))
                    .forEach(item -> initDump(item));
        } catch (PcapNativeException e) {
            log.error("Failed to list NICs {}", e.toString(), e);
            throw new IllegalStateException(e);
        }
    }

    public void initDump(PcapNetworkInterface item) {
        pool.execute(new ListenTask(item, queue));
    }


    private class ListenTask implements Runnable {

        private final PcapNetworkInterface networkInterface;
        private final Queue<Map<String, Object>> queue;

        public ListenTask(PcapNetworkInterface networkInterface, Queue<Map<String, Object>> queue) {
            this.networkInterface = networkInterface;
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                /**
                 * snaplen: 262144 tcpdump default.
                 */
                final PcapHandle pcapHandle = networkInterface.openLive(262144, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
                pcapHandle.setFilter(configuration.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

                PacketListener listener = packet -> {
                    final Timestamp timestamp = pcapHandle.getTimestamp();

                    final Map<String, Object> data = new HashMap<>();
                    data.put(TIMESTAMP, timestamp.getTime());

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

                        data.put(DST_PORT, dstPort.valueAsString());
                        data.put(SRC_PORT, srcPort.valueAsString());

                        try {
                            String type = REQUEST;
                            final byte[] rawData = tcpPacket.getPayload().getRawData();

                            final String firstLine = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rawData), DEFAULT_CHARSET)).readLine();

                            if (firstLine.startsWith(HTTP_1)) {
                                type = RESPONSE;
                            }

                            data.put(R_LINE, firstLine);
                            data.put(TYPE, type);
                        } catch (IOException e) {
                            log.error(e.toString(), e);
                        }

                        data.put(SEQUENCE_NUMBER, tcpPacket.getHeader().getSequenceNumberAsLong());
                        data.put(ACKNOWLEDGMENT_NUMBER, tcpPacket.getHeader().getAcknowledgmentNumberAsLong());

                        log.debug(COLLECTED_DATA_MESSAGE, data);
                        queue.add(data);
                    }

                };

                pcapHandle.loop(-1, listener);
            } catch (PcapNativeException e) {
                log.error(e.toString(), e);
            } catch (NotOpenException e) {
                log.error(e.toString(), e);
            } catch (InterruptedException e) {
                log.error(e.toString(), e);
            }
        }
    }
}
