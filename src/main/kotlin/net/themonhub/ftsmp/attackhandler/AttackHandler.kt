package net.themonhub.ftsmp.attackhandler

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.themonhub.ftsmp.FtSmpConfig
import net.themonhub.ftsmp.pvphandler.PvpHandler

object AttackHandler {
    fun initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, _ ->
            val attacker = source.entity

            val attackerPlayer = attacker as? Player
            val targetPlayer = entity as? Player

            // Get the dueling opponents (returns null if they are not in a duel)
            val attackerDueling = attackerPlayer?.let { PvpHandler.getDuel(it) }
            val targetDueling = targetPlayer?.let { PvpHandler.getDuel(it) }

            // --- 1. DUELING ISOLATION ---

            // If the attacker is in a duel, they can ONLY attack their specific duel opponent.
            // This prevents them from attacking hostile mobs or other players.
            if (attackerDueling != null && entity != attackerDueling) {
                return@register false
            }

            // If the target is in a duel, they can ONLY take entity damage from their duel opponent.
            // (We check attacker != null to still allow environmental damage like falling/drowning.
            // If you want to block environment damage too, remove `attacker != null &&`)
            if (targetDueling != null && attacker != null && attacker != targetDueling) {
                return@register false
            }

            // --- 2. SAFE ZONE LOGIC ---

            val isDuelAttack = (attackerPlayer != null && attackerDueling == entity)

            val pvpSafeZoneRadius = FtSmpConfig.mainConfig.pvpSafeZone.safeZoneRadius.get().toDouble()
            val safeZoneRange = -pvpSafeZoneRadius..pvpSafeZoneRadius
            val inSafeZone = entity.x in safeZoneRange && entity.z in safeZoneRange && FtSmpConfig.mainConfig.pvpSafeZone.safeZoneEnabled.get()

            // If a player is attacking another player inside the safe zone:
            if (inSafeZone && targetPlayer != null && attackerPlayer != null) {

                // ALLOW: They are actively dueling each other
                if (isDuelAttack) {
                    PvpHandler.setInCombat(attackerPlayer, true)
                    PvpHandler.setInCombat(entity, true)
                    return@register true
                }

                // ALLOW: The attacker is already combat tagged (currently in PvP)
                if (PvpHandler.getInPvpTime(attackerPlayer) != 0) {
                    PvpHandler.setInCombat(attackerPlayer, true)
                    PvpHandler.setInCombat(entity, true)
                    return@register true
                }

                // DENY: Normal attack in safe zone
                attackerPlayer.sendOverlayMessage(
                    Component.literal("§cYou cannot attack players in the safe zone!")
                )
                return@register false
            }

            // --- 3. GENERAL COMBAT TAGGING ---

            // If the code reaches this point, the attack is valid (outside safezone or standard PvE)
            // If a player is initiating the attack, put them in combat
            if (attackerPlayer != null && targetPlayer != null) {
                PvpHandler.setInCombat(attackerPlayer, true)
                PvpHandler.setInCombat(targetPlayer, true)
            }

            return@register true
        }
    }
}