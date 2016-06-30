package org.icedesign;

import java.io.File;
import java.util.prefs.Preferences;

import org.icelib.AbstractConfig;
import org.icescene.SceneConfig;

import com.jme3.math.ColorRGBA;

public class DesignConfig extends SceneConfig {

	/**
	 * Default workspace directory
	 */
	public final static String APP_WORKSPACE_DIR_DEFAULT = System.getProperty("user.home") + File.separator + "Documents"
			+ File.separator + "Icedesign";

	public final static String CREATURE_TWEAK = "CreatureTweak";
	public final static String CREATURE_TWEAK_PREVIEW = CREATURE_TWEAK + "Preview";
	public final static boolean CREATURE_TWEAK_PREVIEW_DEFAULT = false;
	public final static String CREATURE_TWEAK_LIGHT = CREATURE_TWEAK + "Light";
	public final static float CREATURE_TWEAK_LIGHT_DEFAULT = 3.0f;
	public final static String CREATURE_TWEAK_PREVIEW_LIGHT = CREATURE_TWEAK_PREVIEW + "Light";
	public final static float CREATURE_TWEAK_PREVIEW_LIGHT_DEFAULT = 3.0f;
	public final static String CREATURE_TWEAK_LIGHT_COLOUR = CREATURE_TWEAK + "LightColour";
	public final static ColorRGBA CREATURE_TWEAK_LIGHT_COLOUR_DEFAULT = ColorRGBA.White;
	public final static String CLOTHING_TEXTURE_EDITOR = "ClothingTextureEditor";
	public final static String ATTACHMENT_FILES_EDITOR = "AttachmentFilesEditor";
	public final static String COLOUR_MAP_EDITOR = "ColourMapEditor";

	// Camera move speed (build mode)
	public final static String CREATURE_TWEAK_MOVE_SPEED = CREATURE_TWEAK + "MoveSpeed";
	public final static float CREATURE_TWEAK_MOVE_SPEED_DEFAULT = 10f;
	// Camera zoom speed
	public final static String CREATURE_TWEAK_ZOOM_SPEED = CREATURE_TWEAK + "ZoomSpeed";
	public final static float CREATURE_TWEAK_ZOOM_SPEED_DEFAULT = 10f;
	// Camera rotate speed
	public final static String CREATURE_TWEAK_ROTATE_SPEED = CREATURE_TWEAK + "RotateSpeed";
	public final static float CREATURE_TWEAK_ROTATE_SPEED_DEFAULT = 10f;

	public static Object getDefaultValue(String key) {
		return AbstractConfig.getDefaultValue(DesignConfig.class, key);
	}

	public static Preferences get() {
		return Preferences.userRoot().node(DesignConstants.APPSETTINGS_NAME).node("game");
	}
}
