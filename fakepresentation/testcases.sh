#!/bin/bash
utopiadir='../messagelib'

# start the utopiaserver
java -cp ${utopiadir}/UtopiaServer.jar nl.ma.utopiaserver.UtopiaServer 8400 1 &
serverpic=$!

# stop all on ctrl-c
killall() {
    kill -9 $serverpic
    }
trap 'killall' SIGINT
# wait for server to wake-up
sleep 3

# start the FakePresentation
java -cp "build/jar/FakePresentation.jar:../messagelib/UtopiaServer.jar" nl.ma.utopia.fakepresentation.FakePresentation - - 100 2 1000 1000

killall
