package org.cyclops.integrateddynamics.core.client.gui.container;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cyclops.cyclopscore.client.gui.container.GuiContainerExtended;
import org.cyclops.cyclopscore.inventory.container.ExtendedInventoryContainer;

/**
 * @author hnOsmium0001
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class GuiChannelMappings extends GuiContainerExtended {

    /**
     * Make a new instance.
     *
     * @param container The container to make the GUI for.
     */
    public GuiChannelMappings(ExtendedInventoryContainer container) {
        super(container);
    }

    @Override
    public String getGuiTexture() {
        return null;
    }

}
