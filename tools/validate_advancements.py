from pathlib import Path
import json, zipfile, struct, zlib
root=Path(__file__).resolve().parents[1]
d=root/'build/generated/advancements/data/zonezero_advancements/advancement'
files={str(p.relative_to(d).with_suffix('')).replace('\\','/'):json.loads(p.read_text(encoding='utf-8')) for p in d.rglob('*.json')}
assert len(files)==203
assert len({f['display']['title'] for f in files.values()})==203
assert len({f['display']['description'] for f in files.values()})==203
# All 202 achievement intros have a distinct flavor line and a separate, explicit condition.
for key,f in files.items():
 if key!='root':
  lines=f['display']['description'].splitlines()
  assert len(lines)==2 and all(line.strip() for line in lines),key
assert len({f['display']['description'].splitlines()[0] for k,f in files.items() if k!='root'})==202
assert sum(f['display']['hidden'] for f in files.values())==46
for key,v in files.items():
 # The server sends one custom announcement (title + visible description).
 # Suppress vanilla's extra copy, while preserving toasts for all actual achievements.
 assert v['display']['announce_to_chat'] is False,key
 assert v['display']['show_toast']==(key!='root'),key
 if 'parent' in v: assert v['parent'].split(':',1)[1] in files
 assert v['criteria']['milestone']['trigger']==('minecraft:tick' if key=='root' else 'minecraft:impossible')
assert {p for p in files if p.startswith('exploration/')}=={f'exploration/{n}' for n in range(1,8)}
assert {p for p in files if p.startswith('zombie/')}=={f'zombie/{n}' for n in range(100,10001,100)}
assert files['death/30']['display']['title']=='某個人的日常'
assert files['death/486']['display']['hidden']
assert files['hidden/centipede_death/1']['display']['title']=='你不要過來啊！'
assert 'hidden/zone7_zombie_kill/1000' in files
songs={'shelter_teleport/10','zombie_survival_streak/50','burning_zombie_kill/25',
       'full_moon_zombie_kill/20','unarmed_zombie_kill/50','all_zones/7',
       'rain_zombie_kill/10','summit/1','villager_kill/1','zombie_burst/5'}
assert all('hidden/'+key in files and files['hidden/'+key]['display']['hidden'] for key in songs)

hk={
 'barehand_concrete_break/1':'大家可以回家啦！',
 'unarmed_zombie_kill/10':'我要打十個！',
 'revive_other/1':'英雄本色',
 'long_range_arrow_kill/1':'一支穿雲箭',
 'zombie_villager_kill/1':'無間道',
}
assert all(files['hidden/'+key]['display']['hidden'] and files['hidden/'+key]['display']['title']==title for key,title in hk.items())
tag=json.loads((root/'src/main/resources/data/zonezero_advancements/tags/block/concrete.json').read_text())
assert not tag['replace']
assert set(tag['values'])=={'minecraft:'+color+'_concrete' for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()}
manifest=json.loads((root/'art/icon_manifest.json').read_text())
assert len(manifest)==203
assert set(manifest)==set(files)
assert {v['model_data'] for v in manifest.values()}==set(range(772000,772203))
assert all('death/'+str(n) in manifest for n in [1,10,30,50,75,100,150,200,250,300,350,400,450,486,500,550,600,650,700,750,800,850,900,950,1000])
assert {p.relative_to(root).as_posix() for p in (root/'art/icons').rglob('*.png')}=={e['texture'] for e in manifest.values()}
# Decode RGBA PNG scanlines using only the Python standard library, so CI
# enforces the same shared palette rule as the hand-drawn pixel art workflow.
def png_palette(data):
 pos=8
 compressed=[]
 while pos+12<=len(data):
  count,kind=struct.unpack_from('>I4s',data,pos)
  payload=data[pos+8:pos+8+count]
  if kind==b'IHDR':
   assert payload==struct.pack('>IIBBBBB',32,32,8,6,0,0,0)
  elif kind==b'IDAT': compressed.append(payload)
  elif kind==b'IEND': break
  pos+=count+12
 raw=zlib.decompress(b''.join(compressed))
 assert len(raw)==32*(32*4+1)
 prev=bytearray(128)
 colors=set()
 for y in range(32):
  mode=raw[y*129]
  assert mode in range(5)
  row=bytearray(raw[y*129+1:y*129+129])
  for x in range(128):
   left=row[x-4] if x>=4 else 0
   up=prev[x]
   ul=prev[x-4] if x>=4 else 0
   if mode==1: predictor=left
   elif mode==2: predictor=up
   elif mode==3: predictor=(left+up)//2
   elif mode==4:
    p=left+up-ul
    predictor=min((left,up,ul),key=lambda n:abs(p-n))
   else: predictor=0
   row[x]=(row[x]+predictor)&255
  colors.update(tuple(row[x:x+4]) for x in range(0,128,4))
  prev=row
 assert (0,0,0,0) in colors and all(c[3] in (0,255) for c in colors)
 return colors
