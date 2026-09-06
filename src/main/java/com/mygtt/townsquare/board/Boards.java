package com.mygtt.townsquare.board;

import com.mojang.serialization.Codec;
import com.mygtt.townsquare.Townsquare;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro persistente de tablones: atriles de vanilla marcados por un admin.
 *
 * <p>Vive como attachment del propio mundo (cada dimension guarda los suyos), con codec y
 * sin NBT a mano. El valor se trata como inmutable: copiar, modificar, volver a colgar.
 */
public final class Boards {
	/** posLong -> contenido del tablon. Solo stacks no vacios. */
	private static AttachmentType<Map<Long, List<ItemStack>>> type;

	private Boards() {
	}

	public static void register() {
		Codec<Map<Long, List<ItemStack>>> codec = Codec.unboundedMap(
				Codec.STRING.xmap(Long::parseLong, String::valueOf),
				ItemStack.CODEC.listOf());

		type = AttachmentRegistry.create(
				Identifier.fromNamespaceAndPath(Townsquare.MOD_ID, "boards"),
				builder -> builder.persistent(codec).initializer(HashMap::new));

		// Ambos eventos: con la mano vacia y con item en mano, el tablon se abre igual.
		BlockEvents.USE_WITHOUT_ITEM.register((state, level, pos, player, hit) ->
				tryOpen(level, pos, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS);
		BlockEvents.USE_ITEM_ON.register((stack, state, level, pos, player, hand, hit) ->
				tryOpen(level, pos, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS);
	}

	private static boolean tryOpen(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!isBoard(serverLevel, pos)) {
			return false;
		}
		BoardMenu.open(serverPlayer, serverLevel, pos);
		return true;
	}

	public static boolean isBoard(ServerLevel level, BlockPos pos) {
		return all(level).containsKey(pos.asLong());
	}

	/** Marca un atril como tablon. Devuelve false si en esa posicion no hay atril. */
	public static boolean create(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos).getBlock() != Blocks.LECTERN) {
			return false;
		}
		Map<Long, List<ItemStack>> map = mutable(level);
		map.putIfAbsent(pos.asLong(), List.of());
		level.setAttached(type, map);
		return true;
	}

	public static boolean remove(ServerLevel level, BlockPos pos) {
		Map<Long, List<ItemStack>> map = mutable(level);
		boolean existed = map.remove(pos.asLong()) != null;
		level.setAttached(type, map);
		return existed;
	}

	public static List<ItemStack> contents(ServerLevel level, BlockPos pos) {
		return all(level).getOrDefault(pos.asLong(), List.of());
	}

	/** Guarda el contenido de un tablon, descartando huecos vacios. */
	public static void save(ServerLevel level, BlockPos pos, List<ItemStack> stacks) {
		Map<Long, List<ItemStack>> map = mutable(level);
		List<ItemStack> kept = new ArrayList<>();
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				kept.add(stack);
			}
		}
		map.put(pos.asLong(), kept);
		level.setAttached(type, map);
	}

	public static int count(ServerLevel level) {
		return all(level).size();
	}

	private static Map<Long, List<ItemStack>> all(ServerLevel level) {
		return level.getAttachedOrCreate(type, HashMap::new);
	}

	private static Map<Long, List<ItemStack>> mutable(ServerLevel level) {
		return new HashMap<>(all(level));
	}
}
