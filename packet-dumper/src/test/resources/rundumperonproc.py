import psutil
import subprocess
import sys
import re

pids = psutil.pids()
i=0
pids_to_process=[]
dumperpids = []
print "Total PIDs: " +  str(len(pids))
regex=r'.*--LISTEN_PID=([0-9]+)'
try:
    for pid in pids:
        p = psutil.Process(pid)
        cmdline = p.cmdline()
        parts = [part for part in cmdline if "--LISTEN_PID=" in part]
        # print ("cmdline", cmdline)
        # print ("parts", parts)
        if len(parts) > 0:
            for part in parts:
                matchObj = re.match(regex,part)
                dumperpids.append(int(matchObj.group(1)))
            continue

        proc = subprocess.Popen(["nsenter", "-t", str(pid), "-n", "netstat", "-natp"], stdout=subprocess.PIPE)
        output,err = proc.communicate()
        rc = proc.returncode
        if "LISTEN" in output:
            lines = output.split('\n')
            listenLines = [line for line in lines if "LISTEN" in line]
            for fullline in listenLines:
                splitlines = fullline.split()
                #print ("split lines",splitlines)
                hostAndIp = splitlines[3]
                processId = splitlines[6].split("/")[0]
                port = hostAndIp[hostAndIp.rfind(":")+1:]
                #print port
                if (str(pid) == str(processId) and int(port) > 1024):
                    #print processId
                    i=i+1
                    #print i
                    #print fullline
                    pids_to_process.append(pid)
                #print (pid, p.name())

except:
    print "Unexpected error:", sys.exc_info()

pids_to_process = list(set(pids_to_process))
print "Found PIDs #" + str(len(pids_to_process))
print "Found PIDs: " + ",".join(str(x) for x in pids_to_process)
print "PIDs to skip: " + ",".join(str(x) for x in dumperpids)

unique_pids_to_process = list(set(pids_to_process) & set(set(pids_to_process) ^ set(dumperpids)))
print unique_pids_to_process

for pid in unique_pids_to_process:
    with open("/var/log/packet-dumper_" + str(pid) + ".txt","wb") as out:
        subprocess.Popen(["nsenter", "-t", str(pid), "-n", "java", "-jar", "/opt/cisco/packet_dumper/packet-dumper-0.0.1-SNAPSHOT.jar", "--kafka.bootstrap-servers=test-machine.il.nds.com:80", "--spring.profiles.active=kafka,vm", "--kafkaConnector.logMode=true", "--pcap.devicePrefix=eth", "--server.port=0", "--LISTEN_PID={0}".format(pid)], stdin=out, stdout=out, stderr=out)