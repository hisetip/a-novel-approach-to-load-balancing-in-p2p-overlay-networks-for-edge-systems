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

tests = collections.OrderedDict()

print("Setting up 1000 nodes in uniform fashion...")
b = subprocess.Popen(["deploy/setup.sh", "1000", "uniform"])
b.wait()
print("1000 nodes uniform set-up.")

tests["test0"] = [
    "config_1000nodes_hyparview_uniform_90_025.properties",
    "config_1rot_simple.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]

for i in range(0, 1):
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
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "1000", "uniform"])
    b.wait()



print("Setting up 1000 nodes in lognormal fashion...")
b = subprocess.Popen(["deploy/setup.sh", "1000", "lognormal"])
b.wait()
print("1000 nodes lognormal set-up.")

tests["test1"] = [
    "config_1000nodes_hyparview_lognormal_90_025.properties",
    "config_1rot_simple.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test2"] = [
    "config_1000nodes_cyclon_lognormal_90_025.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "cyclon",
    "random",
]
tests["test3"] = [
    "config_1000nodes_cyclon_lognormal_90_025.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test4"] = [
    "config_1000nodes_hyparview_lognormal_90_025.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "cyclon",
    "random",
]
tests["test5"] = [
    "config_1000nodes_hyparview_lognormal_90_025.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test6"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test7"] = [
    "config_1000nodes_hyparview_lognormal_95_015_replaceWeak.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test8"] = [
    "config_1000nodes_hyparview_lognormal_95_015_replaceStrong.properties",
    "config_1rot_average.properties",
    "config_005s_1000bytes.properties",
    "hyparview",
    "random",
]
tests["test9"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_005s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test10"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_005s_1000bytes.properties",
    "hyparview",
    "random",
]
tests["test11"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_005s_10000bytes.properties",
    "hyparview",
    "random",
]
tests["test12"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_01s_100bytes.properties",
    "hyparview",
    "random",
]
tests["test13"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_01s_1000bytes.properties",
    "hyparview",
    "random",
]
tests["test14"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_01s_10000bytes.properties",
    "hyparview",
    "random",
]

for i in range(1, 15):
    if i == 7 or i == 8 or i == 9: # test 9 is equal to test 6
        continue
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


tests["test15"] = [
    "config_005s_100bytes_simple.properties",
]
tests["test16"] = [
    "config_005s_1000bytes_simple.properties",
]
tests["test17"] = [
    "config_005s_10000bytes_simple.properties",
]
tests["test18"] = [
    "config_01s_100bytes_simple.properties",
]
tests["test19"] = [
    "config_01s_1000bytes_simple.properties",
]
tests["test20"] = [
    "config_01s_10000bytes_simple.properties",
]

for i in range(15, 21):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "1000", "test"+str(i), "2400",
                          "-appConfig", tests["test"+str(i)][0],
                          "-membership", "hyparview",
                          "-service", "flood",
                          "-noOptimization"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp_noOptimization.py", "test"+str(i), "1000", "lognormal"])
    b.wait()


tests["test21"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_01s_10000bytes.properties",
    "hyparview",
    "mostPowerful",
]
tests["test22"] = [
    "config_1000nodes_hyparview_lognormal_95_015.properties",
    "config_1rot_average.properties",
    "config_01s_10000bytes.properties",
    "hyparview",
    "leastPowerful",
]

for i in range(21, 23):
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