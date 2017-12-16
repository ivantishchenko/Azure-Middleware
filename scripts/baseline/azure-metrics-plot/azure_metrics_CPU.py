import csv
import matplotlib.pyplot as plt

file_path = '/home/ivan/asl-fall17-project/experiments/logfiles/baseline/azure_metrics/cpu_server.csv'
machine = 'foraslvms6'

data = []

with open(file_path) as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        val = row[machine + ' Percentage CPU (Avg)']
        if ( val != '') and (val != machine + ', Percentage CPU (Avg)'):
            out = float(val)
            data.append(out) 

#x = [a for a in range(len(data))]

plt.plot(data, color='blue', alpha=0.8)
plt.ylim(ymax=100)
plt.ylim(ymin=0)
plt.xlim(xmin=0)

plt.xlabel('Time')
plt.ylabel('CPU %')
plt.title("CPU load during the experiments")

plt.savefig("baseline_azure_CPU.png")

