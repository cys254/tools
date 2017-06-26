package com.cisco.oss.tools;

import com.cisco.oss.tools.model.PacketContainer;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.sql.Timestamp;
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

    private final BlockingQueue<PacketContainer> packetQueue;
    private final PcapConfiguration configuration;

    @Autowired
    public PacketDumper(BlockingQueue<PacketContainer> packetQueue, PcapConfiguration configuration) {
        this.packetQueue = packetQueue;
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
        pool.execute(new ListenTask(item, packetQueue));
    }


    private class ListenTask implements Runnable {

        private final PcapNetworkInterface networkInterface;
        private final Queue<PacketContainer> queue;

        public ListenTask(PcapNetworkInterface networkInterface, Queue<PacketContainer> queue) {
            this.networkInterface = networkInterface;
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                final String interfaceIp = networkInterface.getAddresses().stream()
                        .filter(item -> item instanceof PcapIpV4Address)
                        .map(addr -> addr.getAddress().getHostAddress())
                        .findFirst()
                        .orElse("");
                /**
                 * snaplen: 262144 tcpdump default.
                 */
                final PcapHandle pcapHandle = networkInterface.openLive(262144, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 0);
                pcapHandle.setFilter(configuration.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

                PacketListener listener = packet -> {
                    final Timestamp timestamp = pcapHandle.getTimestamp();
                    queue.add(new PacketContainer(timestamp.getTime(), interfaceIp, packet));
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
