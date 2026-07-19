#!/usr/bin/env python3
"""Render the Valet visual kit as deterministic, pixel-perfect PNG assets."""

from __future__ import annotations

from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/valet"
SKIN_SOURCE = ROOT / "docs/assets/visual-pilot/artisan-skin-source.svg"


def draw_artisan_concept_views() -> tuple[Image.Image, Image.Image]:
    """Rebuild the approved SuperMaker front/back concept at Minecraft resolution."""
    transparent = (0, 0, 0, 0)
    front = Image.new("RGBA", (16, 32), transparent)
    back = Image.new("RGBA", (16, 32), transparent)
    f = ImageDraw.Draw(front)
    b = ImageDraw.Draw(back)

    hair_dark = "#4e1f12"
    hair = "#813213"
    hair_mid = "#aa4619"
    hair_light = "#c2531e"
    skin_shadow = "#c97e5b"
    skin = "#e59b73"
    skin_light = "#f0ad82"
    eye = "#403128"
    eye_light = "#ece2c7"
    mouth = "#b76f55"
    coat_darkest = "#19392f"
    coat_dark = "#23473c"
    coat = "#435d4f"
    coat_light = "#4a6858"
    cream_shadow = "#c6bc9f"
    cream = "#e0d6b6"
    cream_light = "#ece2c7"
    leather_dark = "#705033"
    leather = "#9a7e51"
    leather_light = "#ac8d5e"
    leather_highlight = "#be9e67"
    strap_dark = "#553a27"
    strap = "#7e593c"
    trousers_dark = "#513727"
    trousers = "#724f37"
    trousers_light = "#855b3c"
    boot_dark = "#1f1f1f"
    boot = "#323232"
    boot_light = "#595959"
    gold = "#ae882d"
    gold_light = "#deb03a"
    emerald = "#148042"
    emerald_light = "#2eaa5c"

    # Front head: pale face and the approved asymmetric layered auburn fringe.
    f.rectangle((4, 0, 11, 7), fill=skin)
    f.rectangle((4, 0, 11, 1), fill=hair_dark)
    f.rectangle((4, 2, 9, 2), fill=hair)
    f.rectangle((4, 3, 6, 4), fill=hair)
    f.rectangle((5, 2, 8, 3), fill=hair_mid)
    f.rectangle((5, 3, 6, 4), fill=hair_light)
    f.rectangle((10, 2, 11, 4), fill=hair_dark)
    f.point((9, 3), fill=hair_mid)
    f.point((4, 5), fill=skin_shadow)
    f.point((11, 5), fill=skin_shadow)
    f.rectangle((6, 4, 7, 4), fill=eye_light)
    f.point((7, 4), fill=eye)
    f.rectangle((9, 4, 10, 4), fill=eye_light)
    f.point((9, 4), fill=eye)
    f.rectangle((7, 6, 8, 6), fill=mouth)

    # Front coat, cream shirt, apron, pocket and small emerald/brass badge.
    f.rectangle((4, 8, 11, 19), fill=coat)
    f.rectangle((4, 8, 4, 19), fill=coat_dark)
    f.rectangle((11, 8, 11, 19), fill=coat_darkest)
    f.rectangle((5, 9, 5, 12), fill=coat_light)
    f.rectangle((10, 9, 10, 12), fill=coat_dark)
    f.rectangle((7, 8, 8, 9), fill=cream_light)
    f.point((6, 8), fill=cream_shadow)
    f.point((9, 8), fill=cream_shadow)
    f.rectangle((5, 10, 10, 19), fill=leather)
    f.rectangle((5, 10, 5, 19), fill=leather_light)
    f.rectangle((10, 10, 10, 19), fill=leather_dark)
    f.rectangle((6, 12, 9, 15), fill=leather_dark)
    f.rectangle((7, 13, 9, 14), fill="#825f40")
    f.line((6, 12, 9, 12), fill=leather_highlight)
    f.rectangle((6, 17, 9, 19), fill=leather_dark)
    f.line((7, 17, 9, 17), fill=leather_light)
    f.point((5, 9), fill=gold_light)
    f.point((6, 9), fill=emerald_light)
    f.point((6, 10), fill=emerald)

    # Front arms: dark green shoulders, rolled cream sleeves and peach hands.
    for x0, x1, mirrored in ((0, 3, False), (12, 15, True)):
        f.rectangle((x0, 8, x1, 12), fill=coat)
        shadow_x = x1 if not mirrored else x0
        light_x = x0 if not mirrored else x1
        f.rectangle((shadow_x, 9, shadow_x, 12), fill=coat_darkest)
        f.rectangle((light_x, 8, light_x, 10), fill=coat_light)
        f.rectangle((x0, 13, x1, 16), fill=cream)
        f.rectangle((x0, 13, x1, 13), fill=cream_light)
        f.rectangle((shadow_x, 14, shadow_x, 16), fill=cream_shadow)
        f.rectangle((x0, 17, x1, 19), fill=skin)
        f.rectangle((x0, 17, x1, 17), fill=skin_light)
        f.rectangle((shadow_x, 18, shadow_x, 19), fill=skin_shadow)

    # The apron continues over the thighs, then brown trousers and charcoal boots.
    f.rectangle((4, 20, 11, 24), fill=leather)
    f.rectangle((4, 20, 5, 24), fill=leather_light)
    f.rectangle((10, 20, 11, 24), fill=leather_dark)
    f.rectangle((6, 20, 9, 22), fill=leather_light)
    f.rectangle((7, 20, 9, 21), fill=leather_highlight)
    f.rectangle((4, 25, 11, 27), fill=trousers)
    f.rectangle((4, 25, 5, 27), fill=trousers_light)
    f.rectangle((10, 25, 11, 27), fill=trousers_dark)
    f.rectangle((5, 25, 7, 26), fill=trousers_dark)
    f.rectangle((8, 26, 10, 27), fill=trousers_light)
    f.rectangle((4, 28, 11, 31), fill=boot)
    f.rectangle((4, 28, 11, 28), fill=boot_light)
    f.rectangle((4, 31, 11, 31), fill=boot_dark)
    f.rectangle((5, 29, 6, 29), fill="#464646")

    # Back head: layered auburn mass with the orange highlights from the concept.
    b.rectangle((4, 0, 11, 7), fill=hair)
    b.rectangle((4, 0, 11, 1), fill=hair_dark)
    b.rectangle((4, 2, 5, 5), fill=hair_mid)
    b.rectangle((6, 1, 9, 2), fill=hair_mid)
    b.rectangle((7, 2, 10, 4), fill=hair_mid)
    b.rectangle((8, 3, 10, 5), fill=hair_light)
    b.rectangle((4, 6, 6, 7), fill=hair_dark)
    b.rectangle((7, 5, 8, 7), fill=hair_mid)
    b.rectangle((9, 5, 11, 7), fill=hair_dark)
    b.point((5, 3), fill=hair_light)
    b.point((11, 2), fill=hair_dark)

    # Back coat and the prominent crossed leather harness, waist belt and buckle.
    b.rectangle((4, 8, 11, 19), fill=coat)
    b.rectangle((4, 8, 4, 19), fill=coat_dark)
    b.rectangle((11, 8, 11, 19), fill=coat_darkest)
    b.rectangle((5, 9, 5, 13), fill=coat_light)
    b.rectangle((10, 9, 10, 13), fill=coat_dark)
    b.line((5, 9, 10, 15), fill=strap, width=2)
    b.line((10, 9, 5, 15), fill=strap, width=2)
    b.point((5, 9), fill=leather_light)
    b.point((10, 9), fill=leather_dark)
    b.rectangle((4, 16, 11, 17), fill=strap_dark)
    b.rectangle((5, 16, 10, 16), fill=strap)
    b.rectangle((7, 15, 8, 18), fill=strap)
    b.rectangle((7, 16, 8, 17), fill=gold)
    b.point((7, 16), fill=gold_light)

    # Back arms mirror the front materials with shoulder and cuff shading.
    for x0, x1, mirrored in ((0, 3, False), (12, 15, True)):
        b.rectangle((x0, 8, x1, 12), fill=coat)
        shadow_x = x1 if not mirrored else x0
        light_x = x0 if not mirrored else x1
        b.rectangle((shadow_x, 9, shadow_x, 12), fill=coat_darkest)
        b.rectangle((light_x, 8, light_x, 10), fill=coat_light)
        b.rectangle((x0, 13, x1, 16), fill=cream)
        b.rectangle((x0, 13, x1, 13), fill=cream_light)
        b.rectangle((shadow_x, 14, shadow_x, 16), fill=cream_shadow)
        b.rectangle((x0, 17, x1, 19), fill=skin)
        b.rectangle((x0, 17, x1, 17), fill=skin_light)
        b.rectangle((shadow_x, 18, shadow_x, 19), fill=skin_shadow)

    # Back trousers reproduce the large seam/patch shape, followed by shaded boots.
    b.rectangle((4, 20, 11, 27), fill=trousers)
    b.rectangle((4, 20, 5, 27), fill=trousers_light)
    b.rectangle((10, 20, 11, 27), fill=trousers_dark)
    b.rectangle((7, 20, 8, 27), fill=trousers_dark)
    b.rectangle((5, 24, 10, 25), fill=trousers_dark)
    b.rectangle((5, 20, 6, 22), fill="#7b553a")
    b.rectangle((9, 25, 10, 27), fill="#62432f")
    b.rectangle((4, 28, 11, 31), fill=boot)
    b.rectangle((4, 28, 11, 28), fill=boot_light)
    b.rectangle((4, 31, 11, 31), fill=boot_dark)
    b.rectangle((9, 29, 10, 29), fill="#464646")
    return front, back


