package zonezero.advancements.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Avoid applying an integration against a missing or structurally incompatible optional mod. */
public final class IntegrationMixinPlugin implements IMixinConfigPlugin {
    private static final System.Logger LOG=System.getLogger("ZoneZeroAdvancements/Integrations");
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets,Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public boolean shouldApplyMixin(String target,String mixin) {
        try {
            ClassNode node=MixinService.getService().getBytecodeProvider().getClassNode(target,false);
            boolean match=matches(node,mixin);
            LOG.log(System.Logger.Level.INFO,"{0}: {1}",mixin,match?"compatible":"unrecognized target; disabled");
            return match;
        } catch (Exception | LinkageError e) {
            LOG.log(System.Logger.Level.INFO,"{0}: unavailable optional target; disabled",mixin);
            return false;
        }
    }
    private static MethodNode method(ClassNode node,String name,String desc) {
        for (MethodNode m:node.methods)
            if(m.name.equals(name) && m.desc.equals(desc) && (m.access&Opcodes.ACC_STATIC)!=0) return m;
        return null;
    }
    private static boolean call(AbstractInsnNode insn,String owner,String name,String desc) {
        return insn instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);
    }
    private static AbstractInsnNode nextCode(AbstractInsnNode insn) {
        do { insn=insn.getNext(); } while(insn!=null && insn.getOpcode()<0);
        return insn;
    }
    public static boolean matches(ClassNode node,String mixin) {
        if (mixin.endsWith(".PersonalShelterMixin")) {
            MethodNode m=method(node,"createPersonalDimension","(Lnet/minecraft/server/level/ServerPlayer;Lcom/ruadou/personaldimension/dimension/DimensionPreset;Lcom/ruadou/personaldimension/dimension/template/DimensionTemplate;)Z");
            if (m==null) return false;
            for (AbstractInsnNode insn:m.instructions)
                if (call(insn,"net/minecraft/server/level/ServerPlayer","teleportTo","(Lnet/minecraft/server/level/ServerLevel;DDDFF)V")) return true;
            return false;
        }
        if (mixin.endsWith(".TeleportStoneMixin")) {
            MethodNode m=method(node,"finish","(Lnet/minecraft/server/level/ServerPlayer;Ltw/zonezero/teleport/ZoneZeroTeleport$Cast;)V");
            if (m==null) return false;
            int teleports=0,shrinks=0,shrinkIndex=-1,successIndex=-1;
            for (AbstractInsnNode insn:m.instructions) {
                if (call(insn,"net/minecraft/world/item/ItemStack","shrink","(I)V")) {
                    shrinks++; shrinkIndex=m.instructions.indexOf(insn);
                }
                if (call(insn,"net/minecraft/server/level/ServerPlayer","teleportTo","(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z")) {
                    teleports++;
                    AbstractInsnNode after=nextCode(insn);
                    if (!(after instanceof JumpInsnNode jump) || jump.getOpcode()!=Opcodes.IFNE) return false;
                    successIndex=m.instructions.indexOf(jump.label);
                    boolean earlyReturn=false;
                    for(AbstractInsnNode i=after.getNext();i!=null && i!=jump.label;i=i.getNext())
                        if(i.getOpcode()==Opcodes.RETURN) earlyReturn=true;
                    if(!earlyReturn) return false;
                }
            }
            return teleports==1 && shrinks==1 && successIndex>=0 && shrinkIndex>successIndex;
        }
        if (mixin.endsWith(".MedicineFunctionMixin")) {
            MethodNode m=method(node,"executeFunction","(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/resources/ResourceLocation;)V");
            if (m==null) return false;
            for (AbstractInsnNode insn:m.instructions)
                if (call(insn,"net/minecraft/server/ServerFunctionManager","execute","(Lnet/minecraft/commands/functions/CommandFunction;Lnet/minecraft/commands/CommandSourceStack;)V")) return true;
            return false;
        }
        return false;
    }
}
