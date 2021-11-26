import collections
import subprocess

import sys
import time

if sys.argv[1] == "1":
    print("Installing docker...")
    b = subprocess.Popen(["scripts/grid/install_docker.sh"])
    b.wait()
    print("Docker supposedly installed.")
    print("Checking if docker was actually installed well on all machines.")
    f1 = open("../dockerstdout.txt", "w")
    f2 = open("../dockerstderr.txt", "w")
    b = subprocess.Popen(["scripts/grid/test_if_docker_is_installed.sh"], stdout=f1, stderr=f2)
    b.wait()
    print("Files ../dockerstdout.txt and ../dockerstderr.txt supposedly created.")

print("Installing python3 requirements...")
b = subprocess.Popen(["pip3", "install", "-r", "scripts/grid/requirements.txt"])
b.wait()
print("Requirements installed.\n")

print("Setting up 1000 nodes in lognormal fashion...")
b = subprocess.Popen(["deploy/setup.sh", "1000", "lognormal"])
b.wait()
print("1000 nodes lognormal set-up.")


print("Doing lognormal 1000 nodes tests.")

tests = collections.OrderedDict()

tests["test90"] = [
    "config_1000nodes_hyparview_lognormal_90_025.properties",
    "config_1rot_simple.properties",
    "config_01s_10000bytes.properties",
    "hyparview",
    "random",
]

tests["test91"] = [
    "config_1000nodes_hyparview_lognormal_90_025.properties",
    "config_1rot_simple.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]

for i in range(90, 92):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "1000", "test"+str(i), "3720",
                          "-resEstConfig", tests["test"+str(i)][0],
                          "-foutakosConfig", tests["test"+str(i)][1],
                          "-appConfig", tests["test"+str(i)][2],
                          "-membership", tests["test"+str(i)][3],
                          "-hpvRemovalMode", tests["test"+str(i)][4],
                          "-service", "flood"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/resest/aggregate_results_resest.py", "test"+str(i)])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "1000", "lognormal"])
    b.wait()


tests["test24"] = [
    "config_01s_10000bytes_simple.properties",
]

tests["test25"] = [
    "config_005s_100bytes_simple.properties",
]

for i in range(24, 26):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "1000", "test"+str(i), "2400", # TODO change to 3620 after bandwidth tests
                          "-appConfig", tests["test"+str(i)][0],
                          "-membership", "hyparview",
                          "-service", "flood",
                          "-noOptimization"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp_noOptimization.py", "test"+str(i), "1000", "lognormal"])
    b.wait()

