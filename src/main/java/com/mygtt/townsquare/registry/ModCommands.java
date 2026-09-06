package com.mygtt.townsquare.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mygtt.townsquare.board.Boards;
import com.mygtt.townsquare.mail.MailBox;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class ModCommands {
	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> { build(dispatcher); buildMail(dispatcher); });
	}

	private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("townsquare")
				.then(Commands.literal("board")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("create").executes(ctx -> onLookedAtLectern(ctx, true)))
						.then(Commands.literal("remove").executes(ctx -> onLookedAtLectern(ctx, false)))
						.then(Commands.literal("list").executes(ModCommands::list))));
	}

	/** Aplica crear o quitar sobre el bloque que el admin esta mirando, a 8 bloques o menos. */
	private static int onLookedAtLectern(CommandContext<CommandSourceStack> ctx, boolean create) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}

		HitResult hit = player.pick(8, 1.0f, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			ctx.getSource().sendFailure(Component.literal("Look at a lectern within 8 blocks."));
			return 0;
		}

		BlockPos pos = blockHit.getBlockPos();
		if (create) {
			if (!Boards.create(player.level(), pos)) {
				ctx.getSource().sendFailure(Component.literal("That's not a lectern. Look at a lectern to create a board."));
				return 0;
			}
			ctx.getSource().sendSuccess(() -> Component.literal(
					"Board created at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
							+ ". Any player can use it now."), false);
			return 1;
		}

		if (!Boards.remove(player.level(), pos)) {
			ctx.getSource().sendFailure(Component.literal("There is no board there."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Board removed. Its notes were discarded."), false);
		return 1;
	}

	private static void buildMail(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mail")
				.then(Commands.literal("send")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests(ModCommands::suggestOnlinePlayers)
								.then(Commands.argument("text", StringArgumentType.greedyString())
										.executes(ModCommands::mailSend))))
				.then(Commands.literal("sendbook")
						.then(Commands.argument("player", StringArgumentType.word())
								.suggests(ModCommands::suggestOnlinePlayers)
								.executes(ModCommands::mailSendBook)))
				.then(Commands.literal("read").executes(ModCommands::mailRead)));
	}

	/** /mail send <jugador> <texto...> */
	private static int mailSend(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		String recipient = StringArgumentType.getString(ctx, "player");
		String text = StringArgumentType.getString(ctx, "text");
		MailBox.send(player.level().getServer(), recipient, new MailBox.Mail(
				player.getGameProfile().name(), text, java.util.Optional.empty(),
				java.time.Instant.now().getEpochSecond()));
		ctx.getSource().sendSuccess(() -> Component.literal(deliveryNote(player, recipient)), false);
		return 1;
	}

	/** Envia el item de la mano principal como adjunto. Se retira de la mano al enviarlo. */
	private static int mailSendBook(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Hold the book or item you want to send."));
			return 0;
		}
		String recipient = StringArgumentType.getString(ctx, "player");
		MailBox.send(player.level().getServer(), recipient, new MailBox.Mail(
				player.getGameProfile().name(), "", java.util.Optional.of(held.copy()),
				java.time.Instant.now().getEpochSecond()));
		held.setCount(0);
		ctx.getSource().sendSuccess(() -> Component.literal(deliveryNote(player, recipient)), false);
		return 1;
	}

	/** Deja claro a quien fue y si esta conectado: los envios a un nombre mal escrito se ven aqui. */
	private static String deliveryNote(ServerPlayer sender, String recipient) {
		boolean online = sender.level().getServer().getPlayerList().getPlayerByName(recipient) != null;
		return "Mail sent to " + recipient + (online ? "." : " (offline: delivered when they log in).");
	}

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestOnlinePlayers(
			CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
		for (ServerPlayer online : ctx.getSource().getServer().getPlayerList().getPlayers()) {
			builder.suggest(online.getGameProfile().name());
		}
		return builder.buildFuture();
	}

	private static int mailRead(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		int delivered = MailBox.deliver(player);
		if (delivered == 0) {
			ctx.getSource().sendSuccess(() -> Component.literal("You have no mail."), false);
		}
		return delivered;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("This command needs a player."));
			return 0;
		}
		int count = Boards.count(player.level());
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Boards in this dimension: " + count), false);
		return count;
	}
}
