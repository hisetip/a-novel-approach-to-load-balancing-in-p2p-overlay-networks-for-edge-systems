import os
import sys
from datetime import datetime
import json
import matplotlib.pyplot as plt
import numpy as np

# LOG_FOLDER = "/Users/hisetip/Downloads/"
LOG_FOLDER = "../asdLogs/"
NUMBER_OF_NODES = 1000
if len(sys.argv) <= 2:
    sys.stderr.write("You did not enter the number of nodes...")
    exit()

if sys.argv[2] == "100":
    NUMBER_OF_NODES = 100
    print("using 100 nodes")

if sys.argv[2] == "500":
    NUMBER_OF_NODES = 500
    print("using 500 nodes")

if sys.argv[2] == "1000":
    NUMBER_OF_NODES = 1000
    print("using 1000 nodes")

# LOG_FOLDER = "tmp/"

exp_folder = LOG_FOLDER + sys.argv[1] + '/'

msgBroadcastedSet = set()  # set of (mid, time)
nodesMsgsReceived = []  # array of nodes with map mid -> time

nodeResources = []  # array of nodes with int (nodes resources) - length NUMBER_OF_NODES
nodesSentReceivedMessages = []  # array of nodes with
# (
# (resestSentMessages, resestReceivedMessages),
# (resestOverlaySentMessages, resestOverlayReceivedMessages),
# (overlaySentMessages, overlayReceivedMessages),
# )

nodesFloodSentMessages = []

nodesSentReceivedBytes = []  # array of nodes with
# (
# (resestSentBytes, resestReceivedBytes),
# (resestOverlaySentBytes, resestOverlayReceivedBytes),
# (overlaySentBytes, overlayReceivedBytes),
# )

diffNeighNumberPerNode = []  # Error per node

numNeighsAddedPerNodeOverTime = []  # Array of arrays

numNeighsRemovedPerNodeOverTime = []  # Array of arrays

for i in range(NUMBER_OF_NODES):
    nodesMsgsReceived.append(dict())
    numNeighsAddedPerNodeOverTime.append([])
    numNeighsRemovedPerNodeOverTime.append([])

