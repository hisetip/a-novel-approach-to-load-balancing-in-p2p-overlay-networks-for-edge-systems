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

print("Setting up 500 nodes in lognormal fashion...")
b = subprocess.Popen(["deploy/setup.sh", "500", "lognormal"])
b.wait()
print("500 nodes lognormal set-up.")


print("Doing lognormal 500 nodes tests.")
tests = collections.OrderedDict()

"""
tests["test1"] = [
    "config_500nodes_hyparview_lognormal_90_025.yml",
    "config_1rot_simple.yml",
    "config_01s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test2"] = [
    "config_500nodes_cyclon_lognormal_90_025.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "cyclon",
    "random",
]
tests["test3"] = [
    "config_500nodes_cyclon_lognormal_90_025.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test4"] = [
    "config_500nodes_hyparview_lognormal_90_025.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "cyclon",
    "random",
]
tests["test5"] = [
    "config_500nodes_hyparview_lognormal_90_025.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test6"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test7"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_01s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test8"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_01s_1000bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test9"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_01s_100000bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test10"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_05s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test11"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_05s_1000bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test12"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_05s_100000bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test13"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_1s_100bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test14"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_1s_1000bytes_500nodes.yml",
    "hyparview",
    "random",
]
tests["test15"] = [
    "config_500nodes_hyparview_lognormal_95_015.yml",
    "config_1rot_average.yml",
    "config_1s_100000bytes_500nodes.yml",
    "hyparview",
    "random",
]


for i in range(1, len(tests)+1):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "500", "test"+str(i), "2000",
                          "-resEstConfig", tests["test"+str(i)][0],
                          "-foutakosConfig", tests["test"+str(i)][1],
                          "-appConfig", tests["test"+str(i)][2],
                          "-membership", tests["test"+str(i)][3],
                          "-hpvRemovalMode", tests["test"+str(i)][4],
                          "-service", "flood"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/resest/aggregate_results_resest.py", "test"+str(i)])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "500"])
    b.wait()
"""

tests["test16"] = [
    "config_01s_100bytes_500nodes.yml",
]
"""
tests["test17"] = [
    "config_01s_1000bytes_500nodes.yml",
]
tests["test18"] = [
    "config_01s_100000bytes_500nodes.yml",
]
tests["test19"] = [
    "config_05s_100bytes_500nodes.yml",
]
tests["test20"] = [
    "config_05s_1000bytes_500nodes.yml",
]
tests["test21"] = [
    "config_05s_100000bytes_500nodes.yml",
]
tests["test22"] = [
    "config_1s_100bytes_500nodes.yml",
]
tests["test23"] = [
    "config_1s_1000bytes_500nodes.yml",
]
tests["test24"] = [
    "config_1s_100000bytes_500nodes.yml",
]
"""
for i in range(16, 17):
    print(tests["test"+str(i)])
    b = subprocess.Popen(["deploy/deploy.sh", "500", "test"+str(i), "2000",
                          "-appConfig", tests["test"+str(i)][0],
                          "-membership", "hyparview",
                          "-service", "flood",
                          "-noOptimization"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp_noOptimization.py", "test"+str(i), "500"])
    b.wait()

# TODO tests 25-31