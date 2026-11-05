package logisticspipes.interfaces;

import java.util.List;

/**
 * Something that describes its settings in a few lines, which a module item saves into its stack.
 */
public interface IClientInformationProvider {

    /**
     * @return the lines, empty when there is nothing to say
     */
    List<String> getClientInformation();
}
