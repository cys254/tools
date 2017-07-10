import psutil
import subprocess
import sys
import re

pids = psutil.pids()
pids_to_ports = {}
pids_to_process=[]
dumperpids = []
print "Total PIDs: " +  str(len(pids))
regex=r'.*--LISTEN_PID=([0-9]+)'
try:
    for pid in pids:
        p = psutil.Process(pid)
        cmdline = p.cmdline()
        parts = [part for part in cmdline if "--LISTEN_PID=" in part]
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
                hostAndIp = splitlines[3]
                processId = splitlines[6].split("/")[0]
                listenPort = hostAndIp[hostAndIp.rfind(":") + 1:]
                if (str(pid) == str(processId) and int(listenPort) > 1024):
                    pids_to_process.append(pid)
                    pids_to_ports.setdefault(pid, []).append(listenPort)
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
    with open("##log_path##/packet-dumper_" + str(pid) + ".log","wb") as out:
        portFilter = "--pcap.portFilter=" + ",".join(pids_to_ports[pid])
        subprocess.Popen(["nsenter", "-t", str(pid), "-n", "java", "-jar", "/opt/cisco/##software_name##/packet-dumper-##software_version##.jar", portFilter, "--kafka.bootstrap-servers=test-machine.il.nds.com:80", "--spring.profiles.active=kafka,vm,vmCPU", "--kafkaConnector.logMode=false", "--pcap.devicePrefix=eth", "--server.port=0", "--LISTEN_PID={0}".format(pid)], stdin=out, stdout=out, stderr=out)