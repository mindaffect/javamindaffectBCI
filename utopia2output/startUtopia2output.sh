#!/bin/bash
cd `dirname ${BASH_SOURCE[0]}`
exec java -cp ../messagelib/UtopiaServer.jar:utopia2output.jar nl.ma.utopia.utopia2output.Utopia2Output $@
