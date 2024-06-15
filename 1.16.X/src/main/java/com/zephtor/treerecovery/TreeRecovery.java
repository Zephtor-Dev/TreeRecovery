package com.zephtor.treerecovery;

import com.google.gson.Gson;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
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

@SuppressWarnings("unused")
public class TreeRecovery implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<Item> axes = new HashSet<>();
    private final Set<Block> strippedLogs = new HashSet<>();
    private final Set<Block> strippedWoods = new HashSet<>();

    @Override
    public void onInitialize() {
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
                    axes.add(Registry.ITEM.get(new Identifier(id)));
                }
                strippedLogs.clear();
                for (String id : config.strippedLogs) {
                    strippedLogs.add(Registry.BLOCK.get(new Identifier(id)));
                }
                strippedWoods.clear();
                for (String id : config.strippedWoods) {
                    strippedWoods.add(Registry.BLOCK.get(new Identifier(id)));
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
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
        ));
        strippedLogs.addAll(List.of(
                Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_WARPED_STEM, Blocks.STRIPPED_CRIMSON_STEM
        ));
        strippedWoods.addAll(List.of(
                Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD,
                Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD,
                Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE
        ));
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(CommandManager.literal("treerecovery")
                .then(CommandManager.literal("reload").executes(this::reloadConfig))
                .then(CommandManager.literal("version").executes(this::showVersion))
        ));
    }

    private int reloadConfig(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        PlayerEntity player = source.getPlayer();
        if (player != null && source.hasPermissionLevel(4)) {
            loadConfig(source.getMinecraftServer());
            player.sendMessage(new LiteralText("TreeRecovery configuration reloaded."), false);
        } else {
            source.sendError(new LiteralText("You do not have permission to use this command."));
        }
        return 1;
    }

    private int showVersion(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("TreeRecovery Mod Version 1.0.0"), false);
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

    @SuppressWarnings("unchecked")
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
        if (blockState.getBlock() == Blocks.STRIPPED_OAK_LOG) return Blocks.OAK_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_SPRUCE_LOG) return Blocks.SPRUCE_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_BIRCH_LOG) return Blocks.BIRCH_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_JUNGLE_LOG) return Blocks.JUNGLE_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_ACACIA_LOG) return Blocks.ACACIA_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_DARK_OAK_LOG) return Blocks.DARK_OAK_LOG.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_OAK_WOOD) return Blocks.OAK_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_SPRUCE_WOOD) return Blocks.SPRUCE_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_BIRCH_WOOD) return Blocks.BIRCH_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_JUNGLE_WOOD) return Blocks.JUNGLE_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_ACACIA_WOOD) return Blocks.ACACIA_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_DARK_OAK_WOOD) return Blocks.DARK_OAK_WOOD.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_WARPED_STEM) return Blocks.WARPED_STEM.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_CRIMSON_STEM) return Blocks.CRIMSON_STEM.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_WARPED_HYPHAE) return Blocks.WARPED_HYPHAE.getDefaultState();
        if (blockState.getBlock() == Blocks.STRIPPED_CRIMSON_HYPHAE) return Blocks.CRIMSON_HYPHAE.getDefaultState();
        return blockState;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class Config {
        List<String> axes;
        List<String> strippedLogs;
        List<String> strippedWoods;
    }
}
