import matplotlib.pyplot as plt

fig, ax = plt.subplots()

NUM_SERVERS = 3
loads = [318085, 316155, 316358]
loads_multiget = [25023, 25023, 25022]
# rep2 = [319351, 317349, 316976]
# [294879, 293679, 295232]
# [292145, 291415, 291442]
# [291833, 288222, 289314]

servers = list(range(1, NUM_SERVERS+1))
print(servers)

# ax.bar(servers, loads, alpha=0.5, color=['red', 'green', 'blue'])
# ax.set_xticks(servers)
# ax.set_xticklabels(['Server 1', 'Server 2', 'Server 3'])
# ax.margins(0.05)
# ax.set_ylabel('Number of requests')
# ax.set_xlabel('Servers')
# ax.set_title('Servers load round robin scheduling')
#
# plt.savefig("eq_load.png")


ax.bar(servers, loads_multiget, alpha=0.5, color=['red', 'green', 'blue'])
ax.set_xticks(servers)
ax.set_xticklabels(['Server 1', 'Server 2', 'Server 3'])
ax.margins(0.05)
ax.set_ylabel('Number of requests')
ax.set_xlabel('Servers')
ax.set_title('Servers load round robin scheduling')

plt.savefig("eq_load_multi.png")