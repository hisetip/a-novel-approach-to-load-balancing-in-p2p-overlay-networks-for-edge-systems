import os
import sys
import time
from datetime import datetime

# LOG_FOLDER = "tmp/"

exp_folder = "/Users/hisetip/Documents/thesis_tests/resest/1000_nodes/config_1000nodes_cyclon_uniform_95_015"

totalNumHops = []
totalHistErr = []
lastTimes = []
time0 = -1.0

for dirname in os.scandir(exp_folder):
    if not dirname.is_dir():
        continue
    f = open(os.path.join(dirname.path + '/', "ResourceEstimator.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        continue

    time0 = datetime.strptime("2021 " + linesArr[0][6:21], '%Y %b %d %H:%M:%S').timestamp()

    try:
        acquired = 0
        numHops = -1
        histErr = -1
        obtainedCount = 0
        lastTime = -1.0
        for i in range(0, len(linesArr)):
            if "Total number of hops:" in linesArr[i]:
                numHops = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Histogram Error:" in linesArr[i]:
                histErr = float(linesArr[i].split()[-1][:-1])
                acquired += 1
                timePart = linesArr[i][6:21]
                lastTime = datetime.strptime("2021 " + timePart, '%Y %b %d %H:%M:%S').timestamp()

            if acquired >= 2:
                obtainedCount += 1
                acquired = 0
                if obtainedCount >= 10:
                    break
    except:
        print("Error occured in " + dirname.path + ". Skipping.")
        continue

    if numHops != -1 and histErr != -1:
        totalNumHops.append(numHops)
        totalHistErr.append(histErr)
        lastTimes.append(lastTime)
    else:
        print("Error occured in " + dirname.path + ". Skipping.")


highestTime = -1.0
highestNode = -1
for x in range(0, len(lastTimes)):
    if lastTimes[x] > highestTime:
        highestTime = lastTimes[x]
        highestNode = x

fw = open(exp_folder + "/time_to_ten_results.txt", 'w')

towrite = ["Time to 10 results:\n", str(highestTime-time0), "\n\n", str(lastTimes), "\n\n", str(highestNode)]

fw.writelines(towrite)
fw.close()
