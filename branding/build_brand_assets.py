import os
import math
from collections import deque
from PIL import Image, ImageDraw

ORIGINAL_IMAGE = r"C:/Users/carlo/.gemini/antigravity/brain/0776f2e2-b5d1-4215-bb2b-20f319f93351/.user_uploaded/media_1793442919990_b0234767.jpg"
PROJECT_ROOT = r"C:/Users/carlo/AndroidStudioProjects/PictoHabla"
APP_RES = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")
BRANDING_DIR = os.path.join(PROJECT_ROOT, "branding")
SCRATCH_DIR = r"C:/Users/carlo/.gemini/antigravity/brain/0776f2e2-b5d1-4215-bb2b-20f319f93351/scratch"

os.makedirs(BRANDING_DIR, exist_ok=True)
os.makedirs(SCRATCH_DIR, exist_ok=True)

def clean_master_image():
    """Generates a 1024x1024 master image with clean white background, removing black corners."""
    img = Image.open(ORIGINAL_IMAGE).convert("RGBA")
    w, h = img.size
    pixels = img.load()

    # Flood fill from 4 corners to eliminate black background and outer squircle border
    queue = deque([(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)])
    visited = set(queue)

    while queue:
        x, y = queue.popleft()
        r, g, b, a = pixels[x, y]
        is_outer = (x < 35 or x >= w - 35 or y < 35 or y >= h - 35) and (r < 240 or g < 240 or b < 240)
        is_corner = (x < 140 or x >= w - 140) and (y < 140 or y >= h - 140) and (r < 245 or g < 245 or b < 245)
        is_dark = (r < 180 and g < 180 and b < 180)

        if is_dark or is_outer or is_corner:
            pixels[x, y] = (255, 255, 255, 255)
            for nx, ny in [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]:
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in visited:
                    visited.add((nx, ny))
                    queue.append((nx, ny))

    # Outer 15px frame guarantee pure white
    for y in range(h):
        for x in range(w):
            if x < 15 or x >= w - 15 or y < 15 or y >= h - 15:
                pixels[x, y] = (255, 255, 255, 255)

    # Clean corner zones (where no artwork exists)
    for y in range(130):
        for x in range(130):
            pixels[x, y] = (255, 255, 255, 255)
            pixels[w - 1 - x, y] = (255, 255, 255, 255)
            pixels[x, h - 1 - y] = (255, 255, 255, 255)
            pixels[w - 1 - x, h - 1 - y] = (255, 255, 255, 255)

    master_path = os.path.join(BRANDING_DIR, "pictohabla_master_1024.png")
    img.save(master_path, "PNG")
    print(f"[OK] Master 1024x1024 saved to {master_path}")
    return img

def make_transparent_exterior(img_crop):
    """Makes only the exterior white background transparent, keeping face & bubble interiors opaque."""
    img = img_crop.copy().convert("RGBA")
    w, h = img.size
    pixels = img.load()

    queue = deque()
    # Add border pixels that are near white
    for x in range(w):
        if pixels[x, 0][:3] >= (242, 242, 242): queue.append((x, 0))
        if pixels[x, h - 1][:3] >= (242, 242, 242): queue.append((x, h - 1))
    for y in range(h):
        if pixels[0, y][:3] >= (242, 242, 242): queue.append((0, y))
        if pixels[w - 1, y][:3] >= (242, 242, 242): queue.append((w - 1, y))

    visited = set(queue)
    while queue:
        x, y = queue.popleft()
        r, g, b, a = pixels[x, y]
        if r > 240 and g > 240 and b > 240:
            pixels[x, y] = (255, 255, 255, 0)
            for nx, ny in [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]:
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in visited:
                    visited.add((nx, ny))
                    queue.append((nx, ny))
    return img

def create_google_play_icon(master_img):
    """Google Play Store 512x512 icon: 100% opaque white, full composition, 0% clipped under 20% squircle."""
    comp = master_img.crop((45, 70, 980, 895)).convert("RGBA")
    # Target size: width=430, height=379
    comp_resized = comp.resize((430, 379), Image.Resampling.LANCZOS)

    gp_icon = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    offset_x = (512 - 430) // 2
    offset_y = (512 - 379) // 2
    gp_icon.paste(comp_resized, (offset_x, offset_y), comp_resized)

    # Save to branding folder
    out_path = os.path.join(BRANDING_DIR, "google_play_icon_512.png")
    gp_icon.save(out_path, "PNG")
    print(f"[OK] Google Play 512x512 icon saved to {out_path}")
    return gp_icon

