cd %~dp0
jarfile=utopia2output.jar
echo Starting jar-file: %jarfile%
start java -cp ../messagelib/UtopiaServer.jar:%jarfile% nl.ma.utopia.utopia2output $@
