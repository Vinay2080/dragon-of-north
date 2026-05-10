$ErrorActionPreference = "Stop"
$PSDefaultParameterValues['Out-File:Encoding'] = 'utf8'

function Ensure-K6OnPath {
    if (Get-Command k6 -ErrorAction SilentlyContinue) {
        return
    }

    $k6Dir = "C:\Program Files\k6"
    $k6Exe = Join-Path $k6Dir "k6.exe"
    if (Test-Path $k6Exe) {
        $env:Path = "$env:Path;$k6Dir"
        return
    }

    throw "k6 not found on PATH and not found at '$k6Exe'. Install k6 first."
}

function Import-DotEnvIfPresent {
    param(
        [string]$DotEnvPath
    )

    if (-not (Test-Path $DotEnvPath)) {
        return
    }

    Get-Content $DotEnvPath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line) { return }
        if ($line.StartsWith("#")) { return }

        $parts = $line.Split("=", 2)
        if ($parts.Length -ne 2) { return }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()

        # Don't override variables that are already set in the shell.
        if (-not [string]::IsNullOrWhiteSpace($key) -and -not (Test-Path "Env:$key")) {
            Set-Item -Path "Env:$key" -Value $value
        }
    }
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

    # k6 summary-export format differs between versions:
    # - Older: metric.values.{field}
    # - Newer (k6 v2): metric.{field} (and for rates sometimes metric.value)
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

function Format-Num {
    param(
        $Value,
        [int]$Decimals = 2
    )
    if ($null -eq $Value) { return "n/a" }
    return ([math]::Round([double]$Value, $Decimals)).ToString("0." + ("0" * $Decimals))
}

