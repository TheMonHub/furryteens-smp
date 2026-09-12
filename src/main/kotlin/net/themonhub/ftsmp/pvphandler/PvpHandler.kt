package net.themonhub.ftsmp.pvphandler

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent.RunCommand
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent.ShowText
import net.minecraft.network.chat.Style
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.themonhub.ftsmp.FtSmpConfig
import java.util.*


object PvpHandler {

    data class DuelRequest(
        val targetId: UUID,
        val timestamp: Long
    )

    data class QueuedDuel(
        val acceptedPlayerId: UUID,
        val initialPlayerPos: Vec3,
        val initialAcceptedPlayerPos: Vec3,
        val startTime: Long
    )

    private val pvpMap: MutableMap<UUID, Int> = mutableMapOf()
    private val duelsMap: MutableMap<UUID, UUID> = mutableMapOf()
    private val duelRequests: MutableMap<UUID, DuelRequest> = mutableMapOf()
    private val duelQueue: MutableMap<UUID, QueuedDuel> = mutableMapOf()
    private val stopDuelRequests: MutableMap<UUID, Long> = mutableMapOf()

    private var server: MinecraftServer? = null

    private fun extractServer(player: Player?): MinecraftServer? {
        return (player?.level() as? net.minecraft.server.level.ServerLevel)?.server
    }

    private fun getPlayer(uuid: UUID, fallbackPlayer: Player? = null): ServerPlayer? {
        val currentServer = extractServer(fallbackPlayer) ?: server
        return currentServer?.playerList?.getPlayer(uuid)
    }

    private fun isQueued(uuid: UUID): Boolean {
        return duelQueue.containsKey(uuid) || duelQueue.values.any { it.acceptedPlayerId == uuid }
    }

    fun setInCombat(player: Player, inCombat: Boolean) {
        server = extractServer(player) ?: server
        if (inCombat) {
            val decayTime = FtSmpConfig.mainConfig.pvp.inCombatDecayTime.get()
            pvpMap[player.uuid] = decayTime * 20
            return
        }
        pvpMap.remove(player.uuid)
    }

    fun requestDuel(player: Player, target: Player) {
        server = extractServer(player) ?: server

        if (getDuel(player) != null) {
            player.sendSystemMessage(Component.literal("You are already in a duel!"))
            return
        }
        if (getDuel(target) != null) {
            player.sendSystemMessage(Component.literal("${target.name.string} is already in a duel!"))
            return
        }
        if (isQueued(player.uuid)) {
            player.sendSystemMessage(Component.literal("You are already preparing for a duel!"))
            return
        }
        if (isQueued(target.uuid)) {
            player.sendSystemMessage(Component.literal("${target.name.string} is already preparing for a duel!"))
            return
        }

        duelRequests[player.uuid] = DuelRequest(target.uuid, System.currentTimeMillis())

        player.sendSystemMessage(
            Component.literal("Duel request sent to ${target.name.string}!")
        )


        val acceptBtn: Component =
            Component.literal(" [Accept] ")
                .withStyle { style: Style? ->
                    style!!
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(
                            RunCommand("/duel_accept " + player.name.string)
                        )
                        .withHoverEvent(
                            ShowText(
                                Component.literal(
                                    "Click to accept request"
                                )
                            )
                        )
                }

        val denyBtn: Component =
            Component.literal(" [Deny] ")
                .withStyle { style: Style? ->
                    style!!
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(
                            RunCommand("/duel_reject " + player.name.string)
                        )
                        .withHoverEvent(
                            ShowText(
                                Component.literal(
                                    "Click to deny request"
                                )
                            )
                        )
                }

        target.sendSystemMessage(
            Component.literal("${player.name.string} has requested a duel with you!").append(acceptBtn).append(denyBtn)
        )
    }

