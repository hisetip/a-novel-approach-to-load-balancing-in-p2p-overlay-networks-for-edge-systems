#!/bin/bash

for node in $(oarprint host); do
  oarsh $node "sudo-g5k /grid5000/code/bin/g5k-setup-docker" &
done
wait