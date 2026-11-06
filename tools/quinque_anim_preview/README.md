# Предпросмотр анимаций квинке

Точная копия модели игрока Minecraft 1.20.1 (HumanoidModel, ItemInHandLayer, отображение предмета, QuinqueRenderer)
с твоими квинке. Та же математика, что в `client/quinque/QuinqueAnim.java`, поэтому кадры совпадают с игрой.

1. `python3 prep.py` — один раз (нужны numpy и Pillow).
2. Правишь движения в `src_blade.py` (Kura, Narukami) и `src_two.py` (13's Jason, IXA), затем `python3 build.py` —
   получится `quinque_animations.json`. Его копия лежит в `src/main/resources/assets/tokyoghoulrpg/`.
3. `python3 strip.py quinque_animations.json kura attack_1` — кадры приёма (вид спереди, сзади и от первого лица);
   `python3 gif.py kura` — GIF всех приёмов в реальном времени. Нужны Node.js и Playwright с Chromium.

В игре файл можно перезагрузить без перезапуска: F3 + T.
