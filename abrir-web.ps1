# Abre la web en el navegador e inicia el servidor local si hace falta.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$port = 8080
$url = "http://127.0.0.1:$port/index.html"

function Test-ServerUp {
  try {
    $c = New-Object System.Net.Sockets.TcpClient
    $c.Connect('127.0.0.1', $port)
    $c.Close()
    return $true
  } catch {
    return $false
  }
}

function Find-Python {
  foreach ($cmd in @('python', 'py')) {
    if (Get-Command $cmd -ErrorAction SilentlyContinue) { return $cmd }
  }
  return $null
}

if (-not (Test-ServerUp)) {
  $python = Find-Python
  if (-not $python) {
    [System.Windows.Forms.MessageBox]::Show(
      'Necesitas Python 3 instalado para ver la web en local.' + [Environment]::NewLine + [Environment]::NewLine +
      'Descarga Python desde python.org e inténtalo de nuevo.',
      'Siempre Contigo',
      'OK',
      'Error'
    ) | Out-Null
    exit 1
  }

  $serverCmd = "cd /d `"$root`" && title Siempre Contigo - Servidor && $python -m http.server $port --bind 127.0.0.1"
  Start-Process cmd.exe -ArgumentList '/k', $serverCmd -WindowStyle Normal

  $ready = $false
  for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 250
    if (Test-ServerUp) { $ready = $true; break }
  }
  if (-not $ready) {
    [System.Windows.Forms.MessageBox]::Show(
      'El servidor tardó demasiado en arrancar. Comprueba la ventana negra «Siempre Contigo - Servidor».',
      'Siempre Contigo',
      'OK',
      'Warning'
    ) | Out-Null
    exit 1
  }
}

Start-Process $url