def create_symbol_and_logo_assets(master_img):
    """Generates standalone symbol and full logo assets in various sizes with white and transparent backgrounds."""
    # 1. Symbol Crop (Person + Speech lines + Speech bubble with 4 pictograms)
    symbol_crop = master_img.crop((60, 75, 965, 665))
    symbol_trans = make_transparent_exterior(symbol_crop)

    # Symbol 1024x1024 centered
    sym_1024_w = Image.new("RGBA", (1024, 1024), (255, 255, 255, 255))
    sym_1024_t = Image.new("RGBA", (1024, 1024), (255, 255, 255, 0))
    scale = 880 / symbol_crop.width
    sh = int(symbol_crop.height * scale)
    sym_scaled_w = symbol_crop.resize((880, sh), Image.Resampling.LANCZOS)
    sym_scaled_t = symbol_trans.resize((880, sh), Image.Resampling.LANCZOS)
    pos_x = (1024 - 880) // 2
    pos_y = (1024 - sh) // 2

    sym_1024_w.paste(sym_scaled_w, (pos_x, pos_y), sym_scaled_w)
    sym_1024_t.paste(sym_scaled_t, (pos_x, pos_y), sym_scaled_t)

    sym_1024_w.save(os.path.join(BRANDING_DIR, "pictohabla_symbol_1024_white.png"), "PNG")
    sym_1024_t.save(os.path.join(BRANDING_DIR, "pictohabla_symbol_1024_transparent.png"), "PNG")
    sym_1024_t.resize((512, 512), Image.Resampling.LANCZOS).save(
        os.path.join(BRANDING_DIR, "pictohabla_symbol_512_transparent.png"), "PNG"
    )

    # In-app drawable symbol
    app_drawable_sym = os.path.join(APP_RES, "drawable", "ic_pictohabla_symbol.png")
    sym_1024_t.resize((512, 512), Image.Resampling.LANCZOS).save(app_drawable_sym, "PNG")

    # 2. Full Logo Crop (Symbol + PictoHabla + Underline)
    logo_crop = master_img.crop((45, 70, 980, 895))
    logo_trans = make_transparent_exterior(logo_crop)

    logo_1024_w = Image.new("RGBA", (1024, 1024), (255, 255, 255, 255))
    logo_1024_t = Image.new("RGBA", (1024, 1024), (255, 255, 255, 0))
    scale_l = 880 / logo_crop.width
    lh = int(logo_crop.height * scale_l)
    logo_scaled_w = logo_crop.resize((880, lh), Image.Resampling.LANCZOS)
    logo_scaled_t = logo_trans.resize((880, lh), Image.Resampling.LANCZOS)
    pos_lx = (1024 - 880) // 2
    pos_ly = (1024 - lh) // 2

    logo_1024_w.paste(logo_scaled_w, (pos_lx, pos_ly), logo_scaled_w)
    logo_1024_t.paste(logo_scaled_t, (pos_lx, pos_ly), logo_scaled_t)

    logo_1024_w.save(os.path.join(BRANDING_DIR, "pictohabla_logo_1024_white.png"), "PNG")
    logo_1024_t.save(os.path.join(BRANDING_DIR, "pictohabla_logo_1024_transparent.png"), "PNG")
    logo_1024_t.resize((512, 512), Image.Resampling.LANCZOS).save(
        os.path.join(BRANDING_DIR, "pictohabla_logo_512_transparent.png"), "PNG"
    )

    # In-app drawable logo
    app_drawable_logo = os.path.join(APP_RES, "drawable", "ic_pictohabla_logo.png")
    logo_1024_t.resize((512, 512), Image.Resampling.LANCZOS).save(app_drawable_logo, "PNG")

    print("[OK] Standalone symbol and full logo assets generated.")
    return symbol_trans

