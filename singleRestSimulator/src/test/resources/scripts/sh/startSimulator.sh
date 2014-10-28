#!/bin/bash

JARS=`ls ../lib/*jar`
STARTUP=`ls ../lib/singleRestSimulator*jar`

CP=".:../config:../etc"

for x in $JARS
do
  if [ ${x} != ${STARTUP} ] 
  then
     CP="${CP}:${x}"
   fi
done

CP="${CP}:${STARTUP}"

$JAVA_HOME/bin/java -classpath $CP com.cisco.oss.foundation.tools.simulator.rest.startup.SingleRestSimulatorStartup

	