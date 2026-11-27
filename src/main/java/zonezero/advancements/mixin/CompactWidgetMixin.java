package zonezero.advancements.mixin;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Optional client cosmetics: keep the tree intact, but draw only real progress links. */
@Mixin(value = AdvancementWidget.class, remap = false)
public abstract class CompactWidgetMixin {
    @Shadow @Final private AdvancementNode advancementNode;
    @Shadow private AdvancementWidget parent;
    @Shadow @Final private List<AdvancementWidget> children;
    @Shadow @Final private int x;
    @Shadow @Final private int y;

    @Inject(method = "drawConnectivity(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void zz$compactConnections(GuiGraphics graphics, int ox, int oy, boolean shadow, CallbackInfo ci) {
        if (!"zonezero_advancements".equals(advancementNode.holder().id().getNamespace())) return;
        var logicalParent = advancementNode.parent();
        // Root is a structural anchor only. Do not remove actual tree relationships:
        // vanilla needs them for drawing widgets, visibility, hover and progression.
        if (parent != null && logicalParent != null
                && !"root".equals(logicalParent.holder().id().getPath())
                && !(parent.getX() == 0 && parent.getY() == 0)) {
            int px = ox + parent.getX() + 13, py = oy + parent.getY() + 13;
            int cx = ox + x + 13, cy = oy + y + 13;
            int color = shadow ? 0xff000000 : 0xffffffff;
            int thickness = shadow ? 1 : 0;
            if (py == cy) {
                graphics.fill(Math.min(px, cx), py - thickness, Math.max(px, cx) + 1, py + thickness + 1, color);
            } else if (px == cx) {
                graphics.fill(px - thickness, Math.min(py, cy), px + thickness + 1, Math.max(py, cy) + 1, color);
            } else {
                // Fallback for datapack overrides or a skipped/invisible predecessor.
                int mid = (px + cx) / 2;
                graphics.fill(Math.min(px, mid), py - thickness, Math.max(px, mid) + 1, py + thickness + 1, color);
                graphics.fill(mid - thickness, Math.min(py, cy), mid + thickness + 1, Math.max(py, cy) + 1, color);
                graphics.fill(Math.min(mid, cx), cy - thickness, Math.max(mid, cx) + 1, cy + thickness + 1, color);
            }
        }
        for (AdvancementWidget child : children) child.drawConnectivity(graphics, ox, oy, shadow);
        ci.cancel();
    }
}
