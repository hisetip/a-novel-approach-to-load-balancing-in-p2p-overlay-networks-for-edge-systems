#!/bin/bash

idx=$1
nNodes=$2
distribution=$3

declare -a resourceArray
readarray -t resourceArray < "./src/main/java/protocols/optimization/resest/config/resources_$distribution" # Exclude newline.
max=${resourceArray[0]}
for ni in "${resourceArray[@]}" ; do
    ((ni > max)) && max=$ni
done
ind=0
for nj in "${resourceArray[@]}" ; do
    resourceArray[ind]=$(python3 -c "print('%.6f' % ($nj*1.0/$max))")
    ind=$ind+1
done
if [ "${#resourceArray[@]}" -lt "$nNodes" ]
then
  echo "Error! Resource file does not have enough lines."
  exit 0
fi

# 100 nodes:
# maxBW=10000000
# minBW=100000 # THIS HAS TO BE GREATER OR EQUAL TO THE TOTAL NUMBER OF NODES (nNodes)

# 1000 nodes:
maxBW=20000000
minBW=200000 # THIS HAS TO BE GREATER OR EQUAL TO THE TOTAL NUMBER OF NODES (nNodes)

NewMax=$maxBW
NewMin=$minBW
OldMax=1
OldMin=0
OldRange=$(python3 -c "print($OldMax-$OldMin)")
NewRange=$(python3 -c "print($NewMax-$NewMin)")

bandw=$(python3 -c "print(int(((${resourceArray[$idx]}*1.0 * $NewRange*1.0) / $OldRange*1.0) + $NewMin*1.0))")

/bin/bash newSetupTc.sh $idx $nNodes $bandw
# /bin/bash newSetupTc.sh $idx $nNodes
# /bin/bash
