#!/usr/bin/env python3
"""Карточки QR для экрана настроек (задача 9.3).

qr_telegram.png: белая карточка, вырезанная из скриншота tools/QRTG.png (QR от Telegram).
qr_site.png: QR сайта в том же стиле: скруглённые точки, угловые маркеры-кольца, подпись.
Нужны Pillow и qrencode. Запуск из корня проекта: python3 tools/qr-cards.py
"""
import subprocess
from PIL import Image, ImageDraw, ImageFont

OUT = "app/src/main/res/drawable-nodpi/"
FONT = "/usr/share/fonts/noto/NotoSans-Medium.ttf"
# Размеры карточки Telegram в пикселях скриншота
W, H, RADIUS = 288, 308, 27
QR_X, QR_Y, QR_SIZE = 43, 35, 202
CAPTION_Y, CAPTION_H = 260, 28
S = 4  # рисуем крупнее и уменьшаем, чтобы края были гладкими


def card_mask(scale=1):
    mask = Image.new("L", (W * scale, H * scale), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, W * scale - 1, H * scale - 1), RADIUS * scale, fill=255)
    return mask


def telegram():
    card = Image.open("tools/QRTG.png").convert("RGBA").crop((52, 21, 52 + W, 21 + H))
    card.putalpha(card_mask())
    card.save(OUT + "qr_telegram.png", optimize=True)


def matrix(text):
    rows = subprocess.run(["qrencode", "-t", "ASCII", "-m", "0", "-l", "Q", text],
                          capture_output=True, text=True, check=True).stdout.splitlines()
    return [[row[i] == "#" for i in range(0, len(row), 2)] for row in rows]


def site(url, caption):
    m = matrix(url)
    n = len(m)
    img = Image.new("RGB", (W * S, H * S), "white")
    d = ImageDraw.Draw(img)
    cell = QR_SIZE * S / n
    x0, y0 = QR_X * S + (QR_SIZE * S - cell * n) / 2, QR_Y * S

    def box(c, r, w=1, h=1, inset=0):
        return (x0 + c * cell + inset, y0 + r * cell + inset,
                x0 + (c + w) * cell - inset - 1, y0 + (r + h) * cell - inset - 1)

    finders = [(0, 0), (n - 7, 0), (0, n - 7)]

    def in_finder(c, r):
        return any(fc <= c < fc + 7 and fr <= r < fr + 7 for fc, fr in finders)

    def dark(c, r):
        return 0 <= c < n and 0 <= r < n and m[r][c] and not in_finder(c, r)

    # Точки: круг на модуль, соседние модули сливаются перемычками
    for r in range(n):
        for c in range(n):
            if not dark(c, r):
                continue
            d.ellipse(box(c, r), fill="black")
            if dark(c + 1, r):
                d.rectangle((x0 + (c + 0.5) * cell, y0 + r * cell,
                             x0 + (c + 1.5) * cell - 1, y0 + (r + 1) * cell - 1), fill="black")
            if dark(c, r + 1):
                d.rectangle((x0 + c * cell, y0 + (r + 0.5) * cell,
                             x0 + (c + 1) * cell - 1, y0 + (r + 1.5) * cell - 1), fill="black")

    # Угловые маркеры: скруглённое кольцо 7x7 и скруглённый квадрат 3x3
    for fc, fr in finders:
        d.rounded_rectangle(box(fc, fr, 7, 7), 2.2 * cell, fill="black")
        d.rounded_rectangle(box(fc + 1, fr + 1, 5, 5), 1.4 * cell, fill="white")
        d.rounded_rectangle(box(fc + 2, fr + 2, 3, 3), 1.0 * cell, fill="black")

    font = ImageFont.truetype(FONT, CAPTION_H * S)
    d.text((W * S / 2, (CAPTION_Y + CAPTION_H / 2) * S), caption, font=font, fill="black", anchor="mm")

    card = img.convert("RGBA")
    card.putalpha(card_mask(S))
    card.resize((W, H), Image.LANCZOS).save(OUT + "qr_site.png", optimize=True)


telegram()
site("https://irlme.online", "irlme.online")
