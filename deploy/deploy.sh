#!/bin/bash

nNodes=$1
shift

n_nodes=$(uniq $OAR_FILE_NODES | wc -l)

function nextnode {
  local idx=$(($1 % n_nodes))
  local i=0
  for host in $(uniq $OAR_FILE_NODES); do
    if [ $i -eq $idx ]; then
      echo $host
      break;
    fi
    i=$(($i +1))
  done
}


user=$(whoami)

echo "Executing"

beforerunningDate=`date`
echo $beforerunningDate

beforeRunningSeconds=$(date +%s)

printf "%.2d.. " 0

user=$(id -u):$(id -g)

logFolder=$1
shift
totalExpTime=$1
shift

node=$(nextnode 0)
oarsh -n $node docker exec -d node-00 ./start.sh 0 $user $logFolder "$@" -peerID 1 -startTime $beforeRunningSeconds

sleep 1

for i in $(seq 01 $(($nNodes - 1))); do
  node=$(nextnode $i)
  ii=$(printf "%.2d" $i)
  echo -n "$ii.. "
  if [ $((($i + 1) % 10)) -eq 0 ]; then
    echo ""
  fi
  c=$(($i-1))
  cc=$(printf "%.2d" $c)

  dividend=${c}
  divisor=255
  quotient=$((dividend/divisor))
  quotientIp=$((quotient + 10))
  remainder=$((dividend%divisor))
  echo "IP being used as bootstrap: 172.10.${quotientIp}.${remainder}"
  oarsh -n $node docker exec -d node-${ii} ./start.sh $i $user $logFolder "$@" -peerID $(($i+1)) -startTime $beforeRunningSeconds -bootstraps \'172.10.${quotientIp}.${remainder}:10000\'
  sleep 1
done

currentDate=`date`
echo "$currentDate"

currentDateSeconds=$(date +%s)
toSleep=$(($totalExpTime- ($currentDateSeconds - $beforeRunningSeconds) + 120))
echo "Sleeping $toSleep"
sleep $toSleep

newDate=`date`
echo "$newDate"

#make logFolder dir
mkdir -p /home/vmenino/asdLogs/$logFolder # on fct cluster, add . in vmenino

for i in $(seq 00 $(($nNodes - 1))); do
  node=$(nextnode $i)
  ii=$(printf "%.2d" $i)
  echo -n "$ii.. "
  if [ $((($i + 1) % 10)) -eq 0 ]; then
    echo ""
  fi

  #Moving logs from tmp to folder:
  oarsh -n $node cp -r /tmp/logs_protocol-stack/$logFolder /home/vmenino/asdLogs/ # on fct cluster, add . in vmenino

  oarsh -n $node docker exec -d node-${ii} killall java
done

endDate=`date`
echo "$endDate"

echo ""
