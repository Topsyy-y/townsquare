package com.mygtt.townsquare;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Townsquare implements ModInitializer {
	public static final String MOD_ID = "townsquare";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Townsquare] Initialised.");
	}
}
