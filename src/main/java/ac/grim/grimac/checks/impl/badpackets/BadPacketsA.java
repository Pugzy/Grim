package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

import java.util.Collections;
import java.util.Set;

@CheckData(name = "BadPacketsA")
public class BadPacketsA extends Check implements PacketCheck {
    int lastSlot = -1;

    public BadPacketsA(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);

            int slot = packet.getSlot();

            if (slot == lastSlot) {
                flagAndAlert("slot=" + slot);
            }

            lastSlot = packet.getSlot();
        }
    }

    @Override
    public Set<PacketType.Play.Client> typesCheckedOnReceive() {
        return Collections.singleton(PacketType.Play.Client.HELD_ITEM_CHANGE);
    }
}
