package com.mygtt.townsquare.board;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * El tablon abierto: un cofre de vanilla de 3 filas respaldado por el attachment.
 *
 * <p>Interfaz 100% vanilla a proposito: el jugador no tiene el mod instalado, asi que no
 * hay pantalla propia posible. Se guarda al cerrar, en {@link #removed}.
 */
public final class BoardMenu extends ChestMenu {
	public static final int SLOTS = 27;

	private final ServerLevel level;
	private final BlockPos pos;
	private final SimpleContainer container;

	private BoardMenu(int syncId, ServerPlayer player, ServerLevel level, BlockPos pos, SimpleContainer container) {
		super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, syncId, player.getInventory(), container, 3);
		this.level = level;
		this.pos = pos;
		this.container = container;
	}

	public static void open(ServerPlayer player, ServerLevel level, BlockPos pos) {
		SimpleContainer container = new SimpleContainer(SLOTS);
		List<ItemStack> notes = Boards.contents(level, pos);
		for (int i = 0; i < Math.min(notes.size(), SLOTS); i++) {
			container.setItem(i, notes.get(i).copy());
		}

		player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, opener) ->
						new BoardMenu(syncId, (ServerPlayer) opener, level, pos, container),
				Component.literal("Tablon de anuncios")));
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		List<ItemStack> stacks = new ArrayList<>(SLOTS);
		for (int i = 0; i < SLOTS; i++) {
			stacks.add(container.getItem(i));
		}
		Boards.save(level, pos, stacks);
	}
}
