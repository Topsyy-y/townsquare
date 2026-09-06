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
			ctx.getSource().sendFailure(Component.literal("Este comando necesita un jugador."));
			return 0;
		}

		HitResult hit = player.pick(8, 1.0f, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			ctx.getSource().sendFailure(Component.literal("Mira a un atril a 8 bloques o menos."));
			return 0;
		}

		BlockPos pos = blockHit.getBlockPos();
		if (create) {
			if (!Boards.create(player.level(), pos)) {
				ctx.getSource().sendFailure(Component.literal("Eso no es un atril. El tablon se crea mirando a un atril."));
				return 0;
			}
			ctx.getSource().sendSuccess(() -> Component.literal(
					"Tablon creado en " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
							+ ". Cualquier jugador puede usarlo ya."), false);
			return 1;
		}

		if (!Boards.remove(player.level(), pos)) {
			ctx.getSource().sendFailure(Component.literal("Ahi no hay ningun tablon."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Tablon eliminado. Sus notas se han descartado."), false);
		return 1;
	}

	private static void buildMail(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("mail")
				.then(Commands.literal("send")
						.then(Commands.argument("entrada", StringArgumentType.greedyString())
								.executes(ModCommands::mailSend)))
				.then(Commands.literal("sendbook")
						.then(Commands.argument("jugador", StringArgumentType.word())
								.executes(ModCommands::mailSendBook)))
				.then(Commands.literal("read").executes(ModCommands::mailRead)));
	}

	/** /mail send <jugador> <texto...> */
	private static int mailSend(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("Este comando necesita un jugador."));
			return 0;
		}
		String[] parts = StringArgumentType.getString(ctx, "entrada").trim().split("\\s+", 2);
		if (parts.length < 2 || parts[1].isBlank()) {
			ctx.getSource().sendFailure(Component.literal("Uso: /mail send <jugador> <texto>"));
			return 0;
		}
		MailBox.send(player.level().getServer(), parts[0], new MailBox.Mail(
				player.getGameProfile().name(), parts[1], java.util.Optional.empty(),
				java.time.Instant.now().getEpochSecond()));
		ctx.getSource().sendSuccess(() -> Component.literal("Carta enviada a " + parts[0] + "."), false);
		return 1;
	}

	/** Envia el item de la mano principal como adjunto. Se retira de la mano al enviarlo. */
	private static int mailSendBook(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("Este comando necesita un jugador."));
			return 0;
		}
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Lleva en la mano el libro o item que quieras enviar."));
			return 0;
		}
		String recipient = StringArgumentType.getString(ctx, "jugador");
		MailBox.send(player.level().getServer(), recipient, new MailBox.Mail(
				player.getGameProfile().name(), "", java.util.Optional.of(held.copy()),
				java.time.Instant.now().getEpochSecond()));
		held.setCount(0);
		ctx.getSource().sendSuccess(() -> Component.literal("Enviado a " + recipient + "."), false);
		return 1;
	}

	private static int mailRead(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("Este comando necesita un jugador."));
			return 0;
		}
		int delivered = MailBox.deliver(player);
		if (delivered == 0) {
			ctx.getSource().sendSuccess(() -> Component.literal("No tienes cartas."), false);
		}
		return delivered;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("Este comando necesita un jugador."));
			return 0;
		}
		int count = Boards.count(player.level());
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Tablones en esta dimension: " + count), false);
		return count;
	}
}
