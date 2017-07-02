import psutil
from subprocess import call
for pid in psutil.pids():
	p = psutil.Process(pid)
	if p.connections():
		for con in p.connections():
			if con.status == psutil.CONN_LISTEN:
				call(["nsenter", "-t", str(pid), "-n", "java", "-jar", "/opt/cisco/packet_dumper/packet-dumper-0.0.1-SNAPSHOT.jar", "--kafka.bootstrap-servers=test-machine.il.nds.com:80", "--spring.profiles.active=kafka,vm", "--kafkaConnector.logMode=true", "--pcap.devicePrefix=eth"])
				#print (pid, p.name(), con)