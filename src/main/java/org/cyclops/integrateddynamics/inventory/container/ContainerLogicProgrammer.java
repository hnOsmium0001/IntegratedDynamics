package org.cyclops.integrateddynamics.inventory.container;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.inventory.container.ScrollingInventoryContainer;
import org.cyclops.cyclopscore.inventory.slot.SlotSingleItem;
import org.cyclops.cyclopscore.persist.IDirtyMarkListener;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.block.BlockLogicProgrammer;
import org.cyclops.integrateddynamics.client.gui.GuiLogicProgrammer;
import org.cyclops.integrateddynamics.core.item.IVariableFacade;
import org.cyclops.integrateddynamics.core.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.core.logicprogrammer.ILogicProgrammerElement;
import org.cyclops.integrateddynamics.core.logicprogrammer.ILogicProgrammerElementType;
import org.cyclops.integrateddynamics.core.logicprogrammer.LogicProgrammerElementTypes;
import org.cyclops.integrateddynamics.item.ItemVariable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Container for the {@link org.cyclops.integrateddynamics.block.BlockLogicProgrammer}.
 * @author rubensworks
 */
public class ContainerLogicProgrammer extends ScrollingInventoryContainer<ILogicProgrammerElement> implements IDirtyMarkListener {

    public static final int OUTPUT_X = 232;
    public static final int OUTPUT_Y = 110;

    protected static final IItemPredicate<ILogicProgrammerElement> FILTERER = new IItemPredicate<ILogicProgrammerElement>(){

        @Override
        public boolean apply(ILogicProgrammerElement item, Pattern pattern) {
            return pattern.matcher(item.getMatchString()).matches();
        }
    };

    private final SimpleInventory writeSlot;
    private ILogicProgrammerElement activeElement = null;
    private SimpleInventory temporaryInputSlots = null;
    private L10NHelpers.UnlocalizedString lastError;
    private LoadConfigListener loadConfigListener;

    @SideOnly(Side.CLIENT)
    private GuiLogicProgrammer gui;

    /**
     * Make a new instance.
     * @param inventory   The player inventory.
     */
    public ContainerLogicProgrammer(InventoryPlayer inventory) {
        super(inventory, BlockLogicProgrammer.getInstance(), getElements(), FILTERER);
        this.writeSlot = new SimpleInventory(1, "writeSlot", 1);
        this.writeSlot.addDirtyMarkListener(this);
        this.writeSlot.addDirtyMarkListener(loadConfigListener = new LoadConfigListener());
        this.temporaryInputSlots = new SimpleInventory(0, "temporaryInput", 1);
        initializeSlots();
    }

    protected static List<ILogicProgrammerElement> getElements() {
        List<ILogicProgrammerElement> elements = Lists.newLinkedList();
        for(ILogicProgrammerElementType type: LogicProgrammerElementTypes.REGISTRY.getTypes()) {
            elements.addAll(type.createElements());
        }
        return elements;
    }

    @SideOnly(Side.CLIENT)
    public void setGui(GuiLogicProgrammer gui) {
        this.gui = gui;
    }

    @SideOnly(Side.CLIENT)
    public GuiLogicProgrammer getGui() {
        return this.gui;
    }

    protected void initializeSlots() {
        addSlotToContainer(new SlotSingleItem(writeSlot, 0, OUTPUT_X, OUTPUT_Y, ItemVariable.getInstance()));
        addPlayerInventory((InventoryPlayer) getPlayerIInventory(), 88, 131);
    }

    @Override
    public int getPageSize() {
        return 10;
    }

    @Override
    protected int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public void setActiveElementById(String typeId, String elementId) {
        ILogicProgrammerElementType type = LogicProgrammerElementTypes.REGISTRY.getType(typeId);
        if (type != null) {
            ILogicProgrammerElement element = type.getByName(elementId);
            if(!LogicProgrammerElementTypes.areEqual(getActiveElement(), element)) {
                setActiveElement(element, 0, 0);
                onDirty();
            }
        } else {
            setActiveElement(null, 0, 0);
        }
    }

