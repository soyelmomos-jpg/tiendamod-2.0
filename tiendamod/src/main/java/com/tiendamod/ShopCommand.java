package com.tiendamod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Comandos de administrador (nivel 2):
 *  /tienda agregar <compra> [venta] [categoria]        -> agrega el item que tenes en la mano
 *  /tienda precio <item> <compra> [venta] [categoria]  -> agrega o cambia el precio de cualquier item
 *  /tienda quitar [item]                               -> saca un item de la tienda
 *  /tienda puntos <dar|quitar|poner|ver> <jugador> [cantidad]
 *  /tienda recargar                                    -> vuelve a leer config/tiendamod_precios.json
 */
public final class ShopCommand {
    private static final SuggestionProvider<CommandSourceStack> CATS =
            (c, b) -> SharedSuggestionProvider.suggest(ShopManager.CATEGORIES.keySet(), b);

    private ShopCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tienda").requires(s -> s.hasPermission(2));

        // /tienda agregar <compra> [venta] [categoria]  (item en la mano)
        root.then(Commands.literal("agregar")
                .then(Commands.argument("compra", IntegerArgumentType.integer(1))
                        .executes(c -> addHeld(c, -1, null))
                        .then(Commands.argument("venta", IntegerArgumentType.integer(1))
                                .executes(c -> addHeld(c, IntegerArgumentType.getInteger(c, "venta"), null))
                                .then(Commands.argument("categoria", StringArgumentType.word()).suggests(CATS)
                                        .executes(c -> addHeld(c, IntegerArgumentType.getInteger(c, "venta"),
                                                StringArgumentType.getString(c, "categoria")))))));

        // /tienda precio <item> <compra> [venta] [categoria]
        root.then(Commands.literal("precio")
                .then(Commands.argument("item", ItemArgument.item(ctx))
                        .then(Commands.argument("compra", IntegerArgumentType.integer(1))
                                .executes(c -> apply(c, ItemArgument.getItem(c, "item").getItem(), -1, null))
                                .then(Commands.argument("venta", IntegerArgumentType.integer(1))
                                        .executes(c -> apply(c, ItemArgument.getItem(c, "item").getItem(),
                                                IntegerArgumentType.getInteger(c, "venta"), null))
                                        .then(Commands.argument("categoria", StringArgumentType.word()).suggests(CATS)
                                                .executes(c -> apply(c, ItemArgument.getItem(c, "item").getItem(),
                                                        IntegerArgumentType.getInteger(c, "venta"),
                                                        StringArgumentType.getString(c, "categoria"))))))));

        // /tienda quitar [item]
        root.then(Commands.literal("quitar")
                .executes(c -> remove(c, heldItem(c)))
                .then(Commands.argument("item", ItemArgument.item(ctx))
                        .executes(c -> remove(c, ItemArgument.getItem(c, "item").getItem()))));

        // /tienda puntos ...
        LiteralArgumentBuilder<CommandSourceStack> points = Commands.literal("puntos");
        for (String op : new String[]{"dar", "quitar", "poner"}) {
            points.then(Commands.literal(op)
                    .then(Commands.argument("jugador", EntityArgument.player())
                            .then(Commands.argument("cantidad", LongArgumentType.longArg(0))
                                    .executes(c -> points(c, op)))));
        }
        points.then(Commands.literal("ver")
                .then(Commands.argument("jugador", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
                            long v = PointsData.get(p);
                            c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " tiene " + v + " puntos."), false);
                            return 1;
                        })));
        root.then(points);

        root.then(Commands.literal("recargar").executes(c -> {
            ShopManager.load();
            Network.broadcastCatalog(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Tienda recargada (" + ShopManager.all().size() + " items)."), true);
            return 1;
        }));

        d.register(root);

        // Command for all players to request/check mission
        d.register(Commands.literal("mision").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            MissionManager.assignMission(p);
            return 1;
        }));
    }

    private static Item heldItem(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack s = c.getSource().getPlayerOrException().getMainHandItem();
        return s.isEmpty() ? null : s.getItem();
    }

    private static int addHeld(CommandContext<CommandSourceStack> c, int sell, String cat) throws CommandSyntaxException {
        Item item = heldItem(c);
        if (item == null) {
            c.getSource().sendFailure(Component.literal("Tenes que tener un item en la mano."));
            return 0;
        }
        return apply(c, item, sell, cat);
    }

    private static int apply(CommandContext<CommandSourceStack> c, Item item, int sell, String cat) {
        int buy = IntegerArgumentType.getInteger(c, "compra");
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(item));
        ShopEntry old = ShopManager.get(id);
        if (cat == null) cat = old != null ? old.category() : "otros";
        if (!ShopManager.CATEGORIES.containsKey(cat)) {
            c.getSource().sendFailure(Component.literal("Categoria invalida. Opciones: " + String.join(", ", ShopManager.CATEGORIES.keySet())));
            return 0;
        }
        if (!ShopManager.put(cat, id, buy, sell)) {
            c.getSource().sendFailure(Component.literal("Item invalido."));
            return 0;
        }
        ShopManager.save();
        Network.broadcastCatalog(c.getSource().getServer());
        ShopEntry e = ShopManager.get(id);
        c.getSource().sendSuccess(() -> Component.literal(
                id + " -> compra " + e.buy() + ", venta " + e.sell() + ", categoria " + e.category()), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c, Item item) {
        if (item == null) {
            c.getSource().sendFailure(Component.literal("Tenes que tener un item en la mano o indicarlo."));
            return 0;
        }
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(item));
        if (!ShopManager.remove(id)) {
            c.getSource().sendFailure(Component.literal(id + " no estaba en la tienda."));
            return 0;
        }
        ShopManager.save();
        Network.broadcastCatalog(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.literal(id + " fue sacado de la tienda."), true);
        return 1;
    }

    private static int points(CommandContext<CommandSourceStack> c, String op) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "jugador");
        long n = LongArgumentType.getLong(c, "cantidad");
        switch (op) {
            case "dar" -> PointsData.add(p, n);
            case "quitar" -> PointsData.set(p, PointsData.get(p) - n);
            default -> PointsData.set(p, n);
        }
        Network.sendSync(p, SyncPacket.POINTS);
        long now = PointsData.get(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " ahora tiene " + now + " puntos."), true);
        return 1;
    }
}
