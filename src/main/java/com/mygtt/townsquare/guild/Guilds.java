package com.mygtt.townsquare.guild;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mygtt.townsquare.Townsquare;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Gremios del servidor. Igual que el correo: por nombre de jugador en minusculas, en un
 * attachment del overworld, sin backend. La pertenencia se deriva escaneando los gremios,
 * que a escala de un servidor normal es gratis y evita un segundo indice que desincronizar.
 */
public final class Guilds {
	public record Guild(String name, String owner, List<String> members, List<String> invites) {
		public static final Codec<Guild> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("name").forGetter(Guild::name),
				Codec.STRING.fieldOf("owner").forGetter(Guild::owner),
				Codec.STRING.listOf().fieldOf("members").forGetter(Guild::members),
				Codec.STRING.listOf().optionalFieldOf("invites", List.of()).forGetter(Guild::invites)
		).apply(i, Guild::new));
	}

	private static AttachmentType<Map<String, Guild>> type;

	private Guilds() {
	}

	public static void register() {
		type = AttachmentRegistry.create(
				Identifier.fromNamespaceAndPath(Townsquare.MOD_ID, "guilds"),
				builder -> builder.persistent(
						Codec.unboundedMap(Codec.STRING, Guild.CODEC)).initializer(HashMap::new));
	}

	// --- consultas ---

	public static Optional<Guild> byName(MinecraftServer server, String name) {
		return Optional.ofNullable(all(server).get(name.toLowerCase(Locale.ROOT)));
	}

	public static Optional<Guild> of(MinecraftServer server, String playerName) {
		String key = playerName.toLowerCase(Locale.ROOT);
		return all(server).values().stream()
				.filter(guild -> guild.members().contains(key))
				.findFirst();
	}

	// --- mutaciones: copiar, cambiar, volver a colgar ---

	public static String create(MinecraftServer server, String name, String ownerName) {
		if (!name.matches("[A-Za-z0-9_]{3,16}")) {
			return "El nombre debe ser de 3 a 16 caracteres, letras, numeros o _.";
		}
		if (byName(server, name).isPresent()) {
			return "Ya existe un gremio con ese nombre.";
		}
		if (of(server, ownerName).isPresent()) {
			return "Ya estas en un gremio. Sal primero con /guild leave.";
		}
		String owner = ownerName.toLowerCase(Locale.ROOT);
		put(server, new Guild(name, owner, List.of(owner), List.of()));
		return null;
	}

	public static String invite(MinecraftServer server, String inviterName, String invitee) {
		Guild guild = of(server, inviterName).orElse(null);
		if (guild == null) {
			return "No estas en ningun gremio.";
		}
		if (!guild.owner().equals(inviterName.toLowerCase(Locale.ROOT))) {
			return "Solo el fundador puede invitar.";
		}
		String key = invitee.toLowerCase(Locale.ROOT);
		if (guild.members().contains(key)) {
			return "Ya es miembro.";
		}
		if (!guild.invites().contains(key)) {
			List<String> invites = new ArrayList<>(guild.invites());
			invites.add(key);
			put(server, new Guild(guild.name(), guild.owner(), guild.members(), invites));
		}
		ServerPlayer online = server.getPlayerList().getPlayerByName(invitee);
		if (online != null) {
			online.sendSystemMessage(Component.literal(
					inviterName + " te invita al gremio " + guild.name() + ". Usa /guild join " + guild.name()));
		}
		return null;
	}

	public static String join(MinecraftServer server, String playerName, String guildName) {
		if (of(server, playerName).isPresent()) {
			return "Ya estas en un gremio.";
		}
		Guild guild = byName(server, guildName).orElse(null);
		if (guild == null) {
			return "No existe ese gremio.";
		}
		String key = playerName.toLowerCase(Locale.ROOT);
		if (!guild.invites().contains(key)) {
			return "Necesitas una invitacion del fundador.";
		}
		List<String> members = new ArrayList<>(guild.members());
		members.add(key);
		List<String> invites = new ArrayList<>(guild.invites());
		invites.remove(key);
		put(server, new Guild(guild.name(), guild.owner(), members, invites));
		broadcast(server, byName(server, guildName).orElseThrow(), playerName + " se ha unido al gremio.");
		return null;
	}

	public static String leave(MinecraftServer server, String playerName) {
		Guild guild = of(server, playerName).orElse(null);
		if (guild == null) {
			return "No estas en ningun gremio.";
		}
		String key = playerName.toLowerCase(Locale.ROOT);
		if (guild.owner().equals(key)) {
			// El fundador no abandona: disuelve. Un gremio sin dueno es un estado zombi.
			Map<String, Guild> map = mutable(server);
			map.remove(guild.name().toLowerCase(Locale.ROOT));
			server.overworld().setAttached(type, map);
			broadcast(server, guild, "El gremio " + guild.name() + " ha sido disuelto por su fundador.");
			return null;
		}
		List<String> members = new ArrayList<>(guild.members());
		members.remove(key);
		put(server, new Guild(guild.name(), guild.owner(), members, guild.invites()));
		broadcast(server, guild, playerName + " ha dejado el gremio.");
		return null;
	}

	/** Chat de gremio: llega a los miembros conectados. */
	public static String chat(MinecraftServer server, String playerName, String text) {
		Guild guild = of(server, playerName).orElse(null);
		if (guild == null) {
			return "No estas en ningun gremio.";
		}
		broadcast(server, guild, "[" + guild.name() + "] " + playerName + ": " + text);
		return null;
	}

	public static void broadcast(MinecraftServer server, Guild guild, String message) {
		for (String member : guild.members()) {
			ServerPlayer online = server.getPlayerList().getPlayerByName(member);
			if (online != null) {
				online.sendSystemMessage(Component.literal(message));
			}
		}
	}

	private static Map<String, Guild> all(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(type, HashMap::new);
	}

	private static Map<String, Guild> mutable(MinecraftServer server) {
		return new HashMap<>(all(server));
	}

	private static void put(MinecraftServer server, Guild guild) {
		Map<String, Guild> map = mutable(server);
		map.put(guild.name().toLowerCase(Locale.ROOT), guild);
		server.overworld().setAttached(type, map);
	}
}
