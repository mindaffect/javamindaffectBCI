cd %~dp0
set utopiahostport=-
set trighostport=-
set isi=40
set nsymbs=20
set ntrials=100
set nepoch=500
rem `expr 10 '*' 1000 / $isi`
set mode=calibrate.supervised
#mode=prediction.static
set noisestr=0
java -cp "FakePresentation.jar;..\messagelib\UtopiaServer.jar" nl.ma.utopia.fakepresentation.FakePresentation %utopiahostport% %trighostport% %isi% %nsymbs% %ntrials% %nepoch% %mode% %noisestr%