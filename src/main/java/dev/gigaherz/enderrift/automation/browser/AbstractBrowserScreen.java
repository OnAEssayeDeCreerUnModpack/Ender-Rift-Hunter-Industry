package dev.gigaherz.enderrift.automation.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.gigaherz.enderrift.EnderRiftMod;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

public abstract class AbstractBrowserScreen<T extends AbstractBrowserContainer> extends AbstractContainerScreen<T>
{
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(EnderRiftMod.MODID, "textures/gui/browser.png");
    private static final ResourceLocation TABS_TEXTURE = new ResourceLocation("minecraft:textures/gui/container/creative_inventory/tabs.png");

    private boolean isDragging;
    private int scrollY;
    private float scrollAcc = 0;

    private EditBox searchField;
    private Button sortModeButton;

    protected AbstractBrowserScreen(T container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        imageWidth = 194;
        imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);

        //Lighting.turnBackOn();
        drawCustomSlotTexts(graphics);
        //Lighting.turnOff();

        this.renderLowPowerOverlay(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderLowPowerOverlay(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (getMenu().isLowOnPower())
        {
            int l = leftPos + 7;
            int t = topPos + 17;
            int w = 162;
            int h = 54;
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            graphics.fill(l, t, l + w, t + h, 0x7f000000);
            long tm = Minecraft.getInstance().level.getGameTime() % 30;
            if (tm < 15)
            {
                graphics.drawCenteredString(font, "NO POWER", l + w / 2, t + h / 2 - font.lineHeight / 2, 0xFFFFFF);
            }
            RenderSystem.enableDepthTest();
        }
    }

    protected ResourceLocation getBackgroundTexture()
    {
        return BACKGROUND_TEXTURE;
    }

    @Override
    public void init()
    {
        super.init();

        addRenderableWidget(this.sortModeButton = Button.builder(Component.literal(""), (btn) -> {
            SortMode mode = getMenu().sortMode;
            switch (mode)
            {
                case ALPHABETIC:
                    mode = SortMode.STACK_SIZE;
                    break;
                case STACK_SIZE:
                    mode = SortMode.ALPHABETIC;
                    break;
            }

            changeSorting(mode);
        }).pos(leftPos - 22, topPos + 12).size(20, 20).build());

        //Keyboard.enableRepeatEvents(true);
        addRenderableWidget(this.searchField = new EditBox(this.font, leftPos + 114, topPos + 6, 71, this.font.lineHeight, Component.literal(""))
        {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
            {
                if (mouseX >= (double) this.getX() && mouseX < (double) (this.getX() + this.width)
                        && mouseY >= (double) this.getY() && mouseY < (double) (this.getY() + this.height))
                {
                    if (mouseButton == 1 && !Strings.isNullOrEmpty(getValue()) && getValue().length() > 0)
                    {
                        setValue("");
                        return true;
                    }
                }

                return super.mouseClicked(mouseX, mouseY, mouseButton);
            }
        });

        searchField.setMaxLength(15);
        searchField.setBordered(false);
        searchField.setVisible(true);
        searchField.setTextColor(16777215);
        searchField.setCanLoseFocus(false);
        searchField.setFocused(true);
        searchField.setValue(getMenu().filterText);
        searchField.setResponder(this::updateSearchFilter);

        changeSorting(SortMode.STACK_SIZE);
    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_)
    {
        if (searchField.isFocused() && searchField.charTyped(p_charTyped_1_, p_charTyped_2_))
            return true;

        return this.getFocused() != null && this.getFocused().charTyped(p_charTyped_1_, p_charTyped_2_);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers)
    {
        if (searchField.isFocused())
        {
            if (searchField.keyPressed(key, scanCode, modifiers))
                return true;
            if (key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_TAB && key != GLFW.GLFW_KEY_ENTER)
                return false;
        }

        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int scaledWidth, int scaledHeight)
    {
        String s = searchField.getValue();
        super.resize(minecraft, scaledWidth, scaledHeight);
        searchField.setValue(s);
    }

    private void updateSearchFilter(String text)
    {
        getMenu().setFilterText(text);
    }

    private void changeSorting(SortMode mode)
    {
        switch (mode)
        {
            case ALPHABETIC:
                sortModeButton.setMessage(Component.literal("Az"));
                break;
            case STACK_SIZE:
                sortModeButton.setMessage(Component.literal("#"));
                break;
        }

        getMenu().setSortMode(mode);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int xMouse, int yMouse)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.blit(getBackgroundTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(getBackgroundTexture(), leftPos - 27, topPos + 8, 194, 0, 27, 28);

        RenderSystem.setShaderTexture(0, TABS_TEXTURE);

        boolean isEnabled = needsScrollBar();
        if (isEnabled)
            graphics.blit(getBackgroundTexture(), leftPos + 174, topPos + 18 + scrollY, 232, 0, 12, 15);
        else
            graphics.blit(getBackgroundTexture(), leftPos + 174, topPos + 18, 244, 0, 12, 15);

        searchField.render(graphics, xMouse, yMouse, partialTicks);
    }

    private void drawCustomSlotTexts(GuiGraphics graphics)
    {
        var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.leftPos, this.topPos, 300);
        for (int i = 0; i < AbstractBrowserContainer.SCROLL_SLOTS; ++i)
        {
            Slot slot = getMenu().slots.get(i);
            drawSlotText(graphics, slot);
        }
        poseStack.popPose();
    }

