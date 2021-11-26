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


print("Setting up 100 nodes in lognormal fashion...")
b = subprocess.Popen(["deploy/setup.sh", "100", "lognormal"])
b.wait()
print("100 nodes lognormal set-up.")


print("Doing lognormal 100 nodes tests.")
tests = collections.OrderedDict()


tests["test1"] = [
    "config_100nodes_hyparview_lognormal_95_025.properties",  # 200s + 300s
    "config_1rot_simple.properties",
    "config_1s_100000bytes_100nodes_test.properties",  # + 300s
    "hyparview",
    "random",
]

for i in range(1, len(tests)+1):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "100", "test"+str(i), "800",
                          "-resEstConfig", tests["test"+str(i)][0],
                          "-foutakosConfig", tests["test"+str(i)][1],
                          "-appConfig", tests["test"+str(i)][2],
                          "-membership", tests["test"+str(i)][3],
                          "-hpvRemovalMode", tests["test"+str(i)][4],
                          "-service", "flood"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/resest/aggregate_results_resest.py", "test"+str(i)])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "100", "lognormal"])
    b.wait()



tests["test19"] = [
    "config_1s_100000bytes_100nodes_test2.properties",
]

for i in range(19, 20):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "100", "test"+str(i), "600",
                          "-appConfig", tests["test"+str(i)][0],
                          "-membership", "hyparview",
                          "-service", "flood",
                          "-noOptimization"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp_noOptimization.py", "test"+str(i), "100", "lognormal"])
    b.wait()
    