assert all(k in manifest for k,v in files.items() if v['display']['hidden'])
palette=set()
with zipfile.ZipFile(root/'build/distributions/ZoneZeroAdvancements-Icons.zip') as z:
 assert len([n for n in z.namelist() if n.endswith('.png')])==203
 for key,entry in manifest.items():
  assert files[key]['display']['icon']['components']['minecraft:custom_model_data']==entry['model_data']
  data=z.read('assets/zonezero_advancements/textures/item/achievement/'+key.replace('/','_')+'.png')
  assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(32,32)
  assert data[25]==6 # RGBA
  assert data==(root/entry['texture']).read_bytes()
  palette.update(png_palette(data))
  model=json.loads(z.read('assets/zonezero_advancements/models/item/achievement/'+key.replace('/','_')+'.json'))
  assert model['textures']['layer0']=='zonezero_advancements:item/achievement/'+key.replace('/','_')
 overrides=json.loads(z.read('assets/minecraft/models/item/paper.json'))['overrides']
 assert len(overrides)==406
 for i,(key,entry) in enumerate(sorted(manifest.items(),key=lambda x:x[1]['model_data'])):
  assert overrides[2*i]=={'predicate':{'custom_model_data':entry['model_data']},'model':'zonezero_advancements:item/achievement/'+key.replace('/','_')}
  assert overrides[2*i+1]=={'predicate':{'custom_model_data':entry['model_data']+0.5},'model':'zonezero_advancements:item/plain_paper'}
assert len(palette)<=32, f'Shared palette has {len(palette)} entries (including transparency)'
print(f'Palette: {len(palette)-1} opaque colors + transparency.')
print('PASS: 202 achievements, 46 hidden, no duplicate names/descriptions, 100 kill thresholds, 7 zones, 203 linked 32x32 PNGs (202 achievements + root), max 32 shared colors.')

# Every real relationship is adjacent, and no independent achievement is
# attached to another independent achievement for visual purposes.
layout={}
for line in (root/'build/generated/advancements/zonezero_advancements_layout.properties').read_text().splitlines():
 key,xy=line.split('=')
 layout[key]=tuple(map(int,xy.split(',')))
assert set(layout)==set(files) and len(set(layout.values()))==203
assert layout['root']==(0,0)
assert max(x for x,y in layout.values())<=16 and max(y for x,y in layout.values())<=15
assert all(files[f'exploration/{n}']['parent']=='zonezero_advancements:root' for n in range(1,8))
for key,f in files.items():
 parent=f.get('parent','').split(':')[-1]
 if not parent or parent=='root': continue
 x,y=layout[key]
 a,b=layout[parent]
 assert abs(x-a)+abs(y-b)==1,(key,parent,layout[key],layout[parent])
 assert key.rsplit('/',1)[0]==parent.rsplit('/',1)[0], (key,parent)
for key in ['unarmed_zombie_kill','revive_other','alcohol','shelter_teleport']:
 nodes=sorted((k for k in files if k.startswith('hidden/'+key+'/')),key=lambda k:int(k.rsplit('/',1)[1]))
 assert len(nodes)>=2
 for before,after in zip(nodes,nodes[1:]):
  assert files[after]['parent']=='zonezero_advancements:'+before
print('PASS: compact grid, unique coordinates, adjacent milestone chains, independent exploration and hidden feats.')
