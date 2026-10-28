package com.tiendamod;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de la tienda. Se guarda en config/tiendamod_precios.json y se puede editar a mano
 * (y recargar con /tienda recargar) o con los comandos /tienda agregar, /tienda precio, /tienda quitar.
 */
public final class ShopManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Map<String, String> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("materiales", "Materiales");
        CATEGORIES.put("comidas", "Comidas");
        CATEGORIES.put("armas", "Armas");
        CATEGORIES.put("armaduras", "Armaduras");
        CATEGORIES.put("herramientas", "Herramientas");
        CATEGORIES.put("bloques", "Bloques");
        CATEGORIES.put("otros", "Otros");
        CATEGORIES.put("misiones", "Misiones");
    }

    private static final Map<String, ShopEntry> ENTRIES = new LinkedHashMap<>();

    private ShopManager() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("tiendamod_precios.json");
    }

    public static List<ShopEntry> all() {
        return new ArrayList<>(ENTRIES.values());
    }

    public static ShopEntry get(String id) {
        return ENTRIES.get(id);
    }

    /** Agrega o reemplaza un item. sell <= 0 usa la mitad del precio de compra. buy <= 0 es solo venta. */
    public static boolean put(String category, String id, int buy, int sell) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl) || ForgeRegistries.ITEMS.getValue(rl) == Items.AIR) {
            LOGGER.warn("[Tienda] Item desconocido, se ignora: {}", id);
            return false;
        }
        if (!CATEGORIES.containsKey(category)) category = "otros";
        if (buy < 0) buy = 0;
        if (sell <= 0 && buy > 0) sell = Math.max(1, buy / 2);
        else if (sell < 0) sell = 0;
        ENTRIES.put(id, new ShopEntry(id, category, buy, sell));
        return true;
    }

    public static boolean remove(String id) {
        return ENTRIES.remove(id) != null;
    }

    public static void load() {
        ENTRIES.clear();
        Path p = file();
        if (!Files.exists(p)) {
            defaults();
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (JsonElement e : root.getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                String cat = o.has("category") ? o.get("category").getAsString() : "otros";
                int buy = o.get("buy").getAsInt();
                int sell = o.has("sell") ? o.get("sell").getAsInt() : -1;
                put(cat, id, buy, sell);
            }
            LOGGER.info("[Tienda] {} items cargados", ENTRIES.size());
        } catch (Exception ex) {
            LOGGER.error("[Tienda] Error leyendo {}: {}", p, ex.toString());
        }
    }

    public static void save() {
        JsonArray arr = new JsonArray();
        for (ShopEntry e : ENTRIES.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id());
            o.addProperty("category", e.category());
            o.addProperty("buy", e.buy());
            o.addProperty("sell", e.sell());
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("_ayuda", "buy = lo que cuesta comprarlo; sell = lo que paga la tienda si lo vendes. Categorias: " + String.join(", ", CATEGORIES.keySet()));
        root.add("items", arr);
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(root, w);
        } catch (IOException ex) {
            LOGGER.error("[Tienda] No se pudo guardar el catalogo: {}", ex.toString());
        }
    }

    // ---------------------------------------------------------------- precios por defecto

    private static final String[] BASE = {
            // materiales (mas dificil de conseguir = mas caro)
            "materiales|minecraft:coal|10", "materiales|minecraft:copper_ingot|8", "materiales|minecraft:iron_ingot|25",
            "materiales|minecraft:gold_ingot|60", "materiales|minecraft:lapis_lazuli|10", "materiales|minecraft:redstone|8",
            "materiales|minecraft:quartz|15", "materiales|minecraft:amethyst_shard|20", "materiales|minecraft:emerald|120",
            "materiales|minecraft:diamond|300", "materiales|minecraft:netherite_scrap|600", "materiales|minecraft:netherite_ingot|2800",
            "materiales|minecraft:stick|1", "materiales|minecraft:string|6", "materiales|minecraft:leather|10",
            "materiales|minecraft:feather|4", "materiales|minecraft:bone|5", "materiales|minecraft:gunpowder|20",
            "materiales|minecraft:slime_ball|30", "materiales|minecraft:ender_pearl|90", "materiales|minecraft:blaze_rod|80",
            "materiales|minecraft:ghast_tear|200", "materiales|minecraft:glowstone_dust|12", "materiales|minecraft:ink_sac|8",
            "materiales|minecraft:honeycomb|20", "materiales|minecraft:flint|3", "materiales|minecraft:clay_ball|3",
            "materiales|minecraft:phantom_membrane|70", "materiales|minecraft:prismarine_shard|25", "materiales|minecraft:shulker_shell|700",
            "materiales|minecraft:echo_shard|900", "materiales|minecraft:nautilus_shell|400", "materiales|minecraft:heart_of_the_sea|1500",
            "materiales|minecraft:nether_star|5000", "materiales|minecraft:ancient_debris|800",
            // comidas
            "comidas|minecraft:apple|10", "comidas|minecraft:bread|8", "comidas|minecraft:cooked_beef|12",
            "comidas|minecraft:cooked_porkchop|12", "comidas|minecraft:cooked_chicken|10", "comidas|minecraft:cooked_mutton|10",
            "comidas|minecraft:cooked_salmon|10", "comidas|minecraft:cooked_cod|8", "comidas|minecraft:cooked_rabbit|10",
            "comidas|minecraft:baked_potato|5", "comidas|minecraft:carrot|4", "comidas|minecraft:golden_carrot|40",
            "comidas|minecraft:golden_apple|400", "comidas|minecraft:enchanted_golden_apple|4000", "comidas|minecraft:pumpkin_pie|15",
            "comidas|minecraft:cake|40", "comidas|minecraft:cookie|3", "comidas|minecraft:melon_slice|2",
            "comidas|minecraft:sweet_berries|3", "comidas|minecraft:mushroom_stew|12", "comidas|minecraft:honey_bottle|15",
            "comidas|minecraft:beetroot|3", "comidas|minecraft:dried_kelp|2", "comidas|minecraft:rabbit_stew|20",
            // armas (las espadas se generan abajo)
            "armas|minecraft:bow|40", "armas|minecraft:crossbow|90", "armas|minecraft:arrow|2",
            "armas|minecraft:spectral_arrow|12", "armas|minecraft:trident|1500", "armas|minecraft:shield|70",
            // herramientas varias
            "herramientas|minecraft:fishing_rod|30", "herramientas|minecraft:shears|50", "herramientas|minecraft:flint_and_steel|40",
            "herramientas|minecraft:bucket|75", "herramientas|minecraft:water_bucket|80", "herramientas|minecraft:lava_bucket|100",
            "herramientas|minecraft:compass|80", "herramientas|minecraft:clock|150", "herramientas|minecraft:spyglass|100",
            "herramientas|minecraft:lead|20", "herramientas|minecraft:name_tag|120", "herramientas|minecraft:saddle|150",
            // bloques
            "bloques|minecraft:dirt|1", "bloques|minecraft:cobblestone|2", "bloques|minecraft:stone|2",
            "bloques|minecraft:sand|2", "bloques|minecraft:gravel|2", "bloques|minecraft:oak_log|4",
            "bloques|minecraft:spruce_log|4", "bloques|minecraft:birch_log|4", "bloques|minecraft:oak_planks|1",
            "bloques|minecraft:glass|4", "bloques|minecraft:bricks|20", "bloques|minecraft:obsidian|60",
            "bloques|minecraft:crying_obsidian|150", "bloques|minecraft:netherrack|1", "bloques|minecraft:end_stone|15",
            "bloques|minecraft:tnt|50", "bloques|minecraft:glowstone|40", "bloques|minecraft:sea_lantern|100",
            "bloques|minecraft:terracotta|8", "bloques|minecraft:white_wool|12", "bloques|minecraft:ice|5",
            "bloques|minecraft:coal_block|90", "bloques|minecraft:iron_block|225", "bloques|minecraft:gold_block|540",
            "bloques|minecraft:redstone_block|72", "bloques|minecraft:lapis_block|90", "bloques|minecraft:emerald_block|1080",
            "bloques|minecraft:diamond_block|2700", "bloques|minecraft:chest|12", "bloques|minecraft:crafting_table|4",
            "bloques|minecraft:furnace|6", "bloques|minecraft:anvil|300", "bloques|minecraft:ender_chest|1500",
            // otros
            "otros|minecraft:torch|2", "otros|minecraft:rail|4", "otros|minecraft:minecart|30",
            "otros|minecraft:piston|40", "otros|minecraft:sticky_piston|70", "otros|minecraft:hopper|80",
            "otros|minecraft:observer|40", "otros|minecraft:dispenser|45", "otros|minecraft:redstone_torch|10",
            "otros|minecraft:repeater|20", "otros|minecraft:comparator|50", "otros|minecraft:lever|3",
            "otros|minecraft:experience_bottle|60", "otros|minecraft:ender_eye|150", "otros|minecraft:totem_of_undying|3000",
            "otros|minecraft:elytra|8000", "otros|minecraft:slime_block|270"
    };

    private static void defaults() {
        for (String s : BASE) {
            String[] p = s.split("\\|");
            put(p[0], p[1], Integer.parseInt(p[2]), -1);
        }
        // Hueso de elite: solo venta por 300
        put("materiales", "tiendamod:elite_bone", 0, 300);
        // herramientas y armas por material: precio = unidades de material * valor + palos
        toolSet("wooden", 2, 0);
        toolSet("stone", 2, 0);
        toolSet("iron", 25, 0);
        toolSet("golden", 60, 0);
        toolSet("diamond", 300, 0);
        toolSet("netherite", 300, 2800);
        armorSet("leather", 10, 0);
        armorSet("iron", 25, 0);
        armorSet("golden", 60, 0);
        armorSet("diamond", 300, 0);
        armorSet("netherite", 300, 2800);
        put("armaduras", "minecraft:turtle_helmet", 300, -1);
    }

    private static void toolSet(String tier, int unit, int extra) {
        put("armas", "minecraft:" + tier + "_sword", 2 * unit + 2 + extra, -1);
        put("herramientas", "minecraft:" + tier + "_pickaxe", 3 * unit + 2 + extra, -1);
        put("herramientas", "minecraft:" + tier + "_axe", 3 * unit + 2 + extra, -1);
        put("herramientas", "minecraft:" + tier + "_shovel", unit + 2 + extra, -1);
        put("herramientas", "minecraft:" + tier + "_hoe", 2 * unit + 2 + extra, -1);
    }

    private static void armorSet(String tier, int unit, int extra) {
        put("armaduras", "minecraft:" + tier + "_helmet", 5 * unit + extra, -1);
        put("armaduras", "minecraft:" + tier + "_chestplate", 8 * unit + extra, -1);
        put("armaduras", "minecraft:" + tier + "_leggings", 7 * unit + extra, -1);
        put("armaduras", "minecraft:" + tier + "_boots", 4 * unit + extra, -1);
    }
}