def apply_artisan_concept_faces(skin: Image.Image) -> None:
    """Map approved concept views to the correct wide-Java UV faces."""
    front, back = draw_artisan_concept_views()

    # Remove the former coarse front/back outer layers before applying the model.
    clear = Image.new("RGBA", skin.size, (0, 0, 0, 0))
    overlay_faces = (
        (40, 8, 48, 16), (56, 8, 64, 16),
        (20, 36, 28, 48), (32, 36, 40, 48),
        (44, 36, 48, 48), (52, 36, 56, 48),
        (52, 52, 56, 64), (60, 52, 64, 64),
        (4, 36, 8, 48), (12, 36, 16, 48),
        (4, 52, 8, 64), (12, 52, 16, 64),
    )
    for box in overlay_faces:
        skin.paste(clear.crop(box), box)

    mappings = (
        (front, (4, 0, 12, 8), (8, 8)),
        (front, (4, 8, 12, 20), (20, 20)),
        (front, (0, 8, 4, 20), (44, 20)),
        (front, (12, 8, 16, 20), (36, 52)),
        (front, (4, 20, 8, 32), (4, 20)),
        (front, (8, 20, 12, 32), (20, 52)),
        (back, (4, 0, 12, 8), (24, 8)),
        (back, (4, 8, 12, 20), (32, 20)),
        (back, (0, 8, 4, 20), (44, 52)),
        (back, (12, 8, 16, 20), (52, 20)),
        (back, (4, 20, 8, 32), (28, 52)),
        (back, (8, 20, 12, 32), (12, 20)),
    )
    for source, box, destination in mappings:
        skin.paste(source.crop(box), destination)

    # Real second layers give the hair, apron, harness and cuffs visible volume in game.
    front_layer = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    back_layer = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    fl = ImageDraw.Draw(front_layer)
    bl = ImageDraw.Draw(back_layer)

    # Hair tufts retain the asymmetric silhouette without covering the face.
    fl.rectangle((4, 0, 11, 1), fill="#4e1f12")
    fl.rectangle((4, 2, 8, 2), fill="#aa4619")
    fl.rectangle((4, 3, 5, 4), fill="#813213")
    fl.rectangle((10, 2, 11, 4), fill="#4e1f12")
    fl.point((6, 3), fill="#c2531e")
    bl.rectangle((4, 0, 11, 1), fill="#4e1f12")
    bl.rectangle((4, 2, 5, 5), fill="#aa4619")
    bl.rectangle((7, 2, 10, 4), fill="#aa4619")
    bl.rectangle((8, 3, 10, 4), fill="#c2531e")
    bl.rectangle((9, 5, 11, 7), fill="#4e1f12")

    # Raised leather apron, lower flap and rolled cuffs.
    fl.rectangle((5, 10, 10, 19), fill="#9a7e51")
    fl.rectangle((5, 10, 5, 19), fill="#ac8d5e")
    fl.rectangle((10, 10, 10, 19), fill="#705033")
    fl.rectangle((6, 12, 9, 15), fill="#705033")
    fl.line((6, 12, 9, 12), fill="#be9e67")
    fl.rectangle((6, 17, 9, 19), fill="#705033")
    fl.point((5, 9), fill="#deb03a")
    fl.point((6, 9), fill="#2eaa5c")
    fl.rectangle((4, 20, 11, 24), fill="#9a7e51")
    fl.rectangle((4, 20, 5, 24), fill="#ac8d5e")
    fl.rectangle((10, 20, 11, 24), fill="#705033")
    fl.rectangle((6, 20, 9, 22), fill="#ac8d5e")
    for x0, x1 in ((0, 3), (12, 15)):
        fl.rectangle((x0, 13, x1, 13), fill="#ece2c7")
        bl.rectangle((x0, 13, x1, 13), fill="#ece2c7")

    # Raised crossed harness and belt on the back.
    bl.line((5, 9, 10, 15), fill="#7e593c", width=2)
    bl.line((10, 9, 5, 15), fill="#7e593c", width=2)
    bl.rectangle((4, 16, 11, 17), fill="#553a27")
    bl.rectangle((5, 16, 10, 16), fill="#7e593c")
    bl.rectangle((7, 15, 8, 18), fill="#7e593c")
    bl.rectangle((7, 16, 8, 17), fill="#ae882d")
    bl.point((7, 16), fill="#deb03a")

    layer_mappings = (
        (front_layer, (4, 0, 12, 8), (40, 8)),
        (front_layer, (4, 8, 12, 20), (20, 36)),
        (front_layer, (0, 8, 4, 20), (44, 36)),
        (front_layer, (12, 8, 16, 20), (52, 52)),
        (front_layer, (4, 20, 8, 32), (4, 36)),
        (front_layer, (8, 20, 12, 32), (4, 52)),
        (back_layer, (4, 0, 12, 8), (56, 8)),
        (back_layer, (4, 8, 12, 20), (32, 36)),
        (back_layer, (0, 8, 4, 20), (60, 52)),
        (back_layer, (12, 8, 16, 20), (52, 36)),
        (back_layer, (4, 20, 8, 32), (12, 52)),
        (back_layer, (8, 20, 12, 32), (12, 36)),
    )
    for source, box, destination in layer_mappings:
        skin.paste(source.crop(box), destination)


