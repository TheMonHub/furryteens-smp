package net.themonhub.ftsmp.combatlog

import net.minecraft.server.level.ServerPlayer
import net.themonhub.ftsmp.pvpstatus.PvpStatus

object CombatLog {
    fun onPlayerLeave(p0: ServerPlayer) {
        if (PvpStatus.isInCombat(p0)) {
            p0.kill(p0.level())
        }
    }
}