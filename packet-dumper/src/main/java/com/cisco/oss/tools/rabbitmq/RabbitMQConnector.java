package com.cisco.oss.tools.rabbitmq;

import com.cisco.oss.foundation.message.MessageProducer;
import com.cisco.oss.foundation.message.RabbitMQMessagingFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by igreenfi on 6/12/2017.
 */
@Component
@Profile("rabbitmq")
@Slf4j
public class RabbitMQConnector extends Thread {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final BlockingQueue<Map<String, Object>> queue;
    private final MessageProducer messageProducer;
    private boolean stop = false;

    @Autowired
    public RabbitMQConnector(BlockingQueue<Map<String, Object>> queue) {
        super("rabbit-producer");
        this.queue = queue;
        this.messageProducer = RabbitMQMessagingFactory.createProducer("packetProducer");
    }

    @PostConstruct
    public void init(){
        log.info("Start rabbitMQ producer.");
        this.setDaemon(true);
        this.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stop rabbitMQ producer.");
        stop = true;
    }

    @Override
    public void run() {
        while (!stop && !this.isInterrupted()) {
            try {
                final Map<String, Object> take = queue.take();
                messageProducer.sendMessage(OBJECT_MAPPER.writeValueAsBytes(take));
            } catch (InterruptedException e) {
                log.trace(e.toString(), e);
            } catch (JsonProcessingException e) {
                log.error(e.toString(), e);
            }
        }
    }
}
