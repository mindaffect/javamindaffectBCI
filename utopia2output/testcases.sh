#!/bin/bash
bufferdir='../../resources/eeg/buffer/java'
utopiadir='../messagelib'

# start a utopia server
java -jar ${utopiadir}/UtopiaServer.jar &
utopiapid=$!
sleep 1

sleep 1
# utopia2output client
exec java -Xmx64m -cp ${utopiadir}/UtopiaServer.jar:utopia2output.jar nl.ma.utopia.utopia2output.Utopia2Output $@ &
utopia2outputpid=$1

# sending client
sleep 5
java -cp ${utopiadir}/UtopiaServer.jar nl.ma.utopiaserver.UtopiaClient 10

sleep 3
kill ${utopia2outputpid}
kill ${utopiapid}
