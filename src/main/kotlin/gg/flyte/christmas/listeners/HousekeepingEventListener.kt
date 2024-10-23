package gg.flyte.christmas.listeners

import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.util.asComponent
import gg.flyte.twilight.event.event
import gg.flyte.twilight.extension.RemoteFile
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.async
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

class HousekeepingEventListener : Listener {
    init {
        event<ServerListPingEvent> {
            motd("TODO!".asComponent()) // TODO add motd
        }

        event<AsyncPlayerPreLoginEvent> {
            // TODO remove if no player limit \o/
        }

        event<PlayerJoinEvent>(priority = EventPriority.LOWEST) {
            joinMessage(null)

            player.apply {
                async {
                    RemoteFile("https://github.com/flytegg/ls-christmas-rp/releases/latest/download/RP.zip").apply { // TODO change URL/configure pack
//                    println("RP Hash = $hash")
//                    setResourcePack(url, hash, true)
                    }
                }

                playSound(Sound.ENTITY_PLAYER_LEVELUP)

                inventory.clear()
                inventory.helmet = applyChristmasHat((1..3).random())

                ChristmasEventPlugin.getInstance().eventController.onPlayerJoin(this)
                ChristmasEventPlugin.getInstance().eventController.songPlayer?.addPlayer(this)
                ChristmasEventPlugin.getInstance().worldNPCs.forEach { it.spawnFor(this) }
            }
            ChristmasEventPlugin.getInstance().eventController.points.putIfAbsent(player.uniqueId, 0)
        }

        event<PlayerQuitEvent> {
            quitMessage(null)
            ChristmasEventPlugin.getInstance().eventController.onPlayerQuit(player)
        }

        event<EntityCombustEvent> { if (entity is Player) isCancelled = true }

        event<PlayerSwapHandItemsEvent> { isCancelled = true }

        event<PlayerResourcePackStatusEvent> {
            if (status == PlayerResourcePackStatusEvent.Status.ACCEPTED || status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) return@event
//            player.kick("&cYou &f&nmust&c accept the resource pack to play on this server!".asComponent()) // TODO uncomment when pack works
        }

        event<EntityDamageEvent> { isCancelled = true /* TODO examine later*/ }

        event<FoodLevelChangeEvent> { isCancelled = true }

        event<InventoryClickEvent> {
            if (clickedInventory !is PlayerInventory) return@event
            isCancelled = slotType == InventoryType.SlotType.ARMOR
        }
    }

    private fun applyChristmasHat(modelData: Int): ItemStack {
        val map = mapOf(
            1 to NamedTextColor.RED,
            2 to NamedTextColor.GREEN,
            3 to NamedTextColor.BLUE
        )

        return ItemStack(Material.LEATHER).apply {
            itemMeta = itemMeta.apply {
                displayName(text("Christmas Hat", map[modelData]))
                setCustomModelData(modelData)
            }
        }
    }
}

// TODO prevent players from exiting spectator mode when preview.