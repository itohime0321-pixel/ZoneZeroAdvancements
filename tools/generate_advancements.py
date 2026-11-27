import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'design/milestones.json').read_text(encoding='utf-8'))
out=root/'src/main/resources/data/zonezero_advancements/advancement'
def write(name,obj):
 p=out/(name+'.json')
 p.parent.mkdir(parents=True,exist_ok=True)
 p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def node(name,title,desc,item,parent=None,hidden=False,challenge=False):
 d={'display':{'icon':{'id':item},'title':title,'description':desc,'frame':'challenge' if challenge else 'task','show_toast':parent is not None,'announce_to_chat':challenge,'hidden':hidden},'criteria':{'milestone':{'trigger':'minecraft:impossible'}}}
 if parent: d['parent']='zonezero_advancements:'+parent
 else:
  d['display']['background']='minecraft:textures/block/deepslate.png'
  d['criteria']['milestone']['trigger']='minecraft:tick'
 write(name,d)
node('root','零號禁區','在廢土中寫下你的故事。','minecraft:compass')
titles='初見殺|死亡回歸|某個人的日常|你已經死了|今天也是個死去的好日子|死神的老朋友|無限月讀|明日邊界|死亡擱淺|三百壯士，只有你|不死者的詛咒|亞人|死亡輪迴|半千次的告別|命運石之門|死侍|死神拒收|人類補完計畫|不滅的靈魂|緊急撤退|死亡專業戶|向死亡獻上祝福|最後一次，真的|死亡之神'.split('|')
assert len(titles)==len(data['deaths'])
all_titles=titles+[x['title'] for x in data['zombies']]+['Re：486']
assert len(all_titles)==len(set(all_titles))
for i,(n,t) in enumerate(zip(data['deaths'],titles)):
 node('death/'+str(n),t,'累計死亡 '+str(n)+' 次。','minecraft:skeleton_skull','root' if i==0 else 'death/'+str(data['deaths'][i-1]),challenge=n==1000)
node('death/486','Re：486','無論重來多少次，我都會抵達那個未曾見過的明天。','minecraft:clock','death/450',True,True)
for i,z in enumerate(data['zombies']):
 n=z['count']
 node('zombie/'+str(n),z['title'],'累計討伐 '+str(n)+' 隻殭屍。','minecraft:rotten_flesh','root' if i==0 else 'zombie/'+str(n-100),challenge=n%1000==0)
print('Generated 126 advancement definitions (including root).')
