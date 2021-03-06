import networkx as nx
from matplotlib import pyplot as plt
import random
import sys
###
if (len(sys.argv) != 4):
  print "Usage: \n > python graph.py nodeFileName edgeFileName mapFile"
  exit()
###

G = nx.Graph()
nodesFile = open(str(sys.argv[1]),"r")
edgesFile = open(str(sys.argv[2]),"r")
mapFile = open(str(sys.argv[3]),"r")

minWeight = 100000000 #some high number i picked for init
maxWeight = 0
j = 0.3
weights = []
MAXWEIGHT = 5.0
while j <= MAXWEIGHT:
  weights.append(j)
  j =  round(j + 0.05,7)
print weights
#weights = [0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0]

mapNodes = {} # dictionary to map old ids to new ones
# This file can be generated doing:
#    java Utility 25 test25regular-edges_min1.TopoVertices > vertices.txt
# with the option: printData() in the main of Utility
# Format: new_id-1: old_id-1
for line in mapFile:
  line = line.split()
  new_id = int(line[0][:-1]) + 1
  old_id = int(line[1]) + 1
  mapNodes[old_id] = new_id

nodes = {} #dict to store nodes with the x,y coords
for line in nodesFile:
  line = [int(x) for x in line.split()]
  nodes[mapNodes[line[0]]] = [line[1],line[2]] #add node as key, and val is the x,y coord
  G.add_node(line[0])

for line in edgesFile:
  line = [int(x) for x in line.split()]
  dest = line[1]
  origin = line[0]
  #weight = weights[random.randint(0,len(weights)-1)] #random for testing
  weight = int(line[2]) #if weight is in file
  if weight > maxWeight:
    maxWeight = weight
  if weight < minWeight:
    minWeight = weight
  G.add_edge(origin,dest,weight=weight)


#pos=nx.random_layout(G) # positions for all nodes

ew = []

for i in weights:
  #ew.append([(u,v) for (u,v,d) in G.edges(data=True) if d['weight'] == i])
  ew.append([(u,v) for (u,v,d) in G.edges(data=True) if round(((((float(d['weight']) - minWeight) * (MAXWEIGHT - 0.1)) / (maxWeight - minWeight)) + 0.1),1) == i])

#for i in range(len(G.edges())):
#  ew[G.edges[i]]



for i in zip(ew,weights):
  nx.draw_networkx_edges(G,pos=nx.spring_layout(G,iterations = 0,weight=1,pos=nodes),edgelist=i[0],width=i[1])
  #nx.draw_networkx_nodes(G,pos=nx.spring_layout(G,iterations = 0,weight=1,pos=nodes),edgelist=i[0],width=i[1],node_size=1)

# labels
#nx.draw_networkx_labels(G,pos=nx.spring_layout(G,iterations = 0,k=500,weight=1,pos=nodes),font_size=10,font_family='sans-serif')

#plt.show()
plt.savefig("path.pdf")
print "figure saved as: path.pdf"