    fun acceptDuel(player: Player, acceptedPlayer: Player) {
        server = extractServer(player) ?: server

        val request = duelRequests[acceptedPlayer.uuid]
        if (request == null || request.targetId != player.uuid) {
            player.sendSystemMessage(
                Component.literal("You do not have a pending duel request from ${acceptedPlayer.name.string}!")
            )
            return
        }

        val timeLimit = FtSmpConfig.mainConfig.pvpDuel.duelRequestTimeLimit.get()
        if ((System.currentTimeMillis() - request.timestamp) / 1000 > timeLimit) {
            duelRequests.remove(acceptedPlayer.uuid)
            player.sendSystemMessage(
                Component.literal("The duel request from ${acceptedPlayer.name.string} has expired!")
            )
            return
        }

        if (getDuel(player) != null) {
            player.sendSystemMessage(
                Component.literal("You are already in a duel!")
            )
            return
        }
        if (getDuel(acceptedPlayer) != null) {
            player.sendSystemMessage(
                Component.literal("${acceptedPlayer.name.string} is already in a duel!")
            )
            return
        }
        if (isQueued(player.uuid) || isQueued(acceptedPlayer.uuid)) {
            player.sendSystemMessage(
                Component.literal("A duel preparation is already in progress!")
            )
            return
        }

        duelRequests.remove(acceptedPlayer.uuid)

        val prepTime = FtSmpConfig.mainConfig.pvpDuel.duelPreparationTime.get()
        val noMoveWarning = Component.literal("Do not move for $prepTime seconds.")
        player.sendOverlayMessage(noMoveWarning)
        acceptedPlayer.sendOverlayMessage(noMoveWarning)

        player.sendSystemMessage(
            Component.literal("You have accepted a duel with ${acceptedPlayer.name.string}!")
        )
        acceptedPlayer.sendSystemMessage(
            Component.literal("${player.name.string} has accepted a duel with you!")
        )

        duelQueue[player.uuid] = QueuedDuel(
            acceptedPlayerId = acceptedPlayer.uuid,
            initialPlayerPos = player.position(),
            initialAcceptedPlayerPos = acceptedPlayer.position(),
            startTime = System.currentTimeMillis()
        )
    }

    fun rejectDuel(player: Player, rejectedPlayer: Player) {
        server = extractServer(player) ?: server

        val request = duelRequests[rejectedPlayer.uuid]
        if (request == null || request.targetId != player.uuid) {
            player.sendSystemMessage(
                Component.literal("You do not have a pending duel request from ${rejectedPlayer.name.string}!")
            )
            return
        }

        duelRequests.remove(rejectedPlayer.uuid)

        player.sendSystemMessage(
            Component.literal("You have rejected a duel with ${rejectedPlayer.name.string}!")
        )
        rejectedPlayer.sendSystemMessage(
            Component.literal("${player.name.string} has rejected a duel with you!")
        )
    }

    fun stopDuel(player: Player) {
        server = extractServer(player) ?: server

        val opponentId = duelsMap[player.uuid]
        if (opponentId == null) {
            player.sendSystemMessage(Component.literal("You are not in a duel!"))
            return
        }

        val opponent = getPlayer(opponentId, player)
        val timeLimit = FtSmpConfig.mainConfig.pvpDuel.duelRequestTimeLimit.get()
        val opponentReqTime = stopDuelRequests[opponentId]

        if (opponentReqTime != null && (System.currentTimeMillis() - opponentReqTime) / 1000 <= timeLimit) {
            // Both players agreed to stop the duel
            stopDuelRequests.remove(player.uuid)
            stopDuelRequests.remove(opponentId)
            duelsMap.remove(player.uuid)
            duelsMap.remove(opponentId)

            val duelStopMsg = Component.literal("Duel stopped by mutual agreement!")
            player.sendSystemMessage(duelStopMsg)
            player.sendOverlayMessage(duelStopMsg)
            opponent?.sendSystemMessage(duelStopMsg)
            opponent?.sendOverlayMessage(duelStopMsg)
            return
        }

        val playerReqTime = stopDuelRequests[player.uuid]
        if (playerReqTime != null && (System.currentTimeMillis() - playerReqTime) / 1000 <= timeLimit) {
            player.sendSystemMessage(
                Component.literal("You have already requested to stop the duel. Waiting for ${opponent?.name?.string ?: "your opponent"} to agree.")
            )
            return
        }

        stopDuelRequests[player.uuid] = System.currentTimeMillis()
        player.sendSystemMessage(
            Component.literal("You requested to stop the duel. Waiting for ${opponent?.name?.string ?: "your opponent"} to agree.")
        )

        val agreeBtn: Component =
            Component.literal(" [Agree] ")
                .withStyle { style: Style? ->
                    style!!
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(
                            RunCommand("/duel_stop")
                        )
                        .withHoverEvent(
                            ShowText(
                                Component.literal("Click to agree to stop the duel")
                            )
                        )
                }

        opponent?.sendSystemMessage(
            Component.literal("${player.name.string} has requested to stop the duel!").append(agreeBtn)
        )
    }

