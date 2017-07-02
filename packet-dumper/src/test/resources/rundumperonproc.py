import psutil
import subprocess
import sys
import re

pids = psutil.pids()
i=0
pidstoprocess=[]
subprocport = 60000
dumperpids = []
print len(pids)
try:
    for pid in pids:
        p = psutil.Process(pid)
        cmdline = p.cmdline()
        parts = [part for part in cmdline if "--LISTEN_PID=" in part]
        # print ("cmdline", cmdline)
        # print ("parts", parts)
        for part in parts:
            matchObj = re.match(r'.*--LISTEN_PID=([0-9]+)',part)
            dumperpids.append(int(matchObj.group(1)))
            dumperpids.append(pid)
    #
        # if len(cmdline) > 0:
        #     for part in cmdline:
        #         if "--LISTEN_PID=" in part:
        #             matchObj = re.match(r'.*--LISTEN_PID=([0-9]+)',part)
        #             print ("GROUP:{0}".format(matchObj.group(1)))
        #             dumperpids.append(matchObj.group(1))
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
                    pidstoprocess.append(pid)
                #print (pid, p.name())

except:
    print "Unexpected error:", sys.exc_info()

pidstoprocess = list(set(pidstoprocess))
print len(pidstoprocess)
print pidstoprocess
print "dumperpids: "
print dumperpids


for pid in pidstoprocess:
    subprocport = subprocport + 1
    #print subprocport
    # subproc = os.fork()
    #print subproc
    # if subproc == 0:
    #TODO: move dumper logs to files
    if pid not in dumperpids:
        subprocess.Popen(["nsenter", "-t", str(pid), "-n", "java", "-jar", "/opt/cisco/packet_dumper/packet-dumper-0.0.1-SNAPSHOT.jar", "--kafka.bootstrap-servers=test-machine.il.nds.com:80", "--spring.profiles.active=kafka,vm", "--kafkaConnector.logMode=true", "--pcap.devicePrefix=eth", "--server.port={0}".format(subprocport), "--LISTEN_PID={0}".format(pid)], stdin=None, stdout=None, stderr=None)