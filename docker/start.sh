#!/bin/sh

idx=$1
user=$2
logfoldern=$3
shift
shift
shift
mkdir -p logs/${logfoldern}
chown -R $user logs/${logfoldern}
mkdir -p logs/${logfoldern}/node$idx
chown -R $user logs/${logfoldern}/node$idx
# ./main -listenPort 10000 -listenInterface eth0 -logfolder logs/${logfoldern}/node$idx "$@" 2> logs/${logfoldern}/node$idx/stderr.log
# echo './main -logfolder logs/${logfoldern}/node$idx'

java -Xmx2048m -DlogFilename=logs/${logfoldern}/node$idx -cp asdProj.jar:babel-core-0.4.25.jar Main -listenPort 10000 -listenInterface eth0 "$@" 2> logs/${logfoldern}/node$idx/stderr.log
echo "java -DlogFilename=logs/${logfoldern}/node$idx -cp asdProj.jar:babel-core-0.4.25.jar Main -listenPort 10000 -listenInterface eth0 ... 2> logs/${logfoldern}/node$idx/stderr.log"
chown $user logs/${logfoldern}/node$idx.log