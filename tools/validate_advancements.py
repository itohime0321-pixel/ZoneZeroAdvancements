from pathlib import Path
import json, zipfile, struct
root=Path(__file__).resolve().parents[1]
d=root/'build/generated/advancements/data/zonezero_advancements/advancement'
files={str(p.relative_to(d).with_suffix('')).replace('\\','/'):json.loads(p.read_text(encoding='utf-8')) for p in d.rglob('*.json')}
assert len(files)==188
assert len({f['display']['title'] for f in files.values()})==188
assert len({f['display']['description'] for f in files.values()})==188
assert sum(f['display']['hidden'] for f in files.values())==31
for key,v in files.items():
 if 'parent' in v: assert v['parent'].split(':',1)[1] in files
 assert v['criteria']['milestone']['trigger']==('minecraft:tick' if key=='root' else 'minecraft:impossible')
assert {p for p in files if p.startswith('exploration/')}=={f'exploration/{n}' for n in range(1,8)}
assert {p for p in files if p.startswith('zombie/')}=={f'zombie/{n}' for n in range(100,10001,100)}
assert files['death/30']['display']['title']=='某個人的日常'
assert files['death/486']['display']['hidden']
assert files['hidden/centipede_death/1']['display']['title']=='你不要過來啊！'
assert 'hidden/zone7_zombie_kill/1000' in files
manifest=json.loads((root/'art/icon_manifest.json').read_text())
assert len(manifest)==20 and len({v['model_data'] for v in manifest.values()})==20
with zipfile.ZipFile(root/'build/distributions/ZoneZeroAdvancements-Icons-Batch01.zip') as z:
 assert len([n for n in z.namelist() if n.endswith('.png')])==20
 for key,entry in manifest.items():
  assert files[key]['display']['icon']['components']['minecraft:custom_model_data']==entry['model_data']
  data=z.read('assets/zonezero_advancements/textures/item/achievement/'+key.replace('/','_')+'.png')
  assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(32,32)
  assert data[25]==6 # RGBA
print('PASS: 187 achievements, 31 hidden, no duplicate names/descriptions, 100 kill thresholds, 7 zones, 20 linked 32x32 PNGs.')
