# Isotipo nav desde app-icon-light.png — solo emblema azul, sin caja blanca ni sombra
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$brand = Join-Path $root 'images\brand'
$src = Join-Path $brand 'app-icon-light.png'
$out = Join-Path $brand 'logo-isotipo-nav.png'

if (-not (Test-Path $src)) { throw "Falta app-icon-light.png" }

function Test-BackgroundColor([System.Drawing.Color]$c) {
  if ($c.A -lt 8) { return $true }
  $r = [int]$c.R; $g = [int]$c.G; $b = [int]$c.B
  $max = [Math]::Max($r, [Math]::Max($g, $b))
  $min = [Math]::Min($r, [Math]::Min($g, $b))
  $sat = $max - $min
  $avg = ($r + $g + $b) / 3.0
  if ($avg -gt 238) { return $true }
  # Sombra y borde del icono (grises claros, poca saturación)
  if ($sat -le 28 -and $avg -ge 145) { return $true }
  if ($sat -le 45 -and $avg -ge 210) { return $true }
  return $false
}

function Remove-ConnectedBackground([System.Drawing.Bitmap]$bmp) {
  $w = $bmp.Width
  $h = $bmp.Height
  $visited = New-Object 'bool[,]' $h, $w
  $q = [System.Collections.Queue]::new()

  function Try-Enqueue([int]$x, [int]$y) {
    if ($x -lt 0 -or $y -lt 0 -or $x -ge $w -or $y -ge $h) { return }
    if ($visited[$y, $x]) { return }
    $c = $bmp.GetPixel($x, $y)
    if (-not (Test-BackgroundColor $c)) { return }
    $visited[$y, $x] = $true
    [void]$q.Enqueue([int[]]@($x, $y))
  }

  for ($x = 0; $x -lt $w; $x++) {
    Try-Enqueue $x 0
    Try-Enqueue $x ($h - 1)
  }
  for ($y = 0; $y -lt $h; $y++) {
    Try-Enqueue 0 $y
    Try-Enqueue ($w - 1) $y
  }

  while ($q.Count -gt 0) {
    $p = $q.Dequeue()
    Try-Enqueue ($p[0] + 1) $p[1]
    Try-Enqueue ($p[0] - 1) $p[1]
    Try-Enqueue $p[0] ($p[1] + 1)
    Try-Enqueue $p[0] ($p[1] - 1)
  }

  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      if ($visited[$y, $x]) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
      }
    }
  }
}

function Remove-Fringe([System.Drawing.Bitmap]$bmp) {
  $w = $bmp.Width
  $h = $bmp.Height
  for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
      $c = $bmp.GetPixel($x, $y)
      if ($c.A -lt 8) { continue }
      if (Test-BackgroundColor $c) {
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
      }
    }
  }
}

$srcBmp = [System.Drawing.Bitmap]::FromFile($src)
$w = $srcBmp.Width
$h = $srcBmp.Height
$work = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $h; $y++) {
  for ($x = 0; $x -lt $w; $x++) {
    $c = $srcBmp.GetPixel($x, $y)
    if (Test-BackgroundColor $c) {
      $work.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    } else {
      $work.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $c.R, $c.G, $c.B))
    }
  }
}
$srcBmp.Dispose()

Remove-ConnectedBackground $work
Remove-Fringe $work

$minX = $w; $minY = $h; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $h; $y++) {
  for ($x = 0; $x -lt $w; $x++) {
    if ($work.GetPixel($x, $y).A -gt 24) {
      if ($x -lt $minX) { $minX = $x }
      if ($y -lt $minY) { $minY = $y }
      if ($x -gt $maxX) { $maxX = $x }
      if ($y -gt $maxY) { $maxY = $y }
    }
  }
}

$pad = [int]([Math]::Max($maxX - $minX, $maxY - $minY) * 0.04)
$minX = [Math]::Max(0, $minX - $pad)
$minY = [Math]::Max(0, $minY - $pad)
$maxX = [Math]::Min($w - 1, $maxX + $pad)
$maxY = [Math]::Min($h - 1, $maxY + $pad)
$cw = $maxX - $minX + 1
$ch = $maxY - $minY + 1
$rect = New-Object System.Drawing.Rectangle $minX, $minY, $cw, $ch
$crop = $work.Clone($rect, $work.PixelFormat)
$work.Dispose()

$outSize = 256
$canvasPad = [int]($outSize * 0.02)
$inner = $outSize - ($canvasPad * 2)
$nav = New-Object System.Drawing.Bitmap $outSize, $outSize, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($nav)
$g.Clear([System.Drawing.Color]::Transparent)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

$scale = [Math]::Min($inner / $cw, $inner / $ch)
$dw = [int]($cw * $scale)
$dh = [int]($ch * $scale)
$dx = [int](($outSize - $dw) / 2)
$dy = [int](($outSize - $dh) / 2)
$g.DrawImage($crop, $dx, $dy, $dw, $dh)
$g.Dispose()
$crop.Dispose()

Remove-Fringe $nav

$nav.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$nav.Dispose()
Write-Host "OK $out ($dw x $dh en canvas $outSize)"
