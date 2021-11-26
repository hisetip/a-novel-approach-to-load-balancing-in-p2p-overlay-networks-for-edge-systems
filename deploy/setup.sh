#!/bin/bash

nNodes=$1
shift

distribution=$1
shift

n_nodes=$(uniq $OAR_FILE_NODES | wc -l)

restDivision=$(python -c "print($nNodes % $n_nodes)")
if [ $restDivision != 0 ]
then
  echo "Error! Number of total nodes should have a 0 modulo division by the total number of machines being used."
  echo "(So that each machine has the same number of nodes.)"
  exit 0
fi

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

docker network rm leitaonet
echo "Killing everything"

for node in $(oarprint host); do
    oarsh $node 'docker kill $(docker ps -q)' &
done

wait

for node in $(oarprint host); do
    oarsh $node "docker swarm leave -f" 
done

docker swarm init
JOIN_TOKEN=$(docker swarm join-token manager -q)

host=$(hostname)
for node in $(oarprint host); do
  if [ $node != $host ]; then
    oarsh $node "docker swarm join --token $JOIN_TOKEN $host:2377"
  fi
done

echo "Rebuilding image"
for node in $(oarprint host); do
	oarsh $node "cd $(pwd);  docker build --rm -t asdproj ." &
	#oarsh $node "cd $(pwd);  docker load -i protocol-stack.tar" &
done

wait

echo "Creating network"
docker network create -d overlay --attachable --subnet 172.10.0.0/16 leitaonet

user=$(whoami)
mkdir -p ~/asdLogs

declare -a resourceArray
readarray -t resourceArray < "./src/main/java/protocols/optimization/resest/config/resources_$distribution" # Exclude newline.
max=${resourceArray[0]}
for ni in "${resourceArray[@]}" ; do
    ((ni > max)) && max=$ni
done
ind=0
for nj in "${resourceArray[@]}" ; do
    resourceArray[ind]=$(python -c "print('%.6f' % ($nj*1.0/$max))")
    ind=$ind+1
done
if [ "${#resourceArray[@]}" -lt "$nNodes" ]
then
  echo "Error! Resource file does not have enough lines."
  exit 0
fi

nodesPerMachine=$((nNodes / n_nodes))
echo "Nodes per machine: $nodesPerMachine"
echo "Launching containers"
for i in $(seq 00 $(($nNodes - 1))); do
  node=$(nextnode $i)

  get_n_cpus="grep processor /proc/cpuinfo | wc -l"
  number_cpus=$(oarsh -n $node $get_n_cpus)
  cpusPerNode=$(python -c "print($number_cpus.0/$nodesPerMachine)")
  echo "cpuspernode: $cpusPerNode"
  eightyPercentOfCPU="\"$(python -c "print('%.6f' % ($cpusPerNode*0.80*${resourceArray[$i]}))")\""
  echo "eightyPercentOfCPU: $eightyPercentOfCPU"

  get_memory_text="cat /proc/meminfo | grep MemTotal:"
  totalMemoryText=$(oarsh -n $node $get_memory_text)
  totalMemory=$(python -c "print('$totalMemoryText'.split(' ')[-2])")
  echo "totalMemory: $totalMemory"
  memoryPerNode=$(python -c "print($totalMemory/$nodesPerMachine)")
  echo "memoryPerNode: $memoryPerNode"
  echo "resource (0-100): ${resourceArray[$i]}"
  eightyPercentMemoryPerNode="\"$(python -c "print(str(int($memoryPerNode*0.80*${resourceArray[$i]}))+\"k\")")\""
  echo "eightyPercentMemoryPerNode: $eightyPercentMemoryPerNode"

  ii=$(printf "%.2d" $i)
  echo -n "$ii - "
  cf="mkdir -p /tmp/logs_protocol-stack"

  dividend=${i}
  divisor=255
  quotient=$((dividend/divisor))
  quotientIp=$((quotient + 10))
  remainder=$((dividend%divisor))
  echo "IP being used: 172.10.${quotientIp}.${remainder}"
  cmd="docker run -d -t --rm \
    --privileged --cap-add=ALL \
    --mount type=bind,source=/tmp/logs_protocol-stack,target=/logs \
    --net leitaonet --ip 172.10.${quotientIp}.${remainder} -h node-${ii} --name node-${ii} \
    asdproj ${i} ${nNodes} ${distribution}"

    # --cpus=$eightyPercentOfCPU \
    # --memory=$eightyPercentMemoryPerNode \

  oarsh -n $node "$cf && $cmd"
done