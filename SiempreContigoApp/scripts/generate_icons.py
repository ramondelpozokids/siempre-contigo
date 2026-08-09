from PIL import Image
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
brand = ROOT / "images" / "brand" / "app-icon-light.png"
base = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
img = Image.open(brand).convert("RGBA")


def make_fg(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    logo_size = int(size * 0.66)
    logo = img.copy()
    logo.thumbnail((logo_size, logo_size), Image.Resampling.LANCZOS)
    x = (size - logo.width) // 2
    y = (size - logo.height) // 2
    canvas.paste(logo, (x, y), logo)
    return canvas


def make_legacy(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    logo_size = int(size * 0.78)
    logo = img.copy()
    logo.thumbnail((logo_size, logo_size), Image.Resampling.LANCZOS)
    x = (size - logo.width) // 2
    y = (size - logo.height) // 2
    canvas.paste(logo, (x, y), logo)
    return canvas


densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
fg_densities = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

for folder, size in densities.items():
    d = base / folder
    d.mkdir(parents=True, exist_ok=True)
    legacy = make_legacy(size)
    legacy.save(d / "ic_launcher.png")
    legacy.save(d / "ic_launcher_round.png")

for folder, size in fg_densities.items():
    d = base / folder
    d.mkdir(parents=True, exist_ok=True)
    make_fg(size).save(d / "ic_launcher_foreground.png")

anydpi = base / "mipmap-anydpi-v26"
anydpi.mkdir(parents=True, exist_ok=True)
adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
"""
(anydpi / "ic_launcher.xml").write_text(adaptive, encoding="utf-8")
(anydpi / "ic_launcher_round.xml").write_text(adaptive, encoding="utf-8")
print("icons ok")
