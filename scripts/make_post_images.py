"""Compose the DEV post images from emulator screenshots.

Run from the repo root:  python scripts/make_post_images.py
Outputs into post_images/ using the DESIGN.md tokens (paper, ink, hairline, pine).
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "post_images"
FONT_PATH = ROOT / "app/src/main/res/font/schibsted_grotesk.ttf"

PAPER = "#F6F8F4"
INK = "#161A17"
INK_SOFT = "#5E6660"
RULE = "#E0E4DC"
PINE = "#0E4F34"
PINE_WASH = "#EAF0EB"
PANEL = "#EDF0E9"

# Status bar and gesture bar are cropped off every device shot.
APP_TOP = 100
APP_BOTTOM = 2300


def font(size: int, weight: str = "Regular") -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONT_PATH), size)
    f.set_variation_by_name(weight)
    return f


def tracked(draw: ImageDraw.ImageDraw, xy, text, f, fill, spacing):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=f, fill=fill)
        x += f.getlength(ch) + spacing


def wrap(draw: ImageDraw.ImageDraw, text, f, max_width):
    lines, current = [], ""
    for word in text.split():
        candidate = f"{current} {word}".strip()
        if draw.textlength(candidate, font=f) <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_paragraph(draw, xy, text, f, fill, max_width, leading, tracking=0):
    x, y = xy
    for line in wrap(draw, text, f, max_width):
        if tracking:
            tracked(draw, (x, y), line, f, fill, tracking)
        else:
            draw.text((x, y), line, font=f, fill=fill)
        y += leading
    return y


def device(path: Path, box=(0, APP_TOP, 1080, APP_BOTTOM), width=400) -> Image.Image:
    im = Image.open(path).convert("RGB").crop(box)
    height = round(im.height * width / im.width)
    return im.resize((width, height), Image.LANCZOS)


def aligned_device(path: Path, top: int, width=400) -> Image.Image:
    """Crop at the app header, then pad the bottom with paper so every phone
    in a strip has the same height and aligned headers."""
    im = Image.open(path).convert("RGB").crop((0, top, 1080, APP_BOTTOM))
    target = APP_BOTTOM - APP_TOP
    if im.height < target:
        padded = Image.new("RGB", (1080, target), PAPER)
        padded.paste(im, (0, 0))
        im = padded
    height = round(im.height * width / im.width)
    return im.resize((width, height), Image.LANCZOS)


def rule(draw, x1, y1, x2, y2, fill=RULE, width=1):
    draw.line((x1, y1, x2, y2), fill=fill, width=width)


def cover():
    canvas = Image.new("RGB", (1200, 630), PAPER)
    draw = ImageDraw.Draw(canvas)

    # Editorial split: catalog column on the right.
    draw.rectangle((660, 0, 1200, 630), fill=PANEL)
    rule(draw, 660, 0, 660, 630, RULE)

    phone = device(ROOT / "shot_catalog_top.png", box=(0, APP_TOP, 1080, 1780), width=440)
    canvas.paste(phone, (700, 34))

    draw.text((64, 48), "SIFT", font=font(84, "Bold"), fill=INK)
    rule(draw, 64, 164, 600, 164, INK)
    rule(draw, 64, 167, 600, 167, INK)

    y = 200
    for line in ("Share it.", "Sleep.", "It's filed."):
        draw.text((64, y), line, font=font(62, "SemiBold"), fill=INK)
        y += 68

    draw_paragraph(
        draw,
        (64, 432),
        "An on-device Gemma files every link, code and address you share. "
        "Overnight, while the phone charges. No internet permission.",
        font(19),
        INK_SOFT,
        540,
        27,
    )
    tracked(draw, (64, 582), "DEV × HACKTOBERFEST · WEEK 3", font(13, "SemiBold"), PINE, 1.6)

    canvas.save(OUT / "0-cover.png")


def setup():
    canvas = Image.new("RGB", (1400, 820), PAPER)
    draw = ImageDraw.Draw(canvas)

    draw.rectangle((780, 0, 1400, 820), fill=PANEL)
    rule(draw, 780, 0, 780, 820, RULE)

    card = Image.open(ROOT / "shot_setup_prompt.png").convert("RGB").crop((60, 590, 1020, 1899))
    height = 760
    card = card.resize((round(card.width * height / card.height), height), Image.LANCZOS)
    canvas.paste(card, (810 + (560 - card.width) // 2, 30))

    tracked(draw, (72, 84), "SETUP · ONCE", font(13, "SemiBold"), PINE, 1.6)
    y = draw_paragraph(
        draw,
        (72, 118),
        "One file, then it works while you sleep.",
        font(42, "SemiBold"),
        INK,
        640,
        50,
    )
    y += 24
    for item in (
        "Gemma 4 E2B, 2.6 GB, downloaded once from Google's release.",
        "Handed over with the system file picker — no adb, no root.",
        "No internet permission. The app cannot upload anything.",
    ):
        draw.text((72, y), "—", font=font(18, "Medium"), fill=PINE)
        y = draw_paragraph(draw, (102, y), item, font(18), INK_SOFT, 610, 27) + 14

    rule(draw, 72, 700, 660, 700)
    draw_paragraph(
        draw,
        (72, 722),
        "The notification is a receipt, not a requirement. Filing runs either way.",
        font(15),
        INK_SOFT,
        600,
        22,
    )

    canvas.save(OUT / "1-setup.png")


def flow():
    width, height = 1700, 1160
    canvas = Image.new("RGB", (width, height), PAPER)
    draw = ImageDraw.Draw(canvas)

    draw.text((90, 56), "Two taps in. Zero decisions later.", font=font(40, "SemiBold"), fill=INK)
    draw.text(
        (90, 112),
        "A real run on the emulator: seven snippets shared in, one tap on FILE NOW.",
        font=font(18),
        fill=INK_SOFT,
    )
    rule(draw, 90, 154, 1610, 154)

    steps = (
        ("01 · KEEP", "Share to Sift. Two taps, zero decisions.", "shot_demo_02_toast.png", APP_TOP),
        ("02 · FILE", "It sifts while you sleep. FILE NOW for this run.", "shot_demo_04_sifting.png", APP_TOP),
        # This shot was taken after the insets fix, so its header sits lower.
        ("03 · READ", "Morning: drawers, counts, model titles, your text.", "shot_catalog_top.png", 178),
    )
    x = 90
    for label, caption, shot, top in steps:
        tracked(draw, (x, 182), label, font(14, "SemiBold"), PINE, 1.6)
        canvas.paste(aligned_device(ROOT / shot, top, width=400), (x, 214))
        draw_paragraph(draw, (x, 1064), caption, font(16), INK_SOFT, 400, 23)
        x += 560
    canvas.save(OUT / "2-flow.png")


def inbox():
    canvas = Image.new("RGB", (1200, 590), PAPER)
    draw = ImageDraw.Draw(canvas)

    draw.rectangle((660, 0, 1200, 590), fill=PANEL)
    rule(draw, 660, 0, 660, 590, RULE)

    shot = device(ROOT / "shot_catalog_bottom.png", box=(0, 1090, 1080, 2300), width=460)
    canvas.paste(shot, (700, 30))

    tracked(draw, (64, 90), "FAIL-SAFE", font(13, "SemiBold"), PINE, 1.6)
    y = draw_paragraph(
        draw,
        (64, 124),
        "No obvious drawer? INBOX.",
        font(40, "SemiBold"),
        INK,
        540,
        48,
    )
    y = draw_paragraph(
        draw,
        (64, y + 26),
        "The seventh snippet — \u201cSend me the file whenever you get a chance, "
        "no rush.\u201d — had no clear home. It lands in INBOX with a title and "
        "the text untouched. Nothing is thrown away for being ambiguous.",
        font(17),
        INK_SOFT,
        540,
        26,
    )
    y += 20
    tracked(draw, (64, y), "ONE SNIPPET · ONE CARD · NOTHING DROPPED", font(12, "SemiBold"), PINE, 1.4)

    canvas.save(OUT / "3-inbox.png")


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    cover()
    setup()
    flow()
    inbox()
    for p in sorted(OUT.glob("[0-9]-*.png")):
        im = Image.open(p)
        print(f"{p.name}: {im.width}x{im.height} {p.stat().st_size // 1024} KB")