    private void drawSlotText(GuiGraphics graphics, Slot slotIn)
    {
        int xPosition = slotIn.x;
        int yPosition = slotIn.y;
        ItemStack stack = slotIn.getItem();

        if (stack.getCount() > 0)
        {
            int count = getMenu().getClient().getStackSizeForSlot(slotIn.index);

            if (count != 1)
            {
                String s = getSizeString(count);

                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                graphics.drawString(font, s, (float) (xPosition + 19 - 2 - font.width(s)), (float) (yPosition + 6 + 3), 16777215, true);
                //RenderSystem.enableDepthTest();
            }
        }
    }

    private String getSizeString(int count)
    {
        String s;
        if (count >= 1000000000)
            s = (count / 1000000000) + "B";
        else if (count >= 900000000)
            s = ".9B";
        else if (count >= 1000000)
            s = (count / 1000000) + "M";
        else if (count >= 900000)
            s = ".9M";
        else if (count >= 1000)
            s = (count / 1000) + "k";
        else if (count >= 900)
            s = ".9k";
        else
            s = String.valueOf(count);
        return s;
    }

    private boolean needsScrollBar()
    {
        int actualSlotCount = getMenu().getActualSlotCount();

        return actualSlotCount > AbstractBrowserContainer.SCROLL_SLOTS;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double something, double wheelDelta)
    {
        if (super.mouseScrolled(mouseX, mouseY, something, wheelDelta))
            return true;

        scrollAcc += wheelDelta * 120;

        final int h = 62;
        final int bitHeight = 15;
        final int actualSlotCount = getMenu().getActualSlotCount();
        final int rows = (int) Math.ceil(actualSlotCount / 9.0);

        if (rows > AbstractBrowserContainer.SCROLL_ROWS)
        {
            int scrollRows = rows - AbstractBrowserContainer.SCROLL_ROWS;

            int row = getMenu().scroll / 9;

            while (scrollAcc >= 120)
            {
                row -= 1;
                scrollAcc -= 120;
            }
            while (scrollAcc <= -120)
            {
                row += 1;
                scrollAcc += 120;
            }

            row = Math.max(0, Math.min(scrollRows, row));

            scrollY = row * (h - bitHeight) / scrollRows;

            getMenu().setScrollPos(row * 9);
        }
        else
        {
            scrollAcc = 0;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        // scroll
        {
            final int w = 12;
            final int h = 62;
            double mx = mouseX - 174 - leftPos;
            double my = mouseY - 18 - topPos;
            if (mx >= 0 && mx < w && my >= 0 && my < h)
            {
                updateScrollPos((int) my);
                isDragging = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void updateScrollPos(int my)
    {
        final int h = 62;
        final int bitHeight = 15;
        final int actualSlotCount = getMenu().getActualSlotCount();
        final int rows = (int) Math.ceil(actualSlotCount / 9.0);
        final int scrollRows = rows - AbstractBrowserContainer.SCROLL_ROWS;

        boolean isEnabled = scrollRows > 0;
        if (isEnabled)
        {
            double offset = (my - bitHeight / 2.0) * scrollRows / (h - bitHeight);
            int row = Math.round(Math.max(0, Math.min(scrollRows, (int) offset)));

            scrollY = row * (h - bitHeight) / scrollRows;

            final AbstractBrowserContainer container = getMenu();
            getMenu().setScrollPos(row * 9);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double p_mouseDragged_6_, double p_mouseDragged_8_)
    {
        boolean ret = false;

        double my = mouseY - 18 - topPos;
        if (isDragging)
        {
            updateScrollPos((int) my);
            ret = true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, p_mouseDragged_6_, p_mouseDragged_8_) || ret;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton)
    {
        boolean ret = false;

        double my = mouseY - 18 - topPos;
        if (isDragging)
        {
            updateScrollPos((int) my);
            isDragging = false;
            ret = true;
        }

        return super.mouseReleased(mouseX, mouseY, mouseButton) || ret;
    }
}