def render_rect_svg(source: Path, destination: Path) -> None:
    root = ET.parse(source).getroot()
    width = int(root.attrib["width"])
    height = int(root.attrib["height"])
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    def visit(node: ET.Element, inherited_fill: str | None = None) -> None:
        fill = node.attrib.get("fill", inherited_fill)
        if node.tag.endswith("rect"):
            if not fill or fill == "none":
                return
            x = int(node.attrib["x"])
            y = int(node.attrib["y"])
            w = int(node.attrib["width"])
            h = int(node.attrib["height"])
            draw.rectangle((x, y, x + w - 1, y + h - 1), fill=fill)
        for child in node:
            visit(child, fill)

    visit(root)
    apply_artisan_concept_faces(image)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination)


def draw_badge() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    dark = "#1b211f"
    gold_shadow = "#765020"
    gold = "#c58b35"
    gold_light = "#f0cf6b"
    emerald_dark = "#075538"
    emerald = "#0e9a5e"
    emerald_light = "#4be08b"

    d.polygon([(3, 2), (12, 2), (14, 4), (14, 9), (12, 12), (8, 15), (4, 12), (1, 9), (1, 4)], fill=dark)
    d.polygon([(4, 3), (11, 3), (13, 4), (13, 8), (11, 11), (8, 13), (5, 11), (2, 8), (2, 4)], fill=gold_shadow)
    d.polygon([(4, 4), (11, 4), (12, 5), (12, 8), (10, 10), (8, 12), (5, 10), (3, 8), (3, 5)], fill=gold)
    d.line([(4, 4), (10, 4)], fill=gold_light, width=1)
    d.line([(3, 5), (3, 8), (5, 10)], fill=gold_light, width=1)
    d.polygon([(8, 4), (11, 6), (11, 9), (8, 12), (5, 9), (5, 6)], fill=dark)
    d.polygon([(8, 5), (10, 6), (10, 9), (8, 11), (6, 9), (6, 6)], fill=emerald_dark)
    d.polygon([(8, 5), (9, 6), (9, 9), (8, 10), (7, 9), (7, 6)], fill=emerald)
    d.point((7, 6), fill=emerald_light)
    d.point((8, 6), fill="#a7f4c7")
    return image


