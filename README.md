# City Network & Job Scheduler

Alua Rakhimzhanova
SE-2509
DAA Assignment 3

## About

This is my third assignment for DAA. The project works with road networks and job dependencies.

I used Kruskal and Prim to find a minimum spanning forest, Kosaraju to find strongly connected components, and Kahn to find a topological order. The project also finds shortest paths in a DAG and restores the paths using a parent array.

For the bonus tasks, I added Tarjan, a condensation graph and a longest path in that graph.

The graph uses an adjacency list and supports directed and undirected edges. DFS uses a stack instead of recursion so it can handle 100,000 vertices. Total weights and distances use long.

## How to run

Java 17 or newer and Maven are required. Run the commands from the project folder.

Build the project and run the tests:

```sh
mvn test
```

Run the examples:

```sh
java -cp target/classes benchmark.Main data/disconnected.txt
java -cp target/classes benchmark.Main data/cyclic.txt
java -cp target/classes benchmark.Main data/dag.txt
```

Run the main benchmark:

```sh
java -Xmx3g -cp target/classes benchmark.Main benchmark
```

Run the bonus checks:

```sh
java -Xmx2g -cp target/classes benchmark.BonusBenchmark
```

## Tests and results

The tests compare DSU with a simple reference, MSTs with each other and brute force, SCCs with BFS reachability, and DAG paths with Bellman-Ford. They also check disconnected graphs, self-loops, parallel edges, negative weights and a path with 100,000 vertices.

The benchmark uses Random(42), five graph sizes and two densities. Each algorithm runs five times, and the CSV stores the median time. It checks the reference values automatically and stops with an error if a result is wrong.

All ten reference rows match. The main results are in results/results.csv, and the bonus results are in results/bonus.csv. The plots are in results/plots. The report explains the algorithms, complexity and measured results.

## Plots

The three plots are already included. To rebuild them with Python 3.10 or newer:

```sh
python -m pip install -r scripts/requirements.txt
python scripts/plots.py
```

The script also prints the ratios and slopes from the report.

## Project structure

- src/main/java/graph - Graph and Edge
- src/main/java/algorithms - graph algorithms and DSU
- src/main/java/metrics - operation counters
- src/main/java/benchmark - generator and program entry points
- src/test/java/tests - JUnit 5 tests
- data - small input graphs
- results - CSV files and plots
- REPORT.pdf - report

## Git history

The branches are main, feature/dsu, feature/mst, feature/scc, feature/paths and feature/metrics. The release tag is v1.0.
