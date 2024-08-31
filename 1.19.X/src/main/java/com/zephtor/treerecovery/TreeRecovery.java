package com.zephtor.treerecovery;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"unused", "unchecked", "MismatchedQueryAndUpdateOfCollection", "ResultOfMethodCallIgnored"})
public class TreeRecovery implements DedicatedServerModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<Item> axes = new HashSet<>();
    private final Set<Block> strippedLogs = new HashSet<>();
    private final Set<Block> strippedWoods = new HashSet<>();

    @Override
    public void onInitializeServer() {
        LOGGER.info("TreeRecovery Mod initializing");
        ServerLifecycleEvents.SERVER_STARTING.register(this::generateConfig);
        ServerLifecycleEvents.SERVER_STARTING.register(this::loadConfig);
        registerCommands();
        registerEvents();
    }

    private void generateConfig(MinecraftServer server) {
        File configFile = new File(server.getRunDirectory(), "TreeRecovery/config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                Files.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Generated default config.yml at {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not find default config.yml in resources", e);
            }
        }
    }

    private void loadConfig(MinecraftServer server) {
        LOGGER.info("Loading configuration...");
        File configFile = new File(server.getRunDirectory(), "TreeRecovery/config.yml");
        if (configFile.exists()) {
            try {
                Gson gson = new Gson();
                Config config = gson.fromJson(Files.newBufferedReader(configFile.toPath()), Config.class);
                axes.clear();
                for (String id : config.axes) {
                    axes.add(Registries.ITEM.get(new Identifier(id)));
                }
                strippedLogs.clear();
                for (String id : config.strippedLogs) {
                    strippedLogs.add(Registries.BLOCK.get(new Identifier(id)));
                }
                strippedWoods.clear();
                for (String id : config.strippedWoods) {
                    strippedWoods.add(Registries.BLOCK.get(new Identifier(id)));
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load config", e);
            }
        } else {
            LOGGER.warn("Config file not found, using defaults.");
            setDefaultConfig();
        }
    }

    private void setDefaultConfig() {
        axes.addAll(List.of(
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE
        ));
        strippedLogs.addAll(List.of(
                Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG
        ));
        strippedWoods.addAll(List.of(
                Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD,
                Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD
        ));
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("treerecovery")
                .then(CommandManager.literal("reload")
                        .executes(this::reloadConfig))
                .then(CommandManager.literal("version")
                        .executes(this::showVersion))
        ));
    }

    private int reloadConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayer();
        if (player != null && source.hasPermissionLevel(4)) {
            loadConfig(source.getServer());
            player.sendMessage(Text.of("TreeRecovery configuration reloaded."), false);
        } else {
            source.sendError(Text.of("You do not have permission to use this command."));
        }
        return 1;
    }

    private int showVersion(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(Text.of("TreeRecovery Mod Version 1.0.0"), false);
        return 1;
    }

    private void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand == Hand.MAIN_HAND && hitResult instanceof BlockHitResult) {
                BlockPos blockPos = hitResult.getBlockPos();
                BlockState blockState = world.getBlockState(blockPos);
                if (isStrippedLog(blockState) || isStrippedWood(blockState)) {
                    ItemStack itemStack = player.getStackInHand(hand);
                    if (isAxe(itemStack.getItem()) && isAllowedAxe(itemStack.getItem())) {
                        BlockState newBlockState = getNormalType(blockState);
                        BlockState updatedBlockState = copyBlockStateProperties(blockState, newBlockState);
                        world.setBlockState(blockPos, updatedBlockState);
                        damageItem(player, itemStack);
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private <T extends Comparable<T>> BlockState copyBlockStateProperties(BlockState fromState, BlockState toState) {
        BlockState newState = toState;
        for (Property<?> property : fromState.getProperties()) {
            newState = with(newState, property, fromState.get(property));
        }
        return newState;
    }

    private <T extends Comparable<T>, V extends T> BlockState with(BlockState state, Property<T> property, Comparable<?> value) {
        return state.with(property, (V) value);
    }

    private void damageItem(PlayerEntity player, ItemStack itemStack) {
        itemStack.damage(1, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
        if (itemStack.getDamage() >= itemStack.getMaxDamage()) {
            itemStack.decrement(1); // 耐久値が最大になった場合にアイテムを破壊する
        }
    }

    private boolean isStrippedLog(BlockState blockState) {
        return strippedLogs.contains(blockState.getBlock());
    }

    private boolean isStrippedWood(BlockState blockState) {
        return strippedWoods.contains(blockState.getBlock());
    }

    private boolean isAxe(Item item) {
        return axes.contains(item);
    }

    private boolean isAllowedAxe(Item item) {
        return isAxe(item);
    }

    private BlockState getNormalType(BlockState blockState) {
        return switch (getStrippedBlockType(blockState.getBlock())) {
            case STRIPPED_OAK_LOG -> Blocks.OAK_LOG.getDefaultState();
            case STRIPPED_SPRUCE_LOG -> Blocks.SPRUCE_LOG.getDefaultState();
            case STRIPPED_BIRCH_LOG -> Blocks.BIRCH_LOG.getDefaultState();
            case STRIPPED_JUNGLE_LOG -> Blocks.JUNGLE_LOG.getDefaultState();
            case STRIPPED_ACACIA_LOG -> Blocks.ACACIA_LOG.getDefaultState();
            case STRIPPED_DARK_OAK_LOG -> Blocks.DARK_OAK_LOG.getDefaultState();
            case STRIPPED_OAK_WOOD -> Blocks.OAK_WOOD.getDefaultState();
            case STRIPPED_SPRUCE_WOOD -> Blocks.SPRUCE_WOOD.getDefaultState();
            case STRIPPED_BIRCH_WOOD -> Blocks.BIRCH_WOOD.getDefaultState();
            case STRIPPED_JUNGLE_WOOD -> Blocks.JUNGLE_WOOD.getDefaultState();
            case STRIPPED_ACACIA_WOOD -> Blocks.ACACIA_WOOD.getDefaultState();
            case STRIPPED_DARK_OAK_WOOD -> Blocks.DARK_OAK_WOOD.getDefaultState();
            default -> blockState;
        };
    }

    private StrippedBlockType getStrippedBlockType(Block block) {
        if (block == Blocks.STRIPPED_OAK_LOG) return StrippedBlockType.STRIPPED_OAK_LOG;
        if (block == Blocks.STRIPPED_SPRUCE_LOG) return StrippedBlockType.STRIPPED_SPRUCE_LOG;
        if (block == Blocks.STRIPPED_BIRCH_LOG) return StrippedBlockType.STRIPPED_BIRCH_LOG;
        if (block == Blocks.STRIPPED_JUNGLE_LOG) return StrippedBlockType.STRIPPED_JUNGLE_LOG;
        if (block == Blocks.STRIPPED_ACACIA_LOG) return StrippedBlockType.STRIPPED_ACACIA_LOG;
        if (block == Blocks.STRIPPED_DARK_OAK_LOG) return StrippedBlockType.STRIPPED_DARK_OAK_LOG;
        if (block == Blocks.STRIPPED_OAK_WOOD) return StrippedBlockType.STRIPPED_OAK_WOOD;
        if (block == Blocks.STRIPPED_SPRUCE_WOOD) return StrippedBlockType.STRIPPED_SPRUCE_WOOD;
        if (block == Blocks.STRIPPED_BIRCH_WOOD) return StrippedBlockType.STRIPPED_BIRCH_WOOD;
        if (block == Blocks.STRIPPED_JUNGLE_WOOD) return StrippedBlockType.STRIPPED_JUNGLE_WOOD;
        if (block == Blocks.STRIPPED_ACACIA_WOOD) return StrippedBlockType.STRIPPED_ACACIA_WOOD;
        if (block == Blocks.STRIPPED_DARK_OAK_WOOD) return StrippedBlockType.STRIPPED_DARK_OAK_WOOD;
        return StrippedBlockType.UNKNOWN;
    }

    private enum StrippedBlockType {
        STRIPPED_OAK_LOG,
        STRIPPED_SPRUCE_LOG,
        STRIPPED_BIRCH_LOG,
        STRIPPED_JUNGLE_LOG,
        STRIPPED_ACACIA_LOG,
        STRIPPED_DARK_OAK_LOG,
        STRIPPED_OAK_WOOD,
        STRIPPED_SPRUCE_WOOD,
        STRIPPED_BIRCH_WOOD,
        STRIPPED_JUNGLE_WOOD,
        STRIPPED_ACACIA_WOOD,
        STRIPPED_DARK_OAK_WOOD,
        UNKNOWN
    }

    private static class Config {
        List<String> axes;
        List<String> strippedLogs;
        List<String> strippedWoods;
    }
}