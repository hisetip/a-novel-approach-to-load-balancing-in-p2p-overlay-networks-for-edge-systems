import subprocess
for i in range(0,25):
    if i == 9:
        continue
    print("Doing test"+str(i))
    if i == 0:
        b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "1000", "uniform"])
        b.wait()
    elif i >= 15 and i < 21:
        b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp_noOptimization.py", "test"+str(i), "1000", "lognormal"])
        b.wait()
    else:
        b = subprocess.Popen(["python3", "scripts/grid/get_metrics_exp.py", "test"+str(i), "1000", "lognormal"])
        b.wait()
