"""Generate matching Store PNG and Android vector icons from shared geometry.

Requires Pillow. Run: python make_icon.py
"""
from pathlib import Path
from math import cos, sin, radians
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
BG, PURPLE, WHITE, MUTED = '#242235', '#b8adff', '#f4f1ff', '#9891b3'
shapes = []

def polygon(color, points):
    shapes.append((color, points))

def box(color, left, top, right, bottom):
    polygon(color, [(left, top), (right, top), (right, bottom), (left, bottom)])

def arc_arrow(start, end):
    def point(radius, angle):
        return (54 + radius * cos(radians(angle)), 55 + radius * sin(radians(angle)))
    angles = [start + (end-start)*i/32 for i in range(33)]
    polygon(PURPLE, [point(11, a) for a in angles] + [point(7, a) for a in reversed(angles)])
    tip = point(9, end)
    # Arrowhead points along the clockwise tangent of the arc.
    tx, ty = -sin(radians(end)), cos(radians(end))
    nx, ny = cos(radians(end)), sin(radians(end))
    polygon(WHITE, [(tip[0]+tx*4, tip[1]+ty*4),
                    (tip[0]-tx*3+nx*4, tip[1]-ty*3+ny*4),
                    (tip[0]-tx*3-nx*4, tip[1]-ty*3-ny*4)])

box(BG, 0, 0, 108, 108)
# CoreBAR with three lit display segments.
box(PURPLE, 18, 17, 90, 43)
box(BG, 23, 22, 85, 38)
box(WHITE, 28, 27, 48, 33)
box(PURPLE, 53, 27, 65, 33)
box(PURPLE, 70, 27, 80, 33)
# Branches connect the bar, sync symbol and the two Vivo displays.
box(MUTED, 52, 43, 56, 46)
box(MUTED, 52, 64, 56, 70)
box(MUTED, 31, 67, 77, 71)
box(MUTED, 31, 67, 35, 75)
box(MUTED, 73, 67, 77, 75)
arc_arrow(195, 345)
arc_arrow(15, 165)
# Two compact VivoCube faces, with matching lavender screens.
for left in (19, 61):
    box(PURPLE, left, 74, left+28, 96)
    box(BG, left+4, 78, left+24, 92)
    box(WHITE, left+8, 82, left+20, 88)

image = Image.new('RGB', (2048, 2048), BG)
draw = ImageDraw.Draw(image)
for color, points in shapes:
    draw.polygon([(x*2048/108, y*2048/108) for x,y in points], fill=color)
image.resize((512,512), Image.Resampling.LANCZOS).save(ROOT/'icon.png', optimize=True)
paths = []
for color, points in shapes:
    data = 'M' + ' L'.join(f'{x:.3f},{y:.3f}' for x,y in points) + ' Z'
    paths.append(f' <path android:fillColor="{color}" android:pathData="{data}"/>')
vector = '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n android:width="512dp" android:height="512dp" android:viewportWidth="108" android:viewportHeight="108">\n' + '\n'.join(paths) + '\n</vector>\n'
(ROOT/'res/drawable/ic_launcher.xml').write_text(vector, encoding='utf-8')
