import csv
import math
from pathlib import Path
from reportlab.graphics.shapes import Drawing, String, Line
from reportlab.graphics.charts.lineplots import LinePlot
from reportlab.graphics.charts.axes import LogXValueAxis, LogYValueAxis
from reportlab.graphics import renderSVG
from reportlab.lib import colors
import fitz

root = Path(__file__).resolve().parents[1]
rows = list(csv.DictReader((root / "results/results.csv").open()))
for row in rows:
    for key in ("n", "m", "dsu_ops", "edge_scans", "comparisons", "max_depth", "result"):
        row[key] = int(row[key])
    row["time_ms"] = float(row["time_ms"])
    row["operations"] = row["dsu_ops"] + row["edge_scans"] + row["comparisons"]

algorithms = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
palette = [colors.HexColor(value) for value in ["#1858a8", "#e17515", "#15916d", "#9059b5", "#d13c5b"]]

def series(algorithm, density):
    return [r for r in rows if r["algorithm"] == algorithm and r["graph"].endswith(density)]

def slope(points):
    x = [math.log(a) for a, b in points]
    y = [math.log(b) for a, b in points]
    mean_x = sum(x) / len(x)
    mean_y = sum(y) / len(y)
    return sum((a - mean_x) * (b - mean_y) for a, b in zip(x, y)) / sum((a - mean_x) ** 2 for a in x)

def plot(filename, title, subtitle, y_label, selected, value, log_y):
    drawing = Drawing(920, 530)
    drawing.add(String(65, 497, title, fontName="Helvetica-Bold", fontSize=20, fillColor=colors.HexColor("#17263c")))
    drawing.add(String(65, 473, subtitle, fontSize=11, fillColor=colors.HexColor("#516175")))
    chart = LinePlot()
    chart.x = 80
    chart.y = 75
    chart.width = 590
    chart.height = 355
    chart.xValueAxis = LogXValueAxis()
    chart.xValueAxis.valueMin = 1000
    chart.xValueAxis.valueMax = 100000
    chart.xValueAxis.valueSteps = [1000, 3162, 10000, 31623, 100000]
    chart.xValueAxis.labelTextFormat = lambda x: f"{int(x):,}"
    chart.xValueAxis.labels.fontSize = 9
    chart.xValueAxis.visibleGrid = True
    chart.xValueAxis.gridStrokeColor = colors.HexColor("#e4e8ee")
    if log_y:
        chart.yValueAxis = LogYValueAxis()
        chart.yValueAxis.valueMin = 1000
        chart.yValueAxis.valueMax = 100000000
        chart.yValueAxis.valueSteps = [10 ** i for i in range(3, 9)]
    else:
        chart.yValueAxis.valueMin = 0
        chart.yValueAxis.valueMax = 2.2 if len(selected) > 2 else 1
        chart.yValueAxis.valueStep = 0.5 if len(selected) > 2 else 0.2
    chart.yValueAxis.labelTextFormat = lambda x: f"{x:g}"
    chart.yValueAxis.labels.fontSize = 9
    chart.yValueAxis.visibleGrid = True
    chart.yValueAxis.gridStrokeColor = colors.HexColor("#e4e8ee")
    chart.data = [[(r["n"], value(r)) for r in series(a, d)] for a, d in selected]
    for i, (algorithm, density) in enumerate(selected):
        color = palette[algorithms.index(algorithm)]
        chart.lines[i].strokeColor = color
        chart.lines[i].strokeWidth = 2.8 if density == "sparse" else 1.2
        y = 425 - i * 29
        drawing.add(Line(704, y, 734, y, strokeColor=color,
                         strokeWidth=2.8 if density == "sparse" else 1.2))
        drawing.add(String(741, y - 4, algorithm, fontSize=10))
        drawing.add(String(741, y - 15, density, fontSize=9, fillColor=colors.HexColor("#657185")))
    drawing.add(chart)
    drawing.add(String(290, 28, "Vertices n (log scale)", fontSize=12))
    drawing.add(String(80, 447, y_label, fontSize=11))
    drawing.add(String(80, 9, "Seed 42; thick = 4n edges; thin = 20n edges; identical curves overlap", fontSize=10))
    destination = root / "results/plots" / filename
    destination.parent.mkdir(exist_ok=True)
    svg = renderSVG.drawToString(drawing)
    with fitz.open(stream=svg.encode(), filetype="svg") as document:
        document[0].get_pixmap(matrix=fitz.Matrix(1.6, 1.6)).save(destination)

selected = [(a, d) for a in algorithms for d in ("sparse", "dense")]
plot("operations.png", "Counted operations vs graph size", "Composite work = edge scans + weight comparisons + DSU calls", "Operations (log scale)", selected, lambda r: r["operations"], True)
plot("scans_per_edge.png", "Edge scans per input edge", "Prim and Kosaraju overlap at 2; Kahn and DAG relaxation overlap at 1", "edge_scans / m", selected, lambda r: r["edge_scans"] / r["m"], False)
plot("kruskal_share.png", "Share of edges examined by Kruskal", "Sorting still processes the entire edge list before the early stop", "edge_scans / m", [("Kruskal", "sparse"), ("Kruskal", "dense")], lambda r: r["edge_scans"] / r["m"], False)

print("Algorithm,density,normalized_min,normalized_max,scan_slope,work_slope,comparison_slope,dsu_min,dsu_max")
for algorithm in algorithms:
    for density in ("sparse", "dense"):
        group = series(algorithm, density)
        normalized = []
        dsu_ratio = []
        for row in group:
            if algorithm in ("Kruskal", "Prim"):
                divisor = row["m"] * math.log2(row["m"])
            else:
                divisor = row["n"] + row["m"]
            normalized.append(row["operations"] / divisor)
            dsu_ratio.append(row["dsu_ops"] / row["m"])
        scan_slope = slope([(r["n"], r["edge_scans"]) for r in group])
        work_slope = slope([(r["n"], r["operations"]) for r in group])
        comparison_slope = 0
        if group[0]["comparisons"] > 0:
            comparison_slope = slope([(r["n"], r["comparisons"]) for r in group])
        print(f"{algorithm},{density},{min(normalized):.3f},{max(normalized):.3f},"
              f"{scan_slope:.3f},{work_slope:.3f},{comparison_slope:.3f},"
              f"{min(dsu_ratio):.3f},{max(dsu_ratio):.3f}")
