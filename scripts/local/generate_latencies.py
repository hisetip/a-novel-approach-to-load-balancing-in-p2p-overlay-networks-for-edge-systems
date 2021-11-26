import random
import numpy as np
import sys

# Currently using 10 as min latency, 250 as max

f = open("./scripts/latency", "w")

if len(sys.argv) != 4:
    raise ValueError("Please provide three arguments: <numberNodes> <minLatency> <maxLatency>.")

nNodes = int(sys.argv[1])
minLatency = int(sys.argv[2])
maxLatency = int(sys.argv[3])

latencies = np.empty((nNodes, nNodes))

for i in range(0, nNodes):
    for j in range(0, nNodes):
        if i == j:
            latencies[i][j] = "0"
        else:
            v = str(random.randint(minLatency, maxLatency))
            latencies[i][j] = v
            latencies[j][i] = v

for i in range(0, nNodes):
    for j in range(0, nNodes):
        f.write(str(int(latencies[i][j])))

        if j != nNodes - 1:
            f.write("\t")

    if i != nNodes - 1:
        f.write("\n")

f.close()