    /**
     * Set the new active element.
     * @param activeElement The new element.
     * @param baseX The slots X coordinate
     * @param baseY The slots Y coordinate
     */
    public void setActiveElement(ILogicProgrammerElement activeElement, int baseX, int baseY) {
        this.lastError = null;
        if(this.activeElement != null) {
            this.activeElement.deactivate();
        }
        this.activeElement = activeElement;

        // This assumes that there is only one other slot, the remaining slots will be erased!
        // (We can do this because they are all ghost slots)
        inventoryItemStacks = Lists.newArrayList();
        inventorySlots = Lists.newArrayList();
        initializeSlots();
        this.temporaryInputSlots.removeDirtyMarkListener(this);
        if(activeElement != null) {
            activeElement.activate();
        }
        this.temporaryInputSlots = new SimpleInventory(activeElement == null ? 0 : activeElement.getRenderPattern().getSlotPositions().length, "temporaryInput", 1);
        temporaryInputSlots.addDirtyMarkListener(this);
        if(activeElement != null) {
            Pair<Integer, Integer>[] slotPositions = activeElement.getRenderPattern().getSlotPositions();
            for (int i = 0; i < temporaryInputSlots.getSizeInventory(); i++) {
                SlotSingleItem slot = new SlotSingleItem(temporaryInputSlots, i, 1 + baseX + slotPositions[i].getLeft(),
                        1 + baseY + slotPositions[i].getRight(), ItemVariable.getInstance());
                slot.setPhantom(true);
                addSlotToContainer(slot);
            }
        }
    }

    public boolean canWriteActiveElementPre() {
        if(activeElement != null) {
            return activeElement.canWriteElementPre();
        }
        return false;
    }

    public boolean canWriteActiveElement() {
        if(!canWriteActiveElementPre()) {
            return false;
        }
        lastError = activeElement.validate();
        return lastError == null;
    }

    public ILogicProgrammerElement getActiveElement() {
        return activeElement;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.worldObj.isRemote) {
            ItemStack itemStack = writeSlot.getStackInSlot(0);
            if(itemStack != null) {
                player.dropPlayerItemWithRandomChoice(itemStack, false);
            }
        }
    }

    protected ItemStack writeElementInfo() {
        ItemStack itemStack = writeSlot.getStackInSlot(0);
        return getActiveElement().writeElement(itemStack.copy());
    }

    @Override
    public void onDirty() {
        ILogicProgrammerElement activeElement = getActiveElement();
        if(activeElement != null) {
            for (int i = 0; i < temporaryInputSlots.getSizeInventory(); i++) {
                ItemStack itemStack = temporaryInputSlots.getStackInSlot(i);
                activeElement.onInputSlotUpdated(i, itemStack);
            }
        }

        ItemStack itemStack = writeSlot.getStackInSlot(0);
        if(canWriteActiveElement() && itemStack != null) {
            ItemStack outputStack = writeElementInfo();
            writeSlot.removeDirtyMarkListener(this);
            writeSlot.setInventorySlotContents(0, outputStack);
            writeSlot.addDirtyMarkListener(this);
        }
    }

    protected void loadConfigFrom(ItemStack itemStack) {
        // Only do this client-side, a packet will be sent to do the same server-side.
        if(MinecraftHelpers.isClientSide()) {
            IVariableFacadeHandlerRegistry registry = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class);
            IVariableFacade variableFacade = registry.handle(itemStack);
            for(ILogicProgrammerElement element : getElements()) {
                if(element.isFor(variableFacade)) {
                    getGui().handleElementActivation(element);
                }
            }
        }
    }

    public L10NHelpers.UnlocalizedString getLastError() {
        return this.lastError;
    }

    public IInventory getTemporaryInputSlots() {
        return this.temporaryInputSlots;
    }

    public boolean hasWriteItemInSlot() {
        return this.writeSlot.getStackInSlot(0) != null;
    }

    protected class LoadConfigListener implements IDirtyMarkListener {

        @Override
        public void onDirty() {
            // Currently disabled, this requires quite complex negotiation between C and S, not too mention
            // any other players having the gui open!
            /*if ((temporaryInputSlots == null || temporaryInputSlots.isEmpty())
                    && (activeElement == null || activeElement.canCurrentlyReadFromOtherItem())) {
                ItemStack itemStack = writeSlot.getStackInSlot(0);
                if (itemStack != null) {
                    ContainerLogicProgrammer.this.loadConfigFrom(itemStack);
                }
            }*/
        }

    }

}
