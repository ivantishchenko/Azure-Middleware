import matplotlib.pyplot as plt

# Pie chart, where the slices will be ordered and plotted counter-clockwise:
labels = 'Servers', 'Middlewares', 'Worker threads', 'Servers & Middlewares', 'Servers & Worker threads', 'Middlewares & Worker threads', 'Servers & Middlewares & Worker threads'
sizes_SET_T = [4.82, 33.93, 55.39, 0.22, 0.44, 4.7, 0.01]
sizes_SET_R = [5.65, 32.17, 55.32, 0.72, 1.11, 4.43, 0.12]

sizes_GET_T = [0.74, 38.25, 56.25, 0.00, 0.14, 3.28, 0.13]
sizes_GET_R = [2.11, 36.30, 55.90, 0.13, 0.64, 4.13, 0.19]


sizes_MIX_T = [3.62, 33.57, 46.62, 1.32, 0.88, 2.84, 0.45]
sizes_MIX_R = [0.92, 38.23, 55.90, 0.05, 0.49, 2.83, 0.11]



explode = (0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1)  # only "explode" the 2nd slice (i.e. 'Hogs')

fig1, ax1 = plt.subplots()
patches, texts = plt.pie(sizes_SET_T, explode=explode, labels=labels, autopct='%1.1f%%',shadow=True, startangle=90)
plt.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

plt.legend(patches, labels, loc="lower left")
plt.tight_layout()
plt.savefig("pie_SET_T.png")