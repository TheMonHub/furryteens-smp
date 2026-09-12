package net.themonhub.ftsmp.attackhandler

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.themonhub.ftsmp.FtSmpConfig
import net.themonhub.ftsmp.pvpstatus.PvpStatus

object AttackHandler {
    fun initialize() {
        AttackEntityCallback.EVENT.register { player, _, _, entity, _ ->
            if (entity is Player) {
                PvpStatus.setInCombat(player, true)
                PvpStatus.setInCombat(entity, true)
            }

            val currentlyDueling = PvpStatus.getDuel(player)
            if (currentlyDueling == entity) {
                return@register InteractionResult.PASS
            }
            // Hitting hostile mobs or player and is dueling with someone else
            if (currentlyDueling != null && (entity is Monster || entity is Player)) {
                return@register InteractionResult.FAIL
            }

            val pvpSafeZoneRadius = FtSmpConfig.mainConfig.pvpSafeZone.safeZoneRadius.get().toDouble()
            val safeZoneRange = -pvpSafeZoneRadius ..pvpSafeZoneRadius
            if (entity.x in safeZoneRange && entity.z in safeZoneRange && FtSmpConfig.mainConfig.pvpSafeZone.safeZoneEnabled.get()) {
                if (PvpStatus.getDuel(player) == entity) {
                    return@register InteractionResult.PASS
                }
                if (PvpStatus.getInPvpTime(player) != 0) {
                    return@register InteractionResult.PASS
                }

                player.sendOverlayMessage(
                    Component.literal("§cYou cannot attack player in the safe zone!")
                )
                return@register InteractionResult.FAIL
            }
            InteractionResult.PASS
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, _ ->
            if (entity !is Player) {
                return@register true
            }
            val currentlyDueling = PvpStatus.getDuel(entity) ?: return@register true
            return@register currentlyDueling == source
        }
    }
}