# Sound sources

Every OGG in this folder is built by `tools/sounds/make_sounds.py`. The sourced sounds are listed in
`tools/sounds/sources.json`, with the URL and SHA-256 of each downloaded file, its licence, and its edit. The script
downloads the file, checks the hash, cuts and filters it with ffmpeg, and encodes OGG Vorbis (libvorbis, quality 3, mono,
22050 Hz). Loops are joined by a crossfade. The procedural sounds are synthesised in the script.

Licence rule: only CC0 or public domain sources are used. No CC-BY or CC-BY-SA source is in the pack.

## Sourced sounds (recordings, edited)

| Key | Source file | Author (as listed) | Licence | Page | Edit | Length (s) | Peak (dBFS) |
|---|---|---|---|---|---|---|---|
| turret_fire | `22 Pistol.wav` | OpenGameArt "gunshots" pack (uploaders listed on the page: kurt, LarkPay) | CC0 | https://opengameart.org/content/gunshots | first shot, 0.12 s to 0.48 s, 50 Hz high-pass, 12 kHz low-pass | 0.36 | -12.7 |
| turret_reload | `assaultriflereload1_0.wav` | SpringySpringo, Joaken (OpenGameArt "Gun reload sounds") | CC0 | https://opengameart.org/content/gun-reload-sounds | 0 s to 1.5 s, 50 Hz high-pass, 10 kHz low-pass | 1.50 | -11.8 |
| laser_fire | `Digital_SFX_Set.zip` : `phaserDown1.mp3` | Kenney Vleugels (kenney.nl) | CC0 (per the OpenGameArt page; the set's readme allows personal and commercial use, credit not required) | https://opengameart.org/content/63-digital-sound-effects-lasers-phasers-space-etc | whole file, 60 Hz high-pass, 9 kHz low-pass | 0.42 | -13.2 |
| laser_charge | `Digital_SFX_Set.zip` : `phaserUp3.mp3` | Kenney Vleugels (kenney.nl) | CC0 (as above) | https://opengameart.org/content/63-digital-sound-effects-lasers-phasers-space-etc | whole file, 60 Hz high-pass, 9 kHz low-pass | 0.45 | -12.6 |
| diesel_generator | `engine_sound_1.mp3` | pauliuw | CC0 | https://opengameart.org/content/engine-sounds2 | 1.0 s to 3.5 s, loop with 0.4 s crossfade, 40 Hz to 2.5 kHz | 2.50 | -14.9 |
| generator_hum | `loop_generator_1.mp3` | YCbCr (the page also lists Tsorthan Grove) | CC0 | https://opengameart.org/content/generator-loop | 0.2 s to 3.8 s, loop with 0.6 s crossfade, 40 Hz to 1.5 kHz | 3.60 | -15.0 |
| press_stamp | `blacksmithhammer.wav` | VishwaJai | CC0 | https://opengameart.org/content/blacksmiths-hammer | whole file, 50 Hz high-pass | 0.50 | -12.0 |
| purifier | `bubbles-loop1-amp.wav` | BMacZero | CC0 | https://opengameart.org/content/bubble-sound-effects | 0 s to 4.0 s, loop with 0.5 s crossfade, 150 Hz to 6 kHz | 4.00 | -14.1 |
| induction_hiss | `qubodup-edev.flac` | qubodup | CC0 | https://opengameart.org/content/electronic-device-loop | 0 s to 1.2 s, loop with 0.3 s crossfade, 200 Hz to 6 kHz | 1.20 | -15.0 |

Raw downloads are not stored in the repository. The cache folder is `%TEMP%/corefactory-sound-sources` by default,
or the folder given with `--cache`.

## Procedural sounds (synthesised in make_sounds.py)

None of these use a recording. Each builder in the script says how its sound is made.

| Key | Duration (s) | Peak (dBFS) |
|---|---|---|
| turret_rotate | 0.60 | -13.0 |
| belt_hum | 1.00 | -14.8 |
| drill_run | 3.00 | -14.0 |
| lab_run | 2.00 | -15.0 |
| mill_grind | 1.50 | -13.0 |
| picker_clunk | 0.35 | -12.1 |
| wind_generator | 2.00 | -13.9 |
| solar_panel | 2.00 | -15.9 |
| charcoal_generator | 2.00 | -13.9 |
| petroleum_generator | 2.00 | -13.9 |
| nuclear_generator | 2.00 | -14.9 |
| ore_drill | 3.00 | -14.3 |
| petroleum_drill | 3.00 | -13.9 |
| separator | 2.00 | -14.9 |
| roaster | 2.00 | -13.7 |
| converter | 2.00 | -14.8 |
| enrichment_cascade | 2.00 | -14.7 |
| transmuter | 2.00 | -14.8 |
| spectrometer | 2.00 | -15.0 |
| particle_accelerator | 3.00 | -14.8 |
| antimatter_condenser | 2.00 | -14.8 |
| research_console | 2.00 | -14.8 |
| crude_oil_refinery | 2.00 | -13.8 |
| cartridge_press | 1.20 | -13.3 |
| bullet_swaging_machine | 0.80 | -13.0 |
| ammunition_assembly | 0.80 | -14.0 |
| metal_lathe | 2.00 | -15.0 |
| component_assembly | 2.00 | -15.2 |
| heavy_weapons_assembly | 2.00 | -15.0 |
| pylon_hum | 2.00 | -15.7 |
| accumulator_charge | 1.20 | -14.7 |

`turret_rotate` was rewritten as a procedural servo whine. The other procedural builders are unchanged from the earlier
version of the script. Their sound has not been checked by ear.
