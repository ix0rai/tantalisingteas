package io.ix0rai.tantalisingteas.mixin.render;

import io.ix0rai.tantalisingteas.data.NbtUtil;
import io.ix0rai.tantalisingteas.data.TeaColour;
import io.ix0rai.tantalisingteas.registry.TantalisingItems;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Final
    @Shadow
    private ItemModels models;

    @Inject(method = "getHeldItemModel", at = @At("HEAD"))
    public void getHeldItemModel(ItemStack stack, World world, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        if (stack.isOf(TantalisingItems.TEA_BOTTLE) && stack.getNbt() != null && NbtUtil.needsUpdate(stack.getNbt())) {
            TeaColour.updateColourValues(stack, this.models.getModelManager());
            NbtUtil.updateCustomName(stack);
            NbtUtil.setNeedsUpdate(stack.getNbt(), false);
            stack.setNbt(stack.getNbt());
        }
    }
}
