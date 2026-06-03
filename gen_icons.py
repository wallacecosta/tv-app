#!/usr/bin/env python3
"""Gera ícones e banner IPTV usando apenas stdlib do Python."""

import struct, zlib, math, os

# Paleta de cores (R, G, B, A)
BG    = (26,  26,  46, 255)   # #1a1a2e - fundo escuro
WHITE = (255, 255, 255, 255)  # corpo da TV
DARK  = (22,  33,  62, 255)   # #16213e - tela da TV
RED   = (233,  69,  96, 255)  # #e94560 - play / ondas

# ─── Canvas ────────────────────────────────────────────────────────────────────

class Canvas:
    def __init__(self, w, h):
        self.w = w
        self.h = h
        self.px = [BG] * (w * h)       # começa todo com o fundo

    def blend(self, x, y, col):
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        r2, g2, b2, a2 = col
        if a2 == 0:
            return
        if a2 == 255:
            self.px[y * self.w + x] = col
            return
        r1, g1, b1, a1 = self.px[y * self.w + x]
        f = a2 / 255.0
        self.px[y * self.w + x] = (
            int(r1 + (r2 - r1) * f),
            int(g1 + (g2 - g1) * f),
            int(b1 + (b2 - b1) * f),
            min(255, a1 + a2),
        )

    def fill_rect(self, x, y, w, h, col):
        for py in range(max(0, y), min(self.h, y + h)):
            for px in range(max(0, x), min(self.w, x + w)):
                self.blend(px, py, col)

    def fill_circle(self, cx, cy, r, col):
        for py in range(int(cy - r) - 1, int(cy + r) + 2):
            for px in range(int(cx - r) - 1, int(cx + r) + 2):
                d = math.hypot(px - cx, py - cy)
                if d <= r - 0.5:
                    self.blend(px, py, col)
                elif d <= r + 0.5:
                    a = int((r + 0.5 - d) * col[3])
                    self.blend(px, py, (col[0], col[1], col[2], a))

    def fill_rrect(self, x, y, w, h, r, col):
        r = min(r, w // 2, h // 2, 1) if min(w, h) < 4 else r
        self.fill_rect(x + r, y, w - 2 * r, h, col)
        self.fill_rect(x,     y + r, r,     h - 2 * r, col)
        self.fill_rect(x + w - r, y + r, r, h - 2 * r, col)
        self.fill_circle(x + r,     y + r,     r, col)
        self.fill_circle(x + w - r, y + r,     r, col)
        self.fill_circle(x + r,     y + h - r, r, col)
        self.fill_circle(x + w - r, y + h - r, r, col)

    def fill_tri(self, ax, ay, bx, by, cx, cy, col):
        pts = sorted([(ax, ay), (bx, by), (cx, cy)], key=lambda p: p[1])
        (ax, ay), (bx, by), (cx, cy) = pts

        def xi(y, p1, p2):
            if p2[1] == p1[1]:
                return p1[0]
            return p1[0] + (p2[0] - p1[0]) * (y - p1[1]) / (p2[1] - p1[1])

        for y in range(int(ay), int(cy) + 1):
            xl = xi(y, (ax, ay), (bx, by)) if y < by else xi(y, (bx, by), (cx, cy))
            xr = xi(y, (ax, ay), (cx, cy))
            if xl > xr:
                xl, xr = xr, xl
            for x in range(int(xl), int(xr) + 1):
                self.blend(x, y, col)

    def draw_arc(self, cx, cy, r, thick, a0, a1, col):
        """Arco preenchido (setor anular). Ângulos em graus."""
        ro, ri = r + thick // 2, r - thick // 2
        for py in range(int(cy - ro) - 1, int(cy + ro) + 2):
            for px in range(int(cx - ro) - 1, int(cx + ro) + 2):
                d = math.hypot(px - cx, py - cy)
                if ri <= d <= ro:
                    ang = math.degrees(math.atan2(py - cy, px - cx))
                    if a0 <= ang <= a1:
                        self.blend(px, py, col)

    def save(self, path):
        os.makedirs(os.path.dirname(path) or '.', exist_ok=True)

        def chunk(tag, data):
            c = struct.pack('>I', len(data)) + tag + data
            return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

        raw = b''
        for row in range(self.h):
            raw += b'\x00'
            for col in range(self.w):
                raw += bytes(self.px[row * self.w + col])

        png  = b'\x89PNG\r\n\x1a\n'
        png += chunk(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 6, 0, 0, 0))
        png += chunk(b'IDAT', zlib.compress(raw, 9))
        png += chunk(b'IEND', b'')
        with open(path, 'wb') as f:
            f.write(png)
        print(f'  {path} ({self.w}x{self.h})')


