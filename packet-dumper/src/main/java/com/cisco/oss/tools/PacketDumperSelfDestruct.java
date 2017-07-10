package com.cisco.oss.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by Yair Ogen (yaogen) on 10/07/2017.
 */
@Component
@Slf4j
public class PacketDumperSelfDestruct {

    @Value("${LISTEN_PID:-1}")
    private String listenPid = "-1";

    private static final int SLEEP = 30000;

    @Scheduled(fixedDelay = SLEEP)
    public void exitIfListPidIsDead() {
        if (!listenPid.equals("-1")) {
            try {
                if (!isStillAllive()) {
                    System.exit(0);
                }
            } catch (Exception e) {
                log.warn("problem checking if listen pid: {} is still running. error: {}", listenPid, e.toString());
            }
        }
    }

    public boolean isStillAllive() {
        String OS = System.getProperty("os.name").toLowerCase();
        String command = null;
        if (OS.indexOf("win") >= 0) {
            log.debug("Check alive Windows mode. Pid: [{}]", listenPid);
            command = "cmd /c tasklist /FI \"PID eq " + listenPid + "\"";
        } else if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0) {
            log.debug("Check alive Linux/Unix mode. Pid: [{}]", listenPid);
            command = "ps -p " + listenPid;
        } else {
            log.warn("Unsuported OS: Check alive for Pid: [{}] return false", listenPid);
            return false;
        }
        return isProcessIdRunning(listenPid, command); // call generic implementation
    }

    private boolean isProcessIdRunning(String pid, String command) {
        log.debug("Command [{}]", command);
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);
            String strLine = null;
            while ((strLine = bReader.readLine()) != null) {
                if (strLine.contains(" " + pid + " ")) {
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            log.warn("Got exception using system command [{}].", command, ex);
            return true;
        }
    }


}
