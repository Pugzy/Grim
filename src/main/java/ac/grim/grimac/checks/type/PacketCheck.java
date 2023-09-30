package ac.grim.grimac.checks.type;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.Collections;
import java.util.Set;

public interface PacketCheck extends AbstractCheck {

    default void onPacketReceive(final PacketReceiveEvent event) {
    }

    default void onPacketSend(final PacketSendEvent event) {
    }

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }

    default Set<PacketType.Play.Server> typesCheckedOnSend() {
        return Collections.emptySet();
    }

    default Set<PacketType.Play.Client> typesCheckedOnReceive() {
        return Collections.emptySet();
    }
}