def draw_artisan_icon() -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    dark = "#1b211f"
    wood_dark = "#4e301d"
    wood = "#8b582d"
    iron_dark = "#4b5557"
    iron = "#aeb9b8"
    iron_light = "#e2e7df"

    d.line([(3, 13), (11, 5)], fill=dark, width=3)
    d.line([(3, 13), (11, 5)], fill=wood, width=1)
    d.line([(12, 13), (5, 6)], fill=dark, width=3)
    d.line([(12, 13), (5, 6)], fill=wood_dark, width=1)
    d.polygon([(3, 2), (8, 2), (9, 3), (8, 5), (6, 4), (3, 4), (2, 3)], fill=dark)
    d.polygon([(3, 2), (7, 2), (8, 3), (7, 4), (3, 3)], fill=iron)
    d.line([(4, 2), (7, 2)], fill=iron_light, width=1)
    d.line([(9, 2), (12, 2), (14, 4)], fill=dark, width=3)
    d.line([(9, 2), (12, 2), (14, 4)], fill=iron, width=1)
    d.point((10, 2), fill=iron_light)
    d.rectangle((7, 7, 9, 9), fill=dark)
    d.point((8, 8), fill="#31d17c")
    return image


def draw_mod_icon() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    dark = "#1b211f"
    gold_shadow = "#6d481d"
    gold = "#bd8430"
    gold_light = "#f1d273"
    emerald_dark = "#064e34"
    emerald = "#0b8d57"
    emerald_light = "#48d88a"

    d.polygon([(9, 2), (22, 2), (27, 6), (29, 12), (27, 21), (22, 27), (16, 31), (9, 27), (4, 21), (2, 12), (4, 6)], fill=dark)
    d.polygon([(9, 4), (22, 4), (25, 7), (27, 12), (25, 20), (21, 25), (16, 28), (10, 25), (6, 20), (4, 12), (6, 7)], fill=gold_shadow)
    d.polygon([(10, 5), (21, 5), (24, 8), (25, 13), (23, 19), (20, 23), (16, 26), (11, 23), (8, 19), (6, 13), (7, 8)], fill=gold)
    d.line([(9, 6), (20, 6), (23, 9)], fill=gold_light, width=2)
    d.line([(7, 10), (7, 16), (10, 21)], fill=gold_light, width=1)
    d.polygon([(16, 7), (22, 11), (22, 18), (16, 24), (10, 18), (10, 11)], fill=dark)
    d.polygon([(16, 9), (20, 12), (20, 18), (16, 22), (12, 18), (12, 12)], fill=emerald_dark)
    d.polygon([(16, 10), (19, 12), (19, 17), (16, 20), (13, 17), (13, 12)], fill=emerald)
    d.rectangle((14, 11, 15, 13), fill=emerald_light)
    d.point((16, 11), fill="#b2f5cf")
    return image.resize((128, 128), Image.Resampling.NEAREST)


