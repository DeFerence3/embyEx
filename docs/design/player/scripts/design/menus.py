"""Menu contents. Each body is used twice: in a menu anchored to the dock (desktop, tablet) and in a bottom sheet (phone)."""
from .lib import art, btn, esc, ic, kbd

LANGS = [("English", ["English", "English · SDH", "English · Forced"]), ("Dansk", ["Dansk"]), ("Deutsch", ["Deutsch", "Deutsch · SDH"]),
         ("Español", ["Español (España)", "Español (Latinoamérica)", "Español · SDH"]), ("Français", ["Français", "Français · Forced"]),
         ("Italiano", ["Italiano"]), ("Nederlands", ["Nederlands"]), ("Norsk", ["Norsk bokmål", "Norsk · SDH"]), ("Polski", ["Polski"]),
         ("Português", ["Português (Brasil)", "Português (Portugal)"]), ("Svenska", ["Svenska"]), ("Türkçe", ["Türkçe"]),
         ("日本語", ["日本語"]), ("한국어", ["한국어"]), ("中文", ["中文 (简体)", "中文 (繁體)"])]


def radio(label, checked=False, sub="", tag="", attrs="", cls="", style=""):
    two = " two" if sub else ""
    second = f"<span>{esc(sub)}</span>" if sub else ""
    badge = f'<span class="tag">{esc(tag)}</span>' if tag else ""
    st = f' style="{style}"' if style else ""
    return (f'<button class="menu-item{two}{cls}" role="menuitemradio" aria-checked="{str(checked).lower()}" {attrs}><span class="radio"></span>'
            f'<span class="text"><span{st}>{esc(label)}</span>{second}</span>{badge}</button>')


def nav(icon, label, val="", attrs="", key=None, chevron=True):
    value = f'<span class="val">{esc(val)}</span>' if val else ""
    cap = kbd(key) if key else ""
    arrow = ic("chevron_right") if chevron else ""
    return f'<button class="menu-item" role="menuitem" {attrs}>{ic(icon)}<span class="text"><span>{esc(label)}</span></span>{value}{cap}{arrow}</button>'


def header(title, key=None, back=""):
    lead = btn("arrow_back", "Back", "sm", attrs=back) if back else ""
    cap = kbd(key) if key else ""
    return f'<div class="menu-label">{lead}<span>{esc(title)}</span>{cap}</div>'


def menu(title, body, key=None, back="", end=False, scroll=False):
    """Desktop and tablet: anchored above the dock pill that opened it."""
    style = ' style="overflow:auto"' if scroll else ""
    return f'<div class="menu pl-menu up{" end" if end else ""}" role="menu" aria-label="{esc(title)}"{style}>{header(title, key, back)}{body}</div>'


def sheet(title, body, back=""):
    """Phone: the same rows in a bottom sheet."""
    return (f'<div class="overlay sheet-overlay" data-overlay data-static><div class="sheet pl-sheet" role="dialog" aria-modal="true" aria-label="{esc(title)}">'
            f'{header(title, back=back)}{body}</div></div>')


def side_sheet(title, body):
    """Phone landscape: the same rows in a sheet at the right edge."""
    return (f'<div class="overlay side" data-overlay data-static><div class="pl-sidesheet" role="dialog" aria-modal="true" aria-label="{esc(title)}">'
            f'{header(title)}{body}</div></div>')


# ---------------------------------------------------------------- bodies
def subtitles(variant="default", style_attrs=""):
    style_row = "<hr>" + nav("tune", "Subtitle style", attrs=style_attrs)
    if variant == "none":
        return radio("Off", True) + '<p class="menu-note">This video has no subtitles.</p>' + style_row
    if variant == "many":
        rows = radio("Off")
        for lang, tracks in LANGS:
            rows += f'<div class="menu-group">{lang}</div>'
            for t in tracks:
                name, _, kind = t.partition(" · ")
                rows += radio(name, t == "English", tag=("SDH" if kind == "SDH" else kind))
        return rows + style_row
    off = variant == "off"
    return (radio("Off", off) + radio("English", not off, cls=" is-hover" if not off else "")
            + radio("English", sub="For the deaf and hard of hearing", tag="SDH") + radio("Deutsch") + style_row)


def audio():
    return (radio("English", True, sub="5.1 surround · E-AC3", tag="Default") + radio("English", sub="Stereo · AAC")
            + radio("Deutsch", sub="5.1 surround · AC3") + radio("English", sub="Stereo · AAC", tag="Commentary"))


def speed():
    rows = "".join(radio(f"{v}×" + (" · Normal" if v == "1" else ""), v == "1") for v in ["0.5", "0.75", "1", "1.25", "1.5", "1.75", "2"])
    return rows + f'<p class="menu-note">Tip: {kbd("<")} and {kbd(">")} change speed while you watch.</p>'


def more(android=False, narrow=False, routes=None):
    r = routes or {}
    rows = ""
    if narrow:
        rows += nav("speed", "Playback speed", "1×") + nav("picture_in_picture_alt", "Picture in picture", key="I", chevron=False)
    rows += nav("aspect_ratio", "Aspect ratio", "Fit", attrs=r.get("aspect", "")) + nav("tune", "Subtitle style", attrs=r.get("style", ""))
    if android:
        rows += "<hr>" + nav("open_in_new", "Open in mpvEx", chevron=False)
    rows += "<hr>" + nav("keyboard", "Keyboard shortcuts", key="?", attrs=r.get("keys", ""), chevron=False)
    return rows


def aspect():
    return (radio("Fit", True, sub="Whole picture, bars if needed") + radio("Crop", sub="Fills the screen, edges cut off")
            + radio("Stretch", sub="Fills the screen, picture distorted"))


def seg(options, selected, label):
    buttons = "".join(f'<button aria-pressed="{str(o == selected).lower()}">{o}</button>' for o in options)
    return f'<div class="ss-field"><span>{label}</span><div class="segmented fill" role="group" aria-label="{label}">{buttons}</div></div>'


def style(face="nunito", size="Medium", background="Box", reset_attrs=""):
    sub_cls = " ".join(filter(None, [{"nunito": "", "atkinson": "atkinson", "barlow": "barlow"}[face],
                                     {"Box": "", "Outline": "outline", "Shadow": "shadow"}[background], "lg" if size in ("Large", "Larger") else ""]))
    preview = f'<div class="ss-preview" aria-hidden="true">{art()}<p class="pl-sub {sub_cls}"><span>Is that the coast?</span></p></div>'
    faces = "".join(radio(name, face == key, tag=("Default" if key == "nunito" else ""), cls=f" ss-row-{key}", style="") .replace(
        f"<span>{name}</span>", f'<span class="ss-face {key}">{name}</span>') for key, name in
        [("nunito", "Nunito"), ("atkinson", "Atkinson Hyperlegible"), ("barlow", "Barlow Semi Condensed")])
    return (preview + '<div class="menu-group">Typeface</div>' + faces + seg(["Small", "Medium", "Large", "Larger"], size, "Size")
            + seg(["Box", "Outline", "Shadow"], background, "Background") + "<hr>" + nav("restart_alt", "Reset to default", attrs=reset_attrs, chevron=False))