def generate_android_launcher_icons(symbol_trans):
    """Generates all adaptive icon foregrounds and legacy icons across all mipmap densities."""
    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432)
    }

    aspect = symbol_trans.width / float(symbol_trans.height) # ~1.534

    for folder, (legacy_size, adaptive_size) in densities.items():
        folder_path = os.path.join(APP_RES, folder)
        os.makedirs(folder_path, exist_ok=True)

        # 1. Adaptive Icon Foreground (Transparent background, centered in adaptive_size)
        # Target width is 56% of adaptive size (~56dp in 108dp)
        target_w = int(adaptive_size * 0.54)
        target_h = int(target_w / aspect)
        sym_resized = symbol_trans.resize((target_w, target_h), Image.Resampling.LANCZOS)

        fg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
        fg_pos_x = (adaptive_size - target_w) // 2
        fg_pos_y = (adaptive_size - target_h) // 2
        fg_canvas.paste(sym_resized, (fg_pos_x, fg_pos_y), sym_resized)

        fg_webp_path = os.path.join(folder_path, "ic_launcher_foreground.webp")
        fg_canvas.save(fg_webp_path, "WEBP", lossless=True)
        # Also save PNG for maximum compatibility
        fg_png_path = os.path.join(folder_path, "ic_launcher_foreground.png")
        fg_canvas.save(fg_png_path, "PNG")

        # 2. Legacy ic_launcher (Squircle with 20% radius on pure white background)
        legacy_canvas = Image.new("RGBA", (legacy_size, legacy_size), (255, 255, 255, 255))
        leg_sym_w = int(legacy_size * 0.78)
        leg_sym_h = int(leg_sym_w / aspect)
        leg_sym = symbol_trans.resize((leg_sym_w, leg_sym_h), Image.Resampling.LANCZOS)
        leg_x = (legacy_size - leg_sym_w) // 2
        leg_y = (legacy_size - leg_sym_h) // 2
        legacy_canvas.paste(leg_sym, (leg_x, leg_y), leg_sym)

        # Rounded rectangle mask (~20% radius)
        mask_rect = Image.new("L", (legacy_size, legacy_size), 0)
        draw_r = ImageDraw.Draw(mask_rect)
        radius = int(legacy_size * 0.20)
        draw_r.rounded_rectangle((0, 0, legacy_size, legacy_size), radius=radius, fill=255)

        masked_legacy = Image.new("RGBA", (legacy_size, legacy_size), (0, 0, 0, 0))
        masked_legacy.paste(legacy_canvas, (0, 0), mask_rect)

        legacy_webp = os.path.join(folder_path, "ic_launcher.webp")
        masked_legacy.save(legacy_webp, "WEBP", lossless=True)

        # 3. Legacy ic_launcher_round (Circular mask on pure white background)
        round_canvas = Image.new("RGBA", (legacy_size, legacy_size), (255, 255, 255, 255))
        # For circle, target width slightly smaller to fit inside diameter
        round_sym_w = int(legacy_size * 0.72)
        round_sym_h = int(round_sym_w / aspect)
        round_sym = symbol_trans.resize((round_sym_w, round_sym_h), Image.Resampling.LANCZOS)
        round_x = (legacy_size - round_sym_w) // 2
        round_y = (legacy_size - round_sym_h) // 2
        round_canvas.paste(round_sym, (round_x, round_y), round_sym)

        mask_circle = Image.new("L", (legacy_size, legacy_size), 0)
        draw_c = ImageDraw.Draw(mask_circle)
        draw_c.ellipse((0, 0, legacy_size, legacy_size), fill=255)

        masked_round = Image.new("RGBA", (legacy_size, legacy_size), (0, 0, 0, 0))
        masked_round.paste(round_canvas, (0, 0), mask_circle)

        round_webp = os.path.join(folder_path, "ic_launcher_round.webp")
        masked_round.save(round_webp, "WEBP", lossless=True)

    print("[OK] Android launcher icons (adaptive foregrounds + legacy squircle + legacy round) generated for all densities.")