# Reading from files
nodeCount = 0
for dirname in os.scandir(exp_folder):
    if not dirname.is_dir():
        continue
    # Relating with measuring reliability and latency:

    f = open(os.path.join(dirname.path + '/', "Application.log"), 'r')  # open in readonly mode
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
            mTime = datetime.strptime("2021 " + linesArr[i][6:21], '%Y %b %d %H:%M:%S').timestamp()
            msgBroadcastedSet.add((mID, mTime))
        elif "the message" in linesArr[i]:
            mID = linesArr[i].split()[-1][1:-2]
            mTime = datetime.strptime("2021 " + linesArr[i][6:21], '%Y %b %d %H:%M:%S').timestamp()
            nodesMsgsReceived[nodeCount][mID] = mTime

    # Relating with measuring neighbor variability:

    f = open(os.path.join(dirname.path + '/', "Flood.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    for i in range(0, len(linesArr)):
        if "Number of added neighbors:" in linesArr[i]:
            numNeighsAddedPerNodeOverTime[nodeCount].append(int(linesArr[i].split()[-1][:-1]))
        elif "Number of removed neighbors:" in linesArr[i]:
            numNeighsRemovedPerNodeOverTime[nodeCount].append(int(linesArr[i].split()[-1][:-1]))

    # Related to measure number of neighbors that one node should have vs has

    f = open(os.path.join(dirname.path + '/', "Foutakos.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    acquired = 0
    numberOfNeighborsItHas = -1
    numberOfNeighborsItShouldHave = -1
    for i in range(len(linesArr) - 1, -1, -1):
        if "The number of neighbors it requested:" in linesArr[i]:
            numberOfNeighborsItHas = int(linesArr[i].split()[-1][:-1])
            acquired += 1
        elif "The number of neighbors it should have:" in linesArr[i]:
            numberOfNeighborsItShouldHave = int(linesArr[i].split()[-1][:-1])
            acquired += 1
        if acquired >= 2:
            diffNeighNumberPerNode.append(abs(numberOfNeighborsItShouldHave - numberOfNeighborsItHas))
            break

    # Related to measuring number of messages and bytes

    f = open(os.path.join(dirname.path + '/', "ResourceEstimator.log"), 'r')  # open in readonly mode

    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    nodeResource = -1
    resestNumBytesSent = -1
    resestNumBytesReceived = -1
    for i in range(0, len(linesArr)):
        if "Starting with myResource" in linesArr[i]:
            nodeResource = int(linesArr[i].split()[8])
        elif "Number of bytes sent:" in linesArr[i]:
            resestNumBytesSent = int(linesArr[i].split()[-1][:-1])
        elif "Number of bytes received:" in linesArr[i]:
            resestNumBytesReceived = int(linesArr[i].split()[-1][:-1])

    f = open(os.path.join(dirname.path + '/', "streamManager.log"), 'r')  # open in readonly mode

    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        raise "Error! No lines in app file."

    numsMessagesSentRecLine = ""
    for i in range(len(linesArr) - 1, -1, -1):
        if "ApplicationalMessagesSent" in linesArr[i]:
            numsMessagesSentRecLine = linesArr[i].split()[-1][:-1]
            break

    numsMessagesSentRecLine = numsMessagesSentRecLine.replace("\\", "")
    parsed = json.loads(numsMessagesSentRecLine)

    numberOfSentMessagesResest = parsed["ApplicationalMessagesSent"]["23000"]
    numberOfReceivedMessagesResest = parsed["ApplicationalMessagesReceived"]["23000"]
    numberOfSentMessagesOverlayResest = -1
    numberOfReceivedMessagesOverlayResest = -1
    numberOfSentMessagesOverlay = -1
    numberOfReceivedMessagesOverlay = -1
    numberOfSentBytesOverlayResest = -1
    numberOfReceivedBytesOverlayResest = -1
    numberOfSentBytesOverlay = -1
    numberOfReceivedBytesOverlay = -1

    if "21000" in parsed["ApplicationalMessagesSent"]:
        nodesFloodSentMessages.append(parsed["ApplicationalMessagesSent"]["21000"])
    else:
        nodesFloodSentMessages.append(0)
        sys.stderr.write("Node " + str(dirname.path) + " does not have Flood messages\n")

    if "14000" in parsed["ApplicationalMessagesSent"]:
        numberOfSentMessagesOverlay = parsed["ApplicationalMessagesSent"]["14000"]
        numberOfReceivedMessagesOverlay = parsed["ApplicationalMessagesReceived"]["14000"]

        f = open(os.path.join(dirname.path + '/', "Cyclon.log"), 'r')  # open in readonly mode

        lines = f.readlines()
        # Strips the newline character
        linesArr = []
        for line in lines:
            linesArr.append(line)

        if len(linesArr) == 0:
            raise "Error! No lines in app file."

        acquired = 0
        for i in range(len(linesArr) - 1, -1, -1):
            if "Number of bytes sent:" in linesArr[i]:
                numberOfSentBytesOverlay = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Number of bytes received:" in linesArr[i]:
                numberOfReceivedBytesOverlay = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            if acquired >= 2:
                break
    elif "16000" in parsed["ApplicationalMessagesSent"]:
        numberOfSentMessagesOverlay = parsed["ApplicationalMessagesSent"]["16000"]
        numberOfReceivedMessagesOverlay = parsed["ApplicationalMessagesReceived"]["16000"]

        f = open(os.path.join(dirname.path + '/', "Hyparview.log"), 'r')  # open in readonly mode

        lines = f.readlines()
        # Strips the newline character
        linesArr = []
        for line in lines:
            linesArr.append(line)

        if len(linesArr) == 0:
            raise "Error! No lines in app file."

        acquired = 0
        for i in range(len(linesArr) - 1, -1, -1):
            if "Number of bytes sent:" in linesArr[i]:
                numberOfSentBytesOverlay = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Number of bytes received:" in linesArr[i]:
                numberOfReceivedBytesOverlay = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            if acquired >= 2:
                break
    else:
        raise "Error! Could not parse sent/received bytes from the overlay layer."

    if "15000" in parsed["ApplicationalMessagesSent"]:
        numberOfSentMessagesOverlayResest = parsed["ApplicationalMessagesSent"]["15000"]
        numberOfReceivedMessagesOverlayResest = parsed["ApplicationalMessagesReceived"]["15000"]

        f = open(os.path.join(dirname.path + '/', "CyclonResEst.log"), 'r')  # open in readonly mode

        lines = f.readlines()
        # Strips the newline character
        linesArr = []
        for line in lines:
            linesArr.append(line)

        if len(linesArr) == 0:
            raise "Error! No lines in app file."

        acquired = 0
        for i in range(len(linesArr) - 1, -1, -1):
            if "Number of bytes sent:" in linesArr[i]:
                numberOfSentBytesOverlayResest = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Number of bytes received:" in linesArr[i]:
                numberOfReceivedBytesOverlayResest = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            if acquired >= 2:
                break
    elif "12000" in parsed["ApplicationalMessagesSent"]:
        numberOfSentMessagesOverlayResest = parsed["ApplicationalMessagesSent"]["12000"]
        numberOfReceivedMessagesOverlayResest = parsed["ApplicationalMessagesReceived"]["12000"]

        f = open(os.path.join(dirname.path + '/', "HyparviewResEst.log"), 'r')  # open in readonly mode

        lines = f.readlines()
        # Strips the newline character
        linesArr = []
        for line in lines:
            linesArr.append(line)

        if len(linesArr) == 0:
            raise "Error! No lines in app file."

        acquired = 0
        for i in range(len(linesArr) - 1, -1, -1):
            if "Number of bytes sent:" in linesArr[i]:
                numberOfSentBytesOverlayResest = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Number of bytes received:" in linesArr[i]:
                numberOfReceivedBytesOverlayResest = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            if acquired >= 2:
                break
    else:
        raise "Error! Could not read bytes sent/received from resest overlay!"

    nodeResources.append(nodeResource)

    nodesSentReceivedMessages.append(((numberOfSentMessagesResest, numberOfReceivedMessagesResest),
                                            (numberOfSentMessagesOverlayResest, numberOfReceivedMessagesOverlayResest),
                                            (numberOfSentMessagesOverlay, numberOfReceivedMessagesOverlay)))

    nodesSentReceivedBytes.append(((resestNumBytesSent, resestNumBytesReceived),
                                         (numberOfSentBytesOverlayResest, numberOfReceivedBytesOverlayResest),
                                         (numberOfSentBytesOverlay, numberOfReceivedBytesOverlay)))

    nodeCount += 1


reliabilityPerFloodedMsg = []
messageLatencies = []

# Getting latency and throughput
for (mID, mTime) in msgBroadcastedSet:
    highestLatency = 0.0
    receivedCount = 0
    for i in range(0, NUMBER_OF_NODES):
        if mID in nodesMsgsReceived[i]:
            receivedCount += 1
            if nodesMsgsReceived[i][mID] > highestLatency:
                highestLatency = nodesMsgsReceived[i][mID]
    if highestLatency != 0.0:
        messageLatencies.append(highestLatency-mTime)
    else:
        sys.stderr.write("Error in reading latency of message: " + mID + "\n")
        sys.stderr.write("Flooded at time: " + str(mTime) + "\n")
    reliabilityPerFloodedMsg.append(float(receivedCount)/float(NUMBER_OF_NODES))

avgReliability = sum(reliabilityPerFloodedMsg) / len(reliabilityPerFloodedMsg)
avgLatency = sum(messageLatencies) / len(messageLatencies)

avgDiffNeighNumberPerNode = sum(diffNeighNumberPerNode) / len(diffNeighNumberPerNode)


numberOfMessagesSentPerNode = []
numberOfMessagesReceivedPerNode = []
for i in range(0, len(nodesSentReceivedMessages)):
    ((a, b), (c, d), (e, f)) = nodesSentReceivedMessages[i]
    numberOfMessagesSentPerNode.append(a + c + e)
    numberOfMessagesReceivedPerNode.append(b + d + f)

sumOfMessagesSent = 0
sumOfMessagesReceived = 0
sumOfBytesSent = 0
sumOfBytesReceived = 0
for i in range(0, len(nodesSentReceivedMessages)):
    sumOfMessagesSent += numberOfMessagesSentPerNode[i]
    sumOfMessagesReceived += numberOfMessagesReceivedPerNode[i]
    ((a, b), (c, d), (e, f)) = nodesSentReceivedBytes[i]
    sumOfBytesSent += a + c + e
    sumOfBytesReceived += b + d + f

avgNumberOfMessagesSentPerNode = float(sumOfMessagesSent) / float(NUMBER_OF_NODES)
avgNumberOfMessagesReceivedPerNode = float(sumOfMessagesReceived) / float(NUMBER_OF_NODES)
avgNumberOfBytesSentPerNode = float(sumOfBytesSent) / float(NUMBER_OF_NODES)
avgNumberOfBytesReceivedPerNode = float(sumOfBytesReceived) / float(NUMBER_OF_NODES)


avgNodesAddedOverTime = []
avgNodesRemovedOverTime = []
xAxis = []

minLength = 10000000
for i in range(0, NUMBER_OF_NODES):
    if len(numNeighsAddedPerNodeOverTime[i]) < minLength:
        minLength = len(numNeighsAddedPerNodeOverTime[i])

for i in range(0, minLength):
    sumA = 0
    sumR = 0
    for j in range(0, NUMBER_OF_NODES):
        sumA += numNeighsAddedPerNodeOverTime[j][i]
        sumR += numNeighsRemovedPerNodeOverTime[j][i]
    avgNodesAddedOverTime.append(float(sumA) / float(NUMBER_OF_NODES))
    avgNodesRemovedOverTime.append(float(sumR) / float(NUMBER_OF_NODES))
    xAxis.append(str(i+1))

# Plot variability in bars
fig1 = plt.figure(figsize=(10, 5))

plt.bar(xAxis, avgNodesAddedOverTime)  #, avgNodesRemovedOverTime)
plt.bar(xAxis, avgNodesRemovedOverTime)
plt.title('Average Neighbor Variability Over Time')
plt.xlabel('Time (min)')
plt.ylabel('Neighbor Delta')

fig1.savefig(exp_folder + 'variability_over_time.png', bbox_inches='tight', dpi=150)
plt.clf()


fw = open(exp_folder + "final_results.txt", 'w')

towrite = ["Avg latency: ", str(avgLatency), "\n\n", "Avg reliability: ", str(avgReliability), "\n\n",
           "Avg diff. node neighbor number error: ", str(avgDiffNeighNumberPerNode), "\n\n",
           "Avg number of messages (from overlays + resest) sent per node: ", str(avgNumberOfMessagesSentPerNode), "\n\n",
           "Avg number of bytes (from overlays + resest) sent per node: ", str(avgNumberOfBytesSentPerNode), "\n\n",
           # "Avg number of messages received per node: ", str(avgNumberOfMessagesReceivedPerNode), "\n\n",
           # "Avg number of bytes received per node: ", str(avgNumberOfBytesReceivedPerNode), "\n\n",
           ]

fw.writelines(towrite)
fw.close()


# Making the plot of resources - flood messages sent

nodeResMsgSent = dict()

for i in range(len(nodeResources)):
    if nodeResources[i] not in nodeResMsgSent:
        nodeResMsgSent[nodeResources[i]] = []
    nodeResMsgSent[nodeResources[i]].append(nodesFloodSentMessages[i])

uniqueResourcesArr = []
sentMsgsPerUniqueResArr = []
for k, v in sorted(nodeResMsgSent.items()):
    uniqueResourcesArr.append(k)
    sentMsgsPerUniqueResArr.append(sum(v) / len(v))

# Finally, make and save plot
# x axis values
x = uniqueResourcesArr
# corresponding y axis values
y = sentMsgsPerUniqueResArr

fig2 = plt.figure(figsize=(10, 5))

# plotting the points
plt.plot(x, y, 'o')

# Regression
x = np.array(x)
y = np.array(y)
m, b = np.polyfit(x, y, 1)
plt.plot(x, m*x + b)

# naming the x axis
plt.xlabel('x - resource level')
# naming the y axis
plt.ylabel('y - number of messages sent')

# giving a title to my graph
plt.title('Number of applicational messages sent by node with resource level x')

fig2.savefig(exp_folder + 'resources_messages_sent_plot.png', bbox_inches='tight', dpi=150)

# function to show the plot
# plt.show()

# TODO falta testar para ver se isto esta bem
