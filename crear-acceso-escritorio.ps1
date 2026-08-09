# Crea accesos directos en el Escritorio con icono de app — Siempre Contigo
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$brand = Join-Path $root 'images\brand'
$desktop = [Environment]::GetFolderPath('Desktop')
$WshShell = New-Object -ComObject WScript.Shell

function Convert-PngToIco {
  param(
    [Parameter(Mandatory)][string]$PngPath,
    [Parameter(Mandatory)][string]$IcoPath,
    [int[]]$Sizes = @(256, 128, 64, 48, 32, 16)
  )
  if (-not (Test-Path $PngPath)) {
    throw "No se encuentra la imagen: $PngPath"
  }

  $source = [System.Drawing.Bitmap]::FromFile($PngPath)
  $ms = New-Object System.IO.MemoryStream
  $bw = New-Object System.IO.BinaryWriter($ms)

  $bw.Write([uint16]0)
  $bw.Write([uint16]1)
  $bw.Write([uint16]$Sizes.Count)

  $offset = 6 + (16 * $Sizes.Count)
  $pngChunks = New-Object System.Collections.Generic.List[byte[]]

  foreach ($size in $Sizes) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($source, 0, 0, $size, $size)
    $g.Dispose()

    $pngMs = New-Object System.IO.MemoryStream
    $bmp.Save($pngMs, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBytes = $pngMs.ToArray()
    $pngMs.Dispose()
    $bmp.Dispose()

    [void]$pngChunks.Add($pngBytes)

    $bw.Write([byte][Math]::Min($size, 255))
    $bw.Write([byte][Math]::Min($size, 255))
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $bw.Write([byte]1)
    $bw.Write([byte]0)
    $bw.Write([uint16]32)
    $bw.Write([uint32]$pngBytes.Length)
    $bw.Write([uint32]$offset)
    $offset += $pngBytes.Length
  }

  foreach ($chunk in $pngChunks) {
    $bw.Write($chunk)
  }

  $bw.Flush()
  $dir = Split-Path -Parent $IcoPath
  if ($dir -and -not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
  }
  [System.IO.File]::WriteAllBytes($IcoPath, $ms.ToArray())
  $ms.Dispose()
  $source.Dispose()
}

function New-ScShortcut {
  param(
    [string]$LinkPath,
    [string]$TargetPath,
    [string]$Arguments = '',
    [string]$WorkingDirectory = $root,
    [string]$Description = 'Siempre Contigo',
    [string]$IconPath = ''
  )
  $sc = $WshShell.CreateShortcut($LinkPath)
  $sc.TargetPath = $TargetPath
  if ($Arguments) { $sc.Arguments = $Arguments }
  $sc.WorkingDirectory = $WorkingDirectory
  $sc.Description = $Description
  $sc.WindowStyle = 1
  if ($IconPath -and (Test-Path $IconPath)) {
    $sc.IconLocation = ($IconPath -replace '/', '\') + ',0'
  }
  $sc.Save()
}

$abrirBat = Join-Path $root 'Abrir web.bat'
$iniciarBat = Join-Path $root 'iniciar-local.bat'
$pngLight = Join-Path $brand 'app-icon-light.png'
$pngDark = Join-Path $brand 'app-icon-dark.png'
$icoLight = Join-Path $brand 'app-icon-light.ico'
$icoDark = Join-Path $brand 'app-icon-dark.ico'

if (-not (Test-Path $abrirBat)) {
  [System.Windows.Forms.MessageBox]::Show(
    'No encuentro Abrir web.bat en la carpeta del proyecto.',
    'Siempre Contigo',
    'OK',
    'Error'
  ) | Out-Null
  exit 1
}

if (-not (Test-Path $pngLight)) {
  [System.Windows.Forms.MessageBox]::Show(
    'No encuentro images\brand\app-icon-light.png. Copia tus iconos de app en esa carpeta.',
    'Siempre Contigo',
    'OK',
    'Error'
  ) | Out-Null
  exit 1
}

Convert-PngToIco -PngPath $pngLight -IcoPath $icoLight
if (Test-Path $pngDark) {
  Convert-PngToIco -PngPath $pngDark -IcoPath $icoDark
} else {
  Copy-Item $icoLight $icoDark -Force
}

$lnkAbrir = Join-Path $desktop 'Siempre Contigo.lnk'
$lnkServidor = Join-Path $desktop 'Siempre Contigo - Servidor.lnk'

New-ScShortcut -LinkPath $lnkAbrir -TargetPath $abrirBat -IconPath $icoLight -Description 'Abrir Siempre Contigo en el navegador'
New-ScShortcut -LinkPath $lnkServidor -TargetPath $iniciarBat -IconPath $icoLight -Description 'Servidor local Siempre Contigo'

[System.Windows.Forms.MessageBox]::Show(
  "Accesos creados con el icono de la app (boton).`n`n- Siempre Contigo.lnk`n- Siempre Contigo - Servidor.lnk`n`nEscritorio:`n$desktop",
  'Siempre Contigo',
  'OK',
  'Information'
) | Out-Null
