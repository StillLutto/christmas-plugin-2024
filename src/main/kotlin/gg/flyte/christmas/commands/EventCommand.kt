package gg.flyte.christmas.commands

import dev.shreyasayyengar.menuapi.menu.MenuItem
import dev.shreyasayyengar.menuapi.menu.StandardMenu
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donation.DonateEvent
import gg.flyte.christmas.minigame.engine.GameConfig
import gg.flyte.christmas.npc.WorldNPC
import gg.flyte.christmas.util.colourise
import gg.flyte.christmas.util.eventController
import gg.flyte.christmas.util.style
import gg.flyte.christmas.util.toLegacyString
import gg.flyte.twilight.extension.playSound
import gg.flyte.twilight.scheduler.async
import gg.flyte.twilight.scheduler.sync
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

/**
 * Miscellaneous commands to manage the event.
 */
@Suppress("unused") // power of lamp!
class EventCommand(val menu: StandardMenu = StandardMenu("&c☃ ᴇᴠᴇɴᴛ ᴍᴇɴᴜ!".colourise(), 54)) {
    private val availableGames = GameConfig.entries
    private var selectedIndex = -1
    private var modifyingGame: UUID? = null

    init {
        menu.setItem(13, setGameSwitcher())

        menu.setItem(
            21, MenuItem(Material.PAINTING)
                .setName("&cᴛᴀᴋᴇ ᴀ ѕᴄʀᴇᴇɴɪᴇ!".colourise())
                .setEnchantmentGlint(true)
                .closeWhenClicked(true)
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    // TODO
                    whoClicked.sendMessage("<red>ᴄᴏᴍɪɴɢ ѕᴏᴏɴ!".style())
                }
        )
        menu.setItem(
            23, MenuItem(Material.ENDER_PEARL)
                .setName("&cᴛᴇʟᴇᴘᴏʀᴛ ᴇᴠᴇʀʏᴏɴᴇ ᴛᴏ ᴍᴇ!".colourise())
                .closeWhenClicked(true)
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    Bukkit.getOnlinePlayers().forEach { it.teleport(whoClicked) }
                    whoClicked.sendMessage("<green>ᴛᴇʟᴇᴘᴏʀᴛᴇᴅ ᴀʟʟ ᴘʟᴀʏᴇʀѕ ᴛᴏ ʏᴏᴜ!".style())
                    whoClicked.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
                }
        )

        menu.setItem(42, setEndGameButton())

        menu.onClose { whoClosed, inventory, event ->
            if (whoClosed.uniqueId != modifyingGame) return@onClose // not the one who interacted with the game switcher

            modifyingGame = null
            eventController().setMiniGame(availableGames[selectedIndex])
            eventController().sidebarManager.update()
            whoClosed.sendMessage("<grey>ѕᴇʟᴇᴄᴛᴇᴅ ɢᴀᴍᴇ: <0>".style(availableGames[selectedIndex].displayName))
            whoClosed.playSound(Sound.UI_BUTTON_CLICK)
        }
    }

    /**
     * Opens the event panel for the player, provided that they have the
     * permission `event.panel`.
     *
     * @param sender The player who executed the command.
     */
    @Command("event")
    @CommandPermission("event.panel")
    fun openEventPanel(sender: Player) {
        menu.setItem(42, setEndGameButton())
        menu.open(true, sender)
    }

    /**
     * Opt out of the event, or opt back in if the player is already opted out.
     *
     * Players who opt out will not be marked as an active player when minigames start,
     * and instead will teleport them to a viewing area immediately.
     *
     * @param sender The player who executed the command.
     */
    @Command("event optout")
    @CommandPermission("event.optout")
    fun optOut(sender: Player) {
        val remove = eventController().optOut.remove(sender.uniqueId)
        if (remove) {
            sender.sendMessage("<green>ʏᴏᴜ ʜᴀᴠᴇ ᴏᴘᴛᴇᴅ ʙᴀᴄᴋ ɪɴᴛᴏ ᴛʜᴇ ᴇᴠᴇɴᴛ!".style())
        } else {
            eventController().optOut.add(sender.uniqueId)
            sender.sendMessage("<red>ʏᴏᴜ ʜᴀᴠᴇ ᴏᴘᴛᴇᴅ ᴏᴜᴛ ᴏꜰ ᴛʜᴇ ᴇᴠᴇɴᴛ!".style())
        }

        sender.playSound(Sound.UI_BUTTON_CLICK)
    }

    /**
     * Loads the most recent points data from the `config.yml`.
     *
     * @param sender The player who executed the command.
     */
    @Command("event DANGER-load-crash")
    @CommandPermission("event.loadcrash")
    fun loadCrash(sender: CommandSender) {
        async {
            eventController().points.clear()
            ChristmasEventPlugin.INSTANCE.config.getConfigurationSection("points")?.getKeys(false)?.forEach {
                eventController().points[UUID.fromString(it)] = ChristmasEventPlugin.INSTANCE.config.getInt("points.$it")
                sync {
                    eventController().sidebarManager.update()
                    WorldNPC.refreshPodium()
                    sender.sendMessage("<green>ʟᴏᴀᴅᴇᴅ ᴄʀᴀѕʜ ᴅᴀᴛᴀ! ʏᴏᴜʀ ѕᴄᴏʀᴇʙᴏᴀʀᴅ ѕʜᴏᴜʟᴅ ɴᴏᴡ ѕʜᴏᴡ ᴛʜᴇ ᴍᴏѕᴛ ʀᴇᴄᴇɴᴛ ѕᴇʀɪᴀʟɪѕᴇᴅ ᴅᴀᴛᴀ!".style())
                }
            }
        }
    }

    @Command("event mock-donation-now <amount>")
    @CommandPermission("event.mockdonation")
    fun mockDonation(sender: Player, amount: Double) {
        var donationEvent = DonateEvent(null, null, null, amount.toString(), "USD", "mockDonationId")
        Bukkit.getPluginManager().callEvent(donationEvent)
    }

    private fun setGameSwitcher(): MenuItem {
        val menuItem = MenuItem(Material.STRUCTURE_VOID).apply {
            setName("&b&lSelect Game:".colourise())
            updateRotatingItem(this) // initial lore setup
            onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                inventoryClickEvent.isCancelled = true

                if (modifyingGame != null && modifyingGame != whoClicked.uniqueId) {
                    whoClicked.closeInventory()
                    whoClicked.sendMessage("<red>ѕᴏᴍᴇᴏɴᴇ ᴇʟѕᴇ ɪѕ ᴄᴜʀʀᴇɴᴛʟʏ ᴍᴏᴅɪꜰʏɪɴɢ ᴛʜᴇ ɢᴀᴍᴇ!".style())
                    whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                    return@onClick
                }

                modifyingGame = whoClicked.uniqueId

                if (clickType.name.lowercase().contains("left")) {
                    selectedIndex = (selectedIndex + 1) % availableGames.size // cycle around
                } else if (clickType.name.lowercase().contains("right")) {
                    selectedIndex = if (selectedIndex == -1 || selectedIndex == 0) availableGames.size - 1 else selectedIndex - 1
                }

                this.itemStack = availableGames[selectedIndex].menuItem
                setName("&b&lSelect Game:".colourise())
                updateRotatingItem(this)

                menu.setItem(13, this)
                whoClicked.playSound(Sound.UI_BUTTON_CLICK)
            }
        }

        return menuItem
    }

    private fun setEndGameButton(): MenuItem {
        return MenuItem(Material.RED_CONCRETE)
            .setName(
                "<red>ᴋɪʟʟ ᴄᴜʀʀᴇɴᴛ ɢᴀᴍᴇ: <0>".style(eventController().currentGame?.gameConfig?.displayName ?: "ɴᴏɴᴇ".style()).toLegacyString()
                    .colourise()
            )
            .setLore(
                "",
                "&cᴛʜɪѕ ᴡɪʟʟ ꜰᴏʀᴄᴇ ǫᴜɪᴛ ᴛʜᴇ ᴄᴜʀʀᴇɴᴛ ɢᴀᴍᴇ".colourise(),
                "&cᴀɴᴅ ᴛᴇʟᴇᴘᴏʀᴛ ᴀʟʟ ᴘʟᴀʏᴇʀѕ ʙᴀᴄᴋ ᴛᴏ ᴛʜᴇ ʟᴏʙʙʏ.".colourise(),
            )
            .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                if (eventController().currentGame == null) {
                    whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                    whoClicked.sendMessage("<red>ɴᴏ ɢᴀᴍᴇ ɪѕ ᴄᴜʀʀᴇɴᴛʟʏ ʀᴜɴɴɪɴɢ!".style())
                    return@onClick
                }

                eventController().currentGame!!.endGame()
                whoClicked.sendMessage("<red>ɢᴀᴍᴇ ᴛᴇʀᴍɪɴᴀᴛᴇᴅ!".style())
                whoClicked.playSound(Sound.ENTITY_GENERIC_EXPLODE)
                eventController().sidebarManager.update()
            }
    }

    private fun updateRotatingItem(menuItem: MenuItem) {
        val lore = mutableListOf<String>()

        for (index in availableGames.indices) {
            val game = availableGames[index]
            val loreLine = if (index == selectedIndex) {
                "<white><bold>» <0> <white><bold>«".style(game.displayName).toLegacyString().colourise()
            } else game.displayName.toLegacyString().colourise()

            lore.add(loreLine)
        }

        menuItem.setLore(lore)

        if (selectedIndex == -1) return

        menu.setItem(
            38, MenuItem(Material.LIME_CONCRETE)
                .setName(availableGames[selectedIndex].displayName.toLegacyString().colourise())
                .setLore(
                    "",
                    "&aᴛʜɪѕ ᴡɪʟʟ ʙᴇɢɪɴ ᴛʜᴇ ᴄᴏᴜɴᴛᴅᴏᴡɴ".colourise(),
                    "&cɪᴍᴍᴇᴅɪᴀᴛᴇʟʏ &aᴀɴᴅ ᴘʀᴇᴘᴀʀᴇ ᴛʜᴇ ᴘʟᴀʏᴇʀѕ".colourise(),
                    "",
                    "&eɪꜰ ʏᴏᴜ ᴅᴏ ɴᴏᴛ ᴡᴀɴᴛ ᴛᴏ ѕᴛᴀʀᴛ".colourise(),
                    "&eʏᴇᴛ, ѕɪᴍᴘʟʏ ᴇxɪᴛ ᴛʜɪѕ ᴍᴇɴᴜ. ".colourise(),
                    "&eᴛʜᴇ ɢᴀᴍᴇ ʜᴀѕ ᴀʟʀᴇᴀᴅʏ ʙᴇᴇɴ ѕᴇᴛ.".colourise()
                )
                .onClick { whoClicked, itemStack, clickType, inventoryClickEvent ->
                    whoClicked.closeInventory()
                    if (eventController().currentGame == null) {
                        whoClicked.playSound(Sound.ENTITY_VILLAGER_NO)
                        whoClicked.sendMessage("<red>ɴᴏ ɢᴀᴍᴇ ʜᴀѕ ʙᴇᴇɴ ѕᴇʟᴇᴄᴛᴇᴅ!".style())
                        return@onClick
                    }

                    eventController().prepareStart()
                    whoClicked.playSound(Sound.ENTITY_PLAYER_LEVELUP)
                    whoClicked.sendMessage("<green>ɢᴀᴍᴇ ѕᴛᴀʀᴛɪɴɢ! ᴘʟᴇᴀѕᴇ ᴡᴀɪᴛ...".style())

                    selectedIndex = -1
                    menu.setItem(13, setGameSwitcher())
                    menu.removeItem(38)
                }
        )
    }
}
