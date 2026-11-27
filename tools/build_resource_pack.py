"""Build the optional vanilla-client 1.21.1 icon pack from approved PNGs."""
from pathlib import Path
import json, zipfile
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'art/icon_manifest.json').read_text())
output=root/'build/distributions/ZoneZeroAdvancements-Icons.zip'
output.parent.mkdir(parents=True,exist_ok=True)
def encode(obj):return json.dumps(obj,ensure_ascii=False,indent=2)+'\n'
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
    z.writestr('pack.mcmeta',encode({'pack':{'pack_format':34,'description':f'零號禁區｜{len(manifest)} 張成就像素圖示'}}))
    overrides=[]
    for path,item in sorted(manifest.items(),key=lambda x:x[1]['model_data']):
        name=path.replace('/','_')
        tex='zonezero_advancements:item/achievement/'+name
        model='zonezero_advancements:item/achievement/'+name
        z.write(root/item['texture'],'assets/zonezero_advancements/textures/item/achievement/'+name+'.png')
        z.writestr('assets/zonezero_advancements/models/item/achievement/'+name+'.json',
            encode({'parent':'minecraft:item/generated','textures':{'layer0':tex}}))
        overrides.append({'predicate':{'custom_model_data':item['model_data']},'model':model})
        # Reset to vanilla after each exact integer; avoid overriding unrelated higher CMD.
        overrides.append({'predicate':{'custom_model_data':item['model_data']+0.5},'model':'zonezero_advancements:item/plain_paper'})
    z.writestr('assets/zonezero_advancements/models/item/plain_paper.json',encode({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/paper'}}))
    z.writestr('assets/minecraft/models/item/paper.json',encode({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/paper'},'overrides':overrides}))
print(output)
