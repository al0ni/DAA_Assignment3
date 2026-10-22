import csv
import json
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak

root = Path(__file__).resolve().parents[1]
rows = list(csv.DictReader((root / "results/results.csv").open()))
bonus = list(csv.DictReader((root / "results/bonus.csv").open()))
analysis = json.loads((root / "results/analysis.json").read_text())
algorithms = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
pages = [[] for _ in range(5)]

def paragraph(page, text):
    pages[page].append(("p", text))

def heading(page, text):
    pages[page].append(("h", text))

def table(page, headers, values, widths):
    pages[page].append(("table", headers, values, widths))

heading(0, "Assignment 3: City Network & Job Scheduler")
paragraph(0, "Alua Rakhimzhanova | SE-2509 | 23 October 2026")
heading(0, "1. Implementation and experiment")
paragraph(0, "One adjacency-list Graph supports directed and undirected edges. Vertices use int IDs and edge weights use int; total weights and distances use long to prevent int overflow. The implementation handles self-loops, parallel edges, isolated vertices and disconnected graphs. DSU uses union by size and iterative path compression. Kosaraju and the bonus Tarjan both use explicit arrays as DFS stacks, so neither needs recursive DFS.")
paragraph(0, "Each of the five sizes (1,000; 3,162; 10,000; 31,623; 100,000) is tested with m = 4n and m = 20n. Each pair has three separately generated graph kinds. Every generation starts with a fresh Random(42). The undirected generator shuffles a permutation, creates a spanning tree and adds random edges. The DAG generator shuffles ranks and orients edges by those ranks. A self-loop draw repeats only the second endpoint; this detail is necessary to reproduce the reference table.")
heading(0, "2. Complexity of this implementation")
table(0, ["Operation", "Time", "Extra space", "Reason"], [
    ["DSU find / union", "O(alpha(V)) amortized", "O(V) total", "Size balancing plus path compression; O(1) local space."],
    ["DSU makeSet", "O(V)", "O(V)", "Initialize parent and size arrays."],
    ["Kruskal", "O(V + E log E)", "O(V + E)", "Sort a copied edge list, then DSU checks."],
    ["Prim (lazy heap)", "O(V + E log E)", "O(V + E)", "Each adjacency entry is scanned; heap may hold O(E) edges."],
    ["Kosaraju", "O(V + E)", "O(V + E)", "Two passes and a stored transpose; explicit stacks."],
    ["Kahn", "O(V + E)", "O(V)", "In-degrees, queue and output order."],
    ["DAG shortest / longest", "O(V + E)", "O(V)", "Kahn order, source initialization and one relaxation pass."],
    ["Tarjan (bonus)", "O(V + E)", "O(V)", "One DFS pass with index, lowlink and active stack."],
    ["Condensation (bonus)", "O(V + E) expected", "O(V + E)", "Hash map merges parallel component edges."],
], [94, 111, 75, 227])
paragraph(0, "Space excludes the input graph but includes algorithm output. The O(V) term matters when vertices are isolated or E = 0. Prim here stores candidate edges, not an indexed decrease-key heap. With parallel edges E need not be bounded by V squared, so O(E log E) is the honest heap bound for this version.")
heading(0, "3. Counter definitions")
paragraph(0, "edgeScans counts Kruskal candidates or adjacency entries read during the named traversal. Kosaraju counts its two DFS passes; building the transpose is separate preprocessing. Kahn counts queue-processing scans, excluding in-degree construction. DAG paths count relaxation scans; their internal Kahn call has separate metrics. Therefore the required scan counts are exactly 2m, m and m, although complete methods also perform linear preprocessing. Prim scans 2m because an undirected edge has two adjacency entries.")
paragraph(0, "comparisons counts weight-comparator calls in sort or heap. dsuOps counts public find calls and successful unions, including finds called inside union. maxDepth is peak occupied DFS-stack size, with a root at depth 1. The work proxy W = edgeScans + comparisons + dsuOps is counted in the algorithms; it is not a count of every machine instruction or pointer traversal.")

