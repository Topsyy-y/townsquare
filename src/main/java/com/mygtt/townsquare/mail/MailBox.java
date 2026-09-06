package com.mygtt.townsquare.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mygtt.townsquare.Townsquare;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Correo entre jugadores del servidor. Sin backend: los buzones viven en un attachment
 * del overworld, y por eso funcionan con el destinatario desconectado.
 *
 * <p>Los buzones van indexados por <b>nombre en minusculas</b>, no por UUID: es lo que el
 * remitente conoce de alguien que no esta conectado. Limitacion asumida: si un jugador se
 * cambia el nombre, pierde el buzon viejo.
 */
public final class MailBox {
	/** Una carta: texto, o un item (libro, paquete), o ambas cosas. */
	public record Mail(String sender, String text, Optional<ItemStack> item, long epochSeconds) {
		public static final Codec<Mail> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("sender").forGetter(Mail::sender),
				Codec.STRING.optionalFieldOf("text", "").forGetter(Mail::text),
				ItemStack.CODEC.optionalFieldOf("item").forGetter(Mail::item),
				Codec.LONG.fieldOf("sent_at").forGetter(Mail::epochSeconds)
		).apply(i, Mail::new));
	}

	private static AttachmentType<Map<String, List<Mail>>> type;

	private MailBox() {
	}

	public static void register() {
		type = AttachmentRegistry.create(
				Identifier.fromNamespaceAndPath(Townsquare.MOD_ID, "mailboxes"),
				builder -> builder.persistent(
						Codec.unboundedMap(Codec.STRING, Mail.CODEC.listOf())).initializer(HashMap::new));

		// Aviso al conectar. Solo aviso: la entrega es /mail read, para que las cartas con
		// item no caigan a un inventario lleno sin que el jugador se entere.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			int pending = inbox(server, handler.player.getGameProfile().name()).size();
			if (pending > 0) {
				handler.player.sendSystemMessage(Component.literal(
						"Tienes " + pending + " carta(s). Usa /mail read para leerlas."));
			}
		});
	}

	public static void send(MinecraftServer server, String recipient, Mail mail) {
		Map<String, List<Mail>> map = new HashMap<>(all(server));
		List<Mail> inbox = new ArrayList<>(map.getOrDefault(key(recipient), List.of()));
		inbox.add(mail);
		map.put(key(recipient), inbox);
		server.overworld().setAttached(type, map);

		ServerPlayer online = server.getPlayerList().getPlayerByName(recipient);
		if (online != null) {
			online.sendSystemMessage(Component.literal(
					"Carta nueva de " + mail.sender() + ". Usa /mail read."));
		}
	}

	public static List<Mail> inbox(MinecraftServer server, String recipient) {
		return all(server).getOrDefault(key(recipient), List.of());
	}

	/** Entrega todo el buzon al jugador y lo vacia. Los items van al inventario o al suelo. */
	public static int deliver(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		List<Mail> inbox = inbox(server, player.getGameProfile().name());
		if (inbox.isEmpty()) {
			return 0;
		}

		for (Mail mail : inbox) {
			String when = java.time.LocalDate.ofInstant(
					java.time.Instant.ofEpochSecond(mail.epochSeconds()), java.time.ZoneOffset.UTC).toString();
			String line = "[" + when + "] " + mail.sender()
					+ (mail.text().isEmpty() ? "" : ": " + mail.text())
					+ (mail.item().isPresent() ? " (+ item adjunto)" : "");
			player.sendSystemMessage(Component.literal(line));
			// placeItemBackInInventory mete el stack donde quepa y suelta al suelo el resto:
			// no se pierde nada aunque el inventario este lleno.
			mail.item().ifPresent(stack -> player.getInventory().placeItemBackInInventory(stack.copy()));
		}

		Map<String, List<Mail>> map = new HashMap<>(all(server));
		map.remove(key(player.getGameProfile().name()));
		server.overworld().setAttached(type, map);
		return inbox.size();
	}

	private static Map<String, List<Mail>> all(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(type, HashMap::new);
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
