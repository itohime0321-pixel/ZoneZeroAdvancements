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
assert len(manifest)==40
assert {v['model_data'] for v in manifest.values()}==set(range(772000,772040))
assert all('death/'+str(n) in manifest for n in [1,10,30,50,75,100,150,200,250,300,350,400,450,486,500,550,600,650,700,750,800,850,900,950,1000])
assert {p.relative_to(root).as_posix() for p in (root/'art/icons').rglob('*.png')}=={e['texture'] for e in manifest.values()}
with zipfile.ZipFile(root/'build/distributions/ZoneZeroAdvancements-Icons.zip') as z:
 assert len([n for n in z.namelist() if n.endswith('.png')])==40
 for key,entry in manifest.items():
  assert files[key]['display']['icon']['components']['minecraft:custom_model_data']==entry['model_data']
  data=z.read('assets/zonezero_advancements/textures/item/achievement/'+key.replace('/','_')+'.png')
  assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(32,32)
  assert data[25]==6 # RGBA
  assert data==(root/entry['texture']).read_bytes()
  model=json.loads(z.read('assets/zonezero_advancements/models/item/achievement/'+key.replace('/','_')+'.json'))
  assert model['textures']['layer0']=='zonezero_advancements:item/achievement/'+key.replace('/','_')
 overrides=json.loads(z.read('assets/minecraft/models/item/paper.json'))['overrides']
 assert len(overrides)==80
 for i,(key,entry) in enumerate(sorted(manifest.items(),key=lambda x:x[1]['model_data'])):
  assert overrides[2*i]=={'predicate':{'custom_model_data':entry['model_data']},'model':'zonezero_advancements:item/achievement/'+key.replace('/','_')}
  assert overrides[2*i+1]=={'predicate':{'custom_model_data':entry['model_data']+0.5},'model':'zonezero_advancements:item/plain_paper'}
print('PASS: 187 achievements, 31 hidden, no duplicate names/descriptions, 100 kill thresholds, 7 zones, 40 linked 32x32 PNGs.')
