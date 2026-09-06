package com.mygtt.townsquare.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mygtt.townsquare.board.Boards;
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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
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
