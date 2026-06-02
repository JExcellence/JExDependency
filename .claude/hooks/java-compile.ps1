$raw = [Console]::In.ReadToEnd()
$json = $raw | ConvertFrom-Json
$file = $json.tool_input.file_path

if ($file -notmatch '\.java$') {
    exit 0
}

Set-Location 'C:\Users\Privat\IdeaProjects\RaindropCentral\JExDependency'
$output = & .\gradlew.bat compileJava --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    $output | Select-Object -Last 30 | ForEach-Object { Write-Host $_ }
    @{ systemMessage = "Compile FAILED - fix errors before continuing." } | ConvertTo-Json -Compress
}
