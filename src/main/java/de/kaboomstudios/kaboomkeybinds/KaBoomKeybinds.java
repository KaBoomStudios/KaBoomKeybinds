package de.kaboomstudios.kaboomkeybinds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants shared by all packages of the mod.
 *
 * <p>This class has no Minecraft dependencies so that the core package, which is covered by unit tests,
 * can use it as well.
 */
public final class KaBoomKeybinds {
    /** The mod id as declared in fabric.mod.json. */
    public static final String MOD_ID = "kaboomkeybinds";

    /** Logger for every message of the mod. The mod id is the logger name so log lines are easy to filter. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private KaBoomKeybinds() {
    }
}
