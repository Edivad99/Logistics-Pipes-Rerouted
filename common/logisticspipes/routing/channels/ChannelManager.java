package logisticspipes.routing.channels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.ChannelInformationPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerIdentifier;

public class ChannelManager implements IChannelManager {

    private static final String DATA_NAME = LPConstants.ID + "_ChannelManager_SavedData";
    private final ChannelSavedData savedData;

    public ChannelManager(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            savedData = serverLevel.getDataStorage().computeIfAbsent(ChannelSavedData.TYPE);
        } else {
            savedData = new ChannelSavedData();
        }
    }

    @Override
    public List<ChannelInformation> getChannels() {
        return Collections.unmodifiableList(savedData.channels);
    }

    private boolean isChannelAllowedFor(ChannelInformation channel, Player player) {
        switch (channel.getRights()) {
            case PUBLIC:
                return true;
            case SECURED:
                final UUID secUUID = channel.getResponsibleSecurityID();
                final LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager
                    .getStation(secUUID);
                if (station != null) {
                    final SecuritySettings settings = station.getSecuritySettingsForPlayer(player, false);
                    if (settings != null) {
                        return settings.accessRoutingChannels;
                    }
                }
            case PRIVATE:
                return channel.getOwner().equals(PlayerIdentifier.get(player));
        }
        return false;
    }

    @Override
    public List<ChannelInformation> getAllowedChannels(Player player) {
        return savedData.channels.stream().filter(channel -> isChannelAllowedFor(channel, player)).toList();
    }

    @Override
    public ChannelInformation createNewChannel(String name, PlayerIdentifier owner,
        ChannelInformation.AccessRights rights, UUID responsibleSecurityID) {
        ChannelInformation channel =
            new ChannelInformation(name, UUID.randomUUID(), owner, rights, responsibleSecurityID);
        savedData.channels.add(channel);
        savedData.setDirty();
        sendUpdatePacketToClients(channel);
        return channel;
    }

    @Override
    public void updateChannelName(UUID channelIdentifier, String newName) {
        savedData.channels.stream().filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier))
            .forEach(channel -> {
                channel.setName(newName);
                sendUpdatePacketToClients(channel);
            });
        savedData.setDirty();
    }

    @Override
    public void updateChannelRights(UUID channelIdentifier, ChannelInformation.AccessRights rights,
        UUID responsibleSecurityID) {
        savedData.channels.stream().filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier))
            .forEach(channel -> {
                channel.setRights(rights);
                channel.setResponsibleSecurityID(responsibleSecurityID);
                sendUpdatePacketToClients(channel);
            });
        savedData.setDirty();
    }

    @Override
    public void removeChannel(UUID channelIdentifier) {
        Optional<ChannelInformation> optChannel = savedData.channels.stream()
            .filter(channel -> channel.getChannelIdentifier().equals(channelIdentifier)).findFirst();
        savedData.channels.removeIf(channel -> channel.getChannelIdentifier().equals(channelIdentifier));
        optChannel.ifPresent(channelInformation -> sendUpdatePacketToClients(
            new ChannelInformation(null, channelIdentifier, channelInformation.getOwner(),
                channelInformation.getRights(), null)));
        savedData.setDirty();
    }

    public void setChanged() {
        savedData.setDirty();
    }

    private void sendUpdatePacketToClients(ChannelInformation channel) {
        MainProxy.sendToAllPlayers(
            PacketHandler.getPacket(ChannelInformationPacket.class).setInformation(channel).setTargeted(false)
                .setCompressable(true));
    }

    public static class ChannelSavedData extends SavedData {

        private static final Codec<ChannelSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ChannelInformation.CODEC.listOf().optionalFieldOf("channels", List.of())
                .forGetter(ChannelSavedData::getChannels)
        ).apply(instance, ChannelSavedData::new));

        public static final SavedDataType<ChannelSavedData> TYPE =
            new SavedDataType<>(DATA_NAME, ChannelSavedData::new, CODEC);

        @Getter
        List<ChannelInformation> channels = new ArrayList<>();

        public ChannelSavedData() {
        }

        private ChannelSavedData(List<ChannelInformation> channels) {
            this.channels = new ArrayList<>(channels);
        }

        public ChannelSavedData setChannels(List<ChannelInformation> channels) {
            this.channels = channels;
            return this;
        }
    }
}
