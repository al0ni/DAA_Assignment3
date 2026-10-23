# City Network & Job Scheduler

Alua Rakhimzhanova, SE-2509. DAA Assignment 3.

## Run

Install Java 17 or newer and Maven. Open a terminal in this folder.

Build the code and run the JUnit tests:

```sh
mvn test
```

Run the small graphs:

```sh
java -cp target/classes benchmark.Main data/disconnected.txt
java -cp target/classes benchmark.Main data/cyclic.txt
java -cp target/classes benchmark.Main data/dag.txt
```

Run the full benchmark and the bonus checks:

```sh
java -Xmx3g -cp target/classes benchmark.Main benchmark
java -Xmx2g -cp target/classes benchmark.BonusBenchmark
```

The main benchmark writes 50 rows to `results/results.csv`. Each time is the median of five runs. It automatically checks the reference values, MST agreement, topological orders and scan counts. An incorrect result stops the program with an error. The bonus results are in `results/bonus.csv`.

## Files

- `src/main/java/graph`: graph and edge classes.
- `src/main/java/algorithms`: DSU, Kruskal, Prim, Kosaraju, Tarjan, Kahn, DAG paths and condensation.
- `src/main/java/metrics`: operation counters.
- `src/main/java/benchmark`: generator, examples and benchmarks.
- `src/test/java/tests`: JUnit 5 tests.
- `data`: three small input graphs.
- `results`: CSV results and three PNG plots.
- `REPORT.pdf`: five-page report with explanations and measured results.

The graph algorithms use loops, arrays, lists, a queue and a priority queue. DFS is iterative because the assignment requires 100,000 vertices. Distances and total weights use `long` to avoid integer overflow.

## Plots

The finished plots are already in `results/plots`. To rebuild them, install Python 3.10 or newer and run:

```sh
python -m pip install -r scripts/requirements.txt
python scripts/plots.py
```

The script also prints the ratios and slopes used in the report. Times change between runs; the reference results must remain identical.

## GitHub

`git-history.bundle` stores the Git history, feature branches and v1.0 tag. Recover it and push to your own new empty GitHub repository:

```sh
git clone git-history.bundle city-network
cd city-network
git remote set-url origin https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
git push -u origin main
git push origin 'refs/remotes/origin/feature/*:refs/heads/feature/*'
git push origin v1.0
```

Replace the example URL with your actual repository URL and include that link in Moodle. GitHub publication has not been completed.

## AI use

OpenAI ChatGPT assisted with the code, tests, benchmarks and report. Results were checked against independent reference methods and the assignment's reference values.
