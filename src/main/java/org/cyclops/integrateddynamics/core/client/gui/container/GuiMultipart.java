package org.cyclops.integrateddynamics.core.client.gui.container;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import org.cyclops.cyclopscore.client.gui.container.ScrollingGuiContainer;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipart;
import org.cyclops.integrateddynamics.core.part.IPartContainer;
import org.cyclops.integrateddynamics.core.part.IPartState;
import org.cyclops.integrateddynamics.core.part.IPartType;
import org.cyclops.integrateddynamics.core.part.PartTarget;
import org.cyclops.integrateddynamics.core.part.aspect.IAspect;
import org.cyclops.integrateddynamics.core.part.aspect.IAspectVariable;
import org.cyclops.integrateddynamics.item.ItemVariable;

import java.awt.*;
import java.util.List;

/**
 * Gui for parts.
 * @author rubensworks
 */
@EqualsAndHashCode(callSuper = false)
@Data
public abstract class GuiMultipart<P extends IPartType<P, S> & IGuiContainerProvider, S extends IPartState<P>>
        extends ScrollingGuiContainer {

    private static final Rectangle ITEM_POSITION = new Rectangle(8, 17, 18, 18);

    private final PartTarget target;
    private final IPartContainer partContainer;
    private final P partType;
    private final S partState;

    private long lastUpdate = -1;

    /**
     * Make a new instance.
     * @param container The container to make the GUI for.
     */
    public GuiMultipart(ContainerMultipart<P, S> container) {
        super(container);
        this.target = container.getTarget();
        this.partContainer = container.getPartContainer();
        this.partType = container.getPartType();
        this.partState = container.getPartState();
    }

    protected abstract String getNameId();

    @Override
    public String getGuiTexture() {
        return getContainer().getGuiProvider().getMod().getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI)
               + getNameId() + ".png";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        FontRenderer fontRenderer = fontRendererObj;

        // Draw part name
        RenderHelpers.drawScaledCenteredString(fontRenderer, L10NHelpers.localize(getPartType().getUnlocalizedName()),
                this.guiLeft + offsetX + 6, this.guiTop + offsetY + 10, 70, Helpers.RGBToInt(0, 0, 0));

        // Draw aspects
        ContainerMultipart<?, ?> container = (ContainerMultipart) getScrollingInventoryContainer();
        for(int i = 0; i < container.getPageSize(); i++) {
            if(container.isElementVisible(i)) {
                GlStateManager.disableAlpha();
                GlStateManager.color(1, 1, 1, 1);

                // Background
                mc.renderEngine.bindTexture(texture);
                drawTexturedModalRect(guiLeft + offsetX + 9,
                        guiTop + offsetY + 18 + (ContainerMultipart.ASPECT_BOX_HEIGHT) * i, 0, 213, 160,
                        ContainerMultipart.ASPECT_BOX_HEIGHT - 1);

                // Aspect type info
                IAspect aspect = container.getVisibleElement(i);
                String aspectName = L10NHelpers.localize(aspect.getUnlocalizedName());
                RenderHelpers.drawScaledCenteredString(fontRenderer, aspectName,
                        this.guiLeft + offsetX + 26,
                        this.guiTop + offsetY + 25 + ContainerMultipart.ASPECT_BOX_HEIGHT * i,
                        60, Helpers.RGBToInt(40, 40, 40));

                // Current aspect value
                // Client-side, so we need to do a manual part update, but not every frame refresh.
                if(Minecraft.getMinecraft().theWorld.getWorldTime() > lastUpdate) {
                    lastUpdate = Minecraft.getMinecraft().theWorld.getWorldTime();
                    getPartType().update(getTarget(), getPartState());
                }
                IAspectVariable variable = getPartType().getVariable(getTarget(), getPartState(), aspect);
                String value = variable.getType().toCompactString(variable.getValue());
                fontRenderer.drawString(value, this.guiLeft + offsetX + 16,
                        this.guiTop + offsetY + 35 + ContainerMultipart.ASPECT_BOX_HEIGHT * i,
                        variable.getType().getDisplayColor());

                // Render target item
                // This could be cached if this would prove to be a bottleneck
                ItemStack itemStack = container.writeAspectInfo(new ItemStack(ItemVariable.getInstance()), aspect);
                Rectangle pos = getElementPosition(container, i, true);
                itemRender.renderItemAndEffectIntoGUI(itemStack, pos.x, pos.y);
            }
        }
    }

    protected Rectangle getElementPosition(ContainerMultipart<?, ?> container, int i, boolean absolute) {
        return new Rectangle(ITEM_POSITION.x + offsetX + (absolute ? this.guiLeft : 0),
                             ITEM_POSITION.y + ContainerMultipart.ASPECT_BOX_HEIGHT * i + offsetY + (absolute ? this.guiTop : 0),
                             ITEM_POSITION.width, ITEM_POSITION.height
        );
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ContainerMultipart<?, ?> container = (ContainerMultipart) getScrollingInventoryContainer();
        for(int i = 0; i < container.getPageSize(); i++) {
            if(container.isElementVisible(i)) {
                if(isPointInRegion(getElementPosition(container, i, false), new Point(mouseX, mouseY))) {
                    List<String> lines = Lists.newLinkedList();
                    container.getVisibleElement(i).loadTooltip(lines, true);
                    drawTooltip(lines, mouseX - this.guiLeft, mouseY - this.guiTop);
                }
            }
        }
    }

}
