package com.cravencraft.bloodybits.events;

import com.cravencraft.bloodybits.BloodyBitsMod;
import com.cravencraft.bloodybits.client.renderer.entity.layers.InjuryLayer;
import com.cravencraft.bloodybits.config.ClientConfig;
import com.cravencraft.bloodybits.config.CommonConfig;
import com.cravencraft.bloodybits.entity.BloodSprayEntity;
import com.cravencraft.bloodybits.network.BloodyBitsPacketHandler;
import com.cravencraft.bloodybits.network.messages.EntityDamageMessage;
import com.cravencraft.bloodybits.network.messages.EntityHealMessage;
import com.cravencraft.bloodybits.network.messages.EntityMessage;
import com.cravencraft.bloodybits.registries.EntityRegistry;
import com.cravencraft.bloodybits.utils.BloodyBitsUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BloodyBitsMod.MODID)
public class BloodyBitsEvents {

//    /**
//     * Just a simple method made to test blood sprays by right clicking on blocks.
//     */
//    @SubscribeEvent
//    public static void testBloodSpray(PlayerInteractEvent.RightClickBlock event) {
//        if (!event.getEntity().level().isClientSide()) {
//            if (BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.size() >= CommonConfig.maxSpatters()) {
//                BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.get(0).discard();
//                BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.remove(0);
//            }
//
//            BloodSprayEntity bloodSprayEntity = new BloodSprayEntity(EntityRegistry.BLOOD_SPRAY.get(), event.getEntity(), event.getEntity().level());
//            BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.add(bloodSprayEntity);
//
//            bloodSprayEntity.setDeltaMovement(event.getEntity().getLookAngle());
//            event.getEntity().level().addFreshEntity(bloodSprayEntity);
//
//            BloodyBitsPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> bloodSprayEntity),
//                    new EntityMessage(bloodSprayEntity.getId(), event.getEntity().getId()));
//        }
//    }

    /**
     * Removes an entity from the INJURED_ENTITIES list upon death if that configuration is enabled client side.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entityDeathEvent(LivingDeathEvent deathEvent) {
        if (ClientConfig.showEntityDamage() && !deathEvent.isCanceled() && deathEvent.getEntity() != null && deathEvent.getEntity().level().isClientSide()) {
            BloodyBitsUtils.INJURED_ENTITIES.remove(deathEvent.getEntity().getId());
        }
    }

    /**
     * When an entity heals a message will be sent client side with the entity's ID and the heal amount. This will
     * be used to potentially remove injuries from the entity if it is contained within the INJURED_ENTITIES list.
     * An entity will not be added to the list if the config is disabled, or injury textures do not exist for the given
     * entity.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entityHealEvent(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();

        if (ClientConfig.showEntityDamage() && !event.isCanceled() && entity != null) {
            String entityName = (entity instanceof Player) ? "player" : entity.getEncodeId();
            entityName = (entityName == null) ? "" : entityName;

            if (!CommonConfig.blackListEntities().contains(entityName)) {
                BloodyBitsPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                        new EntityHealMessage(entity.getId(), event.getAmount()));
            }
        }
    }

    /**
     * Looks for all the players on a given server and creates blood sprays if the damage event is
     * close enough to any of the players. Will break out of the loop the second a player is found,
     * which should optimize this somewhat.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bloodOnEntityDamage(LivingDamageEvent event) {

        LivingEntity entity = event.getEntity();
        if (!event.isCanceled() && entity != null) {
            String entityName = (entity instanceof Player) ? "player" : entity.getEncodeId();
            entityName = (entityName == null) ? "" : entityName;

            if (!event.getEntity().level().isClientSide() && !CommonConfig.blackListEntities().contains(entityName) && !CommonConfig.blackListDamageSources().contains(event.getSource().type().msgId())) {
                int maxDamage = (int) Math.min(20, event.getAmount());
                createBloodSpray(entity, event.getSource(), maxDamage, false);
            }

            // For adding damage textures to the given entity. Ensure no blacklisted injury sources are added.
            if (!ClientConfig.blackListInjurySources().contains(event.getSource().type().msgId())) {
                boolean isBurn = ClientConfig.burnDamageSources().contains(event.getSource().type().msgId());

                BloodyBitsPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                        new EntityDamageMessage(entity.getId(), event.getAmount(), !isBurn, isBurn));
            }
        }
    }

    /**
     * Adds an injury layer to the entity. Will trigger once for any new entity the first time it is rendered.
     * Once the layer is added, it is then added to a list which will ensure that multiple layers aren't added for
     * entities.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addInjuryLayerToEntity(RenderLivingEvent.Pre<?, ?> event) {

        if (ClientConfig.showEntityDamage() && !event.isCanceled()) {

            LivingEntity livingEntity = event.getEntity();
            if (livingEntity.isAlive() && livingEntity.getHealth() < livingEntity.getMaxHealth()) {
                String entityName = (livingEntity instanceof Player) ? "player" : livingEntity.getEncodeId();
                entityName = (entityName == null) ? "" : entityName;

                if (!BloodyBitsUtils.INJURY_LAYER_ENTITIES.contains(entityName)) {
                    event.getRenderer().addLayer(new InjuryLayer(event.getRenderer()));
                    BloodyBitsUtils.INJURY_LAYER_ENTITIES.add(entityName);
                }
            }
        }
    }

    // TODO: Rework this. This is a cool feature to have. Just want it to be slightly less random. Set up a minimum and
    //       maximum interval that will change as the entity takes more damage. May need to get tick counts for that?

    /**
     *  Makes the entity bleed when damaged below a certain threshold. The entity will bleed more often the lower it is
     *  below that threshold.
     */
    @SubscribeEvent
    public static void entityBleedWhenDamaged(LivingEvent.LivingTickEvent event) {
        if (CommonConfig.bleedWhenDamaged() && event.getEntity() != null && !event.getEntity().level().isClientSide() && !event.getEntity().isDeadOrDying()) {
            LivingEntity entity = event.getEntity();
            double remainingHealthPercentage = entity.getHealth() / entity.getMaxHealth();
            String entityName = (entity instanceof Player) ? "player" : entity.getEncodeId();
            entityName = (entityName == null) ? "" : entityName;

            if (!CommonConfig.blackListEntities().contains(entityName) && remainingHealthPercentage <= 0.5) {

                int mod = (int) (remainingHealthPercentage * 1000);

                if (entity.tickCount % mod == 0) {
                    createBloodSpray(entity, entity.damageSources().genericKill(), 1, true);
                }
            }
        }
    }