heading(1, "4. Correctness arguments")
heading(1, "Cut property and minimum spanning forests")
paragraph(1, "Consider a cut that does not split an already chosen forest component. Let e be a lightest edge crossing that cut. If a minimum spanning tree already contains e, it needs no change. Otherwise, adding e to that tree creates a cycle. The cycle contains another crossing edge f, and weight(e) is no greater than weight(f). Replacing f with e preserves a spanning tree without increasing its weight, so e is safe. Kruskal applies this rule when it joins two DSU components in increasing weight order, and Prim applies it to the cut between visited and unvisited vertices. Restarting Prim in each unvisited component, or processing all useful Kruskal edges, gives a minimum spanning forest when the graph is disconnected.")
heading(1, "Induction on topological order")
paragraph(1, "Every predecessor of a vertex appears earlier in a topological order. All vertices of in-degree zero start at distance zero, which is equivalent to adding a virtual zero-weight super-source. A source already has its correct distance because it has no incoming edge. Assume all earlier vertices have their correct distances. When the current vertex is processed, every possible final incoming edge has already been relaxed from its predecessor. Its distance is therefore the minimum over all such path extensions, so the induction continues. Negative weights are safe because a DAG has no cycle to revisit; in a general graph, a reachable negative cycle can make shortest distances unbounded and a topological pass is unavailable.")
heading(1, "5. Which algorithm when")
table(1, ["Situation", "Choice", "Why"], [
    ["Sparse undirected graph; edge list", "Kruskal", "Simple sorting plus DSU; forest is natural."],
    ["Undirected graph already in adjacency lists", "Prim", "Grow locally from a vertex; no separate sorted list."],
    ["Very dense graph", "Compare representations", "This lazy Prim has O(E log E) work; matrix Prim O(V squared) can be preferable, but is not this implementation."],
    ["Directed graph; cycles/deadlocks possible", "Kosaraju / Tarjan", "Find maximal groups of mutually reachable jobs."],
    ["Need a job order or cycle detection", "Kahn", "If fewer than V vertices leave the queue, reject the order."],
    ["Shortest paths in a DAG, negative weights allowed", "Topological relaxation", "O(V + E); no heap or repeated passes needed."],
    ["General graph shortest paths", "Dijkstra / Bellman-Ford", "Dijkstra needs nonnegative weights; Bellman-Ford supports negative edges and detects reachable negative cycles."],
], [149, 110, 248])
heading(1, "6. Bonus tasks")
paragraph(1, "Tarjan records index[v] when a vertex is discovered. low[v] tracks the earliest active index reachable through its DFS subtree. An edge to an active vertex uses that vertex's index, while a completed DFS child propagates its lowlink to its parent. When low[v] equals index[v], active vertices are popped through v to form one SCC. Tarjan scans m edges, while Kosaraju scans 2m in its two passes and also constructs a transpose. IDs may differ, so tests compare the component partition rather than raw labels.")
paragraph(1, "Condensation replaces every SCC by one vertex and removes internal edges. A cycle between distinct SCCs would make them mutually reachable, contradicting maximality; thus the result is a DAG. Parallel component edges retain their maximum weight for the longest-path model. The longest-path routine uses the same topological pass as shortest paths, with the comparison reversed, which is equivalent to negating weights without int-negation overflow. It starts at all zero-in-degree components. This models the critical chain of dependencies in scheduling; durations inside a cyclic SCC need a separate model. Longest simple paths are hard in general graphs (NP-hard), while allowing repeated vertices can make a positive cycle unbounded; neither obstacle exists in a DAG.")

heading(2, "7. Reference values and independent validation")
reference_rows = []
for i in range(0, len(rows), 5):
    group = rows[i:i + 5]
    reference_rows.append([f"{int(group[0]['n']):,} / {int(group[0]['m']):,}",
                           f"{int(group[0]['result']):,}", group[2]["result"],
                           f"{int(group[4]['result']):,}", "PASS"])