# ─── Desenha a TV + play button ────────────────────────────────────────────────

def draw_tv(c, x0, y0, tv_h):
    """
    Desenha ícone de TV com botão play.
    tv_h = altura do corpo da TV (sem as pernas).
    """
    s    = tv_h / 100.0
    tv_w = int(tv_h * 1.52)

    # Corpo branco
    cr = max(2, int(7 * s))
    c.fill_rrect(x0, y0, tv_w, tv_h, cr, WHITE)

    # Tela interna (escura) — cria efeito de moldura/bisel
    m = max(2, int(8 * s))
    c.fill_rrect(x0 + m, y0 + m, tv_w - 2 * m, tv_h - 2 * m, max(1, int(4 * s)), DARK)

    # Pernas
    lw = max(2, int(13 * s))
    lh = max(2, int(13 * s))
    ly = y0 + tv_h
    c.fill_rect(x0 + int(22 * s),        ly, lw, lh, WHITE)
    c.fill_rect(x0 + tv_w - int(22 * s) - lw, ly, lw, lh, WHITE)

    # Base
    bh = max(2, int(6 * s))
    bx = x0 + int(14 * s)
    bw = tv_w - int(28 * s)
    c.fill_rrect(bx, ly + lh, bw, bh, max(1, int(3 * s)), WHITE)

    # Play triangle (vermelho, dentro da tela)
    scx = x0 + tv_w // 2
    scy = y0 + tv_h // 2
    th  = int(tv_h * 0.27)
    tw  = int(tv_h * 0.30)
    c.fill_tri(scx - tw, scy - th, scx - tw, scy + th, scx + tw + int(2 * s), scy, RED)

    return tv_w   # retorna a largura calculada


# ─── Ícone quadrado ────────────────────────────────────────────────────────────

def make_icon(size):
    c = Canvas(size, size)
    s    = size / 192.0
    tv_h = int(96 * s)
    tv_w = int(tv_h * 1.52)
    tx   = (size - tv_w) // 2
    ty   = (size - tv_h) // 2 - int(5 * s)
    draw_tv(c, tx, ty, tv_h)
    return c


# ─── Banner Android TV (320 × 180) ────────────────────────────────────────────

def make_banner(bw, bh):
    c = Canvas(bw, bh)

    # TV icon à esquerda
    tv_h = int(bh * 0.56)
    tv_w = int(tv_h * 1.52)
    tx   = int(bh * 0.14)
    ty   = (bh - tv_h) // 2 - int(tv_h * 0.04)
    draw_tv(c, tx, ty, tv_h)

    # Ondas de broadcast à direita ()))
    wave_cx = tx + tv_w + int(bh * 0.22)
    wave_cy = ty + tv_h // 2
    thick   = max(2, int(bh * 0.048))
    for r in [int(bh * 0.13), int(bh * 0.21), int(bh * 0.29)]:
        c.draw_arc(wave_cx, wave_cy, r, thick, -52, 52, RED)

    return c


# ─── Gera todos os arquivos ────────────────────────────────────────────────────

BASE  = 'app/src/main/res'
SIZES = [
    ('mipmap-mdpi',    48),
    ('mipmap-hdpi',    72),
    ('mipmap-xhdpi',   96),
    ('mipmap-xxhdpi',  144),
    ('mipmap-xxxhdpi', 192),
]

print('Gerando ícones...')
for density, sz in SIZES:
    ic = make_icon(sz)
    ic.save(f'{BASE}/{density}/ic_launcher.png')
    ic.save(f'{BASE}/{density}/ic_launcher_round.png')

print('Gerando banner TV...')
banner = make_banner(320, 180)
banner.save(f'{BASE}/drawable-xhdpi/tv_banner.png')

print('Concluído!')
