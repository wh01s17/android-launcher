"""Moon geometry adapted from dotfiles pixel-art.py; no third-party sprites. All palette comes from the theme."""

import math

R = 28

TERMINATOR = -9

SEED = 1969

DITHER = 0.6

DITHER_FULL = 0.2

MARIA = [
    (17, 59, 9),  # Crisium
    (28, 18, 9),  # Serenitatis
    (6, 25, 9),  # Tranquillitatis
    (12, 34, 8),
    (0, 36, 7),
    (-4, 52, 7),  # Fecunditatis
    (-12, 50, 6),
    (2, 49, 5),
    (-15, 35, 5),  # Nectaris
    (13, 4, 4),  # Vaporum
    (55, 12, 6),  # Frigoris
    (57, 28, 5),
    (5, 2, 4),  # Sinus Medii
    (-24, 20, 3),  # Sinus Asperitatis edge
    (40, 34, 3),  # Lacus Somniorum
    (35, -18, 13),  # Imbrium
    (30, -6, 7),
    (40, -26, 8),
    (45, -32, 4),  # Sinus Iridum
    (56, -5, 5),  # Frigoris (west)
    (55, -22, 5),
    (54, -38, 5),
    (22, -55, 11),  # Procellarum
    (6, -55, 11),
    (-6, -46, 9),
    (34, -52, 8),
    (12, -42, 8),
    (24, -38, 8),
    (-14, -40, 6),
    (7, -31, 7),  # Insularum
    (-21, -17, 9),  # Nubium
    (-10, -23, 6),  # Cognitum
    (-24, -39, 6),  # Humorum
]

BRIGHT_CRATERS = [
    (-43, -11, 3.0, 42),  # Tycho
    (10, -20, 2.6, 18),  # Copernicus
    (8, -38, 1.6, 10),  # Kepler
    (24, -47, 1.8, 0),  # Aristarchus
    (16, 47, 0.9, 8),  # Proclus
]

DARK_CRATERS = [
    (51, -9, 1.8),  # Plato
    (-5, -68, 3.0),  # Grimaldi
]

CRATERS = [
    (-11, 26, 2.4),  # Theophilus
    (-13, 24, 2.3),  # Cyrillus
    (-18, 24, 2.3),  # Catharina
    (32, 30, 2.4),  # Posidonius
    (-9, 61, 2.6),  # Langrenus
    (-25, 61, 3.0),  # Petavius
    (-37, 3, 2.6),  # Maurolycus
    (-34, 16, 2.6),  # Piccolomini-ish highlands
    (-48, 10, 3.2),
    (-58, 30, 3.0),
    (-45, 45, 2.6),
    (-30, 40, 2.2),
    (-62, 8, 2.8),
    (-70, 40, 3.0),
    (48, 50, 2.6),  # Atlas / Hercules region
    (46, 42, 2.2),
    (70, 30, 3.0),
    (-52, 60, 2.6),
    (-20, 8, 2.4),  # Albategnius-ish
    (-10, 6, 2.2),
]

BAYER = [[0, 2], [3, 1]]

