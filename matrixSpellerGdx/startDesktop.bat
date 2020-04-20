cd %~dp0
for %%f in ( desktop\build\libs\matrixSpellerGDX-*.jar ) DO set jarfile=%%f
echo Starting jar-file: %jarfile%
start java -jar %jarfile%