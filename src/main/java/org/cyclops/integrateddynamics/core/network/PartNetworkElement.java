package org.cyclops.integrateddynamics.core.network;

import lombok.Data;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integrateddynamics.api.network.*;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartState;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import java.util.List;

/**
 * A network element for parts.
 * @author rubensworks
 */
@Data
public class PartNetworkElement<P extends IPartType<P, S>, S extends IPartState<P>> implements IPartNetworkElement<P, S>, IEnergyConsumingNetworkElement {

    private final P part;
    private final PartTarget target;

    protected static DimPos getCenterPos(PartTarget target) {
        return target.getCenter().getPos();
    }

    protected static EnumFacing getCenterSide(PartTarget target) {
        return target.getCenter().getSide();
    }

    protected static DimPos getTargetPos(PartTarget target) {
        return target.getTarget().getPos();
    }

    protected static EnumFacing getTargetSide(PartTarget target) {
        return target.getTarget().getSide();
    }

    @Override
    public IPartContainer getPartContainer() {
        return PartHelpers.getPartContainer(getCenterPos(getTarget()));
    }

    @Override
    public S getPartState() {
        IPartContainer partContainer = getPartContainer();
        if(partContainer != null) {
            return (S) partContainer.getPartState(getCenterSide(getTarget()));
        } else {
            throw new IllegalStateException(String.format("The part container at %s could not be found.", getCenterSide(getTarget())));
        }
    }

    @Override
    public int getConsumptionRate() {
        return getPart().getConsumptionRate(getPartState());
    }

    @Override
    public void postUpdate(INetwork network, boolean updated) {
        part.postUpdate(NetworkHelpers.getPartNetwork(network), network, getTarget(), getPartState(), updated);
    }

    @Override
    public int getUpdateInterval() {
        return part.getUpdateInterval(getPartState());
    }

    @Override
    public boolean isUpdate() {
        return part.isUpdate(getPartState());
    }

    @Override
    public void update(INetwork network) {
        part.update(network, NetworkHelpers.getPartNetwork(network), getTarget(), getPartState());
    }

    @Override
    public void beforeNetworkKill(INetwork network) {
        part.beforeNetworkKill(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void afterNetworkAlive(INetwork network) {
        part.afterNetworkAlive(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void afterNetworkReAlive(INetwork network) {
        part.afterNetworkReAlive(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void addDrops(List<ItemStack> itemStacks, boolean dropMainElement) {
        part.addDrops(getTarget(), getPartState(), itemStacks, dropMainElement);
    }

    @Override
    public boolean onNetworkAddition(INetwork network) {
        IPartNetwork partNetwork = NetworkHelpers.getPartNetwork(network);
        boolean res = partNetwork.addPart(getPartState().getId(), getTarget().getCenter());
        if(res) {
            part.onNetworkAddition(network, partNetwork, target, getPartState());
        }
        return res;
    }

    @Override
    public void onNetworkRemoval(INetwork network) {
        IPartNetwork partNetwork = NetworkHelpers.getPartNetwork(network);
        partNetwork.removePart(getPartState().getId());
        part.onNetworkRemoval(network, partNetwork, target, getPartState());
    }

    @Override
    public void onPreRemoved(INetwork network) {
        part.onPreRemoved(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void onPostRemoved(INetwork network) {
        part.onPostRemoved(network, NetworkHelpers.getPartNetwork(network), target, getPartState());
    }

    @Override
    public void onNeighborBlockChange(INetwork network, IBlockAccess world, Block neighborBlock) {
        part.onBlockNeighborChange(network, NetworkHelpers.getPartNetwork(network), target, getPartState(), world, neighborBlock);
    }

    @Override
    public P getNetworkEventListener() {
        return getPart();
    }

    public boolean equals(Object o) {
        return o instanceof IPartNetworkElement && compareTo((INetworkElement) o) == 0;
    }

    @Override
    public int hashCode() {
        int result = part.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public int compareTo(INetworkElement o) {
        if(o instanceof IPartNetworkElement) {
            IPartNetworkElement p = (IPartNetworkElement) o;
            int compPart = Integer.compare(part.hashCode(), p.getPart().hashCode());
            if(compPart == 0) {
                int compPos = getCenterPos(getTarget()).compareTo(getCenterPos(p.getTarget()));
                if(compPos == 0) {
                    return getCenterSide(getTarget()).compareTo(getCenterSide(p.getTarget()));
                }
                return compPos;
            }
            return compPart;
        }
        return Integer.compare(hashCode(), o.hashCode());
    }
}
