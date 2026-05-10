param(
    [string]$ResultsDir,
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"
$PSDefaultParameterValues['Out-File:Encoding'] = 'utf8'

function Get-LatestResultsDir {
    param([string]$Root)

    $dirs = Get-ChildItem -Path $Root -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
    return $dirs | Select-Object -First 1
}

function Get-MetricValue {
    param(
        [Parameter(Mandatory = $true)]$Summary,
        [Parameter(Mandatory = $true)][string]$MetricName,
        [Parameter(Mandatory = $true)][string]$Field
    )

    $metricProp = $Summary.metrics.PSObject.Properties | Where-Object { $_.Name -eq $MetricName } | Select-Object -First 1
    if (-not $metricProp) { return $null }

    $metric = $metricProp.Value

    $valuesProp = $metric.PSObject.Properties | Where-Object { $_.Name -eq 'values' } | Select-Object -First 1
    if ($valuesProp) {
        $values = $valuesProp.Value
        $fieldProp = $values.PSObject.Properties | Where-Object { $_.Name -eq $Field } | Select-Object -First 1
        if (-not $fieldProp) { return $null }
        return $fieldProp.Value
    }

    $directProp = $metric.PSObject.Properties | Where-Object { $_.Name -eq $Field } | Select-Object -First 1
    if ($directProp) {
        return $directProp.Value
    }

    if ($Field -eq 'rate') {
        $valueProp = $metric.PSObject.Properties | Where-Object { $_.Name -eq 'value' } | Select-Object -First 1
        if ($valueProp) {
            return $valueProp.Value
        }
    }

    return $null
}

function Get-RateFromPassFail {
    param($Metric)

    if (-not $Metric) { return $null }
    if ($Metric.passes -eq $null -or $Metric.fails -eq $null) { return $null }

    $total = [double]($Metric.passes + $Metric.fails)
    if ($total -le 0) { return 0 }
    return [double]$Metric.passes / $total
}

function Format-Num {
    param(
        $Value,
        [int]$Decimals = 2
    )
    if ($null -eq $Value) { return "n/a" }
    return ([math]::Round([double]$Value, $Decimals)).ToString("0." + ("0" * $Decimals))
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$resultsRoot = Join-Path $root "results"

if (-not $ResultsDir) {
    $latest = Get-LatestResultsDir -Root $resultsRoot
    if (-not $latest) { throw "No results directories found under $resultsRoot" }
    $ResultsDir = $latest.FullName
}

if (-not (Test-Path $ResultsDir)) {
    throw "ResultsDir not found: $ResultsDir"
}

$dirName = Split-Path -Leaf $ResultsDir
if (-not $OutputPath) {
    $OutputPath = Join-Path $root ("LOAD_TEST_RESULTS_CLEAN_" + $dirName + ".txt")
}

$summaryFiles = Get-ChildItem -Path $ResultsDir -Filter "*.summary.json" | Sort-Object Name

"Dragon of North - Load test summary" | Out-File -FilePath $OutputPath
("ResultsDir: {0}" -f $ResultsDir) | Out-File -FilePath $OutputPath -Append
("Generated:  {0}" -f (Get-Date)) | Out-File -FilePath $OutputPath -Append
"" | Out-File -FilePath $OutputPath -Append

$worstP95 = 0
$worstTest = ""

foreach ($file in $summaryFiles) {
    $summary = Get-Content $file.FullName -Raw | ConvertFrom-Json

    $reqCount = Get-MetricValue -Summary $summary -MetricName "http_reqs" -Field "count"
    $reqRate = Get-MetricValue -Summary $summary -MetricName "http_reqs" -Field "rate"
    $p95 = Get-MetricValue -Summary $summary -MetricName "http_req_duration" -Field "p(95)"
    $p99 = Get-MetricValue -Summary $summary -MetricName "http_req_duration" -Field "p(99)"

    $checksRate = Get-RateFromPassFail -Metric $summary.metrics.checks
    if ($null -eq $checksRate) {
        $checksRate = Get-MetricValue -Summary $summary -MetricName "checks" -Field "rate"
        if ($null -eq $checksRate) { $checksRate = Get-MetricValue -Summary $summary -MetricName "checks" -Field "value" }
    }

    $failedRate = Get-MetricValue -Summary $summary -MetricName "http_req_failed" -Field "rate"
    if ($null -eq $failedRate) { $failedRate = Get-MetricValue -Summary $summary -MetricName "http_req_failed" -Field "value" }

    if ($p95 -ne $null -and [double]$p95 -gt $worstP95) {
        $worstP95 = [double]$p95
        $worstTest = $file.Name
    }

    ("====================================================================") | Out-File -FilePath $OutputPath -Append
    ("TEST: {0}" -f $file.BaseName.Replace(".summary", "")) | Out-File -FilePath $OutputPath -Append
    ("  Requests:    {0}" -f (Format-Num $reqCount 0)) | Out-File -FilePath $OutputPath -Append
    ("  Throughput:  {0} req/s" -f (Format-Num $reqRate 2)) | Out-File -FilePath $OutputPath -Append
    ("  Latency p95: {0} ms" -f (Format-Num $p95 2)) | Out-File -FilePath $OutputPath -Append
    ("  Latency p99: {0} ms" -f (Format-Num $p99 2)) | Out-File -FilePath $OutputPath -Append
    ("  Checks pass: {0}%" -f (Format-Num ($checksRate * 100) 2)) | Out-File -FilePath $OutputPath -Append
    ("  Req failed:  {0}%" -f (Format-Num ($failedRate * 100) 2)) | Out-File -FilePath $OutputPath -Append

    foreach ($metric in @(
        "health_latency",
        "csrf_latency",
        "login_latency",
        "refresh_latency",
        "sessions_read_latency",
        "protected_latency",
        "sessions_latency",
        "profile_latency",
        "multi_device_flow_latency",
        "profile_get_latency",
        "profile_patch_latency"
    )) {
        $mP95 = Get-MetricValue -Summary $summary -MetricName $metric -Field "p(95)"
        if ($null -ne $mP95) {
            ("  {0} p95: {1} ms" -f $metric, (Format-Num $mP95 2)) | Out-File -FilePath $OutputPath -Append
        }
    }

    foreach ($metric in @(
        "login_success_rate",
        "login_rate_limited_rate",
        "login_invalid_cred_rate",
        "refresh_success_rate",
        "logout_success_rate",
        "sessions_success_rate",
        "profile_success_rate",
        "csrf_success_rate",
        "csrf_token_present_rate",
        "profile_patch_success_rate",
        "profile_patch_skipped_rate"
    )) {
        $rate = Get-MetricValue -Summary $summary -MetricName $metric -Field "rate"
        if ($null -eq $rate) { $rate = Get-MetricValue -Summary $summary -MetricName $metric -Field "value" }
        if ($null -ne $rate) {
            ("  {0}: {1}%" -f $metric, (Format-Num ($rate * 100) 2)) | Out-File -FilePath $OutputPath -Append
        }
    }

    "" | Out-File -FilePath $OutputPath -Append
}

"====================================================================" | Out-File -FilePath $OutputPath -Append
"Conclusions:" | Out-File -FilePath $OutputPath -Append
("- Highest observed overall p95 latency: {0} ms ({1})" -f (Format-Num $worstP95 2), $worstTest) | Out-File -FilePath $OutputPath -Append
"- For auth-related tests, interpret http_req_failed carefully; prefer custom success/ratelimit rates." | Out-File -FilePath $OutputPath -Append
"- If protected endpoints return mostly 401/403, set EMAIL/PASSWORD or ACCESS_COOKIE/REFRESH_COOKIE." | Out-File -FilePath $OutputPath -Append

Write-Host ("Wrote clean summary: {0}" -f $OutputPath)

