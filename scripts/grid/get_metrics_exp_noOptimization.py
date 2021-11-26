import os
import sys
from datetime import datetime
import json
import matplotlib.pyplot as plt
import numpy as np
import statistics
import random

RESOURCE_TYPE = "lognormal"

# LOG_FOLDER = "/Users/hisetip/Downloads/"
# LOG_FOLDER = "/Users/hisetip/Documents/thesis_tests/foutakos/100_nodes/1msgsec/"
LOG_FOLDER = "../asdLogs/"
NUMBER_OF_NODES = 1000
if len(sys.argv) <= 3:
    sys.stderr.write("Format: <test_name> <nNodes> <resource_type>...")
    exit()

NUMBER_OF_NODES = int(sys.argv[2])
RESOURCE_TYPE = sys.argv[3]

# LOG_FOLDER = "tmp/"

exp_folder = LOG_FOLDER + sys.argv[1] + '/'

msgBroadcastedSet = set()  # set of (mid, time)
nodesAppMsgsReceived = []  # array of nodes with map mid -> time
for i in range(NUMBER_OF_NODES):
    nodesAppMsgsReceived.append(dict())

nodeResources = []  # array of nodes with int (nodes resources) - length NUMBER_OF_NODES

nodesFloodSentMessages = [] # Per node
nodesFloodReceivedMessages = [] # Per node
nodesOverlaysSentMessages = [] # Per node
nodesOverlaysReceivedMessages = [] # Per node

nodesOverlaysSentBytes = [] # Per node
nodesOverlaysReceivedBytes = [] # Per node


with open('src/main/java/protocols/optimization/resest/config/resources_'+RESOURCE_TYPE) as my_file:
    for line in my_file:
        nodeResources.append(int(line))

