import csv
import matplotlib.pyplot as plt

file_path = '/home/ivan/asl-fall17-project/experiments/logfiles/baseline/azure_metrics/netout_server.csv'
machine = 'foraslvms6'

data = []

with open(file_path) as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        val = row[machine + ' Network Out (Avg)']
        if ( val != '') and (val != machine + ', Network Out (Avg)'):
            out = float(val)
            data.append(out) 

#x = [a for a in range(len(data))]

data = [x / 10**6 for x in data]

plt.plot(data, color='blue', alpha=0.8)
plt.ylim(ymax=max(data))
plt.ylim(ymin=0)
plt.xlim(xmin=0)

plt.xlabel('Time')
plt.ylabel('Network OUT MBytes')
plt.title("Network output load during the experiments")

plt.savefig("baseline_azure_NETOUT.png")