GLYPHS = {
    "w": [".....", ".....", "#...#", "#...#", "#.#.#", "#.#.#", ".#.#."],
    "h": ["#....", "#....", "#.##.", "##..#", "#...#", "#...#", "#...#"],
    "0": [".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."],
    "s": [".....", ".....", ".####", "#....", ".###.", "....#", "####."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
}

def lattice(ix, iy):
    h = (ix * 374761393 + iy * 668265263 + SEED * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) / 0xFFFFFFFF

def noise(x, y):
    ix, iy = math.floor(x), math.floor(y)
    fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = lattice(ix, iy) + (lattice(ix + 1, iy) - lattice(ix, iy)) * sx
    b = lattice(ix, iy + 1) + (lattice(ix + 1, iy + 1) - lattice(ix, iy + 1)) * sx
    return a + (b - a) * sy

def angular_distance(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dl = math.radians(lon2 - lon1)
    c = math.sin(p1) * math.sin(p2) + math.cos(p1) * math.cos(p2) * math.cos(dl)
    return math.degrees(math.acos(max(-1.0, min(1.0, c))))

def smoothstep(a, b, v):
    t = min(max((v - a) / (b - a), 0.0), 1.0)
    return t * t * (3 - 2 * t)

def surface(lat, lon, full):
    """Albedo at a selenographic point: ~0.35 in maria, ~1 in highlands."""
    wobble = (noise(lon / 7 + 3.1, lat / 7 + 8.2) - 0.5) * 6
    mare = 0.0
    for mlat, mlon, mr in MARIA:
        d = angular_distance(lat, lon, mlat, mlon)
        mare = max(mare, 1 - smoothstep(mr - 1.5, mr + 1.5, d + wobble))
    if full:
        # Flat maria; highlands vary in broad patches, brightest in the south.
        albedo = 1.0 - 0.58 * mare
        highland = (noise(lon / 9 + 4.4, lat / 9 + 6.6) - 0.5) * 0.3
        highland += 0.1 * smoothstep(-15, -40, lat)
        albedo += (1 - mare) * highland
    else:
        # Maria read as solid dark green, broken by patchy darker lava flows.
        flows = noise(lon / 1.6 + 2.2, lat / 1.6 + 7.4)
        albedo = 1.0 - mare * (0.58 + 0.16 * flows)
        albedo *= 0.9 + 0.2 * noise(lon / 2.5 + 5.5, lat / 2.5 + 1.3)

    for clat, clon, cr in CRATERS:
        d = angular_distance(lat, lon, clat, clon)
        if full:
            # Overhead sun: no shadows, ordinary craters vanish.
            continue
        elif d < cr:
            # Sun in the east: the eastern floor sits in the rim's shadow and
            # the western inner wall catches the light.
            east = (lon - clon) * math.cos(math.radians(lat)) / cr
            albedo *= 0.2 if east > -0.2 else 1.3
        elif d < cr * 1.35:
            albedo *= 1.2

    if full:
        for clat, clon, cr in DARK_CRATERS:
            if angular_distance(lat, lon, clat, clon) < cr:
                albedo *= 0.4
        for clat, clon, cr, reach in BRIGHT_CRATERS:
            d = angular_distance(lat, lon, clat, clon)
            if d < cr:
                return 2.0  # sentinel: rendered in SPARK
            if d < reach:
                b = math.atan2(lat - clat, (lon - clon) * math.cos(math.radians(lat)))
                streak = noise(math.cos(b) * 4 + clon, math.sin(b) * 4 + clat)
                albedo += 0.45 * smoothstep(0.62, 0.7, streak) * (1 - d / reach) ** 0.5
    return albedo

def render_moon(full=False):
    sun = 0.0 if full else math.radians(90 + TERMINATOR)  # subsolar longitude
    pixels = {}
    for row in range(2 * R):
        for col in range(2 * R):
            x, y = (col + 0.5) / R - 1, (row + 0.5) / R - 1
            rr = x * x + y * y
            if rr > 1:
                continue
            z = math.sqrt(1 - rr)
            lat = math.degrees(math.asin(-y))
            lon = math.degrees(math.atan2(x, z))
            lam = math.cos(math.radians(lat)) * math.cos(math.radians(lon) - sun)
            # Ragged terminator: crater rims and peaks poke into the night.
            if not full:
                lam += (noise(lon / 3 + 9.7, lat / 3 + 2.4) - 0.5) * 0.14
                if lam <= 0:
                    continue
                shade = smoothstep(0.0, 0.2, lam) * (0.72 + 0.22 * lam)
            else:
                shade = 0.7 + 0.1 * lam  # faint limb darkening
            albedo = surface(lat, lon, full)
            if albedo >= 2.0:
                pixels[(col, row)] = 4
                continue
            v = min(shade * albedo, 1.0)
            # Flat tones with dithering only near each tone boundary.
            t = (BAYER[row % 2][col % 2] + 0.5) / 4 - 0.5
            tone = min(int(v * 3 + 0.5 + t * (DITHER_FULL if full else DITHER)), 3)
            if tone:
                pixels[(col, row)] = tone
    return pixels
