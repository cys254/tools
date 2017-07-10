package com.cisco.oss.tools.hardware.machine;

import com.cisco.oss.tools.hardware.CpuSampler;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import oshi.json.SystemInfo;
import oshi.json.hardware.CentralProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Created by igreenfi on 7/5/2017.
 */
@Component
@Profile("vmCPU")
@Slf4j
public class VmCpuSampler extends Thread implements CpuSampler {

    private static final String User = "User"; //0
    private static final String Nice = "Nice";  //1
    private static final String System = "System"; //2
    private static final String Idle = "Idle";  //3
    private static final String IOwait = "IOwait"; //4
    private static final String IRQ = "IRQ"; //5
    private static final String SoftIRQ = "SoftIRQ"; //6

    private final CentralProcessor processor = new SystemInfo().getHardware().getProcessor();

    private final AtomicDouble cpuUsage = new AtomicDouble(0);

    private boolean stop = false;

    @PostConstruct
    public void init() {
        log.info("Start VmCpuSampler.");
        this.setDaemon(true);
        this.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stop VmCpuSampler.");
        stop = true;
    }


    @Override
    public void run() {
        while (!stop && !this.isInterrupted()) {
            final double systemCpuLoadBetweenTicks = processor.getSystemCpuLoadBetweenTicks();

            cpuUsage.set(systemCpuLoadBetweenTicks);

            log.trace("CPU usage: {}", systemCpuLoadBetweenTicks);

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.trace(e.toString());
            }
        }
    }

    @Override
    public double getCpuUsage() {
        return this.cpuUsage.get();
    }
}
