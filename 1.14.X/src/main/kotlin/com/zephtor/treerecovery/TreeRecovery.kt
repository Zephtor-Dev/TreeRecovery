package com.zephtor.treerecovery

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.state.property.Property
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Files
import com.google.gson.Gson
import net.minecraft.block.Block
import net.minecraft.server.MinecraftServer
import java.nio.file.StandardCopyOption

@Suppress("unused")
object TreeRecovery : ModInitializer {

    private val LOGGER = LogManager.getLogger()
    private val axes = mutableSetOf<Item>()
    private val strippedLogs = mutableSetOf<Block>()
    private val strippedWoods = mutableSetOf<Block>()

    override fun onInitialize() {
        LOGGER.info("TreeRecovery Mod initializing")
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            generateConfig(server)
            loadConfig(server)
        }
        registerCommands()
        registerEvents()
    }

    private fun generateConfig(server: MinecraftServer) {
        val resourcePath = "config.yml"
        val outputPath = File(server.runDirectory, "/TreeRecovery/config.yml")
        if (!outputPath.exists()) {
            outputPath.parentFile.mkdirs()
            val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
            if (inputStream != null) {
                Files.copy(inputStream, outputPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                LOGGER.info("Generated default config.yml at ${outputPath.absolutePath}")
            } else {
                LOGGER.error("Could not find default config.yml in resources")
            }
        }
    }

    private fun loadConfig(server: MinecraftServer) {
        LOGGER.info("Loading configuration...")
        val configFile = File(server.runDirectory, "/TreeRecovery/config.yml")
        if (configFile.exists()) {
            val gson = Gson()
            val config = gson.fromJson(Files.newBufferedReader(configFile.toPath()), Config::class.java)
            axes.clear()
            axes.addAll(config.axes.map { Registry.ITEM.get(Identifier(it)) })

            strippedLogs.clear()
            strippedLogs.addAll(config.strippedLogs.map { Registry.BLOCK.get(Identifier(it)) })

            strippedWoods.clear()
            strippedWoods.addAll(config.strippedWoods.map { Registry.BLOCK.get(Identifier(it)) })
        } else {
            LOGGER.warn("Config file not found, using defaults.")
            axes.addAll(listOf(
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
            ))

            strippedLogs.addAll(listOf(
                Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG
            ))

            strippedWoods.addAll(listOf(
                Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD,
                Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD
            ))
        }
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("treerecovery")
                .then(literal("reload").executes { context: CommandContext<ServerCommandSource> ->
                    val player = context.source.player
                    if (player != null && context.source.hasPermissionLevel(4)) {
                        loadConfig(context.source.minecraftServer)
                        player.sendMessage(LiteralText("TreeRecovery configuration reloaded."))
                    } else {
                        context.source.sendError(LiteralText("You do not have permission to use this command."))
                    }
                    return@executes 1
                })
                .then(literal("version").executes { context: CommandContext<ServerCommandSource> ->
                    context.source.sendFeedback(LiteralText("TreeRecovery Mod Version 1.0.0"), false)
                    return@executes 1
                }))
        }
    }

    private fun registerEvents() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            if (hand == Hand.MAIN_HAND && hitResult is BlockHitResult) {
                val blockPos = hitResult.blockPos
                val blockState = world.getBlockState(blockPos)
                if (isStrippedLog(blockState) || isStrippedWood(blockState)) {
                    val itemStack = player.getStackInHand(hand)
                    if (isAxe(itemStack.item) && isAllowedAxe(itemStack.item)) {
                        val newBlockState = getNormalType(blockState)
                        val oldProperties = blockState.properties
                        var updatedBlockState = newBlockState
                        for (property in oldProperties) {
                            updatedBlockState = updateProperty(updatedBlockState, property, blockState.get(property))
                        }
                        world.setBlockState(blockPos, updatedBlockState)
                        damageItem(player, itemStack)
                        return@register ActionResult.SUCCESS
                    }
                }
            }
            ActionResult.PASS
        }
    }

    private fun <T : Comparable<T>> updateProperty(state: BlockState, property: Property<T>, value: Comparable<*>): BlockState {
        return state.with(property, property.valueType.cast(value))
    }

    private fun damageItem(player: PlayerEntity, itemStack: ItemStack) {
        itemStack.damage(1, player) { _ ->
            player.sendToolBreakStatus(player.activeHand)
        }
        if (itemStack.damage >= itemStack.maxDamage) {
            itemStack.decrement(1) // 耐久値が最大になった場合にアイテムを破壊する
        }
    }


    private fun isStrippedLog(blockState: BlockState): Boolean {
        return strippedLogs.contains(blockState.block)
    }

    private fun isStrippedWood(blockState: BlockState): Boolean {
        return strippedWoods.contains(blockState.block)
    }

    private fun isAxe(item: Item): Boolean {
        return axes.contains(item)
    }

    private fun isAllowedAxe(item: Item): Boolean {
        return isAxe(item)
    }

    private fun getNormalType(blockState: BlockState): BlockState {
        return when (blockState.block) {
            Blocks.STRIPPED_OAK_LOG -> Blocks.OAK_LOG.defaultState
            Blocks.STRIPPED_SPRUCE_LOG -> Blocks.SPRUCE_LOG.defaultState
            Blocks.STRIPPED_BIRCH_LOG -> Blocks.BIRCH_LOG.defaultState
            Blocks.STRIPPED_JUNGLE_LOG -> Blocks.JUNGLE_LOG.defaultState
            Blocks.STRIPPED_ACACIA_LOG -> Blocks.ACACIA_LOG.defaultState
            Blocks.STRIPPED_DARK_OAK_LOG -> Blocks.DARK_OAK_LOG.defaultState
            Blocks.STRIPPED_OAK_WOOD -> Blocks.OAK_WOOD.defaultState
            Blocks.STRIPPED_SPRUCE_WOOD -> Blocks.SPRUCE_WOOD.defaultState
            Blocks.STRIPPED_BIRCH_WOOD -> Blocks.BIRCH_WOOD.defaultState
            Blocks.STRIPPED_JUNGLE_WOOD -> Blocks.JUNGLE_WOOD.defaultState
            Blocks.STRIPPED_ACACIA_WOOD -> Blocks.ACACIA_WOOD.defaultState
            Blocks.STRIPPED_DARK_OAK_WOOD -> Blocks.DARK_OAK_WOOD.defaultState
            else -> blockState
        }
    }

    data class Config(
        val axes: List<String>,
        val strippedLogs: List<String>,
        val strippedWoods: List<String>
    )
}
