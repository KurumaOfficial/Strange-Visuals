package dev.strangevisuals.ui;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.i18n.I18n;
import dev.strangevisuals.render.BackgroundPipeline;
import dev.strangevisuals.render.CirclePipeline;
import dev.strangevisuals.render.GlowPipeline;
import dev.strangevisuals.render.GlyphAtlas;
import dev.strangevisuals.render.IconPipeline;
import dev.strangevisuals.render.PanoramaManager;
import dev.strangevisuals.render.MsdfText;
import dev.strangevisuals.render.NeonEdge;
import dev.strangevisuals.render.PanelPipeline;
import dev.strangevisuals.render.RoundedRectPipeline;
import dev.strangevisuals.render.WidgetStylePipeline;
import dev.strangevisuals.ui.style.MenuStyle;
import dev.strangevisuals.ui.style.StyleManager;
import net.minecraft.SharedConstants;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 100% Custom Multiplayer Server Browser Screen for StrangeVisuals.
 * Features a high-performance 3x3 Grid Layout (9 servers per page) with responsive
 * pagination, server status badges, MOTD display, and frosted acrylic glass modals.
 */
public final class CustomMultiplayerScreen extends Screen {
    private static final Identifier TITLE_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_bold.json");
    private static final Identifier TITLE_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_bold.png");
    private static final Identifier TEXT_FONT_DATA = StrangeVisuals.id("fonts/msdf/sf_regular.json");
    private static final Identifier TEXT_FONT_ATLAS = StrangeVisuals.id("fonts/msdf/sf_regular.png");
    private static final Identifier ICON_DATA = StrangeVisuals.id("fonts/msdf/wtmico.json");
    private static final Identifier ICON_ATLAS = StrangeVisuals.id("fonts/msdf/wtmico.png");
    private static final Identifier ARROW_BACK = StrangeVisuals.id("textures/gui/arrow_back.png");
    private static final Identifier CHEVRON_LEFT = StrangeVisuals.id("textures/gui/chevron_left.png");
    private static final Identifier CHEVRON_RIGHT = StrangeVisuals.id("textures/gui/chevron_right.png");

    private final Screen parent;
    private GlyphAtlas titleFont;
    private GlyphAtlas textFont;
    private GlyphAtlas icons;

    private ServerList serverList;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    private final List<ServerEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private int currentPage = 0;

    private long lastFrame = System.nanoTime();
    private long openedAt;
    private long lastClickTime = 0L;

    // Hover animation states (unified with Main Menu)
    private float backHover = 0.0f;
    private float prevHover = 0.0f;
    private float nextHover = 0.0f;
    private final float[] toolbarHover = new float[6];
    private float modalCancelHover = 0.0f;
    private float modalConfirmHover = 0.0f;
    private float modalCloseHover = 0.0f;

    private static float lerpHover(float current, boolean hovered, double deltaSeconds) {
        float target = hovered ? 1.0f : 0.0f;
        return current + (target - current) * Math.min(1.0f, (float) Math.max(0.016, deltaSeconds) * 14.0f);
    }

    // Modals
    private Modal activeModal = Modal.NONE;
    private TextFieldWidget nameField;
    private TextFieldWidget addressField;

    private enum Modal {
        NONE, DIRECT_CONNECT, ADD_SERVER, EDIT_SERVER, DELETE_CONFIRM
    }

    public CustomMultiplayerScreen(Screen parent) {
        super(Text.literal("Multiplayer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        titleFont = GlyphAtlas.load(TITLE_FONT_DATA, TITLE_FONT_ATLAS);
        textFont = GlyphAtlas.load(TEXT_FONT_DATA, TEXT_FONT_ATLAS);
        icons = GlyphAtlas.load(ICON_DATA, ICON_ATLAS);
        openedAt = Util.getMeasuringTimeMs();

        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.OPEN_SOUND, 0.9f, 1.0f));
        }

        if (client != null) {
            serverList = new ServerList(client);
            serverList.loadFile();
        }

        refreshEntries();

        int mw = getModalWidth();
        int mx = getModalX();
        int my = getModalY();

        nameField = new TextFieldWidget(textRenderer, mx + 27, my + 61, mw - 54, 18, Text.literal("Server Name"));
        nameField.setMaxLength(32);
        nameField.setDrawsBackground(false);
        addressField = new TextFieldWidget(textRenderer, mx + 27, my + 113, mw - 54, 18, Text.literal("Server IP Address"));
        addressField.setMaxLength(128);
        addressField.setDrawsBackground(false);

