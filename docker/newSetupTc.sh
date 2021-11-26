#!/bin/bash

echo "starting setupTC"

idx=$1
nNodes=$2
bandwidth=$3
latencyMap="latency"

if [ -z "$bandwidth" ]; then
  bandwidth=10000000000
fi

bandwidthPerNode=$(python3 -c "print(int(round($bandwidth/$nNodes)))")

echo $idx
echo $nNodes
echo $bandwidth
echo $bandwidthPerNode

setupTC() {
  inBandwidth=$((bandwidth * 2))

  cmd="tc qdisc add dev eth0 root handle 1: htb"
  echo "$cmd"
  eval $cmd

  cmd="tc class add dev eth0 parent 1: classid 1:1 htb rate ${bandwidth}kbps"
  echo "$cmd"
  eval $cmd

  j=0
  for n in $1; do
    if [ $idx -eq $j ]; then
      j=$((j + 1))
      continue
    fi

    cmd="tc class add dev eth0 parent 1: classid 1:${j}1 htb rate ${bandwidthPerNode}kbps"
    echo "$cmd"
    eval $cmd

    cmd="tc qdisc add dev eth0 parent 1:${j}1 netem delay ${n}ms"
    echo "$cmd"
    eval $cmd

    dividend=${j}
    divisor=255
    quotient=$((dividend/divisor))
    quotientIp=$((quotient + 10))
    remainder=$((dividend%divisor))
    echo "IP being used: 172.10.${quotientIp}.${remainder}"
    cmd="tc filter add dev eth0 protocol ip parent 1:0 prio 1 u32 match ip dst 172.10.${quotientIp}.${remainder} flowid 1:${j}1"
    echo "$cmd"
    eval $cmd
    j=$((j + 1))
  done
}

i=0
echo "Setting up tc emulated network..."
while read -r line; do
  if [ $idx -eq $i ]; then
    setupTC "$line"
    break
  fi
  i=$((i + 1))
done <"$latencyMap"

echo "Done."

/bin/sh


