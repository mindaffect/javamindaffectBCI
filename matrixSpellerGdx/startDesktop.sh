#/bin/bash
cd `dirname ${BASH_SOURCE[0]}`
jarfile=desktop/build/libs/matrixSpellerGdx-*.jar
echo Starting: $jarfile
java -Xmx64m -XX:CompressedClassSpaceSize=64m -jar $jarfile