    fun getInPvpTime(player: Player): Int {
        return pvpMap[player.uuid] ?: 0
    }

    fun getDuel(player: Player): Player? {
        val opponentId = duelsMap[player.uuid] ?: return null
        return getPlayer(opponentId, player)
    }

    fun isInCombat(player: Player): Boolean {
        if (duelsMap.containsKey(player.uuid)) return true
        val combatTime = pvpMap[player.uuid]
        return combatTime != null && combatTime > 0
    }

    fun onServerTick(server: MinecraftServer? = null) {
        if (server != null) {
            this.server = server
        }
        val pvpIterator = pvpMap.iterator()
        while (pvpIterator.hasNext()) {
            val (uuid, time) = pvpIterator.next()
            val player = getPlayer(uuid)
            if (player == null) {
                pvpIterator.remove()
                continue
            }
            if (duelsMap.containsKey(uuid)) {
                continue
            }

            if (time > 1) {
                pvpMap[uuid] = time - 1
                player.sendOverlayMessage(
                    Component.literal("You are in combat! Do not log off! You will be out of combat in ${(time - 1) / 20 + 1} seconds.")
                )
            } else {
                pvpIterator.remove()
                player.sendOverlayMessage(
                    Component.literal("You are no longer in combat!")
                )
            }
        }

        val requestLimit = FtSmpConfig.mainConfig.pvpDuel.duelRequestTimeLimit.get()
        val reqIterator = duelRequests.iterator()
        val now = System.currentTimeMillis()
        while (reqIterator.hasNext()) {
            val (senderId, request) = reqIterator.next()
            if ((now - request.timestamp) / 1000 > requestLimit) {
                val sender = getPlayer(senderId)
                val target = getPlayer(request.targetId)
                val targetName = target?.name?.string ?: "the player"
                sender?.sendSystemMessage(
                    Component.literal("Your duel request to $targetName has expired!")
                )
                reqIterator.remove()
            }
        }

        val stopReqIterator = stopDuelRequests.iterator()
        while (stopReqIterator.hasNext()) {
            val (requesterId, timestamp) = stopReqIterator.next()
            if (!duelsMap.containsKey(requesterId)) {
                stopReqIterator.remove()
                continue
            }
            if ((now - timestamp) / 1000 > requestLimit) {
                val requester = getPlayer(requesterId)
                requester?.sendSystemMessage(
                    Component.literal("Your request to stop the duel has expired!")
                )
                stopReqIterator.remove()
            }
        }

        val prepLimit = FtSmpConfig.mainConfig.pvpDuel.duelPreparationTime.get()
        val queueIterator = duelQueue.iterator()
        while (queueIterator.hasNext()) {
            val (player1Id, request) = queueIterator.next()
            val player2Id = request.acceptedPlayerId

            val p1 = getPlayer(player1Id)
            val p2 = getPlayer(player2Id)

            if (p1 == null || p2 == null) {
                p1?.sendSystemMessage(Component.literal("Duel cancelled: opponent disconnected!"))
                p2?.sendSystemMessage(Component.literal("Duel cancelled: opponent disconnected!"))
                queueIterator.remove()
                continue
            }

            if (duelsMap.containsKey(player1Id)) {
                p1.sendSystemMessage(Component.literal("You are already in a duel!"))
                queueIterator.remove()
                continue
            }
            if (duelsMap.containsKey(player2Id)) {
                p1.sendSystemMessage(Component.literal("${p2.name.string} is already in a duel!"))
                queueIterator.remove()
                continue
            }

            val maxMovementSqr = 0.09
            val p1Moved = p1.position().distanceToSqr(request.initialPlayerPos) > maxMovementSqr
            val p2Moved = p2.position().distanceToSqr(request.initialAcceptedPlayerPos) > maxMovementSqr

            if (p1Moved || p2Moved) {
                val movedPlayer = if (p1Moved) p1 else p2
                val cancelMsg = Component.literal("Duel cancelled: ${movedPlayer.name.string} moved during preparation!")
                p1.sendSystemMessage(cancelMsg)
                p1.sendOverlayMessage(cancelMsg)
                p2.sendSystemMessage(cancelMsg)
                p2.sendOverlayMessage(cancelMsg)
                queueIterator.remove()
                continue
            }

            val currentPrepareTime = (now - request.startTime) / 1000
            if (currentPrepareTime >= prepLimit) {
                val go = Component.literal("GO!")
                p1.sendOverlayMessage(go)
                p2.sendOverlayMessage(go)

                p1.sendSystemMessage(Component.literal("Duel started against ${p2.name.string}!"))
                p2.sendSystemMessage(Component.literal("Duel started against ${p1.name.string}!"))

                duelsMap[player1Id] = player2Id
                duelsMap[player2Id] = player1Id
                pvpMap.remove(player1Id)
                pvpMap.remove(player2Id)
                queueIterator.remove()
            } else {
                val remainingSeconds = prepLimit - currentPrepareTime
                val countdownMsg = Component.literal("Do not move for $remainingSeconds seconds.")
                p1.sendOverlayMessage(countdownMsg)
                p2.sendOverlayMessage(countdownMsg)
            }
        }
    }