# Reading from files
nodeCount = 0
print(NUMBER_OF_NODES)
for nodeN in range(0, NUMBER_OF_NODES):
    nodePath = exp_folder + "node" + str(nodeN)

    # Relating with measuring reliability and latency:
    f = open(os.path.join(nodePath + '/', "Application.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    for i in range(0, len(linesArr)):
        if "flooding" in linesArr[i]:
            mID = linesArr[i].split()[-1][1:-2]
            mTime = datetime.strptime("2021 " + linesArr[i][6:25], '%Y %b %d %H:%M:%S.%f').timestamp()
            msgBroadcastedSet.add((mID, mTime))
        elif "the message" in linesArr[i]:
            mID = linesArr[i].split()[-1][1:-2]
            mTime = datetime.strptime("2021 " + linesArr[i][6:25], '%Y %b %d %H:%M:%S.%f').timestamp()
            nodesAppMsgsReceived[nodeCount][mID] = mTime
            
    # Relating with measuring node received and sent messages and bytes:
    f = open(os.path.join(nodePath + '/', "MultiLoggerChannel.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    haveOSM = False
    haveOSB = False
    haveORM = False
    haveORB = False
    haveFSM = False
    haveFRM = False
    for i in range(len(linesArr) - 1, -1, -1):
        if "Number of messages sent by overlays" in linesArr[i] and not haveOSM:
            haveOSM = True
            nodesOverlaysSentMessages.append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of bytes sent by overlays" in linesArr[i] and not haveOSB:
            haveOSB = True
            nodesOverlaysSentBytes.append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of messages received by overlays" in linesArr[i] and not haveORM:
            haveORM = True
            nodesOverlaysReceivedMessages.append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of bytes received by overlays" in linesArr[i] and not haveORB:
            haveORB = True
            nodesOverlaysReceivedBytes.append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of messages sent by flood" in linesArr[i] and not haveFSM:
            haveFSM = True
            nodesFloodSentMessages.append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of messages received by flood" in linesArr[i] and not haveFRM:
            haveFRM = True
            nodesFloodReceivedMessages.append(int(linesArr[i].split()[-1][:-1]))
        
    nodeCount += 1


reliabilityPerFloodedMsg = []
messageLatencies = []
messageLatenciesUpTo90 = []
messageLatenciesUpTo95 = []
messageLatenciesUpTo99 = []
sumLatenciesPerNode = []
countLatenciesPerNode = []
for j in range(0, NUMBER_OF_NODES):
    sumLatenciesPerNode.append(0.0)
    countLatenciesPerNode.append(0)

# Getting latency and throughput
for (mID, mTime) in msgBroadcastedSet:
    highestLatency = 0.0
    receivedCount = 0
    latenciesArr = []
    for i in range(0, NUMBER_OF_NODES):
        if mID in nodesAppMsgsReceived[i]:
            latenciesArr.append(float(nodesAppMsgsReceived[i][mID]) - float(mTime))
            sumLatenciesPerNode[i] += (float(nodesAppMsgsReceived[i][mID]) - float(mTime))
            countLatenciesPerNode[i] += 1
            receivedCount += 1
            if nodesAppMsgsReceived[i][mID] > highestLatency:
                highestLatency = nodesAppMsgsReceived[i][mID]
    if highestLatency != 0.0:
        messageLatencies.append(highestLatency-mTime)
        numpyLatencies = np.array(latenciesArr, dtype=float)
        numpyLatencies.sort()
        messageLatenciesUpTo90.append(numpyLatencies[int(0.9*numpyLatencies.size)])
        messageLatenciesUpTo95.append(numpyLatencies[int(0.95*numpyLatencies.size)])
        messageLatenciesUpTo99.append(numpyLatencies[int(0.99*numpyLatencies.size)])
    else:
        sys.stderr.write("Error in reading latency of message: " + mID + "\n")
        sys.stderr.write("Flooded at time: " + str(mTime) + "\n")
    reliabilityPerFloodedMsg.append(float(receivedCount)/float(NUMBER_OF_NODES))

avgReliability = sum(reliabilityPerFloodedMsg) / len(reliabilityPerFloodedMsg)
avgLatency = sum(messageLatencies) / len(messageLatencies)
medianLatency = statistics.median(messageLatencies)
avg90Latency = sum(messageLatenciesUpTo90) / len(messageLatenciesUpTo90)
avg95Latency = sum(messageLatenciesUpTo95) / len(messageLatenciesUpTo95)
avg99Latency = sum(messageLatenciesUpTo99) / len(messageLatenciesUpTo99)
median90Latency = statistics.median(messageLatenciesUpTo90)
median95Latency = statistics.median(messageLatenciesUpTo95)
median99Latency = statistics.median(messageLatenciesUpTo99)

sumOfOverlaysMessagesSent = 0
sumOfOverlaysMessagesReceived = 0
sumOfOverlaysBytesSent = 0
sumOfOverlaysBytesReceived = 0
sumOfFloodMessagesSent = 0
sumOfFloodMessagesReceived = 0
for i in range(0, NUMBER_OF_NODES):
    sumOfOverlaysMessagesSent += nodesOverlaysSentMessages[i]
    sumOfOverlaysMessagesReceived += nodesOverlaysReceivedMessages[i]
    sumOfOverlaysBytesSent += nodesOverlaysSentBytes[i]
    sumOfOverlaysBytesReceived += nodesOverlaysReceivedBytes[i]
    sumOfFloodMessagesSent += nodesFloodSentMessages[i]
    sumOfFloodMessagesReceived += nodesFloodReceivedMessages[i]

avgNumberOfOverlayMessagesSentPerNode = float(sumOfOverlaysMessagesSent) / float(NUMBER_OF_NODES)
avgNumberOfOverlayMessagesReceivedPerNode = float(sumOfOverlaysMessagesReceived) / float(NUMBER_OF_NODES)
avgNumberOfOverlayBytesSentPerNode = float(sumOfOverlaysBytesSent) / float(NUMBER_OF_NODES)
avgNumberOfOverlayBytesReceivedPerNode = float(sumOfOverlaysBytesReceived) / float(NUMBER_OF_NODES)
avgNumberOfFloodMessagesSentPerNode = float(sumOfFloodMessagesSent) / float(NUMBER_OF_NODES)
avgNumberOfFloodMessagesReceivedPerNode = float(sumOfFloodMessagesReceived) / float(NUMBER_OF_NODES)

avgLatencyPerNode = []
nodeWithHighestAvgLatency = "none"
highestAvgLatency = -1
for i in range(0, NUMBER_OF_NODES):
    avgLatencyPerNode.append(float(sumLatenciesPerNode[i]) / float(countLatenciesPerNode[i]))
    if avgLatencyPerNode[i] > highestAvgLatency:
        nodeWithHighestAvgLatency = "node" + str(i)
        highestAvgLatency = avgLatencyPerNode[i]
sortedAvgLatencyPerNode = sorted(avgLatencyPerNode)

fw = open(exp_folder + "final_results.txt", 'w')

towrite = ["Avg latency: ", str(avgLatency), "\n\n",
           "Median latency: ", str(medianLatency), "\n\n",
           "Avg 90percent latency: ", str(avg90Latency), "\n\n",
           "Median 90percent latency: ", str(median90Latency), "\n\n",
           "Avg 95percent latency: ", str(avg95Latency), "\n\n",
           "Median 95percent latency: ", str(median95Latency), "\n\n",
           "Avg 99percent latency: ", str(avg99Latency), "\n\n",
           "Median 99percent latency: ", str(median99Latency), "\n\n",
           "Avg reliability: ", str(avgReliability), "\n\n",
           "Avg of avg latency per node: ", str(sum(avgLatencyPerNode) / len(avgLatencyPerNode)), "\n\n",
           "90pc of the nodes have an avg message latency below: ", str(sortedAvgLatencyPerNode[int(0.9*NUMBER_OF_NODES)]), "\n\n",
           "95pc of the nodes have an avg message latency below: ", str(sortedAvgLatencyPerNode[int(0.95*NUMBER_OF_NODES)]), "\n\n",
           "99pc of the nodes have an avg message latency below: ", str(sortedAvgLatencyPerNode[int(0.99*NUMBER_OF_NODES)]), "\n\n",
           "The node with the highest avg message latency is " + nodeWithHighestAvgLatency + " and has an avg message latency of: ", str(highestAvgLatency), "\n\n",
           "-------------------------------------------------------", "\n\n",
           "Avg number of messages (overlays) sent per node: ", str(avgNumberOfOverlayMessagesSentPerNode), "\n\n",
           "Avg number of bytes (overlays) sent per node: ", str(avgNumberOfOverlayBytesSentPerNode), "\n\n",
           "Avg number of messages (overlays) received per node: ", str(avgNumberOfOverlayMessagesReceivedPerNode), "\n\n",
           "Avg number of bytes (overlays) received per node: ", str(avgNumberOfOverlayBytesReceivedPerNode), "\n\n",
           "-------------------------------------------------------", "\n\n",
           "Avg number of messages (flood) sent per node: ", str(avgNumberOfFloodMessagesSentPerNode), "\n\n",
           "Avg number of messages (flood) received per node: ", str(avgNumberOfFloodMessagesReceivedPerNode), "\n\n",
           ]

fw.writelines(towrite)
fw.close()


# Making the plot of resources - flood messages sent

nodeResFloodMsgSent = dict()

for i in range(NUMBER_OF_NODES):
    if nodeResources[i] not in nodeResFloodMsgSent:
        nodeResFloodMsgSent[nodeResources[i]] = []
    nodeResFloodMsgSent[nodeResources[i]].append(nodesFloodSentMessages[i])

uniqueResourcesArr = []
uniqueResourcesArrS = []
sentFloodMsgsPerUniqueResArr = []
nodeResFloodMsgSentArr = []
for k, v in sorted(nodeResFloodMsgSent.items()):
    uniqueResourcesArrS.append(str(k))
    uniqueResourcesArr.append(k)
    sentFloodMsgsPerUniqueResArr.append(sum(v) / len(v))
    nodeResFloodMsgSentArr.append(v)

# Finally, make and save plot
# x axis values
x = uniqueResourcesArr
# corresponding y axis values
y = sentFloodMsgsPerUniqueResArr

fig2 = plt.figure(figsize=(10, 5))

# plotting the points
plt.plot(x, y, 'o')

# Regression
x = np.array(x)
y = np.array(y)
m, b = np.polyfit(x, y, 1)
plt.plot(x, m*x + b)

# naming the x axis
plt.xlabel('x - Resource level')
# naming the y axis
plt.ylabel('y - Number of messages sent')

fig2.savefig(exp_folder + 'resources_messages_sent_plot_unique.png', bbox_inches='tight', dpi=150)

# function to show the plot
# plt.show()
plt.clf()

fig4 = plt.figure(figsize=(10, 5))

# Make and save plot w/ unique
x = uniqueResourcesArrS[:-1]
y = nodeResFloodMsgSentArr[:-1]
for xe, ye in zip(x, y):
    plt.scatter([xe] * len(ye), ye, c='#000000', s=70, alpha=0.03)

plt.gray()

# naming the x axis
plt.xlabel('x - Node resource level')
# naming the y axis
plt.ylabel('y - Number of flood messages sent by the node')

fig4.savefig(exp_folder + 'resources_messages_sent_plot.png', bbox_inches='tight', dpi=150)

plt.clf()

# Plotting histogram on average latency per node
fig3 = plt.figure(figsize=(10, 5))

plt.bar(range(0,NUMBER_OF_NODES), avgLatencyPerNode, width=1.0)
plt.xlabel('Node ID')
plt.ylabel('Average latency of received messages')

fig3.savefig(exp_folder + 'avg_latency_per_node.png', bbox_inches='tight', dpi=150)
plt.clf()
