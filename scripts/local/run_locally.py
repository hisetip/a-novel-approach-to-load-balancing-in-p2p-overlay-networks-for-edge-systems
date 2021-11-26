import subprocess
import time

LOG_FOLDER = "/Users/hisetip/Google_Drive/FCT_NOVA/5thYear/MSc-Thesis/code/protocol-stack/tmp/logs/"
NUMBER_OF_PROCESSES = 20
FIRST_PORT = 53000

print("Building the project.")
b = subprocess.Popen(["go", "build", "-o", "local-protocol-stack", "main.go"])
b.wait()

processes = []
for i in range(0, NUMBER_OF_PROCESSES):
    process_number = i + 1
    time.sleep(1)
    print("Starting process " + str(process_number))
    argArray = ["./local-protocol-stack",
                "-membership", "cyclon",
                "-service", "resource-estimator",
                "-peerID", str(process_number),
                "-listenPort", str(FIRST_PORT + i),
                "-confidenceInterval", "0.25",
                "-safetyTTL", str(NUMBER_OF_PROCESSES - 3),
                "-logfolder", LOG_FOLDER]
    # Note that we're not using -usechannel.
    if process_number >= 2:
        argArray.append("-bootstraps")
        argArray.append("127.0.0.1:" + str(FIRST_PORT + i - 1))

    processes.append(subprocess.Popen(argArray,
                                      stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True))

time.sleep(60)
subprocess.Popen(["killall", "local-protocol-stack"])

for i in range(0, NUMBER_OF_PROCESSES):
    std_out, std_err = processes[i].communicate()
    print(std_err)
