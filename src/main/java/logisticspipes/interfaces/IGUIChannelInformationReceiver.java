package logisticspipes.interfaces;

import logisticspipes.routing.channels.ChannelInformation;

/**
 * A screen that shows channels and is told about them by the server.
 */
public interface IGUIChannelInformationReceiver {

    /**
     * @param targeted true when this answers something the screen asked for, false when it is the
     *                 channel manager telling every watcher that a channel changed
     */
    void handleChannelInformation(ChannelInformation channel, boolean targeted);
}
