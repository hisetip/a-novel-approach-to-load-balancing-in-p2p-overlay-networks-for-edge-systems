#!/bin/bash

for node in $(oarprint host); do
  oarsh $node "docker --help" &
done
wait