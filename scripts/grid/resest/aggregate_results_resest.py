import os
import sys

LOG_FOLDER = "../asdLogs/"

# LOG_FOLDER = "tmp/"
# LOG_FOLDER = "/Users/hisetip/Downloads/"

exp_folder = LOG_FOLDER + sys.argv[1] + '/'

totalNumHops = []
totalHistErr = []

totalNumberOfTriggered = 0
totalNumberTTLHit = 0
totalNumberAM = 0

for dirname in os.scandir(exp_folder):
    if not dirname.is_dir():
        continue
    f = open(os.path.join(dirname.path + '/', "ResEst.log"), 'r')  # open in readonly mode
    lines = f.readlines()
    # Strips the newline character
    linesArr = []
    for line in lines:
        linesArr.append(line)

    if len(linesArr) == 0:
        continue

    try:
        acquired = 0
        numHops = -1
        histErr = -1
        for i in range(len(linesArr) - 1, -1, -1):
            if "Total number of hops:" in linesArr[i]:
                numHops = int(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Histogram Error:" in linesArr[i]:
                histErr = float(linesArr[i].split()[-1][:-1])
                acquired += 1
            elif "Sending resource estimation message" in linesArr[i]:
                totalNumberOfTriggered+=1
            elif "TTL hit." in linesArr[i]:
                totalNumberTTLHit+=1
            elif "Received histogram answermessage from" in linesArr[i]:
                totalNumberAM+=1

            if acquired >= 2:
                break
    except:
        print("Error occured in " + dirname.path + ". Skipping.")
        continue

    if numHops != -1 and histErr != -1:
        totalNumHops.append(numHops)
        totalHistErr.append(histErr)
    else:
        print("Error occured in " + dirname.path + ". Skipping.")

fw = open(exp_folder + "resest_results.txt", 'w')

towrite = ["Number of experiments with results:\n", str(len(totalNumHops)), '\n\n',
           "Number of hops of all experiments:\n", str(totalNumHops), '\n\n', "Histogram errors of all experiments:\n",
           str(totalHistErr), '\n\n', "Average number of hops:\n", str(sum(totalNumHops) / len(totalNumHops)), '\n',
           "Average histogram errors:\n", str(sum(totalHistErr) / len(totalHistErr)), '\n',
           "totalNumberOfTriggered:\n", str(totalNumberOfTriggered), '\n',
           "totalNumberTTLHit:\n", str(totalNumberTTLHit), '\n',
           "totalNumberAM:\n", str(totalNumberAM), '\n']


fw.writelines(towrite)
fw.close()
