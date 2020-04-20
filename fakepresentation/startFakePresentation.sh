#!/bin/bash
cd `dirname ${BASH_SOURCE[0]}`
utopiahostport=-
trighostport=-
isi=20
nsymbs=20
ntrials=10
nepoch=`expr 4 '*' 1000 / $isi`
mode=calibrate.supervised
#mode=prediction.static
noisestr=0
java -cp "FakePresentation.jar:../messagelib/UtopiaServer.jar" nl.ma.utopia.fakepresentation.FakePresentation $utopiahostport $trighostport $isi $nsymbs $ntrials $nepoch $mode $noisestr
