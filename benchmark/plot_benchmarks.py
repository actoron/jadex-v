import os
import json
import re

import matplotlib.pyplot as plt
import numpy as np


######## fetch values from file system ########

#dirname = '.benchmark_ide'
dirname = '.benchmark_gradle'

memory_values = {}
time_values = {}

files = os.listdir('.')
dirs = [f for f in files if os.path.isdir(f)]
for dir in dirs:
    if os.path.isdir(dir+'/'+dirname):
        files = os.listdir(dir+'/'+dirname)
        for file in files:
            if file.endswith('.json'):
                with open(dir+'/'+dirname+'/'+file) as jfile:
                    
                    # load / adapt json
                    val = json.load(jfile)
                    if type(val) == float:
                        val = {'best':val, 'last':val}
                    if file.endswith('Time.json'):
                        # round from nanos to microseconds
                        if val['best']>10000:
                            val['best']   = round(val['best']/1000)
                        else:
                            val['best']   = round(val['best']/100)/10
                        if val['last']>10000:
                            val['last']   = round(val['last']/1000)
                        else:
                            val['last']   = round(val['last']/100)/10
                    
                    val['delta']    = val['last'] - val['best']
                    
                    # get bar name from filename
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
                        time_values[name]   = val
                    elif file.endswith('Memory.json'):
                        memory_values[name] = val


######## global plot settings ########

plt.rcParams.update({'font.size': 12})
width   = max(len(memory_values), len(time_values))

######## plot/save memory usage ########

if len(memory_values)>0:
    best    = [val['best'] for val in memory_values.values()]
    plt.figure(figsize=(width, 6))
    ax  = plt.subplot(1, 1, 1)
    bars    = plt.bar(memory_values.keys(), best, color='skyblue')
    bars2   = plt.bar(memory_values.keys(), [val['delta'] for val in memory_values.values()], color='red', bottom=best)
    ax.bar_label(bars, label_type='center')
    ax.bar_label(bars2)
    ymax = max([val['last'] for val in memory_values.values()])
    ax.set_ylim(0, ymax*1.2)
    plt.ylabel('Memory Footprint (KB)')
    #plt.title('Memory Footprint of Different Agents/Components')
    
    plt.tight_layout()
    plt.margins(x=0.2/len(memory_values))
    
    plt.savefig('benchmark_memory.png')
    
    plt.show()
else:
    print("No memory benchmarks found. Try e.g. ./gradlew benchmark-micro:benchmark")


######## plot/save time usage ########

if len(time_values)>0:
    best    = [val['best'] for val in time_values.values()]
    plt.figure(figsize=(width, 6))
    ax  = plt.subplot(1, 1, 1)
    bars    = plt.bar(time_values.keys(), best, color='green')
    bars2   = plt.bar(time_values.keys(), [val['delta'] for val in time_values.values()], color='red', bottom=best)
    ax.bar_label(bars, label_type='center')
    ax.bar_label(bars2)
    ymin = min(best)
    ymax = max([val['last'] for val in time_values.values()])
    ax.set_ylim(ymin/2, ymax*2)
    plt.yscale('log')
    plt.ylabel('Execution Time (µs)')
    #plt.title('Startup/Shutdown Time of Different Agents/Components')
    plt.grid(True, which="both", ls="--")
    
    plt.tight_layout()
    plt.margins(x=0.2/len(time_values))
    
    plt.savefig('benchmark_execution.png')
    
    plt.show()
else:
    print("No time benchmarks found. Try e.g. ./gradlew benchmark-micro:benchmark")
