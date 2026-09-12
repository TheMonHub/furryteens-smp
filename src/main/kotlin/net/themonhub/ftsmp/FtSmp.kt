package net.themonhub.ftsmp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.themonhub.ftsmp.combatlog.CombatLog
import net.themonhub.ftsmp.pvpstatus.PvpStatus
import net.themonhub.ftsmp.attackhandler.AttackHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FtSmp : ModInitializer {
	const val MOD_ID: String = "ftsmp"

	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("I'm feeling fluffy today.")
		FtSmpConfig.init()
		AttackHandler.initialize()

		ServerTickEvents.START_LEVEL_TICK.register {
			PvpStatus.onServerTick()
		}

		ServerPlayerEvents.LEAVE.register { p0 ->
			PvpStatus.onPlayerLeave(p0)
			CombatLog.onPlayerLeave(p0)
		}

		ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
			if (entity !is Player) {
				return@register
			}
			PvpStatus.onPlayerDeath(entity)
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			dispatcher.register(Commands.literal("duel").then(Commands.argument("target", EntityArgument.player())).executes { context ->
				val player = context.source.player
				if (player == null) {
					context.source.sendFailure(Component.literal("Unable to find the player who sent the duel request"))
					return@executes 0
				}
				if (player == EntityArgument.getPlayer(context, "target")) {
					context.source.sendFailure(Component.literal("You cannot duel yourself!"))
					return@executes 0
				}

				PvpStatus.requestDuel(player, EntityArgument.getPlayer(context, "target"))

				return@executes 0
			})
			dispatcher.register(Commands.literal("duel_accept").then(Commands.argument("target", EntityArgument.player())).executes { context ->
				val player = context.source.player
				if (player == null) {
					context.source.sendFailure(Component.literal("Unable to find the player who accept the duel request"))
					return@executes 0
				}
				PvpStatus.acceptDuel(player, EntityArgument.getPlayer(context, "target"))
				return@executes 0
			})
			dispatcher.register(Commands.literal("duel_reject").then(Commands.argument("target", EntityArgument.player())).executes { context ->
				val player = context.source.player
				if (player == null) {
					context.source.sendFailure(Component.literal("Unable to find the player who reject the duel request"))
					return@executes 0
				}
				PvpStatus.rejectDuel(player, EntityArgument.getPlayer(context, "target"))
				return@executes 0
			})
		}
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
