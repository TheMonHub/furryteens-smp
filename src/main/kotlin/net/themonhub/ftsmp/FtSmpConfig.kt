package net.themonhub.ftsmp

import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.FileType
import me.fzzyhmstrs.fzzy_config.api.SaveType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt
import net.minecraft.resources.Identifier
import net.themonhub.ftsmp.FtSmp.MOD_ID

@Version(1)
class FtSmpMainConfig : Config(Identifier.fromNamespaceAndPath(MOD_ID, "config")) {
    var pvp = Pvp()
    class Pvp :
        ConfigSection() {
        var inCombatDecayTime = ValidatedInt(
            60,
            3600,
            1
        )

        var combatLogPrevention = ValidatedBoolean(true)
    }

    var pvpSafeZone = PvpSafeZone()
    class PvpSafeZone :
        ConfigSection() {
        var safeZoneEnabled = ValidatedBoolean(true)
        var safeZoneRadius = ValidatedInt(
            300,
            5000,
            0
        )
    }

    var pvpDuel = PvpDuel()
    class PvpDuel :
        ConfigSection() {
        var duelRequestTimeLimit = ValidatedInt(
            60,
            600,
            1
        )
        var duelPreparationTime = ValidatedInt(
            5,
            10,
            2
        )
    }

    override fun defaultPermLevel(): Int {
        return 4
    }

    override fun fileType(): FileType {
        return FileType.TOML
    }

    override fun saveType(): SaveType {
        return SaveType.SEPARATE
    }
}

object FtSmpConfig {
    var mainConfig = ConfigApi.registerAndLoadConfig(::FtSmpMainConfig)
}