table(2, ["n / m", "MST weight", "SCCs", "DAG sum", "Reference"], reference_rows, [128, 100, 67, 116, 96])
paragraph(2, "All ten reference rows match. Both MST algorithms agree and return n - 1 tree edges on generated connected graphs. Kahn returns n vertices and every directed edge satisfies pos[u] < pos[v]. Kosaraju has exactly 2m scans; Kahn and DAG relaxation each have m scans. The CSV has 50 rows and records medians of five runs, not invented times. The benchmark also checks that results and operation counters stay identical across those five runs.")
heading(2, "Tests")
paragraph(2, "The JUnit suite has 10 tests with no failures. DSU is checked after 1,000 random unions against a label-based reference. MSTs are compared on 100 connected random graphs and on 30 tiny graphs against exhaustive spanning-tree enumeration. Both SCC algorithms are checked on 100 random graphs of up to 200 vertices against BFS reachability in both directions. Topological validation rejects duplicates, missing vertices, wrong orders and cycles. On 100 random DAGs, shortest and longest distances match independent Bellman-Ford-style repeated relaxation, including negative weights; restored paths are checked too. Both SCC implementations finish a 100,000-vertex path and then a 100,000-vertex cycle without recursive DFS. File graphs, empty inputs, parallel edges, self-loops, isolated vertices and long totals are also tested.")
heading(2, "Bonus measurements")
bonus_rows = [[f"{int(r['n']):,} / {int(r['m']):,}", f"{int(r['kosaraju_scans']):,}",
               f"{int(r['tarjan_scans']):,}", r["condensation_edges"], r["critical_weight"]] for r in bonus]
table(2, ["n / m", "Kosaraju scans", "Tarjan scans", "DAG edges", "Critical weight"], bonus_rows, [128, 104, 95, 80, 100])
paragraph(2, "Every bonus graph passes partition equality and condensation topological validation. Dense random directed graphs each form one SCC, so their condensation has one vertex, no edges and critical weight zero. The small cyclic file gives a nontrivial example: 4 SCCs, 2 condensation edges and critical weight 12. Full restored critical paths are recorded in results/bonus.csv.")

heading(3, "8. Measured work and scans")
paragraph(3, "The first chart uses W on log-log axes, with one series for each main algorithm and density. The second divides scans by m. Identical curves overlap: Prim and Kosaraju equal 2, while Kahn and DAG relaxation equal 1. These exact ratios validate traversal work; they do not include heap or sorting comparisons.")
pages[3].append(("image", "operations.png", 497))
pages[3].append(("image", "scans_per_edge.png", 497))
paragraph(3, "Kruskal's scan fraction is not flat because it stops after V - 1 accepted edges on connected graphs. That early stop affects candidate scanning only; sorting still sees all E input edges. All plotted values are read from the included CSV by scripts/analyze.py.")

heading(4, "9. Normalized counters and fitted slopes")
paragraph(4, "For each density, ordinary least squares fits log(counter) against log(n). The normalization uses W / (m log2(m)) for MSTs and W / (n + m) for traversals. The table shows the minimum-to-maximum ratio across all five sizes; full values are in results/analysis.json. Since m is a fixed multiple of n, all complete traversal scan slopes are exactly 1.00.")
ratio_rows = []
for algorithm in algorithms:
    for density in ("sparse", "dense"):
        stats = analysis[algorithm + "_" + density]
        normalized = stats["normalized_work"]
        ratio_rows.append([algorithm + " / " + density, "m log2(m)" if algorithm in ("Kruskal", "Prim") else "n + m",
                           f"{min(normalized):.3f}-{max(normalized):.3f}", f"{stats['scan_slope']:.3f}", f"{stats['operations_slope']:.3f}"])
