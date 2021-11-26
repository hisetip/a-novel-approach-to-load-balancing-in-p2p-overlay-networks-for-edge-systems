import subprocess

import sys
import time

if sys.argv[1] == "1":
    print("Installing docker...")
    b = subprocess.Popen(["scripts/grid/install_docker.sh"])
    b.wait()
    print("Docker installed.")


print("Setting up 500 nodes in lognormal fashion...")
b = subprocess.Popen(["deploy/setup.sh", "500", "lognormal"])
b.wait()
print("500 nodes lognormal set-up.")


print("Doing lognormal 500 nodes tests (8).")
test_names_lognormal = [
    "config_500nodes_hyparview_lognormal_90_025",
    "config_500nodes_hyparview_lognormal_90_015",
    "config_500nodes_hyparview_lognormal_95_025",
    "config_500nodes_hyparview_lognormal_95_015",
    "config_500nodes_cyclon_lognormal_90_025",
    "config_500nodes_cyclon_lognormal_90_015",
    "config_500nodes_cyclon_lognormal_95_025",
    "config_500nodes_cyclon_lognormal_95_015",
]

for i in range(0, len(test_names_lognormal)):
    print(test_names_lognormal[i])
    b = subprocess.Popen(["deploy/deploy.sh", "500", test_names_lognormal[i], "1300",
                          "-resEstConfig", test_names_lognormal[i]+".yml"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/resest/aggregate_results_resest.py", test_names_lognormal[i]])
    b.wait()


print("Setting up 500 nodes in uniform fashion...")
b = subprocess.Popen(["deploy/setup.sh", "500", "uniform"])
b.wait()
print("500 nodes uniform set-up.")

print("Doing uniform 500 nodes tests (8).")
test_names_uniform = [
    "config_500nodes_hyparview_uniform_90_025",
    "config_500nodes_hyparview_uniform_90_015",
    "config_500nodes_hyparview_uniform_95_025",
    "config_500nodes_hyparview_uniform_95_015",
    "config_500nodes_cyclon_uniform_90_025",
    "config_500nodes_cyclon_uniform_90_015",
    "config_500nodes_cyclon_uniform_95_025",
    "config_500nodes_cyclon_uniform_95_015",
]

for i in range(0, len(test_names_uniform)):
    print(test_names_uniform[i])
    b = subprocess.Popen(["deploy/deploy.sh", "500", test_names_uniform[i], "1300",
                          "-resEstConfig", test_names_uniform[i]+".yml"])
    b.wait()
    b = subprocess.Popen(["python3", "scripts/grid/resest/aggregate_results_resest.py", test_names_uniform[i]])
    b.wait()