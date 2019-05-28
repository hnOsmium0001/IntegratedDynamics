package org.cyclops.integrateddynamics.core.client.gui.container;

import com.google.common.collect.BiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.component.button.GuiButtonText;
import org.cyclops.cyclopscore.client.gui.component.input.GuiNumberField;
import org.cyclops.cyclopscore.client.gui.container.GuiContainerExtended;
import org.cyclops.cyclopscore.helper.GuiHelpers;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integrateddynamics.GeneralConfig;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.*;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.client.gui.GuiTextFieldDropdown;
import org.cyclops.integrateddynamics.core.client.gui.IDropdownEntry;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartSettings;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gui for part settings.
 * @author rubensworks
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class GuiPartSettings extends GuiContainerExtended {

    public static final int BUTTON_SAVE = 0;
    public static final int BUTTON_NEW_CHANNEL = 1;

    private final PartTarget target;
    private final IPartContainer partContainer;
    private final IPartType partType;

    private BiMap<String, Integer> channelMappings;

    private GuiNumberField numberFieldUpdateInterval = null;
    private GuiNumberField numberFieldPriority = null;
    private GuiTextFieldDropdown<String> dropdownFieldChannel = null;
    private Map<String, ChannelDropdownEntry> dropdownChannelEntries;
    private GuiTextFieldDropdown<EnumFacing> dropdownFieldSide = null;
    private List<SideDropdownEntry> dropdownSideEntries;

    /**
     * Make a new instance.
     * @param target The target.
     * @param player The player.
     * @param partContainer The part container.
     * @param partType The part type.
     */
    public GuiPartSettings(EntityPlayer player, PartTarget target, IPartContainer partContainer, IPartType partType) {
        this(new ContainerPartSettings(player, target, partContainer, partType), player, target, partContainer, partType);
    }

    public GuiPartSettings(ContainerPartSettings containerPartSettings, EntityPlayer player, PartTarget target, IPartContainer partContainer, IPartType partType) {
        super(containerPartSettings);
        this.target = target;
        this.partContainer = partContainer;
        this.partType = partType;

        putButtonAction(BUTTON_SAVE, (buttonId, gui, container) -> onSave());
    }

    protected void onSave() {
        IntegratedDynamics._instance.getGuiHandler().setTemporaryData(ExtendedGuiHandler.PART, getTarget().getCenter().getSide());
        try {
            ContainerPartSettings container = (ContainerPartSettings) getContainer();
            if (isFieldSideEnabled()) {
                EnumFacing selectedSide = dropdownFieldSide.getSelectedDropdownPossibility() == null ? null : dropdownFieldSide.getSelectedDropdownPossibility().getValue();
                int side = selectedSide != null && selectedSide != getDefaultSide() ? selectedSide.ordinal() : -1;
                ValueNotifierHelpers.setValue(getContainer(), container.getLastSideValueId(), side);
            }
            if (isFieldUpdateIntervalEnabled()) {
                int updateInterval = numberFieldUpdateInterval.getInt();
                ValueNotifierHelpers.setValue(getContainer(), container.getLastUpdateValueId(), updateInterval);
            }
            if (isFieldPriorityEnabled()) {
                int priority = numberFieldPriority.getInt();
                ValueNotifierHelpers.setValue(getContainer(), container.getLastPriorityValueId(), priority);
            }
            if (isFieldChannelEnabled()) {
                String selectedChannel = dropdownFieldChannel.getSelectedDropdownPossibility() == null ? null : dropdownFieldChannel.getSelectedDropdownPossibility().getValue();
                int channel = selectedChannel != null ? channelMappings.get(selectedChannel) : -1;
                ValueNotifierHelpers.setValue(getContainer(), container.getLastChannelValueId(), channel);
            }
        } catch (NumberFormatException e) { }
    }

    @Override
    public String getGuiTexture() {
        return getContainer().getGuiProvider().getModGui().getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI)
                + "part_settings.png";
    }

    protected EnumFacing getCurrentSide() {
        return getTarget().getTarget().getSide();
    }

    protected EnumFacing getDefaultSide() {
        return getTarget().getCenter().getSide().getOpposite();
    }

    protected String getSideText(EnumFacing side) {
        return side.getName().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        if (isFieldSideEnabled()) {
            dropdownSideEntries = Arrays.stream(EnumFacing.VALUES).map(SideDropdownEntry::new).collect(Collectors.toList());
            dropdownFieldSide = new GuiTextFieldDropdown(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + getFieldSideY(),
                    70, 14, true, Sets.newHashSet(dropdownSideEntries));
            setSideInDropdownField(getCurrentSide());
            dropdownFieldSide.setMaxStringLength(15);
            dropdownFieldSide.setVisible(true);
            dropdownFieldSide.setTextColor(16777215);
            dropdownFieldSide.setCanLoseFocus(true);
        }

        if (isFieldUpdateIntervalEnabled()) {
            numberFieldUpdateInterval = new GuiNumberField(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + getFieldUpdateIntervalY(), 70, 14, true, true);
            numberFieldUpdateInterval.setMaxStringLength(15);
            numberFieldUpdateInterval.setVisible(true);
            numberFieldUpdateInterval.setTextColor(16777215);
            numberFieldUpdateInterval.setCanLoseFocus(true);
            ContainerPartSettings container = (ContainerPartSettings) getContainer();
            numberFieldUpdateInterval.setMinValue(container.getPartType().getMinimumUpdateInterval(container.getPartState()));
        }

        if (isFieldPriorityEnabled()) {
            numberFieldPriority = new GuiNumberField(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + getFieldPriorityY(), 70, 14, true, true);
            numberFieldPriority.setPositiveOnly(false);
            numberFieldPriority.setMaxStringLength(15);
            numberFieldPriority.setVisible(true);
            numberFieldPriority.setTextColor(16777215);
            numberFieldPriority.setCanLoseFocus(true);
        }

        if (isFieldChannelEnabled()) {
            Pair<IPartType, IPartState> state = PartPos.getPartData(target.getCenter());
            // TODO
            INetwork network = null;
            channelMappings = network.getChannelMappings();
            dropdownChannelEntries = channelMappings.entrySet().stream()
                    .map(ChannelDropdownEntry::new)
                    .collect(Collectors.toMap(entry -> entry.channelName, Function.identity()));
            dropdownFieldChannel = new GuiTextFieldDropdown(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + getFieldChannelY(), 70, 14, true, Sets.newHashSet(dropdownChannelEntries.values()));
            dropdownFieldChannel.setMaxStringLength(15);
            dropdownFieldChannel.setVisible(true);
            dropdownFieldChannel.setTextColor(16777215);
            dropdownFieldChannel.setCanLoseFocus(true);
            dropdownFieldChannel.setEnabled(isChannelEnabled());
        }

        String save = L10NHelpers.localize("gui.integrateddynamics.button.save");
        buttonList.add(new GuiButtonText(BUTTON_SAVE, this.guiLeft + 178, this.guiTop + 8, fontRenderer.getStringWidth(save) + 6, 16, save, true));
        buttonList.add(new GuiButtonText(BUTTON_NEW_CHANNEL, this.guiLeft + 178, this.guiTop + getFieldChannelY(), fontRenderer.getCharWidth('+'), 16, "+", true));

        this.refreshValues();
    }

    protected int getFieldSideY() {
        return 9;
    }

    protected int getFieldUpdateIntervalY() {
        return 34;
    }

    protected int getFieldPriorityY() {
        return 59;
    }

    protected int getFieldChannelY() {
        return 84;
    }

    protected boolean isFieldSideEnabled() {
        return true;
    }

    protected boolean isFieldUpdateIntervalEnabled() {
        return true;
    }

    protected boolean isFieldPriorityEnabled() {
        return true;
    }

    protected boolean isFieldChannelEnabled() {
        return true;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!this.checkHotbarKeys(keyCode)) {
            if (!(isFieldUpdateIntervalEnabled() && this.numberFieldUpdateInterval.textboxKeyTyped(typedChar, keyCode))
                    && !(isFieldPriorityEnabled() && this.numberFieldPriority.textboxKeyTyped(typedChar, keyCode))
                    && !(isFieldChannelEnabled() && this.dropdownFieldChannel.textboxKeyTyped(typedChar, keyCode))
                    && !(isFieldSideEnabled() && this.dropdownFieldSide.textboxKeyTyped(typedChar, keyCode))) {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isFieldSideEnabled()) {
            this.dropdownFieldSide.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (isFieldUpdateIntervalEnabled()) {
            this.numberFieldUpdateInterval.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (isFieldPriorityEnabled()) {
            this.numberFieldPriority.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (isFieldChannelEnabled()) {
            this.dropdownFieldChannel.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        if (isFieldSideEnabled()) {
            fontRenderer.drawString(L10NHelpers.localize("gui.integrateddynamics.partsettings.side"), guiLeft + 8, guiTop + getFieldSideY() + 3, Helpers.RGBToInt(0, 0, 0));
            dropdownFieldSide.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        }
        if (isFieldUpdateIntervalEnabled()) {
            fontRenderer.drawString(L10NHelpers.localize("gui.integrateddynamics.partsettings.update_interval"), guiLeft + 8, guiTop + getFieldUpdateIntervalY() + 3, Helpers.RGBToInt(0, 0, 0));
            numberFieldUpdateInterval.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        }
        if (isFieldPriorityEnabled()) {
            fontRenderer.drawString(L10NHelpers.localize("gui.integrateddynamics.partsettings.priority"), guiLeft + 8, guiTop + getFieldPriorityY() + 3, Helpers.RGBToInt(0, 0, 0));
            numberFieldPriority.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        }
        if (isFieldChannelEnabled()) {
            fontRenderer.drawString(L10NHelpers.localize("gui.integrateddynamics.partsettings.channel"), guiLeft + 8, guiTop + getFieldChannelY() + 3, isChannelEnabled() ? Helpers.RGBToInt(0, 0, 0) : Helpers.RGBToInt(100, 100, 100));
            dropdownFieldChannel.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (!isChannelEnabled()) {
            GuiHelpers.renderTooltip(this, 8, 87, 100, 20, mouseX, mouseY,
                    () -> Lists.newArrayList(L10NHelpers.localize("gui.integrateddynamics.partsettings.channel.disabledinfo")));
        }
    }

    protected boolean isChannelEnabled() {
        return GeneralConfig.energyConsumptionMultiplier > 0;
    }

    @Override
    protected int getBaseXSize() {
        return 214;
    }

    @Override
    protected int getBaseYSize() {
        return 191;
    }

    protected void setSideInDropdownField(EnumFacing side) {
        dropdownFieldSide.selectPossibility(dropdownSideEntries.get(side.ordinal()));
    }

    protected void setChannelInDropdownField(String channelName) {
        dropdownFieldChannel.selectPossibility(dropdownChannelEntries.get(channelName));
    }

    @Override
    public void onUpdate(int valueId, NBTTagCompound value) {
        ContainerPartSettings container = (ContainerPartSettings) getContainer();
        if (isFieldSideEnabled() && valueId == container.getLastSideValueId()) {
            int side = container.getLastSideValue();
            setSideInDropdownField(side == -1 ? getDefaultSide() : EnumFacing.VALUES[side]);
        }
        if (isFieldUpdateIntervalEnabled() && valueId == container.getLastUpdateValueId()) {
            numberFieldUpdateInterval.setText(Integer.toString(container.getLastUpdateValue()));
        }
        if (isFieldPriorityEnabled() && valueId == container.getLastPriorityValueId()) {
            numberFieldPriority.setText(Integer.toString(container.getLastPriorityValue()));
        }
        if (isFieldChannelEnabled() && valueId == container.getLastChannelValueId()) {
            String lastChannelName = channelMappings.inverse().get(container.getLastChannelValue());
            setChannelInDropdownField(lastChannelName);
        }
    }

    public class ChannelDropdownEntry implements IDropdownEntry<String> {

        private final String channelName;
        private final int channelID;

        public ChannelDropdownEntry(Map.Entry<String, Integer> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public ChannelDropdownEntry(String channelName, int channelID) {
            this.channelName = channelName;
            this.channelID = channelID;
        }

        @Override
        public String getMatchString() {
            return channelName + ": " + channelID;
        }

        @Override
        public String getDisplayString() {
            return getMatchString();
        }

        @Override
        public List<String> getTooltip() {
            return Collections.emptyList();
        }

        @Override
        public String getValue() {
            return this.channelName;
        }
    }


    public class SideDropdownEntry implements IDropdownEntry<EnumFacing> {

        private final EnumFacing side;

        public SideDropdownEntry(EnumFacing side) {
            this.side = side;
        }

        @Override
        public String getMatchString() {
            return getSideText(side);
        }

        @Override
        public String getDisplayString() {
            return (getDefaultSide() == this.side ? TextFormatting.YELLOW : "") + getMatchString();
        }

        @Override
        public List<String> getTooltip() {
            return Collections.emptyList();
        }

        @Override
        public EnumFacing getValue() {
            return this.side;
        }
    }

}
