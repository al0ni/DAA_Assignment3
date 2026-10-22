$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
mvn clean test
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
java -Xmx3g -cp target/classes benchmark.Main benchmark
if ($LASTEXITCODE -ne 0) { throw 'Main benchmark failed' }
java -Xmx2g -cp target/classes benchmark.BonusBenchmark
if ($LASTEXITCODE -ne 0) { throw 'Bonus benchmark failed' }
python scripts/analyze.py
if ($LASTEXITCODE -ne 0) { throw 'Plot generation failed' }
python scripts/make_report.py
if ($LASTEXITCODE -ne 0) { throw 'Report generation failed' }
