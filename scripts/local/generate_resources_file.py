import random
import sys
import numpy
from collections import Counter

f = open("./scripts/resources", "w")

if len(sys.argv) != 5:
    raise ValueError("Please provide three arguments: <numberNodes> <minResources> <maxResources>"
                     "<distribution (uniform, lognormal)>.")

nNodes = int(sys.argv[1])
minResources = int(sys.argv[2])
maxResources = int(sys.argv[3])
distribution = sys.argv[4]

allValues = []

for i in range(0, nNodes):
    valueToWrite = ""
    if distribution == "uniform":
        valueToWrite = str(random.randint(minResources, maxResources))
    elif distribution == "lognormal":
        valueToWrite = str(int(numpy.random.lognormal(mean=2.8, sigma=1, size=None)))
    else:
        raise ValueError("I don't know that distribution.")

    allValues.append(int(valueToWrite))
    f.write(valueToWrite)

    if i != nNodes - 1:
        f.write("\n")

print(Counter(allValues))

f.close()
