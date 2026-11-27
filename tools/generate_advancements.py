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
titles='初見殺|死亡回歸|某個人的日常|笑死，真的死了|我就爛|死神的老朋友|無限月讀|明日邊界|死亡擱淺|三百壯士，只有你|不死者的詛咒|亞人|死亡輪迴|半千次的告別|命運石之門|死侍|死神拒收|人類補完計畫|不滅的靈魂|緊急撤退|死亡專業戶|向死亡獻上祝福|最後一次，真的|死亡之神'.split('|')
assert len(titles)==len(data['deaths'])
memes={300:'你各位啊',2100:'這很不科學',3900:'這不是肯德基',5600:'我全都要',7300:'蛤？還有喔？',8700:'這個我不能接受'}
for z in data['zombies']:
 if z['count'] in memes: z['title']=memes[z['count']]
all_titles=titles+[x['title'] for x in data['zombies']]+['Re：486']
assert len(all_titles)==len(set(all_titles))
for i,(n,t) in enumerate(zip(data['deaths'],titles)):
 node('death/'+str(n),t,'累計死亡 '+str(n)+' 次。','minecraft:skeleton_skull','root' if i==0 else 'death/'+str(data['deaths'][i-1]),challenge=n==1000)
node('death/486','Re：486','無論重來多少次，我都會抵達那個未曾見過的明天。','minecraft:clock','death/450',True,True)
for i,z in enumerate(data['zombies']):
 n=z['count']
 node('zombie/'+str(n),z['title'],'累計討伐 '+str(n)+' 隻殭屍。','minecraft:rotten_flesh','root' if i==0 else 'zombie/'+str(n-100),challenge=n%1000==0)
series=[
 ('genetics',data['genetics']['thresholds'],'人類進化的第一步|我不做人啦！|感染者的未來|超級士兵計畫|生命的奇蹟|零號生命體','投入基因技能點','minecraft:ghast_tear'),
 ('guns',data['guns']['thresholds'],'我需要一把槍|槍之勇者|薩科塔的天賦|這就是我的槍道|專業人士|戰爭機器|企鵝物流|槍之惡魔|傳奇傭兵|人形兵器|零號戰爭兵器','投入槍械技能點','minecraft:crossbow'),
 ('exploration',list(range(1,15)),'初入禁區|離開舒適圈|潛行者|地鐵：最後曙光|無人之境|來自深淵|深入敵後|死亡之地|寂靜之丘|地獄的入口|人類禁足之地|薩米的深處|有去無回|零號禁區的盡頭','抵達禁區第','minecraft:map'),
 ('economy',[1,100,500,1000,5000,10000,50000,100000],'戰爭，戰爭從未改變|廢土拾荒者|有錢能使鬼推磨|廢土商人|資本主義的勝利|瓶蓋大亨|財富自由|廢土華爾街','持有瓶蓋','minecraft:gold_nugget')
]
for key,thresholds,names,desc,icon in series:
 names=names.split('|')
 assert len(names)==len(thresholds)
 all_titles.extend(names)
 for i,(n,t) in enumerate(zip(thresholds,names)):
  node(key+'/'+str(n),t,desc+' '+str(n)+(' 區。' if key=='exploration' else '。'),icon,'root' if i==0 else key+'/'+str(thresholds[i-1]),challenge=i==len(names)-1)
assert len(all_titles)==len(set(all_titles))
hidden=json.loads((root/'design/hidden.json').read_text(encoding='utf-8'))['hidden']
descriptions={'revived':'成功被救起','revive_other':'成功救援其他玩家','downed_death':'倒地後死亡','zombie_death':'被殭屍擊殺','mutual_death':'與敵人同歸於盡','player_death':'被其他玩家擊殺','fall_death':'因墜落死亡','drown_death':'溺水死亡','fire_death':'因火焰死亡','unarmed_zombie_kill':'徒手擊殺殭屍','gun_headshot_kill':'以槍械爆頭擊殺','melee_zombie_kill':'以近戰武器擊殺殭屍','guai_place':'放置乖乖','guai_varieties':'放置不同款式的乖乖','shelter_create':'建立私人庇護所','shelter_teleport':'傳送回庇護所','midnight_food':'在遊戲午夜進食','alcohol':'飲用酒類','herb_thrombosis':'因草藥使用不當而血栓','both_trees_full':'點滿兩套技能樹','medical_use':'使用醫療用品','session_hours':'連續遊玩時數','low_health_teleport':'低血量時傳送撤離','zone14_zombie_kill':'在第十四區擊殺殭屍','low_health_zombie_kill':'低血量時擊殺殭屍','peaceful_game_day':'一個遊戲日未擊殺生物','core_complete':'完成全部公開成就'}
for h in hidden:
 if h['event']=='deaths': continue
 key=h['event']
 n=h['threshold']
 all_titles.append(h['title'])
 node('hidden/'+key+'/'+str(n),h['title'],descriptions[key]+'（'+str(n)+'）。','minecraft:echo_shard','root',True,False)
assert len(all_titles)==194
assert len(all_titles)==len(set(all_titles))
assert len(list(out.rglob('*.json')))==195
print('Generated 194 achievements and one root.')
