"""Subset the bundled Japanese fonts down to a practical charset.
Source fonts (SIL OPEN FONT LICENSE Version 1.1 licenses in this directory):
  https://github.com/google/fonts/tree/main/ofl/notosansjp     NotoSansJP[wght].ttf
  https://github.com/google/fonts/tree/main/ofl/notoserifjp    NotoSerifJP[wght].ttf
  https://github.com/google/fonts/tree/main/ofl/zenmarugothic  ZenMaruGothic-{Light,Regular,Medium,Bold}.ttf

  fonts were chosen for japanese learning purposes :)
"""

import argparse
import os
import sys

from fontTools.subset import Options, Subsetter, load_font, save_font
from fontTools.ttLib import TTFont

FILES = {
    "NotoSansJP.ttf": "noto_sans_jp.ttf",
    "NotoSerifJP.ttf": "noto_serif_jp.ttf",
    "ZenMaruGothic-Light.ttf": "zen_maru_gothic_light.ttf",
    "ZenMaruGothic-Regular.ttf": "zen_maru_gothic_regular.ttf",
    "ZenMaruGothic-Medium.ttf": "zen_maru_gothic_medium.ttf",
    "ZenMaruGothic-Bold.ttf": "zen_maru_gothic_bold.ttf",
}

SAMPLE_TEXT = "日本語にほんごカタカナ漢字、。「」ー〜…鬱薔薇ōūāアヴ①×÷"


def build_charset() -> set[int]:
    cps: set[int] = set()
    cps.update(range(0x20, 0x7F))
    cps.update(range(0xA0, 0x100))
    cps.update(range(0x100, 0x180))
    cps.update(range(0x2000, 0x2070))  
    cps.update(range(0x2190, 0x2194))  
    cps.add(0x20AC)
    cps.update(range(0x31F0, 0x3200))
    for cp in range(0x80, 0x10000):
        if 0xD800 <= cp <= 0xDFFF:
            continue
        try:
            chr(cp).encode("cp932")
        except UnicodeEncodeError:
            continue
        cps.add(cp)
    return cps


def subset_file(src: str, dst: str, unicodes: set[int]) -> None:
    options = Options()
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.name_legacy = True
    options.notdef_outline = True

    font = load_font(src, options)
    subsetter = Subsetter(options)
    subsetter.populate(unicodes=unicodes)
    subsetter.subset(font)
    save_font(font, dst, options)

    before = os.path.getsize(src)
    after = os.path.getsize(dst)
    checked = TTFont(dst, lazy=True)
    cmap = checked.getBestCmap()
    axes = [a.axisTag for a in checked["fvar"].axes] if "fvar" in checked else []
    missing = [c for c in SAMPLE_TEXT if ord(c) not in cmap]
    print(
        f"{os.path.basename(src):28} {before / 1e6:5.1f} MB -> {after / 1e6:4.1f} MB, "
        f"{len(cmap)} chars, axes={axes or 'static'}"
        + (f", MISSING: {missing}" if missing else "")
    )
    checked.close()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--src", required=True, help="directory with original font files")
    parser.add_argument("--out", required=True, help="composeResources/font output directory")
    args = parser.parse_args()

    unicodes = build_charset()
    print(f"charset: {len(unicodes)} codepoints")
    os.makedirs(args.out, exist_ok=True)
    for src_name, out_name in FILES.items():
        subset_file(os.path.join(args.src, src_name), os.path.join(args.out, out_name), unicodes)


if __name__ == "__main__":
    main()
