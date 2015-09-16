package org.cyclops.integrateddynamics.core.part.aspect;

import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.integrateddynamics.core.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.core.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.network.Network;
import org.cyclops.integrateddynamics.core.part.IPartState;
import org.cyclops.integrateddynamics.core.part.IPartType;
import org.cyclops.integrateddynamics.core.part.PartTarget;
import org.cyclops.integrateddynamics.core.part.aspect.property.AspectProperties;
import org.cyclops.integrateddynamics.core.part.aspect.property.AspectPropertyTypeInstance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An element that can be used inside parts to access a specific aspect of something to read/write.
 * @param <V> The value type.
 * @param <T> The value type type.
 * @author rubensworks
 */
public interface IAspect<V extends IValue, T extends IValueType<V>> {

    /**
     * @return The unique unlocalized name for this aspect.
     */
    public String getUnlocalizedName();

    /**
     * Add tooltip lines for this aspect when hovered in a gui.
     * @param lines The list to add lines to.
     * @param appendOptionalInfo If shift-to-show info should be added.
     */
    public void loadTooltip(List<String> lines, boolean appendOptionalInfo);

    /**
     * @return The type of value this aspect can handle.
     */
    public T getValueType();

    /**
     * Called inside part types for updating a part on a block.
     * @param network The network to update in.
     * @param partType The part type.
     * @param target The position that is targeted by the given part.
     * @param state The current state of the given part.
     * @param <P> The part type type.
     * @param <S> The part state.
     */
    public <P extends IPartType<P, S>, S extends IPartState<P>> void update(Network network, P partType, PartTarget target, S state);

    /**
     * @return If this aspect supports additional properties.
     * @param <P> The part type type
     * @param <S> The part state type
     */
    public <P extends IPartType<P, S>, S extends IPartState<P>> boolean hasProperties();

    /**
     * Get the current properties of this aspect in the given part.
     * * @param network The network to update in.
     * @param <P> The part type type.
     * @param <S> The part state.
     * @param partType The part type.
     * @param target The position that is targeted by the given part.
     * @param state The current state of the given part.
     * @return The current properties.
     */
    public <P extends IPartType<P, S>, S extends IPartState<P>> AspectProperties getProperties(P partType, PartTarget target, S state);

    /**
     * Set the new properties of this aspect in the given part.
     * @param <P> The part type type.
     * @param <S> The part state.
     * @param partType The part type.
     * @param target The position that is targeted by the given part.
     * @param state The current state of the given part.
     * @param properties The new properties.
     */
    public <P extends IPartType<P, S>, S extends IPartState<P>> void setProperties(P partType, PartTarget target, S state, AspectProperties properties);

    /**
     * @return The default properties for this aspect.
     */
    public AspectProperties getDefaultProperties();

    /**
     * These are the properties that are supported for this aspect.
     * It is possible that some deprecated properties are available inside the retrieved properties, so use
     * this to iterate over the values.
     * @return The types that are available for this aspect.
     */
    public Collection<AspectPropertyTypeInstance> getPropertyTypes();

    /**
     * This will only be called if this aspect has properties.
     * @return The gui container provider for the gui to configure the properties.
     */
    public IGuiContainerProvider getPropertiesGuiProvider();

    /**
     * Use this comparator for any comparisons with aspects.
     */
    public static class AspectComparator implements Comparator<IAspect> {

        private static AspectComparator INSTANCE = null;

        private AspectComparator() {

        }

        public static AspectComparator getInstance() {
            if(INSTANCE == null) INSTANCE = new AspectComparator();
            return INSTANCE;
        }

        @Override
        public int compare(IAspect o1, IAspect o2) {
            int comp = IValueType.ValueTypeComparator.getInstance().compare(o1.getValueType(), o2.getValueType());
            if(comp == 0) {
                return o1.getUnlocalizedName().compareTo(o2.getUnlocalizedName());
            }
            return comp;
        }
    }

}
