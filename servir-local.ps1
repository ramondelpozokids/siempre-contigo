# Servidor local — Siempre Contigo (requerido para ES modules)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$port = 8080
$url = "http://127.0.0.1:$port/index.html"

$python = $null
foreach ($cmd in @('python', 'py')) {
  if (Get-Command $cmd -ErrorAction SilentlyContinue) {
    $python = $cmd
    break
  }
}
if (-not $python) {
  Write-Host "ERROR: Instala Python 3 para servir la web en local." -ForegroundColor Red
  exit 1
}

Write-Host ""
Write-Host "  Siempre Contigo — modo local" -ForegroundColor Cyan
Write-Host "  $url" -ForegroundColor Green
Write-Host "  Cierra esta ventana (Ctrl+C) para detener el servidor." -ForegroundColor DarkGray
Write-Host ""

Start-Process $url
& $python -m http.server $port --bind 127.0.0.1
