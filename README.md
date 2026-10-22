# City Network & Job Scheduler

Alua Rakhimzhanova, SE-2509. DAA Assignment 3.

Java 17 or newer and Maven are required. All graph algorithms are implemented in this project. Java collections are used for lists, queues and sorting, as allowed by the assignment. Source code contains no comments.

## Build and test

Run these commands from the project folder:

```sh
mvn clean test
```

There are 10 JUnit 5 tests. The randomized tests include many separate graphs and checks. Tests cover DSU against simple labels, 100 connected MST graphs, exhaustive MST search, SCC against BFS reachability, DAG paths against Bellman-Ford, file inputs, unusual edges, condensation, reference values and 100,000-vertex stack safety.

## Run a small example

```sh
java -cp target/classes benchmark.Main data/disconnected.txt
java -cp target/classes benchmark.Main data/cyclic.txt
java -cp target/classes benchmark.Main data/dag.txt
```

The undirected example prints forest weight 6 and 4 components. The cyclic example prints 4 SCCs, 18 Kosaraju scans, 9 Tarjan scans and critical path weight 12. SCC IDs are labels, not original vertex numbers. The DAG example prints distances `[0, 0, 2, -3, -2, 1, 0]` and restored paths.

## Reproduce the results

```sh
mvn test
java -Xmx3g -cp target/classes benchmark.Main benchmark
java -Xmx2g -cp target/classes benchmark.BonusBenchmark
```

The main benchmark produces `results/results.csv`: exactly 50 rows, five algorithms on ten size/density pairs. It uses a fresh `Random(42)` for every graph kind and pair. When a random edge is a self-loop, only its second endpoint is redrawn. Every main algorithm is run five times, and the CSV stores the median time. Results and counters must agree across runs. All ten reference rows, both MST results, tree sizes, topological orders and required scan counts are checked automatically. A failed check throws an exception; the complete CSV is replaced only after a successful run.

For a faster reference check:

```sh
java -cp target/classes benchmark.Main benchmark quick
```

This checks both densities at n = 1,000 and writes `results/quick.csv`; it does not replace the full results.

The separate bonus benchmark produces `results/bonus.csv`. It checks Tarjan and Kosaraju partitions on all ten directed graphs, verifies m versus 2m scans, constructs each condensation DAG and restores a critical path. Bonus rows stay separate so the required main CSV remains exactly 50 rows.

## Rebuild the plots

Python 3.10 or newer is needed only for the plots and analysis:

```sh
python -m pip install -r scripts/requirements.txt
python scripts/analyze.py
```

This uses ReportLab charts and PyMuPDF to produce the three PNG charts and `results/analysis.json`. All chart values come from the measured CSV. `scripts/make_report.py` also rebuilds `REPORT.md` and the five-page `REPORT.pdf` from those results.

On Windows, after installing the Python requirements, the whole workflow is:

```powershell
.\run-all.ps1
```

Times will change between machines and runs. The reference results and required traversal scan counts must stay the same. The report supplied here describes the included run.

## Files and counter meanings

`graph` contains Graph and Edge; `algorithms` contains DSU, both MSTs, both SCCs, Kahn, DAG paths and condensation; `metrics` stores counters; `benchmark` contains the exact generator, demo and benchmarks. The three files in `data/` include disconnected, cyclic and negative-weight cases.

`edge_scans` counts adjacency entries visited by traversals, or sorted candidates examined by Kruskal. Kosaraju counts its two DFS passes; building the reversed graph is separate preprocessing. Kahn counts removal-time scans; computing in-degrees is separate preprocessing. DAG paths count the relaxation pass; their internal Kahn call has separate metrics. These conventions give exactly 2m, m and m as required, while each complete procedure remains O(V + E). An undirected edge has two adjacency entries, so Prim scans 2m.

`comparisons` counts calls to the weight comparator in sorting or the priority queue. `dsu_ops` is public find calls plus successful unions; a successful Kruskal union performs its own two finds after the two explicit cycle-check finds. `max_depth` is the largest occupied explicit DFS stack, with a root counting as depth 1; it is zero for algorithms without that stack. Tarjan's active SCC stack is a separate data structure. None of these metrics is filled from a formula.

The main operation plot uses `edge_scans + comparisons + dsu_ops`. This is a defined work proxy, not a count of every CPU instruction. In particular, DSU find calls do not count every parent pointer traversal, so this benchmark illustrates scaling but does not empirically prove the inverse-Ackermann theorem.

Condensation removes internal edges and keeps the largest weight among parallel component-to-component edges, because the bonus asks for a longest path. A critical path here is a path in the condensed dependency model. It does not describe a longest simple route through the original cyclic graph or the duration of work inside an SCC.

## Git and submission

The archive includes `git-history.bundle` with main, all five required feature branches and tag v1.0. To recover the repository and publish it to a new empty GitHub repository:

```sh
git clone git-history.bundle city-network
cd city-network
git remote set-url origin https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
git push -u origin main
git push origin --all
git push origin v1.0
```

Replace the example URL with your real empty repository URL. The repository has not been published to GitHub. Add the real GitHub link to your Moodle submission. Upload `DAA_Assignment3_Alua_Rakhimzhanova_SE-2509.zip`.

## AI-use acknowledgement

OpenAI ChatGPT assisted with the implementation, tests, benchmarks and report. Correctness was checked with JUnit tests, independent reference methods and the assignment's reference values.