    /**
     * For when entities explode. Like a creeper.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void creeperExplosionEvent(ExplosionEvent event) {
        Entity entity = event.getExplosion().getDirectSourceEntity();

        if (entity instanceof LivingEntity livEnt && !event.isCanceled()) {
            createBloodSpray(livEnt, event.getExplosion().getDamageSource(), 15, false);
        }
    }

    /**
     * Clears all injury related lists when the client player logs out (If they have entity damage enabled).
     * Allows the player to log out and back in if they modify their texture packs, and damage doesn't appear on entities.
     */
    @SubscribeEvent
    public static void clearInjuryTextureListsOnResourcePackReload(ClientPlayerNetworkEvent.LoggingOut event) {
        if (ClientConfig.showEntityDamage()) {
            BloodyBitsUtils.INJURY_LAYER_ENTITIES.clear();
            BloodyBitsUtils.INJURED_ENTITIES.clear();
        }
    }

    /**
     * Creates blood sprays for any entity that is damaged. Also adds each blood spray to a list BLOOD_SPRAY_ENTITIES
     * when it is created. This list maxes out at a configurable limit to ensure the blood spray entities impact the
     * game's performance as little as possible. When a new blood spray is created, if the limit of the list is passed,
     * the first index of the list is removed to ensure that the oldest sprays are removed first.
     */
    private static void createBloodSpray(LivingEntity entity, DamageSource damageSource, int damageAmount, boolean isBleedingDamage) {
        if (entity != null && damageSource != null) {
            String entityName = (entity instanceof Player) ? "player" : entity.getEncodeId();
            entityName = (entityName == null) ? "" : entityName;

            if (!entity.level().isClientSide() && !CommonConfig.blackListEntities().contains(entityName) && !CommonConfig.blackListDamageSources().contains(damageSource.type().msgId())) {
                //TODO: Currently, creepers don't produce blood when exploding because it's not registered as a LivingAttackEvent on THEMSELF.
                //      So, maybe have an exception happen in a damage event?
                for (int i = 0; i < damageAmount; i++) {
                    if (BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.size() >= CommonConfig.maxSpatters()) {
                        BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.get(0).discard();
                        BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.remove(0);
                    }

                    BloodSprayEntity bloodSprayEntity = new BloodSprayEntity(EntityRegistry.BLOOD_SPRAY.get(), entity, entity.level());
                    BloodyBitsUtils.BLOOD_SPRAY_ENTITIES.add(bloodSprayEntity);
                    Vec3 sourceAngle;
                    if (damageSource.getEntity() != null) {
                        sourceAngle = (damageSource.getDirectEntity() != null) ? damageSource.getDirectEntity().getLookAngle() : damageSource.getEntity().getLookAngle();
                    }
                    else {
                        sourceAngle = entity.getLookAngle();
                    }

                    double xAngle = sourceAngle.x;
                    double yAngle = (isBleedingDamage) ? -sourceAngle.y : -sourceAngle.y + Math.random();
                    double zAngle = sourceAngle.z;
                    double adjustedDamage = damageAmount * CommonConfig.bloodSprayDistance();

                    // Ensure the angles are always going where they are expected to go.
                    xAngle = (xAngle > 0) ? (xAngle - Math.random()) : (xAngle + Math.random());
                    zAngle = (zAngle > 0) ? (zAngle - Math.random()) : (zAngle + Math.random());

                    xAngle *= adjustedDamage;
                    zAngle *= adjustedDamage;

                    bloodSprayEntity.setDeltaMovement(xAngle, yAngle * 0.35, zAngle);
                    entity.level().addFreshEntity(bloodSprayEntity);

                    BloodyBitsPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> bloodSprayEntity),
                            new EntityMessage(bloodSprayEntity.getId(), entity.getId()));
                }
            }
        }
    }
}
