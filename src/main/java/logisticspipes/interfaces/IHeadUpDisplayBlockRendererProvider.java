package logisticspipes.interfaces;

/**
 * A HUD that belongs to a block, so it has to go when the block does.
 */
public interface IHeadUpDisplayBlockRendererProvider extends IHeadUpDisplayRendererProvider {

    boolean isHUDInvalid();

    /**
     * @return whether the level still holds this very block
     */
    boolean isHUDExistent();
}
