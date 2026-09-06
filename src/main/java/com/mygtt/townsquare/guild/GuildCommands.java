package com.mygtt.townsquare.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiFunction;

public final class GuildCommands {
	private GuildCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
	}

	private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("guild")
				.then(Commands.literal("create")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> run(ctx, (player, arg) ->
										Guilds.create(server(player), arg, name(player)), "name",
										"Guild created. Invite players with /guild invite <player>."))))
				.then(Commands.literal("invite")
						.then(Commands.argument("player", StringArgumentType.word())
								.executes(ctx -> run(ctx, (player, arg) ->
										Guilds.invite(server(player), name(player), arg), "player",
										"Invitation sent."))))
				.then(Commands.literal("join")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> run(ctx, (player, arg) ->
										Guilds.join(server(player), name(player), arg), "name", null))))
				.then(Commands.literal("leave")
						.executes(ctx -> run(ctx, (player, arg) ->
								Guilds.leave(server(player), name(player)), null, null)))
				.then(Commands.literal("info")
						.executes(GuildCommands::info)));

		dispatcher.register(Commands.literal("g")
				.then(Commands.argument("text", StringArgumentType.greedyString())
						.executes(ctx -> run(ctx, (player, arg) ->
								Guilds.chat(server(player), name(player), arg), "text", null))));
	}

	/** Ejecuta una operacion que devuelve null si fue bien o el mensaje de error si no. */
	private static int run(CommandContext<CommandSourceStack> ctx,
			BiFunction<ServerPlayer, String, String> operation, String argName, String successMessage) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		String arg = argName == null ? null : StringArgumentType.getString(ctx, argName);
		String error = operation.apply(player, arg);
		if (error != null) {
			ctx.getSource().sendFailure(Component.literal(error));
			return 0;
		}
		if (successMessage != null) {
			ctx.getSource().sendSuccess(() -> Component.literal(successMessage), false);
		}
		return 1;
	}

	private static int info(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		Guilds.Guild guild = Guilds.of(server(player), name(player)).orElse(null);
		if (guild == null) {
			ctx.getSource().sendFailure(Component.literal("You are not in a guild. Crea uno con /guild create <nombre>."));
			return 0;
		}
		String text = "Guild " + guild.name() + " (" + guild.members().size() + " members)\n"
				+ "  founder: " + guild.owner() + "\n"
				+ "  members: " + String.join(", ", guild.members());
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
		return guild.members().size();
	}

	private static net.minecraft.server.MinecraftServer server(ServerPlayer player) {
		return player.level().getServer();
	}

	private static String name(ServerPlayer player) {
		return player.getGameProfile().name();
	}
}
