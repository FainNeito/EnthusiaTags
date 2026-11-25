"""Draws peppermint_royalty.png: a 16x16 peppermint pinwheel wearing a little gold crown (Peppermint Royalty, U+A0A3).

Usage: python3 resourcepack/holiday-tags/peppermint_royalty_icon.py <out.png>
Same palette and outline as the peppermint find icon (EnthusiaHolidays tools/art/peppermint_find_icon.py).
"""
import math
import sys
from PIL import Image

OUT = (58, 10, 16, 255)
RED, RED_D, RED_L = (214, 40, 52, 255), (160, 22, 34, 255), (240, 90, 96, 255)
WH, WH_D, HI = (255, 250, 244, 255), (222, 214, 210, 255), (255, 255, 255, 255)
GOLD, GOLD_D, GOLD_L, GEM = (255, 196, 46, 255), (196, 132, 18, 255), (255, 236, 140, 255), (64, 160, 255, 255)
CROWN = [  # rows 0-4, 16 wide: three points, a band, a blue gem in the middle
    "....o..o..o.....",
    "...oLo.oLo.oLo..",
    "...oGGoGgGoGGo..",
    "...oGGGGEGGGGo..",
    "...oDDDDDDDDDo..",
]
PAL = {"o": OUT, "G": GOLD, "g": GOLD_L, "L": GOLD_L, "D": GOLD_D, "E": GEM}


def draw():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy, radius = 8, 10.5, 5.6
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d >= radius:
                continue
            a = math.atan2(dy, dx) + 0.15 * d
            red = int(((a + math.pi) / (2 * math.pi)) * 8) % 2 == 0
            col = WH if d < 1.0 else RED if red else WH
            rim = d > radius - 1.1
            if rim and dx + dy > 1.2:
                col = RED_D if red else WH_D
            if rim and dx + dy < -3.0:
                col = RED_L if red else HI
            img.putpixel((x, y), col)
    src = img.copy()
    for y in range(16):
        for x in range(16):
            near = [(x + a, y + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if src.getpixel((x, y))[3] == 0 and any(0 <= p < 16 and 0 <= q < 16 and src.getpixel((p, q))[3] for p, q in near):
                img.putpixel((x, y), OUT)
    for y, row in enumerate(CROWN):
        for x, ch in enumerate(row):
            if ch in PAL:
                img.putpixel((x, y), PAL[ch])
    return img


if __name__ == "__main__":
    draw().save(sys.argv[1] if len(sys.argv) > 1 else "peppermint_royalty.png")
