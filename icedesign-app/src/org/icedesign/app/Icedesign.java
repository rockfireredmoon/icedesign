package org.icedesign.app;

import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.icedesign.CreatureEditorAppState;
import org.icedesign.DesignConfig;
import org.icedesign.DesignConstants;
import org.icelib.AppInfo;
import org.icelib.Appearance;
import org.icelib.Persona;
import org.icelib.UndoManager;
import org.icescene.HUDMessageAppState;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.audio.AudioAppState;
import org.icescene.entities.EntityLoader;
import org.icescene.environment.EnvironmentLight;
import org.icescene.environment.PostProcessAppState;
import org.icescene.io.ModifierKeysAppState;
import org.icescene.io.MouseManager;
import org.icescene.options.OptionsAppState;
import org.icescene.props.EntityFactory;
import org.icescene.ui.WindowManagerAppState;
import org.lwjgl.opengl.Display;

import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;

import icemoon.iceloader.ServerAssetManager;

public class Icedesign extends IcesceneApp implements ActionListener {

	private final static String MAPPING_OPTIONS = "Options";
	private static final Logger LOG = Logger.getLogger(Icedesign.class.getName());

	public static void main(String[] args) throws Exception {

		AppInfo.context = Icedesign.class;

		// Parse command line
		Options opts = createOptions();
		Assets.addOptions(opts);

		CommandLine cmdLine = parseCommandLine(opts, args);

		// A single argument must be supplied, the URL (which is used to
		// deterime router, which in turn locates simulator)
		if (cmdLine.getArgList().isEmpty()) {
			throw new Exception("No URL supplied.");
		}
		Icedesign app = new Icedesign(cmdLine);
		startApp(app, cmdLine, AppInfo.getName() + " - " + AppInfo.getVersion(),
				DesignConstants.APPSETTINGS_NAME);
	}

	private EntityFactory propFactory;
	private EntityLoader spawnLoader;

	private Icedesign(CommandLine commandLine) {
		super(DesignConfig.get(), commandLine, DesignConstants.APPSETTINGS_NAME, "META-INF/DesignAssets.cfg");
		getInitScripts().add("Scripts/designInit.js");
		setUseUI(true);
		setPauseOnLostFocus(false);
	}

	@Override
	public void restart() {
		Display.setResizable(true);
		super.restart();
	}

	@Override
	public void destroy() {
		super.destroy();
		spawnLoader.close();
		LOG.info("Destroyed application");
	}

	@Override
	public void onInitialize() {

		propFactory = new EntityFactory(this, rootNode);
		spawnLoader = new EntityLoader(getWorldLoaderExecutorService(), this, propFactory);

		// The default creature
		Appearance appearance = new Appearance();

		appearance.setName(Appearance.Name.C2);
		appearance.setName(Appearance.Name.C2);
		appearance.setBody(Appearance.Body.NORMAL);
		appearance.setGender(Appearance.Gender.MALE);
		appearance.setRace(Appearance.Race.HART);
		appearance.setHead(Appearance.Head.NORMAL);

		Persona persona = new Persona();
		persona.setEntityId(1l);
		persona.setDisplayName("Random Hart");
		persona.setAppearance(appearance);

		flyCam.setDragToRotate(true);
		flyCam.setMoveSpeed(prefs.getFloat(DesignConfig.CREATURE_TWEAK_MOVE_SPEED, DesignConfig.CREATURE_TWEAK_MOVE_SPEED_DEFAULT));
		flyCam.setRotationSpeed(prefs.getFloat(DesignConfig.CREATURE_TWEAK_ROTATE_SPEED,
				DesignConfig.CREATURE_TWEAK_ROTATE_SPEED_DEFAULT));
		flyCam.setZoomSpeed(-prefs.getFloat(DesignConfig.CREATURE_TWEAK_ZOOM_SPEED, DesignConfig.CREATURE_TWEAK_ZOOM_SPEED_DEFAULT));
		flyCam.setEnabled(true);
		setPauseOnLostFocus(false);

		// Undo manager
		UndoManager undoManager = new UndoManager();

		// Light
		EnvironmentLight el = new EnvironmentLight(cam, rootNode, prefs);
		el.setDirectionalEnabled(false);
		el.setAmbientEnabled(true);

		// Some animations need audio (we can also set UI volume now)
		final AudioAppState audioAppState = new AudioAppState(prefs);
		stateManager.attach(audioAppState);
		screen.setUIAudioVolume(audioAppState.getActualUIVolume());

		// Need the post processor for bloom
		stateManager.attach(new PostProcessAppState(prefs, el));

		// For error messages and stuff
		stateManager.attach(new HUDMessageAppState());

		// Some windows need management
		stateManager.attach(new WindowManagerAppState(prefs));

		// Mouse manager requires modifier keys to be monitored
		stateManager.attach(new ModifierKeysAppState());

		// Mouse manager for dealing with clicking, dragging etc.
		final MouseManager mouseManager = new MouseManager(rootNode, getAlarm());
		stateManager.attach(mouseManager);

		// A menu
		stateManager.attach(new MenuAppState(undoManager, spawnLoader, propFactory, prefs, persona));

		// Other UI bits
		stateManager.attach(new UIAppState(undoManager, prefs));

		// The main on screen model
		final PreviewAppState previewAppState = new PreviewAppState(el, rootNode, prefs, persona, spawnLoader) {
			@Override
			protected void modelLoaded() {
				super.modelLoaded();
				CreatureEditorAppState cas = stateManager.getState(CreatureEditorAppState.class);
				if (cas != null) {
					cas.setTargetCreatureSpatial(getSpatial());

					// AnimationHandler animHandler =
					// entity.getSpatial().getControl(AnimationHandler.class);
					// if (animHandler == null) {
					// LOG.info("Model has no animation control, no poses will be possible");
					// cas.setAnimations(null, null);
					// }
					// else {
					// cas.setAnimations(animHandler.getAnimations().keySet(),
					// animHandler.getAnimations().values());
					// }

					// if
					// (entity.getSpatial().getControl(CreatureAnimationControl.class)
					// == null) {
					// LOG.info("Model has no animation control, no poses will be possible");
					// cas.setAnimations(null, null);
					// } else {
					// cas.setAnimations(entity.getSpatial().getControl(CreatureAnimationControl.class).getAnims(),
					// entity.getAnimationPresets());
					// }
				}
			}
		};

		getStateManager().attach(previewAppState);

		// Input
		getKeyMapManager().addMapping(MAPPING_OPTIONS);
		getKeyMapManager().addListener(this, MAPPING_OPTIONS);

		cam.setLocation(new Vector3f(-1, 9f, 32f));
	}

	@Override
	protected void configureAssetManager(ServerAssetManager serverAssetManager) {
		getAssets().setAssetsExternalLocation(prefs.get(DesignConfig.APP_WORKSPACE_DIR, DesignConfig.APP_WORKSPACE_DIR_DEFAULT));
	}

	public void onAction(String name, boolean isPressed, float tpf) {
		if (name.equals(MAPPING_OPTIONS)) {
			if (!isPressed) {
				final OptionsAppState state = stateManager.getState(OptionsAppState.class);
				if (state == null) {
					stateManager.attach(new OptionsAppState(prefs));
				} else {
					stateManager.detach(state);
				}
			}
		}
	}
}
