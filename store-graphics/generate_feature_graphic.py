"""
Generates the 1024x500 Google Play Store feature graphic.

Reuses the existing app icon artwork (app-icon/play-store-icon-512.png)
rather than commissioning new illustration — same warm color palette,
extended into a wide banner with the app name/tagline alongside it.

Run: python store-graphics/generate_feature_graphic.py
Output: store-graphics/feature-graphic.png (1024x500, 24-bit PNG, no alpha)
"""

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

WIDTH, HEIGHT = 1024, 500

# Palette sampled directly from app-icon/play-store-icon-512.png via color
# quantization, so the banner reads as the same brand rather than a
# mismatched reproduction.
CREAM = (255, 248, 236)
SKY_LIGHT = (134, 212, 229)
SKY_DEEP = (84, 142, 169)
WOOD_LIGHT = (239, 187, 95)
WOOD_MID = (217, 146, 75)
BROWN_DARK = (115, 78, 70)
SUN_YELLOW = (255, 244, 155)

FONT_TITLE = "C:/Windows/Fonts/comicbd.ttf"
FONT_SUBTITLE = "C:/Windows/Fonts/comici.ttf"


def horizontal_gradient(width, height, left_color, right_color):
    left = np.array(left_color, dtype=np.float32)
    right = np.array(right_color, dtype=np.float32)
    t = np.linspace(0.0, 1.0, width, dtype=np.float32).reshape(1, width, 1)
    row = left.reshape(1, 1, 3) * (1 - t) + right.reshape(1, 1, 3) * t
    arr = np.repeat(row, height, axis=0).astype(np.uint8)
    return Image.fromarray(arr, mode="RGB")


def main():
    # Background: cream on the text side fading into sky blue behind the
    # ark medallion, echoing the icon's own sky-into-water composition.
    bg = horizontal_gradient(WIDTH, HEIGHT, CREAM, SKY_LIGHT)

    # Soft sun glow behind the title, matching the icon's sun motif.
    glow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse([-180, -160, 420, 660], fill=(*SUN_YELLOW, 110))
    glow = glow.filter(ImageFilter.GaussianBlur(70))
    bg = Image.alpha_composite(bg.convert("RGBA"), glow).convert("RGB")

    # Ark medallion, reused from the real app icon (not redrawn).
    icon = Image.open("app-icon/play-store-icon-512.png").convert("RGBA")
    medallion_size = 420
    icon = icon.resize((medallion_size, medallion_size), Image.LANCZOS)

    # Soft drop shadow behind the medallion for depth.
    shadow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    shadow_mask = Image.new("L", (medallion_size, medallion_size), 0)
    ImageDraw.Draw(shadow_mask).rounded_rectangle(
        [0, 0, medallion_size, medallion_size], radius=90, fill=110,
    )
    medallion_x = WIDTH - medallion_size - 46
    medallion_y = (HEIGHT - medallion_size) // 2
    shadow.paste((80, 50, 30, 255), (medallion_x + 10, medallion_y + 14), shadow_mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))
    bg = Image.alpha_composite(bg.convert("RGBA"), shadow).convert("RGBA")

    bg.paste(icon, (medallion_x, medallion_y), icon)
    bg = bg.convert("RGB")

    draw = ImageDraw.Draw(bg)

    title_font = ImageFont.truetype(FONT_TITLE, 76)
    subtitle_font = ImageFont.truetype(FONT_SUBTITLE, 42)

    text_left = 64
    text_right_bound = medallion_x - 30

    def wrapped_lines(text, font, max_width):
        words = text.split(" ")
        lines, current = [], ""
        for word in words:
            candidate = f"{current} {word}".strip()
            if draw.textlength(candidate, font=font) <= max_width:
                current = candidate
            else:
                lines.append(current)
                current = word
        if current:
            lines.append(current)
        return lines

    title_lines = wrapped_lines("Bible Adventures", title_font, text_right_bound - text_left)
    line_heights = [title_font.getbbox(line)[3] - title_font.getbbox(line)[1] for line in title_lines]
    line_gap = 10
    title_block_height = sum(line_heights) + line_gap * (len(title_lines) - 1)

    subtitle_text = "Walk in Faith"
    subtitle_bbox = subtitle_font.getbbox(subtitle_text)
    subtitle_height = subtitle_bbox[3] - subtitle_bbox[1]

    block_gap = 22
    total_height = title_block_height + block_gap + subtitle_height
    y = (HEIGHT - total_height) // 2

    for line, lh in zip(title_lines, line_heights):
        draw.text((text_left, y), line, font=title_font, fill=BROWN_DARK)
        y += lh + line_gap

    y += block_gap - line_gap
    draw.text((text_left, y), subtitle_text, font=subtitle_font, fill=WOOD_MID)

    bg.save("store-graphics/feature-graphic.png", "PNG")
    print("Wrote store-graphics/feature-graphic.png", bg.size, bg.mode)


if __name__ == "__main__":
    main()