def validate(path: Path, size: tuple[int, int]) -> None:
    image = Image.open(path)
    if image.mode != "RGBA" or image.size != size:
        raise ValueError(f"{path}: expected RGBA {size}, got {image.mode} {image.size}")
    alpha = image.getchannel("A").getextrema()
    if alpha != (0, 255):
        raise ValueError(f"{path}: expected transparent and opaque pixels, got alpha {alpha}")


def render_skin_view(skin: Image.Image, back: bool = False) -> Image.Image:
    view = Image.new("RGBA", (16, 32), (0, 0, 0, 0))

    def part(base: tuple[int, int, int, int], overlay: tuple[int, int, int, int], x: int, y: int) -> None:
        base_image = skin.crop(base)
        overlay_image = skin.crop(overlay)
        view.alpha_composite(base_image, (x, y))
        view.alpha_composite(overlay_image, (x, y))

    if back:
        part((24, 8, 32, 16), (56, 8, 64, 16), 4, 0)
        part((32, 20, 40, 32), (32, 36, 40, 48), 4, 8)
        part((44, 52, 48, 64), (60, 52, 64, 64), 0, 8)
        part((52, 20, 56, 32), (52, 36, 56, 48), 12, 8)
        part((28, 52, 32, 64), (12, 52, 16, 64), 4, 20)
        part((12, 20, 16, 32), (12, 36, 16, 48), 8, 20)
    else:
        part((8, 8, 16, 16), (40, 8, 48, 16), 4, 0)
        part((20, 20, 28, 32), (20, 36, 28, 48), 4, 8)
        part((44, 20, 48, 32), (44, 36, 48, 48), 0, 8)
        part((36, 52, 40, 64), (52, 52, 56, 64), 12, 8)
        part((4, 20, 8, 32), (4, 36, 8, 48), 4, 20)
        part((20, 52, 24, 64), (4, 52, 8, 64), 8, 20)
    return view


