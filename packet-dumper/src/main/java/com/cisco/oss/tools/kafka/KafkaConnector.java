package com.cisco.oss.tools.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Component
@Profile("kafka")
@Slf4j
public class KafkaConnector extends Thread {

    private static final String HTTP_DATA = "http.data.t";

    private final BlockingQueue<Map<String, Object>> queue;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private boolean stop = false;

    @Autowired
    public KafkaConnector(BlockingQueue<Map<String, Object>> queue, KafkaTemplate<String, Map<String, Object>> kafkaTemplate) {
        super("kafkaConnector");
        this.queue = queue;
        this.kafkaTemplate = kafkaTemplate;
    }

    private void send(String topic, Map<String, Object> message) {
        // the KafkaTemplate provides asynchronous send methods returning a Future
        ListenableFuture<SendResult<String, Map<String, Object>>> future = kafkaTemplate.send(topic, message);

        // register a callback with the listener to receive the result of the send asynchronously
        future.addCallback(new ListenableFutureCallback<SendResult<String, Map<String, Object>>>() {

            @Override
            public void onSuccess(SendResult<String, Map<String, Object>> result) {
                log.info("sent message='{}' with offset={}", message,
                        result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("unable to send message='{}'", message, ex);
            }
        });

    }

    @PostConstruct
    public void init() {
        log.info("Start KAFKA connector.");
        this.setDaemon(true);
        this.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stop KAFKA producer.");
        stop = true;
    }

    @Override
    public void run() {
        while (!stop && !this.isInterrupted()) {
            try {
                final Map<String, Object> item = queue.take();
                send(HTTP_DATA, item);
            } catch (InterruptedException e) {
                log.trace(e.toString(), e);
            }
        }
    }
}