def generate_svg_assets():
    """Generates standalone clean SVG files for the logo and symbol."""
    # SVG for symbol
    symbol_svg_path = os.path.join(BRANDING_DIR, "pictohabla_symbol.svg")
    with open(symbol_svg_path, "w", encoding="utf-8") as f:
        f.write('''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="100%" height="100%">
  <!-- PictoHabla Official Brand Symbol -->
  <!-- Person speaking silhouette, 3 orange speech waves, speech bubble with 4 pictograms (apple, house, smiley, tree) -->
  <rect width="1024" height="1024" fill="#FFFFFF"/>
  <g transform="translate(60, 180) scale(0.98)">
    <!-- Person Head & Body -->
    <path d="M 235 480 C 170 480, 100 520, 65 600 C 130 630, 240 650, 350 640 C 370 600, 360 540, 320 495 C 295 485, 265 480, 235 480 Z" fill="#5EA521" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round"/>
    <!-- Head outline and open mouth -->
    <path d="M 230 180 C 145 180, 85 240, 85 330 C 85 390, 120 440, 175 465 L 175 510 L 295 510 L 295 465 C 330 450, 360 415, 375 375 L 315 375 C 300 375, 290 390, 280 405 C 275 395, 285 360, 320 340 L 375 340 C 385 310, 380 270, 355 235 C 325 195, 280 180, 230 180 Z" fill="#FFFFFF" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round" stroke-linecap="round"/>
    <!-- Eye -->
    <circle cx="280" cy="275" r="22" fill="#1D1E20"/>
    <!-- Three Orange Communication Waves -->
    <path d="M 365 375 L 435 345" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
    <path d="M 370 425 L 455 425" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
    <path d="M 365 475 L 435 505" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
    <!-- Speech Bubble Container -->
    <path d="M 520 80 L 840 80 C 910 80, 960 130, 960 200 L 960 460 C 960 530, 910 580, 840 580 L 580 580 L 465 670 L 495 580 L 520 580 C 450 580, 400 530, 400 460 L 400 200 C 400 130, 450 80, 520 80 Z" fill="#FFFFFF" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round" stroke-linecap="round"/>
    <!-- 4 Pictogram Tile Grid -->
    <!-- Tile 1: Apple -->
    <rect x="445" y="125" width="220" height="200" rx="30" fill="#F4F5F7"/>
    <circle cx="535" cy="235" r="50" fill="#E63023"/>
    <circle cx="575" cy="235" r="50" fill="#E63023"/>
    <path d="M 555 185 C 555 165, 570 150, 580 145" stroke="#795548" stroke-width="12" stroke-linecap="round" fill="none"/>
    <path d="M 560 175 C 585 165, 605 175, 600 190 C 580 195, 565 185, 560 175 Z" fill="#5EA521"/>
    <!-- Tile 2: House -->
    <rect x="695" y="125" width="220" height="200" rx="30" fill="#F4F5F7"/>
    <polygon points="805,145 725,215 885,215" fill="#D32F2F" stroke="#1D1E20" stroke-width="14" stroke-linejoin="round"/>
    <rect x="740" y="215" width="130" height="90" fill="#FFF8E7" stroke="#1D1E20" stroke-width="14"/>
    <rect x="765" y="245" width="30" height="60" fill="#795548"/>
    <rect x="820" y="240" width="30" height="30" fill="#42A5F5" stroke="#1D1E20" stroke-width="8"/>
    <!-- Tile 3: Smiley Face -->
    <rect x="445" y="345" width="220" height="200" rx="30" fill="#FFF8E7"/>
    <circle cx="555" cy="445" r="65" fill="#F4B41A" stroke="#1D1E20" stroke-width="14"/>
    <circle cx="530" cy="425" r="10" fill="#1D1E20"/>
    <circle cx="580" cy="425" r="10" fill="#1D1E20"/>
    <path d="M 525 460 C 535 485, 575 485, 585 460" stroke="#1D1E20" stroke-width="12" stroke-linecap="round" fill="none"/>
    <!-- Tile 4: Tree -->
    <rect x="695" y="345" width="220" height="200" rx="30" fill="#F4F5F7"/>
    <rect x="795" y="450" width="22" height="70" rx="6" fill="#795548" stroke="#1D1E20" stroke-width="12"/>
    <path d="M 805 385 C 770 385, 755 410, 760 435 C 745 445, 750 470, 770 475 C 780 485, 830 485, 840 475 C 860 470, 865 445, 850 435 C 855 410, 840 385, 805 385 Z" fill="#439819" stroke="#1D1E20" stroke-width="14" stroke-linejoin="round"/>
  </g>
</svg>''')

    # SVG for full logo
    logo_svg_path = os.path.join(BRANDING_DIR, "pictohabla_logo.svg")
    with open(logo_svg_path, "w", encoding="utf-8") as f:
        f.write('''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="100%" height="100%">
  <!-- PictoHabla Official Full Logo -->
  <rect width="1024" height="1024" fill="#FFFFFF"/>
  <g transform="translate(60, 40) scale(0.88)">
    <!-- Symbol -->
    <g transform="translate(0, 0)">
      <path d="M 235 480 C 170 480, 100 520, 65 600 C 130 630, 240 650, 350 640 C 370 600, 360 540, 320 495 C 295 485, 265 480, 235 480 Z" fill="#5EA521" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round"/>
      <path d="M 230 180 C 145 180, 85 240, 85 330 C 85 390, 120 440, 175 465 L 175 510 L 295 510 L 295 465 C 330 450, 360 415, 375 375 L 315 375 C 300 375, 290 390, 280 405 C 275 395, 285 360, 320 340 L 375 340 C 385 310, 380 270, 355 235 C 325 195, 280 180, 230 180 Z" fill="#FFFFFF" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round" stroke-linecap="round"/>
      <circle cx="280" cy="275" r="22" fill="#1D1E20"/>
      <path d="M 365 375 L 435 345" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
      <path d="M 370 425 L 455 425" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
      <path d="M 365 475 L 435 505" stroke="#FD9717" stroke-width="24" stroke-linecap="round"/>
      <path d="M 520 80 L 840 80 C 910 80, 960 130, 960 200 L 960 460 C 960 530, 910 580, 840 580 L 580 580 L 465 670 L 495 580 L 520 580 C 450 580, 400 530, 400 460 L 400 200 C 400 130, 450 80, 520 80 Z" fill="#FFFFFF" stroke="#1D1E20" stroke-width="26" stroke-linejoin="round" stroke-linecap="round"/>
      <rect x="445" y="125" width="220" height="200" rx="30" fill="#F4F5F7"/>
      <circle cx="535" cy="235" r="50" fill="#E63023"/>
      <circle cx="575" cy="235" r="50" fill="#E63023"/>
      <path d="M 555 185 C 555 165, 570 150, 580 145" stroke="#795548" stroke-width="12" stroke-linecap="round" fill="none"/>
      <path d="M 560 175 C 585 165, 605 175, 600 190 C 580 195, 565 185, 560 175 Z" fill="#5EA521"/>
      <rect x="695" y="125" width="220" height="200" rx="30" fill="#F4F5F7"/>
      <polygon points="805,145 725,215 885,215" fill="#D32F2F" stroke="#1D1E20" stroke-width="14" stroke-linejoin="round"/>
      <rect x="740" y="215" width="130" height="90" fill="#FFF8E7" stroke="#1D1E20" stroke-width="14"/>
      <rect x="765" y="245" width="30" height="60" fill="#795548"/>
      <rect x="820" y="240" width="30" height="30" fill="#42A5F5" stroke="#1D1E20" stroke-width="8"/>
      <rect x="445" y="345" width="220" height="200" rx="30" fill="#FFF8E7"/>
      <circle cx="555" cy="445" r="65" fill="#F4B41A" stroke="#1D1E20" stroke-width="14"/>
      <circle cx="530" cy="425" r="10" fill="#1D1E20"/>
      <circle cx="580" cy="425" r="10" fill="#1D1E20"/>
      <path d="M 525 460 C 535 485, 575 485, 585 460" stroke="#1D1E20" stroke-width="12" stroke-linecap="round" fill="none"/>
      <rect x="695" y="345" width="220" height="200" rx="30" fill="#F4F5F7"/>
      <rect x="795" y="450" width="22" height="70" rx="6" fill="#795548" stroke="#1D1E20" stroke-width="12"/>
      <path d="M 805 385 C 770 385, 755 410, 760 435 C 745 445, 750 470, 770 475 C 780 485, 830 485, 840 475 C 860 470, 865 445, 850 435 C 855 410, 840 385, 805 385 Z" fill="#439819" stroke="#1D1E20" stroke-width="14" stroke-linejoin="round"/>
    </g>
    <!-- Wordmark -->
    <text x="50" y="800" font-family="Arial, Helvetica, sans-serif" font-weight="900" font-size="160" letter-spacing="-2">
      <tspan fill="#1D1E20">Picto</tspan><tspan fill="#5A9F1E">Habla</tspan>
    </text>
    <!-- Smile Underline -->
    <path d="M 220 890 C 420 830, 600 830, 800 890 C 600 850, 420 850, 220 890 Z" fill="#5EA521"/>
  </g>
</svg>''')

    print(f"[OK] SVGs saved to {symbol_svg_path} and {logo_svg_path}")

def main():
    print("=== Generating Official Brand Assets for PictoHabla ===")
    master = clean_master_image()
    create_google_play_icon(master)
    symbol_trans = create_symbol_and_logo_assets(master)
    generate_android_launcher_icons(symbol_trans)
    generate_svg_assets()
    print("=== All Brand Assets Generated Successfully ===")

if __name__ == "__main__":
    main()
