package com.cravencraft.bloodybits.client.renderer.entity.layers;

import com.cravencraft.bloodybits.BloodyBitsMod;
import com.cravencraft.bloodybits.client.model.EntityInjuries;
import com.cravencraft.bloodybits.config.ClientConfig;
import com.cravencraft.bloodybits.utils.BloodyBitsUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class InjuryLayer <T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>  {

    public InjuryLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int pPackedLight, @NotNull T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (ClientConfig.showEntityDamage() && livingEntity.isAlive() && livingEntity.getHealth() < livingEntity.getMaxHealth()) {
//            BloodyBitsMod.LOGGER.info("ARE WE RENDERIN???");
//            poseStack.pushPose();
            int entityId = livingEntity.getId();

            // Will render a random assortment of injury textures on the given entity
            // if it is contained within the map.
            if (BloodyBitsUtils.INJURED_ENTITIES.containsKey(entityId)) {

                EntityInjuries entityInjuries = BloodyBitsUtils.INJURED_ENTITIES.get(entityId);

                if (entityInjuries.smallInjuries != null && !entityInjuries.smallInjuries.isEmpty()) {
                    for (var smallInjury : entityInjuries.smallInjuries.entrySet()) {
                        this.renderDamageLayerToBuffer(smallInjury.getKey(), livingEntity, bufferSource, poseStack, partialTicks, pPackedLight);
                    }
                }

                // TODO:
                //  - Textures are a bit too dark. Use some lighter greys.
                //  - Actually, just have heals slowly change the opacity of the image. Once the image is at 0, then remove it.
                //      Think about that more and see if that will work.
                if (entityInjuries.mediumInjuries != null && !entityInjuries.mediumInjuries.isEmpty()) {
                    for (var mediumInjury : entityInjuries.mediumInjuries.entrySet()) {
                        this.renderDamageLayerToBuffer(mediumInjury.getKey(), livingEntity, bufferSource, poseStack, partialTicks, pPackedLight);
                    }
                }

                if (entityInjuries.largeInjuries != null && !entityInjuries.largeInjuries.isEmpty()) {
                    for (var largeInjury : entityInjuries.largeInjuries.entrySet()) {
                        this.renderDamageLayerToBuffer(largeInjury.getKey(), livingEntity, bufferSource, poseStack, partialTicks, pPackedLight);
                    }
                }
            }
//            poseStack.popPose();
        }
    }

    /**
     * All the code needed to render the new entity damage layer. Essentially copied from the original render method that is being mixed into.
     */
    private void renderDamageLayerToBuffer(NativeImage damageLayerTexture, T pEntity, MultiBufferSource buffer, PoseStack poseStack, float pPartialTicks, int pPackedLight) {
        DynamicTexture dynamicTexture = new DynamicTexture(damageLayerTexture);
        VertexConsumer customVertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(Minecraft.getInstance().getTextureManager().register("damage_layer", dynamicTexture)));

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        boolean isEntityVisible = !pEntity.isInvisible();
        boolean canPlayerSeeInvisibleEntity;
        if (player != null) {
            canPlayerSeeInvisibleEntity = !isEntityVisible && !pEntity.isInvisibleTo(player);
        }
        else {
            canPlayerSeeInvisibleEntity = false;
        }

//        boolean shouldEntityAppearGlowing = minecraft.shouldEntityAppearGlowing(pEntity);
//        this.getParentModel().renderType(this.getTextureLocation(pEntity));
//        RenderType rendertype = this.getParentModel().renderType(this.getTextureLocation(pEntity));
//        int i = LivingEntityRenderer.getOverlayCoords(pEntity, this.getWhiteOverlayProgress(pEntity, pPartialTicks));
        this.getParentModel().renderToBuffer(poseStack, customVertexConsumer, pPackedLight, 0, 1.0F, 1.0F, 1.0F, canPlayerSeeInvisibleEntity ? 0.15F : 1.0F);
    }
}