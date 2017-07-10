package com.cisco.oss.tools;

import com.cisco.oss.tools.model.PacketContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.pcap4j.packet.Packet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;
import java.util.concurrent.*;

@SpringBootApplication
@ComponentScan({"com.cisco.oss"})
@EnableAsync
@EnableScheduling
public class PacketDumperApplication {

    public static void main(String[] args) {
        SpringApplication.run(PacketDumperApplication.class, args);
    }

    @Bean
    public BlockingQueue<Map<String, Object>> queue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BlockingQueue<PacketContainer> packetQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public ScheduledExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(1);
    }
}
