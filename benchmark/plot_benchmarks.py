import os
import json
import re

import matplotlib.pyplot as plt
import numpy as np


######## fetch values from file system ########

dirname = '.benchmark'
#dirname = '.benchmark_gradle'

memory_names = []
memory_values = []
time_names = []
time_values = []

files = os.listdir('.')
dirs = [f for f in files if os.path.isdir(f)]
for dir in dirs:
    if os.path.isdir(dir+'/'+dirname):
        files = os.listdir(dir+'/'+dirname)
        for file in files:
            if file.endswith('.json'):
                with open(dir+'/'+dirname+'/'+file) as jfile:
                    val = json.load(jfile)
                    names   = re.findall('[A-Z][^A-Z]*', file)
                    names   = [n for n in names if not 'enchmark' in n and not '.json' in n]
                    name    = None
                    for n in names:
                        if name is None:
                            name    = n
                        elif name[-1].islower():
                            name    = name + '\n' + n
                        else:
                            name    = name + n
                    #print(name+': '+str(val))
                    if file.endswith('Time.json'):
                        time_names.append(name)
                        # round from nanos to microseconds
                        if val>10000:
                            time_values.append(round(val/1000))
                        else:
                            time_values.append(round(val/100)/10)
                    elif file.endswith('Memory.json'):
                        memory_names.append(name)
                        memory_values.append(val)


######## global plot settings ########

plt.rcParams.update({'font.size': 12})
width   = max(len(memory_names), len(time_names))

######## plot/save memory usage ########

plt.figure(figsize=(width, 6))
ax  = plt.subplot(1, 1, 1)
bars    = plt.bar(memory_names, memory_values, color='skyblue')
ax.bar_label(bars)
plt.ylabel('Memory Footprint (KB)')
#plt.title('Memory Footprint of Different Agents/Components')

plt.tight_layout()

plt.savefig('benchmark_memory.png')

plt.show()


######## plot/save time usage ########

plt.figure(figsize=(width, 6))
ax  = plt.subplot(1, 1, 1)
bars    = plt.bar(time_names, time_values, color='green')
ax.bar_label(bars)
plt.yscale('log')
plt.ylabel('Startup/Shutdown Time (µs)')
#plt.title('Startup/Shutdown Time of Different Agents/Components')
plt.grid(True, which="both", ls="--")

plt.tight_layout()

plt.savefig('benchmark_execution.png')

plt.show()