Ensure-K6OnPath

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Import-DotEnvIfPresent (Join-Path $root ".env")

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsDir = Join-Path $root ("results\" + $timestamp)
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

$reportPath = Join-Path $root ("LOAD_TEST_RESULTS_" + $timestamp + ".txt")

$useInsecureTls = $false
if ($env:K6_INSECURE_SKIP_TLS_VERIFY) {
    $val = $env:K6_INSECURE_SKIP_TLS_VERIFY.ToLower()
    $useInsecureTls = ($val -eq "1" -or $val -eq "true" -or $val -eq "yes")
}

$tests = @(
    "health-stability-test.js",
    "csrf-token-test.js",
    "auth-load-test.js",
    "login-load-test.js",
    "refresh-storm-test.js",
    "session-read-test.js",
    "profile-read-test.js",
    "logout-test.js",
    "protected-concurrency-test.js",
    "multi-device-session-test.js",
    "mixed-traffic-test.js",
    "profile-patch-idempotent-test.js"
)

"Dragon of North - k6 load test run" | Out-File -FilePath $reportPath -Encoding utf8
("Timestamp: {0}" -f (Get-Date)) | Out-File -FilePath $reportPath -Append
("BASE_URL: {0}" -f ($env:BASE_URL)) | Out-File -FilePath $reportPath -Append
"" | Out-File -FilePath $reportPath -Append
"NOTE: If EMAIL/PASSWORD are not configured, auth-required tests will likely produce 401/403 or skip early." | Out-File -FilePath $reportPath -Append
"" | Out-File -FilePath $reportPath -Append

foreach ($test in $tests) {
    $testPath = Join-Path $root $test
    if (-not (Test-Path $testPath)) {
        ("[SKIP] Missing file: {0}" -f $test) | Out-File -FilePath $reportPath -Append
        continue
    }

    $baseName = [IO.Path]::GetFileNameWithoutExtension($test)
    $summaryPath = Join-Path $resultsDir ("{0}.summary.json" -f $baseName)
    $logPath = Join-Path $resultsDir ("{0}.log" -f $baseName)

    ("====================================================================") | Out-File -FilePath $reportPath -Append
    ("TEST: {0}" -f $test) | Out-File -FilePath $reportPath -Append

    $cmd = @(
        "run",
        "--address", "localhost:0",
        $(if ($useInsecureTls) { "--insecure-skip-tls-verify" } else { $null }),
        "--summary-export", $summaryPath,
        $testPath
    )

    $cmd = $cmd | Where-Object { $_ -ne $null }

    # Execute and capture console output.
    # We run through cmd.exe and redirect stderr to stdout, so PowerShell doesn't
    # treat k6's stderr logs as ErrorRecords (which can abort when EAP=Stop).
    $cmdTokens = @("k6") + $cmd
    $cmdLine = ($cmdTokens | ForEach-Object {
        if ($_ -match "\\s") { '"' + $_ + '"' } else { $_ }
    }) -join " "

    cmd /c "$cmdLine 2>&1" | Tee-Object -FilePath $logPath | Out-Null
    $exitCode = $LASTEXITCODE

    ("Exit code: {0}" -f $exitCode) | Out-File -FilePath $reportPath -Append
    ("Raw log:   {0}" -f $logPath) | Out-File -FilePath $reportPath -Append
    ("Summary:   {0}" -f $summaryPath) | Out-File -FilePath $reportPath -Append

    if (-not (Test-Path $summaryPath)) {
        "No summary export produced." | Out-File -FilePath $reportPath -Append
        continue
    }

    $summary = Get-Content $summaryPath -Raw | ConvertFrom-Json

    $reqCount = Get-MetricValue -Summary $summary -MetricName "http_reqs" -Field "count"
    $reqRate = Get-MetricValue -Summary $summary -MetricName "http_reqs" -Field "rate"
    $p95 = Get-MetricValue -Summary $summary -MetricName "http_req_duration" -Field "p(95)"
    $p99 = Get-MetricValue -Summary $summary -MetricName "http_req_duration" -Field "p(99)"
    $checksRate = $null
    if ($summary.metrics.checks -and $summary.metrics.checks.passes -ne $null -and $summary.metrics.checks.fails -ne $null) {
        $total = [double]($summary.metrics.checks.passes + $summary.metrics.checks.fails)
        $checksRate = $(if ($total -gt 0) { [double]$summary.metrics.checks.passes / $total } else { 0 })
    } else {
        $checksRate = Get-MetricValue -Summary $summary -MetricName "checks" -Field "rate"
        if ($null -eq $checksRate) { $checksRate = Get-MetricValue -Summary $summary -MetricName "checks" -Field "value" }
    }

    $failedRate = Get-MetricValue -Summary $summary -MetricName "http_req_failed" -Field "rate"
    if ($null -eq $failedRate) { $failedRate = Get-MetricValue -Summary $summary -MetricName "http_req_failed" -Field "value" }

    if ($null -eq $checksRate) { $checksRate = 0 }
    if ($null -eq $failedRate) { $failedRate = 0 }

    "Key metrics:" | Out-File -FilePath $reportPath -Append
    ("  Requests:      {0}" -f (Format-Num $reqCount 0)) | Out-File -FilePath $reportPath -Append
    ("  Throughput:    {0} req/s" -f (Format-Num $reqRate 2)) | Out-File -FilePath $reportPath -Append
    ("  Latency p95:   {0} ms" -f (Format-Num $p95 2)) | Out-File -FilePath $reportPath -Append
    ("  Latency p99:   {0} ms" -f (Format-Num $p99 2)) | Out-File -FilePath $reportPath -Append
    ("  Checks pass:   {0}%" -f (Format-Num ($checksRate * 100) 2)) | Out-File -FilePath $reportPath -Append
    ("  http_req_failed: {0}%" -f (Format-Num ($failedRate * 100) 2)) | Out-File -FilePath $reportPath -Append

    # Common custom metrics (present only in some tests)
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
            ("  {0} p95: {1} ms" -f $metric, (Format-Num $mP95 2)) | Out-File -FilePath $reportPath -Append
        }
    }

    foreach ($metric in @(
        "login_success_rate",
        "login_rate_limited_rate",
        "login_invalid_cred_rate",
        "protected_success_rate",
        "sessions_success_rate",
        "profile_success_rate",
        "refresh_success_rate",
        "logout_success_rate",
        "csrf_success_rate",
        "csrf_token_present_rate",
        "profile_patch_success_rate",
        "profile_patch_skipped_rate"
    )) {
        $rate = Get-MetricValue -Summary $summary -MetricName $metric -Field "rate"
        if ($null -ne $rate) {
            ("  {0}: {1}%" -f $metric, (Format-Num ($rate * 100) 2)) | Out-File -FilePath $reportPath -Append
        }
    }

    "" | Out-File -FilePath $reportPath -Append
}

"====================================================================" | Out-File -FilePath $reportPath -Append
"Conclusions (quick guidance):" | Out-File -FilePath $reportPath -Append
"- Use p95/p99 latency and throughput for headline numbers." | Out-File -FilePath $reportPath -Append
"- For auth-heavy tests, prefer custom success/ratelimit rates over http_req_failed." | Out-File -FilePath $reportPath -Append
"- If profile/session endpoints show high auth error rates, confirm cookies are being set/sent and the account has access." | Out-File -FilePath $reportPath -Append

Write-Host ("\nDONE. Report written to: {0}" -f $reportPath)
Write-Host ("Results directory: {0}\n" -f $resultsDir)