table(4, ["Algorithm / density", "Divisor for W", "Ratio range", "Scan slope", "W slope"], ratio_rows, [161, 90, 91, 82, 83])
paragraph(4, "Kruskal's scan slopes are 1.055 (sparse) and 1.119 (dense), while its comparator slopes are 1.055 and 1.016. A scan-only slope cannot establish E log E sorting complexity. Early stopping changes the scan fraction; bounded weights (1..1,000), repeated keys and adaptive library sorting also influence measured comparisons. Normalized MST work remains of similar magnitude rather than exactly constant; these finite measurements are consistent with the stated bounds, not a proof of them. Kruskal dsuOps/m ranges from 2.290 to 2.748 (sparse) and 0.565 to 0.894 (dense); calls alone cannot demonstrate alpha(V).")
pages[4].append(("image", "kruskal_share.png", 400))
paragraph(4, "Kruskal examines 77.05%-99.92% of sparse edges but only 20.74%-37.18% of dense edges. At n = 100,000 and m = 2,000,000 it scans 743,572 candidates versus Prim's 4,000,000 adjacency entries, and makes 27,890,128 weight comparisons versus Prim's 79,103,986. This explains its lower counted work on these graphs, without implying that it always wins on other inputs.")
heading(4, "Time measurements")
kruskal = [r for r in rows if r["algorithm"] == "Kruskal" and r["graph"].endswith("dense")]
prim = [r for r in rows if r["algorithm"] == "Prim" and r["graph"].endswith("dense")]
paragraph(4, f"On the included Java 25.0.1 run, dense Kruskal medians rise from {float(kruskal[0]['time_ms']):.3f} ms to {float(kruskal[-1]['time_ms']):.3f} ms; dense Prim rises from {float(prim[0]['time_ms']):.3f} ms to {float(prim[-1]['time_ms']):.3f} ms. Timed tasks include result construction and immediate checks; Kahn also validates its order. Small-input times are noisy due to JVM compilation, allocation and background load. Hardware, JVM and garbage collection change times, so the counters and independent correctness checks provide the main evidence.")

styles = {
    "p": ParagraphStyle("body", fontName="Helvetica", fontSize=9.1, leading=12, spaceAfter=6, alignment=TA_LEFT),
    "h": ParagraphStyle("heading", fontName="Helvetica-Bold", fontSize=12, leading=15, spaceBefore=7, spaceAfter=7),
    "cell": ParagraphStyle("cell", fontName="Helvetica", fontSize=7.8, leading=10),
    "head": ParagraphStyle("head", fontName="Helvetica-Bold", fontSize=7.8, leading=10, textColor=colors.white),
}
story = []
markdown = []
for number, blocks in enumerate(pages):
    if number:
        story.append(PageBreak())
        markdown.append('\n<div style="page-break-after: always;"></div>\n')
    for block in blocks:
        kind = block[0]
        if kind in ("p", "h"):
            story.append(Paragraph(escape(block[1]), styles[kind]))
            markdown.append(("## " if kind == "h" else "") + block[1] + "\n")
        elif kind == "image":
            image_path = root / "results/plots" / block[1]
            story.append(Image(str(image_path), width=block[2], height=block[2] * 530 / 920))
            story.append(Spacer(1, 6))
            markdown.append(f"![{block[1]}](results/plots/{block[1]})\n")
        else:
            header, values, widths = block[1:]
            cells = [[Paragraph(escape(str(x)), styles["head"]) for x in header]]
            cells += [[Paragraph(escape(str(x)), styles["cell"]) for x in row] for row in values]
            output = Table(cells, colWidths=widths, repeatRows=1, hAlign="LEFT")
            output.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#233e61")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f0f4f8"), colors.white]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]))
            story.extend([output, Spacer(1, 7)])
            markdown.append("| " + " | ".join(header) + " |")
            markdown.append("| " + " | ".join(["---"] * len(header)) + " |")
            markdown.extend("| " + " | ".join(map(str, row)) + " |" for row in values)
            markdown.append("")

def footer(canvas, document):
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#54657a"))
    canvas.drawString(44, 24, "DAA Assignment 3 | Alua Rakhimzhanova | SE-2509")
    canvas.drawRightString(A4[0] - 44, 24, str(document.page))

(root / "REPORT.md").write_text("\n".join(markdown), encoding="utf-8")
SimpleDocTemplate(str(root / "REPORT.pdf"), pagesize=A4, rightMargin=44, leftMargin=44,
                  topMargin=32, bottomMargin=40, title="DAA Assignment 3", author="Alua Rakhimzhanova").build(story, onFirstPage=footer, onLaterPages=footer)
print("Created REPORT.md and REPORT.pdf")