def render_preview(skin_path: Path, destination: Path) -> None:
    skin = Image.open(skin_path).convert("RGBA")
    front = render_skin_view(skin)
    back = render_skin_view(skin, back=True)
    preview = Image.new("RGBA", (400, 360), "#d7d1c4")
    preview.alpha_composite(front.resize((160, 320), Image.Resampling.NEAREST), (20, 20))
    preview.alpha_composite(back.resize((160, 320), Image.Resampling.NEAREST), (220, 20))
    destination.parent.mkdir(parents=True, exist_ok=True)
    preview.save(destination)


def main() -> None:
    outputs = {
        ASSETS / "textures/item/valet_tag.png": draw_badge(),
        ASSETS / "textures/gui/role/artisan.png": draw_artisan_icon(),
        ASSETS / "icon.png": draw_mod_icon(),
    }
    for path, image in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path)

    skin_path = ASSETS / "textures/entity/valet/artisan.png"
    render_rect_svg(SKIN_SOURCE, skin_path)
    preview_path = ROOT / "docs/assets/visual-pilot/artisan-skin-preview.png"
    render_preview(skin_path, preview_path)

    validate(ASSETS / "textures/item/valet_tag.png", (16, 16))
    validate(ASSETS / "textures/gui/role/artisan.png", (16, 16))
    validate(ASSETS / "icon.png", (128, 128))
    validate(skin_path, (64, 64))
    print("Generated Valet visual assets:")
    for path in (*outputs, skin_path):
        print(f"- {path.relative_to(ROOT)}")
    print(f"- {preview_path.relative_to(ROOT)} (preview only)")


if __name__ == "__main__":
    main()
