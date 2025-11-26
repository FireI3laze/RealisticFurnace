package com.fireblaze.realistic_furnace.commands;

import com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

import static com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry.ITEM_FUEL;
import static com.fireblaze.realistic_furnace.fuel.FurnaceFuelRegistry.save;

public class FuelCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("realistic_furnace")
                .then(Commands.literal("fuel")
                    .then(Commands.literal("add")
                            .then(Commands.argument("nbt", BoolArgumentType.bool())
                                    .then(Commands.argument("burnTime", IntegerArgumentType.integer(1))
                                            .then(Commands.argument("heatStrength", FloatArgumentType.floatArg(0))
                                                    .then(Commands.argument("maxHeat", IntegerArgumentType.integer(1))
                                                            .executes(ctx -> addFuel(ctx.getSource(),
                                                                    BoolArgumentType.getBool(ctx, "nbt"),
                                                                    IntegerArgumentType.getInteger(ctx, "burnTime"),
                                                                    FloatArgumentType.getFloat(ctx, "heatStrength"),
                                                                    IntegerArgumentType.getInteger(ctx, "maxHeat")))
                                                    )
                                                    .executes(ctx -> addFuel(ctx.getSource(),
                                                            BoolArgumentType.getBool(ctx, "nbt"),
                                                            IntegerArgumentType.getInteger(ctx, "burnTime"),
                                                            FloatArgumentType.getFloat(ctx, "heatStrength"),
                                                            null))
                                            )
                                    )
                            )
                    )
                    .then(Commands.literal("remove")
                            .then(Commands.argument("itemid", StringArgumentType.string())
                                    .executes(ctx -> removeFuel(ctx.getSource(), StringArgumentType.getString(ctx, "itemid")))
                            )
                    )
                );
    }

    private static int addFuel(CommandSourceStack source, boolean nbt, int burnTime, float heatStrength, Integer maxHeat) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only player can execute the command!"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("You have to hold an item!"));
            return 0;
        }


        FurnaceFuelRegistry.StackKey key = nbt ? new FurnaceFuelRegistry.StackKey(stack.copy()) : new FurnaceFuelRegistry.StackKey(stack.getItem(), null);
        FurnaceFuelRegistry.FuelData data = new FurnaceFuelRegistry.FuelData(burnTime, heatStrength, maxHeat != null ? maxHeat : 0,
                nbt && stack.hasTag() ? stack.getTag().toString() : null);

        ITEM_FUEL.put(key, data);

        save();
        source.sendSuccess(() -> Component.literal("Fuel added: " + ForgeRegistries.ITEMS.getKey(stack.getItem())), true);
        return 1;
    }



    private static int removeFuel(CommandSourceStack source, String itemId) {
        // Item aus Registry finden
        var rl = ForgeRegistries.ITEMS.getKey(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId)));
        if (rl == null) {
            source.sendFailure(Component.literal("Item not found: " + itemId));
            return 0;
        }

        var item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            source.sendFailure(Component.literal("Invalid item: " + itemId));
            return 0;
        }

        // Alle registrierten Fuels für dieses Item entfernen
        int removed = FurnaceFuelRegistry.removeAllForItem(item);

        save();

        source.sendSuccess(() -> Component.literal("Fuel removed for " + itemId + " (Amount: " + removed + ")"), true);
        return 1;
    }
}