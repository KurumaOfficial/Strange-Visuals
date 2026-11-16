package dev.strangevisuals.ui;

import dev.strangevisuals.account.AccountStorage;
import dev.strangevisuals.account.StoredAccount;
import dev.strangevisuals.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Optional;

public final class AltManagerScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private ButtonWidget addBtn;
    private ButtonWidget selectBtn;
    private ButtonWidget deleteBtn;
    private ButtonWidget backBtn;
    private AccountListWidget accountList;
    private String errorMessage = null;

    public AltManagerScreen(Screen parent) {
        super(Text.translatable("strangevisuals.altmanager.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldW = 180;
        int addBtnW = 75;
        int startX = width / 2 - (fieldW + 4 + addBtnW) / 2;
        int topRowY = 38;

        nameField = new TextFieldWidget(textRenderer, startX, topRowY, fieldW, 20, Text.translatable("strangevisuals.altmanager.username"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.translatable("strangevisuals.altmanager.placeholder"));
        nameField.setTextPredicate(s -> s.isEmpty() || s.matches("^[a-zA-Z0-9_]*$"));
        addDrawableChild(nameField);

        addBtn = ButtonWidget.builder(Text.translatable("strangevisuals.altmanager.add"), b -> {
            String name = nameField.getText().trim();
            if (AccountStorage.isValidUsername(name)) {
                if (AccountStorage.add(name)) {
                    nameField.setText("");
                    errorMessage = null;
                    if (accountList != null) {
                        accountList.reload();
                    }
                    updateButtons();
                } else {
                    errorMessage = Text.translatable("strangevisuals.altmanager.error_exists").getString();
                }
            } else {
                errorMessage = Text.translatable("strangevisuals.altmanager.error_invalid").getString();
            }
        }).dimensions(startX + fieldW + 4, topRowY, addBtnW, 20).build();
        addDrawableChild(addBtn);

        int listY = 64;
        int listH = height - listY - 50;
        accountList = new AccountListWidget(client, width, listH, listY, 24);
        addDrawableChild(accountList);
        accountList.reload();

        int btnW = 98;
        int gap = 6;
        int totalBtnsW = btnW * 3 + gap * 2;
        int btnStartX = width / 2 - totalBtnsW / 2;
        int btnY = height - 38;

        selectBtn = ButtonWidget.builder(Text.translatable("strangevisuals.altmanager.use"), b -> {
            AccountListWidget.AccountEntry sel = accountList.getSelectedOrNull();
            if (sel != null) {
                apply(sel.account);
            }
        }).dimensions(btnStartX, btnY, btnW, 20).build();

        deleteBtn = ButtonWidget.builder(Text.translatable("strangevisuals.altmanager.delete"), b -> {
            AccountListWidget.AccountEntry sel = accountList.getSelectedOrNull();
            if (sel != null) {
                AccountStorage.remove(sel.account);
                accountList.reload();
                updateButtons();
            }
        }).dimensions(btnStartX + btnW + gap, btnY, btnW, 20).build();

        backBtn = ButtonWidget.builder(ScreenTexts.BACK, b -> close())
            .dimensions(btnStartX + (btnW + gap) * 2, btnY, btnW, 20).build();

        addDrawableChild(selectBtn);
        addDrawableChild(deleteBtn);
        addDrawableChild(backBtn);

        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelected = accountList != null && accountList.getSelectedOrNull() != null;
        if (selectBtn != null) {
            selectBtn.active = hasSelected;
        }
        if (deleteBtn != null) {
            deleteBtn.active = hasSelected;
        }
    }

    private String getActiveUsername() {
        if (client != null && client.getSession() != null) {
            return client.getSession().getUsername();
        }
        return "Player";
    }

    private void apply(StoredAccount account) {
        if (client == null || !AccountStorage.isValidUsername(account.username())) {
            return;
        }
        Session session = new Session(account.username(), account.uuid(), "0", Optional.empty(), Optional.empty());
        ((MinecraftClientAccessor) client).strangevisuals$setSession(session);
        errorMessage = null;
        if (accountList != null) {
            accountList.reload();
        }
        updateButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 12, 0xFFFFFFFF);

        String activeUser = getActiveUsername();
        String activeLabel = Text.translatable("strangevisuals.altmanager.active", "§a" + activeUser).getString();
        context.drawCenteredTextWithShadow(textRenderer, activeLabel, width / 2, 24, 0xFFFFFFFF);

        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, "§c⚠ " + errorMessage, width / 2, height - 52, 0xFFFF5555);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    public final class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountListWidget.AccountEntry> {
        public AccountListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(320, width - 40);
        }

        public void reload() {
            clearEntries();
            for (StoredAccount acc : AccountStorage.accounts()) {
                AccountEntry entry = new AccountEntry(acc);
                addEntry(entry);
                if (acc.username().equalsIgnoreCase(getActiveUsername())) {
                    setSelected(entry);
                }
            }
        }

        public final class AccountEntry extends AlwaysSelectedEntryListWidget.Entry<AccountEntry> {
            final StoredAccount account;

            public AccountEntry(StoredAccount account) {
                this.account = account;
            }

            @Override
            public Text getNarration() {
                return Text.literal(account.username());
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickProgress) {
                int x = this.getX();
                int y = this.getY();
                int w = this.getWidth();
                int h = this.getHeight();

                boolean isSelected = getSelectedOrNull() == this;
                boolean isActive = account.username().equalsIgnoreCase(getActiveUsername());

                if (isSelected) {
                    context.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
                }

                int nameColor = isActive ? 0xFF55FF55 : (hovered ? 0xFFFFFFA0 : 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, account.username(), x + 6, y + (h - 8) / 2, nameColor);

                if (isActive) {
                    context.drawTextWithShadow(textRenderer, "✔ [Активен]", x + w - 75, y + (h - 8) / 2, 0xFF55FF55);
                }
            }

            @Override
            public boolean mouseClicked(Click click, boolean doubleClick) {
                if (doubleClick) {
                    apply(account);
                    return true;
                }
                setSelected(this);
                updateButtons();
                return true;
            }
        }
    }
}
