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

		// El tablon solo retiene libros. Cualquier otra cosa que alguien deje se le
		// devuelve al cerrar: filtrar por ranura exigiria un menu propio, y devolver
		// consigue lo mismo sin tocar internals de vanilla.
		List<ItemStack> kept = new ArrayList<>(SLOTS);
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = container.getItem(i);
			if (stack.isEmpty() || stack.getItem() == net.minecraft.world.item.Items.WRITTEN_BOOK
					|| stack.getItem() == net.minecraft.world.item.Items.WRITABLE_BOOK) {
				kept.add(stack);
			} else {
				kept.add(ItemStack.EMPTY);
				player.getInventory().placeItemBackInInventory(stack);
			}
		}
		Boards.save(level, pos, kept);
	}
}
