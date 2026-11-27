import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'design/milestones.json').read_text(encoding='utf-8'))
out=root/'build/generated/advancements/data/zonezero_advancements/advancement'
import shutil
icons=json.loads((root/'art/icon_manifest.json').read_text(encoding='utf-8'))
if out.exists(): shutil.rmtree(out)
def write(name,obj):
 p=out/(name+'.json')
 p.parent.mkdir(parents=True,exist_ok=True)
 p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def node(name,title,desc,item,parent=None,hidden=False,challenge=False):
 d={'display':{'icon':{'id':item},'title':title,'description':desc,'frame':'challenge' if challenge else 'task','show_toast':parent is not None,'announce_to_chat':challenge,'hidden':hidden},'criteria':{'milestone':{'trigger':'minecraft:impossible'}}}
 if name in icons:
  d['display']['icon']={'id':'minecraft:paper','components':{'minecraft:custom_model_data':icons[name]['model_data']}}
 if parent: d['parent']='zonezero_advancements:'+parent
 else:
  d['display']['background']='minecraft:textures/block/deepslate.png'
  d['criteria']['milestone']['trigger']='minecraft:tick'
 write(name,d)
node('root','零號禁區','在廢土中寫下你的故事。','minecraft:compass')
titles='初見殺|死亡回歸|某個人的日常|笑死，真的死了|我就爛|死神的老朋友|無限月讀|明日邊界|死亡擱淺|三百壯士，只有你|不死者的詛咒|亞人|死亡輪迴|半千次的告別|命運石之門|死侍|死神拒收|人類補完計畫|不滅的靈魂|緊急撤退|雨中的眼淚|向死亡獻上祝福|最後一次，真的|死亡之神'.split('|')
assert len(titles)==len(data['deaths'])
memes={300:'你各位啊',2100:'這很不科學',3900:'這不是肯德基',5600:'我全都要',7300:'蛤？還有喔？',8700:'這個我不能接受'}
for z in data['zombies']:
 if z['count'] in memes: z['title']=memes[z['count']]
all_titles=titles+[x['title'] for x in data['zombies']]+['Re：486']
assert len(all_titles)==len(set(all_titles))
death_flavor=['歡迎來到零號禁區。', '每一次輪迴，都是新的開始。', '這次一定能活下來……吧？', '原來這句話不是比喻。', '至少重生按鈕很可靠。', '冥界已經幫你預留座位。', '世界是否只是一場幻術？', '生存、失敗、重來。', '你往返在兩個世界之間。', '一個人承包了整支軍隊的傷亡。', '你的死因報告越寫越厚。', '研究員開始懷疑你的體質。', '這條世界線怎麼又走錯？', '這座城市快沒有空地放墓碑了。', '一定是哪條世界線出了問題。', '復活後，記得把裝備撿回來。', '冥界宣布暫停受理。', '你似乎正在超越人類。', '身體倒下，意志仍不肯投降。', '博士，行動又失敗了。', '那些無法帶回重生點的回憶，就讓大雨替你收下。', '為每一次重生獻上祝福。', '真的，這次絕對是最後一次。', '死亡不再是終點，而是你的頭銜。']
for i,(n,t) in enumerate(zip(data['deaths'],titles)):
 node('death/'+str(n),t,death_flavor[i]+'\n累計死亡 '+str(n)+' 次。','minecraft:skeleton_skull','root' if i==0 else 'death/'+str(data['deaths'][i-1]),challenge=n==1000)