    fun onPlayerDeath(player: Player) {

        setInCombat(player, false)

        val opponentId = duelsMap.remove(player.uuid) ?: return
        duelsMap.remove(opponentId)
        stopDuelRequests.remove(player.uuid)
        stopDuelRequests.remove(opponentId)

        val duelOver = Component.literal("Duel Over!")
        player.sendSystemMessage(duelOver)
        player.sendOverlayMessage(duelOver)

        val opponent = getPlayer(opponentId, player)
        if (opponent != null) {
            setInCombat(opponent, false)
            opponent.sendSystemMessage(duelOver)
            opponent.sendOverlayMessage(duelOver)
        }
    }

    fun onPlayerLeave(player: Player) {
        val opponentId = duelsMap.remove(player.uuid)
        if (opponentId != null) {
            duelsMap.remove(opponentId)
            stopDuelRequests.remove(opponentId)
            val opponent = getPlayer(opponentId, player)
            if (opponent != null) {
                setInCombat(opponent, false)
            }
            opponent?.sendOverlayMessage(
                Component.literal("Duel Over: ${player.name.string} left the server!")
            )
        }

        stopDuelRequests.remove(player.uuid)

        duelRequests.remove(player.uuid)
        duelRequests.values.removeIf { it.targetId == player.uuid }
        duelQueue.remove(player.uuid)
        duelQueue.values.removeIf { it.acceptedPlayerId == player.uuid }

        if (!FtSmpConfig.mainConfig.pvp.combatLogPrevention.get()) {
            return
        }
        if (isInCombat(player)) {
            player.hurtServer(player.level() as ServerLevel, player.damageSources().fellOutOfWorld(), 100000F)
        }
        pvpMap.remove(player.uuid)
    }
}
