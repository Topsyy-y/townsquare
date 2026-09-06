package com.mygtt.townsquare;

import com.mygtt.townsquare.board.Boards;
import com.mygtt.townsquare.registry.ModCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Townsquare implements ModInitializer {
	public static final String MOD_ID = "townsquare";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Boards.register();
		com.mygtt.townsquare.mail.MailBox.register();
		ModCommands.register();
		LOGGER.info("[Townsquare] Initialised. Boards ready.");
	}
}