        nameField.setVisible(false);
        addressField.setVisible(false);
        addDrawableChild(nameField);
        addDrawableChild(addressField);
    }

    @Override
    public void tick() {
        super.tick();
        this.pinger.tick();
    }

    private int getModalWidth() {
        return Math.min(380, width - 40);
    }

    private int getModalHeight() {
        return switch (activeModal) {
            case DIRECT_CONNECT -> 176;
            case DELETE_CONFIRM -> 155;
            default -> 232;
        };
    }

    private int getModalX() {
        return (width - getModalWidth()) / 2;
    }

    private int getModalY() {
        return (height - getModalHeight()) / 2;
    }

    private void refreshEntries() {
        entries.clear();
        pinger.cancel();
        if (serverList == null) return;
        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo info = serverList.get(i);
            entries.add(new ServerEntry(info));
            final ServerInfo target = info;
            target.setStatus(ServerInfo.Status.INITIAL);
            Util.getIoWorkerExecutor().execute(() -> {
                try {
                    NetworkingBackend backend = client != null && client.options != null
                            ? NetworkingBackend.remote(client.options.shouldUseNativeTransport())
                            : NetworkingBackend.remote(false);
                    pinger.add(target, () -> {
                        if (client != null) {
                            client.execute(() -> {
                                target.setStatus(target.protocolVersion == SharedConstants.getGameVersion().protocolVersion()
                                        ? ServerInfo.Status.SUCCESSFUL
                                        : ServerInfo.Status.INCOMPATIBLE);
                            });
                        }
                    }, () -> {
                        if (client != null) {
                            client.execute(() -> {
                                target.setStatus(ServerInfo.Status.UNREACHABLE);
                            });
                        }
                    }, backend);
                } catch (UnknownHostException e) {
                    target.setStatus(ServerInfo.Status.UNREACHABLE);
                    target.label = Text.translatable("multiplayer.status.cannot_resolve").withColor(0xFFFF5555);
                } catch (Exception e) {
                    target.setStatus(ServerInfo.Status.UNREACHABLE);
                    target.label = Text.translatable("multiplayer.status.cannot_connect").withColor(0xFFFF5555);
                }
            });
        }
        int totalPages = getTotalPages();
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
    }

    public int getGridCols() {
        float gridW = width - 48.0f;
        return (gridW >= 760.0f) ? 4 : 3;
    }

    public int getGridRows() {
        float gridH = height - 46.0f - 48.0f;
        float gapY = 8.0f;
        return Math.max(3, (int) Math.floor((gridH + gapY) / (56.0f + gapY)));
    }

    public int getCardsPerPage() {
        return getGridCols() * getGridRows();
    }

    private int getTotalPages() {
        int perPage = getCardsPerPage();
        return Math.max(1, (entries.size() + perPage - 1) / perPage);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double deltaSeconds = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
        lastFrame = now;

        float visible = visibility();
        MenuStyle style = StyleManager.getStyle();
        if (style == MenuStyle.VANILLA) {
            PanoramaManager.render(context, PanoramaManager.PanoramaType.MULTIPLAYER, width, height, 0.0f);
            context.fillGradient(0, 0, width, height, 0x66000000, 0x88000000);
        } else {
            BackgroundPipeline.draw(visible);
            if (style == MenuStyle.BETA) {
                float cx = width * 0.85f;
                float cy = height * 0.70f;
                GlowPipeline.radial(context, cx, cy, 140.0f, withAlpha(NeonEdge.cool(), Math.round(30 * visible)));
            }
        }

        renderHeader(context, mouseX, mouseY, deltaSeconds, visible);
        renderServerGrid(context, mouseX, mouseY, deltaSeconds, visible);
        renderBottomToolbar(context, mouseX, mouseY, deltaSeconds, visible);

        if (activeModal != Modal.NONE) {
            renderModal(context, mouseX, mouseY, deltaSeconds, visible);
        }
    }

    private float visibility() {
        long now = Util.getMeasuringTimeMs();
        float t = (float) Math.min((now - openedAt) / 340.0f, 1.0);
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY, double deltaSeconds, float visible) {
        float headerY = 14.0f;
        float backX = 24.0f;
        float backW = 68.0f;
        float backH = 24.0f;

        boolean backHovered = activeModal == Modal.NONE && mouseX >= backX && mouseX <= backX + backW && mouseY >= headerY && mouseY <= headerY + backH;
        backHover = lerpHover(backHover, backHovered, deltaSeconds);
        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(backX), Math.round(headerY), Math.round(backW), Math.round(backH),
                I18n.get("multiplayer.back"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("return"), true, backHover > 0.5f);
        } else {
            WidgetStylePipeline.drawStyledButton(context, backX, headerY, backW, backH,
                I18n.get("multiplayer.back"), ARROW_BACK, true, backHover, visible, 0);
        }

        float titleX = backX + backW + 16.0f;
        if (titleFont != null) {
            MsdfText.draw(context, titleFont, I18n.get("multiplayer.title"), titleX, headerY + 4.0f, 10.0f,
                withAlpha(0xFFFFFF, Math.round(240 * visible)));
        }

        // Pagination controls & server count in header
        int totalPages = getTotalPages();
        float pageControlW = 140.0f;
        float pageX = width - 24.0f - pageControlW;

        float prevBtnX = pageX;
        float nextBtnX = pageX + pageControlW - 22.0f;
        float btnH = 22.0f;
        float btnW = 22.0f;

        boolean canPrev = currentPage > 0;
        boolean canNext = currentPage < totalPages - 1;

        boolean prevHov = activeModal == Modal.NONE && canPrev && mouseX >= prevBtnX && mouseX <= prevBtnX + btnW && mouseY >= headerY && mouseY <= headerY + btnH;
        boolean nextHov = activeModal == Modal.NONE && canNext && mouseX >= nextBtnX && mouseX <= nextBtnX + btnW && mouseY >= headerY && mouseY <= headerY + btnH;
        prevHover = lerpHover(prevHover, prevHov, deltaSeconds);
        nextHover = lerpHover(nextHover, nextHov, deltaSeconds);

        // Prev Button
        WidgetStylePipeline.drawStyledButton(context, prevBtnX, headerY, btnW, btnH,
            "", CHEVRON_LEFT, canPrev, prevHover, visible, 0);

        // Page Indicator
        String pageStr = (currentPage + 1) + " / " + totalPages;
        if (titleFont != null) {
            MsdfText.drawCenteredOn(context, titleFont, pageStr, pageX + pageControlW * 0.5f, headerY + btnH * 0.5f, 7.5f,
                withAlpha(0xD8E0EE, Math.round(230 * visible)));
        } else {
            context.drawCenteredTextWithShadow(textRenderer, pageStr, Math.round(pageX + pageControlW * 0.5f), Math.round(headerY + 6.0f), withAlpha(0xD8E0EE, Math.round(230 * visible)));
        }

        // Next Button
        WidgetStylePipeline.drawStyledButton(context, nextBtnX, headerY, btnW, btnH,
            "", CHEVRON_RIGHT, canNext, nextHover, visible, 0);
    }

    private void renderServerGrid(DrawContext context, int mouseX, int mouseY, double deltaSeconds, float visible) {
        float gridX = 24.0f;
        float gridY = 46.0f;
        float gridW = width - 48.0f;
        float gridH = height - gridY - 48.0f;

        if (entries.isEmpty()) {
            if (titleFont != null && textFont != null) {
                MsdfText.drawCenteredOn(context, titleFont, I18n.get("multiplayer.no_servers"), width * 0.5f, gridY + gridH * 0.45f, 9.5f,
                    withAlpha(0x6A758A, Math.round(180 * visible)));
                MsdfText.drawCenteredOn(context, textFont, I18n.get("multiplayer.prompt_add"), width * 0.5f, gridY + gridH * 0.45f + 16.0f, 7.0f,
                    withAlpha(0x4B5568, Math.round(160 * visible)));
            }
            return;
        }

        int cols = getGridCols();
        int rows = getGridRows();
        int cardsPerPage = cols * rows;

        float gapX = 10.0f;
        float gapY = 8.0f;
        float cardW = (gridW - gapX * (cols - 1)) / (float) cols;
        float cardH = (gridH - gapY * (rows - 1)) / (float) rows;

        int startIndex = currentPage * cardsPerPage;
        int endIndex = Math.min(entries.size(), startIndex + cardsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            int col = slot % cols;
            int row = slot / cols;

            float cx = gridX + col * (cardW + gapX);
            float cy = gridY + row * (cardH + gapY);

            ServerEntry entry = entries.get(i);
            boolean hovered = activeModal == Modal.NONE && mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            entry.hover.step(hovered ? 1.0 : 0.0, deltaSeconds);
            float hov = (float) entry.hover.get();
            boolean isSelected = (i == selectedIndex);

            // Card body (Deep frosted acrylic obsidian glass)
            int fillAlpha = Math.round((isSelected ? 230 : (150 + hov * 45)) * visible);
            int fillColor = isSelected ? 0x141C2B : 0x0C0E14;
            RoundedRectPipeline.draw(context, cx, cy, cardW, cardH, withAlpha(fillColor, fillAlpha));

            // Card outline with NeonEdge
            int borderAlpha = Math.round((isSelected ? 255 : (25 + hov * 85)) * visible);
            int borderColor = isSelected ? NeonEdge.cool() : (hov > 0.05f ? 0x6080A8 : 0x242A38);
            RoundedRectPipeline.drawOutline(context, cx, cy, cardW, cardH, withAlpha(borderColor, borderAlpha));

            // Left vertical accent strip
            if (isSelected || hov > 0.05f) {
                float barAlpha = isSelected ? 240.0f : (hov * 180.0f);
                RoundedRectPipeline.draw(context, cx + 2.0f, cy + 4.0f, 2.5f, cardH - 8.0f,
                    withAlpha(NeonEdge.cool(), Math.round(barAlpha * visible)));
            }

            renderCardContent(context, entry.info, cx, cy, cardW, cardH, visible);
        }
    }

    private void renderCardContent(DrawContext context, ServerInfo info, float cx, float cy, float cw, float ch, float visible) {
        // 1. Status Indicator Dot
        int pingCol;
        if (info.ping < 0L) {
            pingCol = 0xFF64748B; // grey (querying)
        } else if (info.ping < 120L) {
            pingCol = 0xFF34D399; // emerald green
        } else if (info.ping < 260L) {
            pingCol = 0xFFFBBF24; // amber
        } else {
            pingCol = 0xFFF87171; // red
        }
        CirclePipeline.draw(context, cx + 12.0f, cy + 9.0f, 3.0f, withAlpha(pingCol, Math.round(240 * visible)));

        // 2. Server Name (Truncated cleanly)
        String sName = info.name != null ? info.name : "Server";
        int maxNameChars = Math.max(10, Math.round((cw - 85.0f) / 7.0f));
        if (sName.length() > maxNameChars) {
            sName = sName.substring(0, maxNameChars - 2) + "...";
        }
        if (titleFont != null && titleFont.canRender(sName)) {
            MsdfText.draw(context, titleFont, sName, cx + 20.0f, cy + 4.0f, 8.0f, withAlpha(0xFFFFFF, Math.round(245 * visible)));
        } else {
            context.drawTextWithShadow(textRenderer, sName, Math.round(cx + 20.0f), Math.round(cy + 5.0f), withAlpha(0xFFFFFF, Math.round(245 * visible)));
        }

        // 3. Ping Tag Top Right
        String pingText;
        int pingTextCol;
        if (info.ping < 0L) {
            pingText = "• • •";
            pingTextCol = 0x758298;
        } else {
            pingText = info.ping + " ms";
            pingTextCol = 0x88CCAA;
        }
        if (textFont != null && textFont.canRender(pingText)) {
            MsdfText.drawRightOn(context, textFont, pingText, cx + cw - 10.0f, cy + 9.0f, 6.5f, withAlpha(pingTextCol, Math.round(220 * visible)));
        } else {
            context.drawTextWithShadow(textRenderer, pingText,
                Math.round(cx + cw - textRenderer.getWidth(pingText) - 10.0f), Math.round(cy + 5.0f),
                withAlpha(pingTextCol, Math.round(220 * visible)));
        }

        // 4. Server Description (MOTD) - 100% vanilla colors & formatting
        if (info.label != null) {
            int motdWidth = Math.round(cw - 22.0f);
            List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(info.label, motdWidth);
            for (int l = 0; l < Math.min(2, lines.size()); l++) {
                context.drawTextWithShadow(textRenderer, lines.get(l),
                    Math.round(cx + 12.0f), Math.round(cy + 17.0f + l * 10.0f), withAlpha(0xFFFFFF, Math.round(230 * visible)));
            }
        } else if (info.ping < 0L) {
            String pingingStr = I18n.get("multiplayer.pinging");
            context.drawTextWithShadow(textRenderer, pingingStr, Math.round(cx + 12.0f), Math.round(cy + 17.0f), withAlpha(0x758298, Math.round(190 * visible)));
        }

        // 5. Bottom line: Server IP & Players Count Pill
        if (textFont != null && textFont.canRender(info.address)) {
            MsdfText.draw(context, textFont, info.address, cx + 12.0f, cy + ch - 14.0f, 6.8f, withAlpha(0x566278, Math.round(180 * visible)));
        } else {
            context.drawTextWithShadow(textRenderer, info.address, Math.round(cx + 12.0f), Math.round(cy + ch - 14.0f), withAlpha(0x566278, Math.round(180 * visible)));
        }

        String players = info.playerCountLabel != null ? info.playerCountLabel.getString() : "";
        if (!players.isEmpty()) {
            int pw = (textFont != null && textFont.canRender(players))
                ? Math.round(textFont.width(players, 6.8f)) : textRenderer.getWidth(players);
            float pillX = cx + cw - pw - 18.0f;
            float pillY = cy + ch - 16.0f;
            float pillW = pw + 10.0f;
            RoundedRectPipeline.draw(context, pillX, pillY, pillW, 12.0f, withAlpha(0x111624, Math.round(180 * visible)));
            RoundedRectPipeline.drawOutline(context, pillX, pillY, pillW, 12.0f, withAlpha(0x283850, Math.round(200 * visible)));
            if (textFont != null && textFont.canRender(players)) {
                MsdfText.drawCenteredOn(context, textFont, players, pillX + pillW * 0.5f, pillY + 6.0f, 6.5f, withAlpha(0x8A98B0, Math.round(220 * visible)));
            } else {
                context.drawCenteredTextWithShadow(textRenderer, players, Math.round(pillX + pillW * 0.5f), Math.round(pillY + 2.0f), withAlpha(0x8A98B0, Math.round(220 * visible)));
            }
        }
    }

    private void renderBottomToolbar(DrawContext context, int mouseX, int mouseY, double deltaSeconds, float visible) {
        float barY = height - 38.0f;
        float startX = 24.0f;
        float btnH = 26.0f;

        boolean canJoin = selectedIndex >= 0 && selectedIndex < entries.size();

        if (StyleManager.getStyle() == MenuStyle.VANILLA) {
            float joinW = 108.0f;
            boolean joinHov = canJoin && activeModal == Modal.NONE && mouseX >= startX && mouseX <= startX + joinW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[0] = lerpHover(toolbarHover[0], joinHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(startX), Math.round(barY), Math.round(joinW), Math.round(btnH),
                I18n.get("multiplayer.join"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("join"), canJoin, toolbarHover[0] > 0.5f);

            float curX = startX + joinW + 8.0f;
            float dirW = 114.0f;
            boolean dirHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + dirW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[1] = lerpHover(toolbarHover[1], dirHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(curX), Math.round(barY), Math.round(dirW), Math.round(btnH),
                I18n.get("multiplayer.direct"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("multiplayer"), true, toolbarHover[1] > 0.5f);
            curX += dirW + 8.0f;

            float addW = 96.0f;
            boolean addHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + addW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[2] = lerpHover(toolbarHover[2], addHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(curX), Math.round(barY), Math.round(addW), Math.round(btnH),
                I18n.get("multiplayer.add"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("add"), true, toolbarHover[2] > 0.5f);
            curX += addW + 8.0f;

            float editW = 60.0f;
            boolean editHov = canJoin && activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + editW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[3] = lerpHover(toolbarHover[3], editHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(curX), Math.round(barY), Math.round(editW), Math.round(btnH),
                I18n.get("multiplayer.edit"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("edit"), canJoin, toolbarHover[3] > 0.5f);
            curX += editW + 8.0f;

            float delW = 68.0f;
            boolean delHov = canJoin && activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + delW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[4] = lerpHover(toolbarHover[4], delHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(curX), Math.round(barY), Math.round(delW), Math.round(btnH),
                I18n.get("multiplayer.delete"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("delete"), canJoin, toolbarHover[4] > 0.5f);
            curX += delW + 8.0f;

            float refW = 74.0f;
            boolean refHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + refW && mouseY >= barY && mouseY <= barY + btnH;
            toolbarHover[5] = lerpHover(toolbarHover[5], refHov, deltaSeconds);
            dev.strangevisuals.render.VanillaStylePipeline.renderButton(context, Math.round(curX), Math.round(barY), Math.round(refW), Math.round(btnH),
                I18n.get("multiplayer.refresh"), dev.strangevisuals.render.VanillaStylePipeline.getIconFor("refresh"), true, toolbarHover[5] > 0.5f);
            return;
        }

        // 1. Join Server
        float joinW = 108.0f;
        boolean joinHov = canJoin && activeModal == Modal.NONE && mouseX >= startX && mouseX <= startX + joinW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[0] = lerpHover(toolbarHover[0], joinHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, startX, barY, joinW, btnH,
            I18n.get("multiplayer.join"), 0, null, canJoin, toolbarHover[0], visible, NeonEdge.cool());

        // 2. Direct Connect
        float curX = startX + joinW + 8.0f;
        float dirW = 114.0f;
        boolean dirHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + dirW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[1] = lerpHover(toolbarHover[1], dirHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, curX, barY, dirW, btnH,
            I18n.get("multiplayer.direct"), 0, null, true, toolbarHover[1], visible, 0);
        curX += dirW + 8.0f;

        // 3. Add Server
        float addW = 96.0f;
        boolean addHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + addW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[2] = lerpHover(toolbarHover[2], addHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, curX, barY, addW, btnH,
            I18n.get("multiplayer.add"), 0, null, true, toolbarHover[2], visible, 0);
        curX += addW + 8.0f;

        // 4. Edit
        float editW = 60.0f;
        boolean editHov = canJoin && activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + editW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[3] = lerpHover(toolbarHover[3], editHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, curX, barY, editW, btnH,
            I18n.get("multiplayer.edit"), 0, null, canJoin, toolbarHover[3], visible, 0);
        curX += editW + 8.0f;

        // 5. Delete
        float delW = 68.0f;
        boolean delHov = canJoin && activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + delW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[4] = lerpHover(toolbarHover[4], delHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, curX, barY, delW, btnH,
            I18n.get("multiplayer.delete"), 0, null, canJoin, toolbarHover[4], visible, 0);
        curX += delW + 8.0f;

        // 6. Refresh
        float refW = 74.0f;
        boolean refHov = activeModal == Modal.NONE && mouseX >= curX && mouseX <= curX + refW && mouseY >= barY && mouseY <= barY + btnH;
        toolbarHover[5] = lerpHover(toolbarHover[5], refHov, deltaSeconds);
        WidgetStylePipeline.drawStyledButton(context, curX, barY, refW, btnH,
            I18n.get("multiplayer.refresh"), 0, null, true, toolbarHover[5], visible, 0);
    }

    private void renderModal(DrawContext context, int mouseX, int mouseY, double deltaSeconds, float visible) {
        context.fillGradient(0, 0, width, height, 0xB8020408, 0xD003050C);

        int mw = getModalWidth();
        int mh = getModalHeight();
        int mx = getModalX();
        int my = getModalY();

        GlowPipeline.radial(context, mx + mw * 0.5f, my + mh * 0.5f, mw * 0.7f, withAlpha(NeonEdge.cool(), 36));

        // Frosted acrylic modal background
        RoundedRectPipeline.draw(context, mx, my, mw, mh, 0xFA0B0E15);
        RoundedRectPipeline.drawOutline(context, mx, my, mw, mh, 0xFF243044);

        // Top neon accent line
        PanelPipeline.draw(context, mx + 16.0f, my + 1.2f, mw - 32.0f, 1.4f, withAlpha(NeonEdge.cool(), 180));

        // Header accent divider line
        PanelPipeline.draw(context, mx + 16.0f, my + 34.0f, mw - 32.0f, 1.0f, 0x1E3A4D68);

        // Close button [✕] at top-right
        float closeX = mx + mw - 28.0f;
        float closeY = my + 9.0f;
        float closeSize = 18.0f;
        boolean closeHov = mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize;
        modalCloseHover = lerpHover(modalCloseHover, closeHov, deltaSeconds);
        if (modalCloseHover > 0.01f) {
            RoundedRectPipeline.draw(context, closeX, closeY, closeSize, closeSize,
                withAlpha(0x242F42, Math.round(180 * modalCloseHover)));
            RoundedRectPipeline.drawOutline(context, closeX, closeY, closeSize, closeSize,
                withAlpha(NeonEdge.cool(), Math.round(200 * modalCloseHover)));
        }
        int closeColor = WidgetStylePipeline.mix(0x8A96AA, 0xFFFFFF, modalCloseHover);
        context.drawCenteredTextWithShadow(textRenderer, "✕", Math.round(closeX + closeSize * 0.5f), Math.round(closeY + 5.0f), closeColor);

        String title = switch (activeModal) {
            case DIRECT_CONNECT -> I18n.get("multiplayer.modal_direct_title");
            case ADD_SERVER -> I18n.get("multiplayer.modal_add_title");
            case EDIT_SERVER -> I18n.get("multiplayer.modal_edit_title");
            case DELETE_CONFIRM -> I18n.get("multiplayer.modal_delete_title");
            default -> "";
        };

        if (titleFont != null && titleFont.canRender(title)) {
            MsdfText.draw(context, titleFont, title, mx + 20.0f, my + 13.0f, 9.2f, 0xFFFFFFFF);
        } else {
            context.drawTextWithShadow(textRenderer, title, mx + 20, my + 13, 0xFFFFFFFF);
        }

        float fieldX = mx + 20.0f;
        float fieldW = mw - 40.0f;

        if (activeModal == Modal.DIRECT_CONNECT) {
            String addrLabel = I18n.get("multiplayer.server_address");
            if (textFont != null && textFont.canRender(addrLabel)) {
                MsdfText.draw(context, textFont, addrLabel, fieldX + 2.0f, my + 48.0f, 7.0f, 0xFF94A3B8);
            } else {
                context.drawTextWithShadow(textRenderer, addrLabel, Math.round(fieldX + 2.0f), my + 48, 0xFF94A3B8);
            }

            boolean afFoc = addressField.isFocused();
            RoundedRectPipeline.draw(context, fieldX, my + 62.0f, fieldW, 24.0f, afFoc ? 0xF0090D14 : 0xCC0B0F17);
            RoundedRectPipeline.drawOutline(context, fieldX, my + 62.0f, fieldW, 24.0f, afFoc ? NeonEdge.cool() : 0xFF242E40);
            if (afFoc) {
                PanelPipeline.draw(context, fieldX + 2.0f, my + 65.0f, 1.6f, 18.0f, NeonEdge.cool());
            }
            addressField.render(context, mouseX, mouseY, 0.0f);
        } else if (activeModal == Modal.DELETE_CONFIRM) {
            String prompt = I18n.get("multiplayer.modal_delete_prompt");
            if (textFont != null && textFont.canRender(prompt)) {
                MsdfText.drawCenteredOn(context, textFont, prompt, mx + mw * 0.5f, my + 52.0f, 7.5f, 0xFFCCD5E4);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, prompt, mx + mw / 2, my + 52, 0xFFCCD5E4);
            }
            if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                String delName = entries.get(selectedIndex).info.name;
                if (titleFont != null && titleFont.canRender(delName)) {
                    MsdfText.drawCenteredOn(context, titleFont, delName, mx + mw * 0.5f, my + 72.0f, 8.8f, NeonEdge.cool());
                } else {
                    context.drawCenteredTextWithShadow(textRenderer, delName, mx + mw / 2, my + 72, NeonEdge.cool());
                }
            }
        } else {
            String nameLabel = I18n.get("multiplayer.server_name");
            if (textFont != null && textFont.canRender(nameLabel)) {
                MsdfText.draw(context, textFont, nameLabel, fieldX + 2.0f, my + 42.0f, 7.0f, 0xFF94A3B8);
            } else {
                context.drawTextWithShadow(textRenderer, nameLabel, Math.round(fieldX + 2.0f), my + 42, 0xFF94A3B8);
            }

            boolean nfFoc = nameField.isFocused();
            RoundedRectPipeline.draw(context, fieldX, my + 56.0f, fieldW, 24.0f, nfFoc ? 0xF0090D14 : 0xCC0B0F17);
            RoundedRectPipeline.drawOutline(context, fieldX, my + 56.0f, fieldW, 24.0f, nfFoc ? NeonEdge.cool() : 0xFF242E40);
            if (nfFoc) {
                PanelPipeline.draw(context, fieldX + 2.0f, my + 59.0f, 1.6f, 18.0f, NeonEdge.cool());
            }
            nameField.render(context, mouseX, mouseY, 0.0f);

            String addrLabel = I18n.get("multiplayer.server_address");
            if (textFont != null && textFont.canRender(addrLabel)) {
                MsdfText.draw(context, textFont, addrLabel, fieldX + 2.0f, my + 92.0f, 7.0f, 0xFF94A3B8);
            } else {
                context.drawTextWithShadow(textRenderer, addrLabel, Math.round(fieldX + 2.0f), my + 92, 0xFF94A3B8);
            }

            boolean afFoc = addressField.isFocused();
            RoundedRectPipeline.draw(context, fieldX, my + 106.0f, fieldW, 24.0f, afFoc ? 0xF0090D14 : 0xCC0B0F17);
            RoundedRectPipeline.drawOutline(context, fieldX, my + 106.0f, fieldW, 24.0f, afFoc ? NeonEdge.cool() : 0xFF242E40);
            if (afFoc) {
                PanelPipeline.draw(context, fieldX + 2.0f, my + 109.0f, 1.6f, 18.0f, NeonEdge.cool());
            }
            addressField.render(context, mouseX, mouseY, 0.0f);
        }

        // Action Buttons: Cancel (Left) & Confirm (Right)
        float btnY = my + mh - 36.0f;
        float btnW = (mw - 50.0f) * 0.5f;
        float btnH = 26.0f;
        float cancX = fieldX;
        float confX = fieldX + btnW + 10.0f;

        boolean cancHov = mouseX >= cancX && mouseX <= cancX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean confHov = mouseX >= confX && mouseX <= confX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        modalCancelHover = lerpHover(modalCancelHover, cancHov, deltaSeconds);
        modalConfirmHover = lerpHover(modalConfirmHover, confHov, deltaSeconds);

        WidgetStylePipeline.drawStyledButton(context, cancX, btnY, btnW, btnH,
            I18n.get("multiplayer.cancel"), 0, null, true, modalCancelHover, visible, 0);

        String confirmLabel = switch (activeModal) {
            case DIRECT_CONNECT -> I18n.get("multiplayer.connect");
            case DELETE_CONFIRM -> I18n.get("multiplayer.delete_confirm");
            default -> I18n.get("multiplayer.save");
        };
        int confBorder = (activeModal == Modal.DELETE_CONFIRM) ? 0xFFF87171 : 0xFF34D399;
        WidgetStylePipeline.drawStyledButton(context, confX, btnY, btnW, btnH,
            confirmLabel, 0, null, true, modalConfirmHover, visible, confBorder);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (activeModal != Modal.NONE) {
            return handleModalClick(click);
        }

        float headerY = 14.0f;
        float backX = 24.0f;
        float backW = 68.0f;
        float backH = 24.0f;
        if (inRect(click.x(), click.y(), backX, headerY, backW, backH)) {
            playClick();
            close();
            return true;
        }

        // Pagination buttons in header
        int totalPages = getTotalPages();
        float pageControlW = 140.0f;
        float pageX = width - 24.0f - pageControlW;
        float prevBtnX = pageX;
        float nextBtnX = pageX + pageControlW - 22.0f;
        float btnH = 22.0f;
        float btnW = 22.0f;

        if (inRect(click.x(), click.y(), prevBtnX, headerY, btnW, btnH)) {
            if (currentPage > 0) {
                currentPage--;
                playClick();
                return true;
            }
        }
        if (inRect(click.x(), click.y(), nextBtnX, headerY, btnW, btnH)) {
            if (currentPage < totalPages - 1) {
                currentPage++;
                playClick();
                return true;
            }
        }

        // Check dynamic compact server grid items
        float gridX = 24.0f;
        float gridY = 46.0f;
        float gridW = width - 48.0f;
        float gridH = height - gridY - 48.0f;

        int cols = getGridCols();
        int rows = getGridRows();
        int cardsPerPage = cols * rows;

        float gapX = 10.0f;
        float gapY = 8.0f;
        float cardW = (gridW - gapX * (cols - 1)) / (float) cols;
        float cardH = (gridH - gapY * (rows - 1)) / (float) rows;

        int startIndex = currentPage * cardsPerPage;
        int endIndex = Math.min(entries.size(), startIndex + cardsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            int col = slot % cols;
            int row = slot / cols;

            float cx = gridX + col * (cardW + gapX);
            float cy = gridY + row * (cardH + gapY);

            if (inRect(click.x(), click.y(), cx, cy, cardW, cardH)) {
                playClick();
                long now = Util.getMeasuringTimeMs();
                if (selectedIndex == i && (now - lastClickTime < 350L)) {
                    joinSelectedServer();
                } else {
                    selectedIndex = i;
                    lastClickTime = now;
                }
                return true;
            }
        }

        // Toolbar buttons
        float barY = height - 38.0f;
        float startX = 24.0f;
        float tBtnH = 26.0f;
        boolean canJoin = selectedIndex >= 0 && selectedIndex < entries.size();

        // 1. Join Server
        float joinW = 108.0f;
        if (canJoin && inRect(click.x(), click.y(), startX, barY, joinW, tBtnH)) {
            playClick();
            joinSelectedServer();
            return true;
        }

        // 2. Direct Connect
        float curX = startX + joinW + 8.0f;
        float bW = 96.0f;
        if (inRect(click.x(), click.y(), curX, barY, bW + 18.0f, tBtnH)) {
            playClick();
            openDirectConnectModal();
            return true;
        }
        curX += bW + 26.0f;

        // 3. Add Server
        if (inRect(click.x(), click.y(), curX, barY, bW, tBtnH)) {
            playClick();
            openAddServerModal();
            return true;
        }
        curX += bW + 8.0f;

        // 4. Edit
        if (canJoin && inRect(click.x(), click.y(), curX, barY, 60.0f, tBtnH)) {
            playClick();
            openEditServerModal();
            return true;
        }
        curX += 68.0f;

        // 5. Delete
        if (canJoin && inRect(click.x(), click.y(), curX, barY, 68.0f, tBtnH)) {
            playClick();
            openDeleteModal();
            return true;
        }
        curX += 76.0f;

        // 6. Refresh
        if (inRect(click.x(), click.y(), curX, barY, 74.0f, tBtnH)) {
            playClick();
            refreshEntries();
            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    private boolean handleModalClick(Click click) {
        int mw = getModalWidth();
        int mh = getModalHeight();
        int mx = getModalX();
        int my = getModalY();

        // Close button [✕]
        float closeX = mx + mw - 28.0f;
        float closeY = my + 9.0f;
        float closeSize = 18.0f;
        if (inRect(click.x(), click.y(), closeX, closeY, closeSize, closeSize)) {
            playClick();
            closeModal();
            return true;
        }

        // Action Buttons: Cancel (Left) & Confirm (Right)
        float btnY = my + mh - 36.0f;
        float btnW = (mw - 50.0f) * 0.5f;
        float btnH = 26.0f;
        float fieldX = mx + 20.0f;
        float cancX = fieldX;
        float confX = fieldX + btnW + 10.0f;

        if (inRect(click.x(), click.y(), cancX, btnY, btnW, btnH)) {
            playClick();
            closeModal();
            return true;
        }

        if (inRect(click.x(), click.y(), confX, btnY, btnW, btnH)) {
            playClick();
            confirmModal();
            return true;
        }

        // Direct focus to fields
        float fieldW = mw - 40.0f;
        float fieldH = 24.0f;

        if (activeModal == Modal.DIRECT_CONNECT) {
            if (inRect(click.x(), click.y(), fieldX, my + 62.0f, fieldW, fieldH)) {
                addressField.setFocused(true);
                setFocused(addressField);
                addressField.mouseClicked(click, false);
                return true;
            }
        } else if (activeModal == Modal.ADD_SERVER || activeModal == Modal.EDIT_SERVER) {
            if (inRect(click.x(), click.y(), fieldX, my + 56.0f, fieldW, fieldH)) {
                addressField.setFocused(false);
                nameField.setFocused(true);
                setFocused(nameField);
                nameField.mouseClicked(click, false);
                return true;
            }
            if (inRect(click.x(), click.y(), fieldX, my + 106.0f, fieldW, fieldH)) {
                nameField.setFocused(false);
                addressField.setFocused(true);
                setFocused(addressField);
                addressField.mouseClicked(click, false);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (activeModal != Modal.NONE) {
            if (nameField != null && nameField.isFocused()) {
                return nameField.charTyped(input);
            }
            if (addressField != null && addressField.isFocused()) {
                return addressField.charTyped(input);
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (activeModal != Modal.NONE) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                playClick();
                closeModal();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                playClick();
                confirmModal();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB && activeModal != Modal.DIRECT_CONNECT && activeModal != Modal.DELETE_CONFIRM) {
                if (nameField.isFocused()) {
                    nameField.setFocused(false);
                    addressField.setFocused(true);
                    setFocused(addressField);
                } else {
                    addressField.setFocused(false);
                    nameField.setFocused(true);
                    setFocused(nameField);
                }
                return true;
            }
            return super.keyPressed(input);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (currentPage > 0) {
                currentPage--;
                playClick();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
                playClick();
                return true;
            }
        }
        int cols = getGridCols();
        if (keyCode == GLFW.GLFW_KEY_UP && selectedIndex >= cols) {
            selectedIndex -= cols;
            updatePageForSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN && selectedIndex + cols < entries.size()) {
            selectedIndex += cols;
            updatePageForSelection();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && selectedIndex >= 0 && selectedIndex < entries.size()) {
            joinSelectedServer();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) && selectedIndex >= 0 && selectedIndex < entries.size()) {
            playClick();
            openDeleteModal();
            return true;
        }

        return super.keyPressed(input);
    }

    private void updatePageForSelection() {
        if (selectedIndex >= 0) {
            currentPage = selectedIndex / getCardsPerPage();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeModal != Modal.NONE) return true;
        if (verticalAmount > 0 && currentPage > 0) {
            currentPage--;
            playClick();
            return true;
        } else if (verticalAmount < 0 && currentPage < getTotalPages() - 1) {
            currentPage++;
            playClick();
            return true;
        }
        return true;
    }

    private void joinSelectedServer() {
        if (client == null || selectedIndex < 0 || selectedIndex >= entries.size()) return;
        ServerInfo info = entries.get(selectedIndex).info;
        ConnectScreen.connect(this, client, ServerAddress.parse(info.address), info, false, null);
    }

    private void openDirectConnectModal() {
        activeModal = Modal.DIRECT_CONNECT;
        int mx = getModalX();
        int my = getModalY();
        int mw = getModalWidth();
        nameField.setVisible(false);
        nameField.setFocused(false);
        addressField.setVisible(true);
        addressField.setDimensionsAndPosition(mw - 54, 18, mx + 27, my + 65);
        addressField.setText("");
        setFocused(addressField);
        addressField.setFocused(true);
    }

    private void openAddServerModal() {
        activeModal = Modal.ADD_SERVER;
        int mx = getModalX();
        int my = getModalY();
        int mw = getModalWidth();
        nameField.setVisible(true);
        addressField.setVisible(true);
        nameField.setDimensionsAndPosition(mw - 54, 18, mx + 27, my + 59);
        addressField.setDimensionsAndPosition(mw - 54, 18, mx + 27, my + 109);
        nameField.setText("Minecraft Server");
        addressField.setText("");
        setFocused(addressField);
        nameField.setFocused(false);
        addressField.setFocused(true);
    }

    private void openEditServerModal() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;
        activeModal = Modal.EDIT_SERVER;
        int mx = getModalX();
        int my = getModalY();
        int mw = getModalWidth();
        ServerInfo info = entries.get(selectedIndex).info;
        nameField.setVisible(true);
        addressField.setVisible(true);
        nameField.setDimensionsAndPosition(mw - 54, 18, mx + 27, my + 59);
        addressField.setDimensionsAndPosition(mw - 54, 18, mx + 27, my + 109);
        nameField.setText(info.name);
        addressField.setText(info.address);
        setFocused(nameField);
        nameField.setFocused(true);
        addressField.setFocused(false);
    }

    private void openDeleteModal() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;
        activeModal = Modal.DELETE_CONFIRM;
        nameField.setVisible(false);
        addressField.setVisible(false);
        setFocused(null);
    }

    private void confirmModal() {
        if (activeModal == Modal.DIRECT_CONNECT && client != null) {
            String ip = addressField.getText().trim();
            if (!ip.isEmpty()) {
                ServerInfo direct = new ServerInfo("Direct Connection", ip, ServerInfo.ServerType.OTHER);
                closeModal();
                ConnectScreen.connect(this, client, ServerAddress.parse(ip), direct, false, null);
                return;
            }
        } else if (activeModal == Modal.ADD_SERVER && serverList != null) {
            String name = nameField.getText().trim();
            String ip = addressField.getText().trim();
            if (!ip.isEmpty()) {
                ServerInfo info = new ServerInfo(name.isEmpty() ? "Server" : name, ip, ServerInfo.ServerType.OTHER);
                serverList.add(info, false);
                serverList.saveFile();
                refreshEntries();
            }
        } else if (activeModal == Modal.EDIT_SERVER && serverList != null && selectedIndex >= 0 && selectedIndex < entries.size()) {
            ServerInfo info = entries.get(selectedIndex).info;
            info.name = nameField.getText().trim();
            info.address = addressField.getText().trim();
            serverList.saveFile();
            refreshEntries();
        } else if (activeModal == Modal.DELETE_CONFIRM && serverList != null && selectedIndex >= 0 && selectedIndex < entries.size()) {
            serverList.remove(entries.get(selectedIndex).info);
            serverList.saveFile();
            selectedIndex = -1;
            refreshEntries();
        }
        closeModal();
    }

    private void closeModal() {
        activeModal = Modal.NONE;
        nameField.setVisible(false);
        addressField.setVisible(false);
        setFocused(null);
    }

    private boolean inRect(double x, double y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private void playClick() {
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f));
        }
    }

    public Screen getParentScreen() {
        return this.parent;
    }

    public void close() {
        pinger.cancel();
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(StrangeVisuals.BACK_SOUND, 0.9f, 1.0f));
        }
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }

    private static final class ServerEntry {
        final ServerInfo info;
        final Spring hover = new Spring(0.0, 260.0, 20.0);

        ServerEntry(ServerInfo info) {
            this.info = info;
        }
    }
}