node('death/486','Re：486','無論重來多少次，我都會抵達那個未曾見過的明天。','minecraft:clock','death/450',True,True)
zombie_flavor=['生化警報響起，感染者已被清除。', '活人還有下一頁可以書寫。', '全體都有，下一批感染者準備報到。', '列車已抵達，請勿靠近車門。', '安全屋沒有子彈補給的上限就好了。', '今晚的清理工作才剛開始。', '畢業考的考題似乎都是感染者。', '寄生的生命終於停止活動。', '冥界請你暫停送貨。', '這座城市開始流傳你的故事。', '清潔費用請向生化研究所請款。', '末日還沒結束，但你仍在這裡。', '黑夜降臨之前，先找個屋頂。', '渡假行程改為無限期掃蕩。', '第七天的警報已成每日例行公事。', '博士，理智補給到了嗎？', '生存紀錄還沒走到最後一頁。', '機車的油箱需要比敵人更耐用。', '不靠源石也能突破防線。', '牆外的恐懼正在消失。', '生物學老師可能要重新出考卷。', '自由的旗幟在廢墟中飄揚。', '今日配送：過量彈藥。', '戰術分析完成，交戰許可已核發。', '王牌不必等待第二次命令。', '方舟收到你的戰場回報。', '基地的警戒燈終於熄滅。', '街道清理完畢，下一個街區。', '目標已停止活動；任務報告上不會寫下它曾經的名字。', '千軍萬馬也擋不住一個人。', '地圖上的安全屋只是傳說。', '理智用完了，彈藥還有剩。', '獵人執照終於有了用武之地。', '委託完成，素材卻用不完。', '這次接到的工作不需要魔力。', '警告：神經負荷持續上升，身體已逼近承受極限。', '部署完畢，防線保持完整。', '黑色球體也無法解釋這份戰績。', '應該說是另一種炸雞嗎？', '每次出勤結束，都要確認自己是否還認得自己。', '在廢墟裡找出自己的生存理由。', '只要還有補給，戰線就不會倒下。', '所有武裝系統運作正常。', '敵人似乎記得一段不願回想的過去。', '派對散場，請把地板打掃乾淨。', '就算有影子軍團，也要繼續前進。', '銀劍未必是唯一解答。', '篝火旁又多了一位平安歸來的人。', '今晚的獵殺並未結束。', '如今該輪到感染者恐懼。', '粉碎的不是恐懼，而是它們的防線。', '灰燼不會阻止新的火焰。', '黃金律法未必適用於這座城市。', '下一次交戰，決意比武器重要。', '火山沉寂後，戰場才終於安靜。', '這一萬隻的路，我全都要走完。', '廢墟深處也傳來老搖滾的回音。', '你的任務不必再躲在紙箱裡。', '未留下任何能夠追蹤的足跡。', '死亡奏起一首漫長的曲子。', '特工回報：區域威脅持續降低。', '污染區正在成為你的主場。', '這一趟總算平安撤離。', '老兵的履歷上新增一整頁。', '合約的危險等級由你親自選擇。', '鋼鐵與信念仍在抵抗災厄。', '願人類的火炬永遠不熄。', '清除行動進入最後階段。', '即使化為灰燼，仍要繼續前進。', '敵群的數量再也不是決勝關鍵。', '新職業的工作量不比舊職業少。', '這份委託沒有期限，只有越來越長的清除名單。', '補給還沒到，下一波卻先到了。', '戰利品的寶庫快裝不下了。', '廢墟中的每一把劍都寫著故事。', '探測器的讀數超出了刻度。', '身體比思考更早做出反應。', '這裡是你的主場。', '超乎預測的結果，列為特級紀錄。', '任務簡報只有一條指示：不必等待支援。', '下一次警報，誰會先感到害怕？', '封鎖線終於能夠向外推進。', '傷亡報告上的敵人代號已被刪除。', '現實與夢境都無法藏起感染者。', '回合結束，敵方沒有倖存者。', '戰況紀錄已經寫入檔案。', '地圖上的紅色標記正在消失。', '最後的戰士仍在守望。', '每一聲槍響，都為倖存者爭取時間。', '戰場的規則由倖存者重新書寫。', '隔絕兩個世界的防火牆，似乎也擋不住你的腳步。', '終止威脅的指令已下達。', '重建文明需要一片乾淨的土地。', '最漫長的黑夜仍有一團火。', '感染者開始迴避你的腳步。', '警戒等級已超出常規部隊的處理範圍。', '你的紀錄依然合格，但沒有人敢詢問你真正的意志。', '你不必再自我介紹，廢土上的傳聞已替你作證。', '即使無法一起離開，有些約定仍值得走到最後。', '曾經這裡是它們的領地，如今你才是禁區。']
for i,z in enumerate(data['zombies']):
 n=z['count']
 node('zombie/'+str(n),z['title'],zombie_flavor[i]+'\n累計討伐 '+str(n)+' 隻殭屍。','minecraft:rotten_flesh','root' if i==0 else 'zombie/'+str(n-100),challenge=n%1000==0)
