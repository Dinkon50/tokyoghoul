import json
exec(open('src_blade.py').read()); exec(open('src_two.py').read())
json.dump({'styles': {'blade': blade, 'scythe': scythe, 'spear': spear}}, open('quinque_animations.json', 'w'), indent=1)
