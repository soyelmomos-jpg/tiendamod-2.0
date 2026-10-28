package com.tiendamod.client;

import com.tiendamod.Network;
import com.tiendamod.ShopEntry;
import com.tiendamod.ShopManager;
import com.tiendamod.TradePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de la tienda. Usa solo widgets y GuiGraphics de vanilla (sin mixins ni texturas propias),
 * asi que es compatible con OptiFine.
 */
public class ShopScreen extends Screen {
    private static final int W = 292, H = 212;
    private static final int COLS = 9, ROWS = 6, SLOT = 22;
    private static final int[] QTYS = {1, 10, 50};

    private int left, top;
    private String category = ShopManager.CATEGORIES.keySet().iterator().next();
    private boolean sellMode = false;
    private int qty = 1;
    private int page = 0;
    private List<ShopEntry> visible = new ArrayList<>();

    private final List<Button> catButtons = new ArrayList<>();
    private final List<String> catKeys = new ArrayList<>();
    private final List<Button> qtyButtons = new ArrayList<>();
    private Button buyBtn, sellBtn, prevBtn, nextBtn;

    public ShopScreen() {
        super(Component.literal("Tienda"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        catButtons.clear();
        catKeys.clear();
        qtyButtons.clear();

        int i = 0;
        for (Map.Entry<String, String> c : ShopManager.CATEGORIES.entrySet()) {
            final String key = c.getKey();
            Button b = Button.builder(Component.literal(c.getValue()), btn -> {
                category = key;
                page = 0;
                refresh();
            }).bounds(left + 8, top + 34 + i * 22, 72, 20).build();
            addRenderableWidget(b);
            catButtons.add(b);
            catKeys.add(key);
            i++;
        }

        buyBtn = addRenderableWidget(Button.builder(Component.literal("Comprar"), b -> {
            sellMode = false;
            refresh();
        }).bounds(left + 86, top + 8, 60, 20).build());
        sellBtn = addRenderableWidget(Button.builder(Component.literal("Vender"), b -> {
            sellMode = true;
            refresh();
        }).bounds(left + 150, top + 8, 60, 20).build());

        for (int k = 0; k < QTYS.length; k++) {
            final int q = QTYS[k];
            Button b = Button.builder(Component.literal("x" + q), btn -> {
                qty = q;
                refresh();
            }).bounds(left + 86 + k * 34, top + H - 26, 30, 20).build();
            addRenderableWidget(b);
            qtyButtons.add(b);
        }

        prevBtn = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            refresh();
        }).bounds(left + W - 52, top + H - 26, 20, 20).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            refresh();
        }).bounds(left + W - 28, top + H - 26, 20, 20).build());

        refresh();
    }

    /** Reconstruye la lista visible y el estado de los botones. */
    public void refresh() {
        visible = new ArrayList<>();
        for (ShopEntry e : ClientHandler.entries) {
            if (e.category().equals(category)) visible.add(e);
        }
        int perPage = COLS * ROWS;
        int pages = Math.max(1, (visible.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        if (buyBtn == null) return; // init() todavia no termino
        for (int i = 0; i < catButtons.size(); i++) catButtons.get(i).active = !catKeys.get(i).equals(category);
        buyBtn.active = sellMode;
        sellBtn.active = !sellMode;
        for (int i = 0; i < qtyButtons.size(); i++) qtyButtons.get(i).active = QTYS[i] != qty;
        prevBtn.active = page > 0;
        nextBtn.active = page < pages - 1;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);

        // panel
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF7C5CFF);
        g.fill(left, top, left + W, top + H, 0xF0141420);
        // separador
        g.fill(left + 82, top + 32, left + 83, top + H - 30, 0xFF3A3A55);

        super.render(g, mx, my, partial);

        // titulo y puntos
        g.drawString(font, "Tienda", left + 8, top + 14, 0xFFE9C46A, true);
        String pts = ClientHandler.points + " puntos";
        g.drawString(font, pts, left + W - 8 - font.width(pts), top + 14, 0xFFFFD84D, true);

        int perPage = COLS * ROWS;
        int pages = Math.max(1, (visible.size() + perPage - 1) / perPage);
        String pg = (page + 1) + "/" + pages;
        g.drawString(font, pg, left + 190 + (46 - font.width(pg)) / 2, top + H - 20, 0xFFCCCCCC, false);

        int gx = left + 86, gy = top + 34;

        if ("misiones".equals(category)) {
            // Render de Misiones
            g.drawString(font, "=== MISIONES DISPONIBLES ===", gx, gy, 0xFFFFD84D, false);
            g.drawString(font, "Usa /mision para pedir o ver tu", gx, gy + 20, 0xFFFFFFFF, false);
            g.drawString(font, "misión activa en el chat.", gx, gy + 32, 0xFFFFFFFF, false);

            g.drawString(font, "Tipos de Misiones:", gx, gy + 60, 0xFFE9C46A, false);
            g.drawString(font, "- Comunes: Matar 10 Zombies,", gx, gy + 75, 0xFFAAAAAA, false);
            g.drawString(font, "  Creepers o Esqueletos.", gx, gy + 87, 0xFFAAAAAA, false);

            g.drawString(font, "- Épicas (Raras): Matar 10 Blazes,", gx, gy + 105, 0xFFFFAA00, false);
            g.drawString(font, "  20 Piglins o 10 Wither Skeletons.", gx, gy + 117, 0xFFFFAA00, false);
            return;
        }

        // grilla
        int hovered = -1;
        int start = page * perPage;
        for (int n = 0; n < perPage; n++) {
            int idx = start + n;
            if (idx >= visible.size()) break;
            ShopEntry e = visible.get(idx);
            Item item = e.item();
            if (item == null) continue;
            int sx = gx + (n % COLS) * SLOT;
            int sy = gy + (n / COLS) * SLOT;

            boolean ok;
            if (sellMode) ok = countInInventory(item) > 0;
            else ok = e.buy() > 0 && (long) e.buy() * qty <= ClientHandler.points;

            boolean over = mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT;
            if (over) hovered = idx;

            int bg = ok ? 0xFF2A2A3C : (sellMode ? 0xFF1A1A22 : 0xFF4A2020);
            if (over) bg = ok ? 0xFF4A4A70 : 0xFF6A3030;
            g.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, bg);

            ItemStack stack = new ItemStack(item);
            g.renderItem(stack, sx + 3, sy + 3);

            // precio chiquito abajo a la derecha
            if (sellMode || e.buy() > 0) {
                int price = sellMode ? e.sell() : e.buy();
                String ps = String.valueOf(price);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.pose().scale(0.5f, 0.5f, 1f);
                g.drawString(font, ps, (sx + SLOT - 2) * 2 - font.width(ps), (sy + SLOT - 6) * 2, 0xFFFFE066, true);
                g.pose().popPose();
            }
        }

        if (visible.isEmpty()) {
            g.drawString(font, "No hay items en esta categoria.", gx + 4, gy + 4, 0xFF999999, false);
        }

        // tooltip
        if (hovered >= 0) {
            ShopEntry e = visible.get(hovered);
            Item item = e.item();
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                List<Component> lines = new ArrayList<>(getTooltipFromItem(Minecraft.getInstance(), stack));
                lines.add(Component.empty());
                lines.add(Component.literal("Compra: " + e.buy() + " pts c/u").withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("Venta: " + e.sell() + " pts c/u").withStyle(ChatFormatting.GREEN));
                lines.add(Component.empty());
                if (sellMode) {
                    lines.add(Component.literal("Click: vender hasta " + qty + " (" + (long) e.sell() * qty + " pts)")
                            .withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal("Tenes: " + countInInventory(item)).withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(Component.literal("Click: comprar x" + qty + " (" + (long) e.buy() * qty + " pts)")
                            .withStyle(ChatFormatting.YELLOW));
                }
                g.renderComponentTooltip(font, lines, mx, my);
            }
        }
    }

    private int countInInventory(Item item) {
        if (minecraft == null || minecraft.player == null) return 0;
        int total = 0;
        for (ItemStack s : minecraft.player.getInventory().items) {
            if (s.is(item) && !s.hasTag()) total += s.getCount();
        }
        for (ItemStack s : minecraft.player.getInventory().offhand) {
            if (s.is(item) && !s.hasTag()) total += s.getCount();
        }
        return total;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        int gx = left + 86, gy = top + 34;
        double rx = mx - gx, ry = my - gy;
        if (rx < 0 || ry < 0 || rx >= COLS * SLOT || ry >= ROWS * SLOT) return false;
        int col = (int) (rx / SLOT), row = (int) (ry / SLOT);
        int idx = page * COLS * ROWS + row * COLS + col;
        if (idx >= visible.size()) return false;

        ShopEntry e = visible.get(idx);
        if (!sellMode && e.buy() <= 0) return false; // No se puede comprar
        Network.CHANNEL.sendToServer(new TradePacket(e.id(), qty, sellMode));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta < 0) page++;
        else if (delta > 0) page--;
        refresh();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