series=[
 ('genetics',data['genetics']['thresholds'],'仿生人會夢見電子羊嗎？|我不做人啦！|感染者的未來|超級士兵計畫|生命的奇蹟|零號生命體','投入基因技能點','minecraft:ghast_tear'),
 ('guns',data['guns']['thresholds'],'我需要一把槍|槍之勇者|薩科塔的天賦|這就是我的槍道|超夢射擊課|戰爭機器|企鵝物流|槍之惡魔|傳奇傭兵|人形兵器|零號戰爭兵器','投入槍械技能點','minecraft:crossbow'),
 ('exploration',list(range(1,8)),'|'.join(data['exploration']['names']),'抵達禁區第','minecraft:map'),
 ('economy',data['economy']['thresholds'],'戰爭，戰爭從未改變|廢土拾荒者|歐元？這裡只收瓶蓋！|廢土商人|資本主義的勝利|瓶蓋大亨|財富自由|廢土華爾街','持有瓶蓋','minecraft:gold_nugget')
]
flavor={
 'genetics':['血清第一次改寫你的基因，而你還記得昨晚的夢。','不做人，也不代表不能守護人類。','改變不一定意味著毀滅。','普通士兵的極限已被跨越。','連研究員也無法解釋這份變化。','新的生命形態誕生於零號禁區。'],
 'guns':['在廢土，可靠的裝備能救命。','槍械訓練正式開始。','銃械彷彿是與生俱來的才能。','你的戰鬥方式愈加成熟。','訓練紀錄已完成，下一次實戰可沒辦法倒帶。','一個人也能守住一條戰線。','貨物抵達，障礙已排除。','連敵人都開始記得你的槍聲。','廢土傳說逐漸成形。','武器儼然成為身體的延伸。','你不需要援軍，你就是援軍。'],
 'exploration':['每一步都是新世界的開始。','安全地帶已經在身後。','安靜才是這片土地的警訊。','前方深處有比黑夜更深的危險。','霧中的街道似乎從未醒來。','風雪與荒野都無法阻止你前進。','你已到達文明最後的邊界。'],
 'economy':['廢土的第一筆新資產。','收集與保存同樣重要。','夜城的通貨在廢土可買不到半顆子彈。','你開始在廢土站穩腳步。','物資充足，交易才有籌碼。','你的儲蓄值得一個保險箱。','今天終於不用為一個瓶蓋發愁。','廢土金融界記住了你的名字。']
}
for key,thresholds,names,desc,icon in series:
 names=names.split('|')
 assert len(names)==len(thresholds)
 all_titles.extend(names)
 for i,(n,t) in enumerate(zip(thresholds,names)):
  node(key+'/'+str(n),t,flavor[key][i]+'\n'+desc+' '+str(n)+(' 區。' if key=='exploration' else '。'),icon,'root' if i==0 else key+'/'+str(thresholds[i-1]),challenge=i==len(names)-1)
assert len(all_titles)==len(set(all_titles))
hidden=json.loads((root/'design/hidden.json').read_text(encoding='utf-8'))['hidden']
descriptions={'revived':'成功被救起','revive_other':'成功救援其他玩家','downed_death':'倒地後死亡','centipede_death':'首次被艾利克斯的生物蜈蚣擊殺','zombie_death':'被殭屍擊殺','mutual_death':'與敵人同歸於盡','player_death':'被其他玩家擊殺','fall_death':'因墜落死亡','drown_death':'溺水死亡','fire_death':'因火焰死亡','unarmed_zombie_kill':'徒手擊殺殭屍','gun_headshot_kill':'以槍械爆頭擊殺','melee_zombie_kill':'以近戰武器擊殺殭屍','guai_place':'放置乖乖','guai_varieties':'放置不同款式的乖乖','shelter_create':'建立私人庇護所','shelter_teleport':'傳送回庇護所','midnight_food':'在遊戲午夜進食','alcohol':'飲用酒類','herb_thrombosis':'因草藥使用不當而血栓','both_trees_full':'點滿兩套技能樹','medical_use':'使用醫療用品','session_hours':'連續遊玩時數','low_health_teleport':'低血量時傳送撤離','zone7_zombie_kill':'在第七區擊殺殭屍','low_health_zombie_kill':'低血量時擊殺殭屍','peaceful_game_day':'一個遊戲日未擊殺生物','core_complete':'完成死亡、討伐及兩套技能系列'}
hidden_flavor=['還有人在等你回來。','即使災難蔓延，也不放棄生命。','撤退信號已經太遲了。','那麼多條腿實在太不公平了。','這場戰鬥沒有真正的贏家。','請先確認識別標記再開火。','信仰能飛翔，但你不能。','下次記得先浮出水面。','溫度不是越高越好。','沒有武器也能守住防線。','瞄準的那一刻，決定了結果。','刀刃上的每一道痕跡都是戰績。','設備運作前，別忘了小小儀式。','六種祝福，六種安心。','終於有一個能稱作家的地方。','那扇門後，是自己的安身之處。','即使午夜，也值得一份熱飯。','為還活著的今天乾杯。','廢土酒吧記住了你的名字。','用藥前確實需要讀說明書。','兩本訓練手冊都已翻到最後一頁。','你的病歷需要獨立檔案櫃。','博士，該休息一下了。','最後一瞬間，你找到了歸途。','最後的禁區也沒有讓你退縮。','每一雙伸出的手都有意義。','只要還有一點生命，就還有希望。','今天的成就只有好好活著。','病歷已經記滿感染者的名字。','這是倖存者用生命寫成的歷史。']
for index,h in enumerate(hidden):
 if h['event']=='deaths': continue
 key=h['event']
 n=h['threshold']
 all_titles.append(h['title'])
 desc=h['description'] if 'description' in h else hidden_flavor[index]+'\n'+descriptions[key]+'（'+str(n)+'）。'
 node('hidden/'+key+'/'+str(n),h['title'],desc,'minecraft:echo_shard','root',True,False)
assert len(all_titles)==197
assert len(all_titles)==len(set(all_titles))
assert len(list(out.rglob('*.json')))==198
print('Generated 197 achievements and one root.')

