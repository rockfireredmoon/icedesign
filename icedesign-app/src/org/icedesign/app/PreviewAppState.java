package org.icedesign.app;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import org.icedesign.DesignConfig;
import org.icelib.AbstractCreature;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.entities.EntityLoader;
import org.icescene.environment.EnvironmentLight;
import org.icescene.scene.AbstractDebugSceneAppState;
import org.iceui.IceUI;

import com.jme3.scene.Node;

public class PreviewAppState extends AbstractDebugSceneAppState {

	private static final Logger LOG = Logger.getLogger(PreviewAppState.class.getName());
	private EntityLoader loader;
	protected AbstractSpawnEntity entity;
	private AbstractCreature creature;
	private final EnvironmentLight environmentLight;

	public PreviewAppState(EnvironmentLight environmentLight, Node parentNode, Preferences prefs, AbstractCreature creature,
			EntityLoader spawnLoader) {
		super(prefs, parentNode);
		addPrefKeyPattern(DesignConfig.CREATURE_TWEAK + ".*");
		this.environmentLight = environmentLight;
		this.creature = creature;
		this.loader = spawnLoader;
	}

	@Override
	public void postInitialize() {
		super.postInitialize();
		createSpatial();
		setLightColour();
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		super.handlePrefUpdateSceneThread(evt);
		if (evt.getKey().equals(DesignConfig.CREATURE_TWEAK_LIGHT)
				|| evt.getKey().equals(DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR)) {
			setLightColour();
		}
	}

	public void setCreature(AbstractCreature creature) {
		this.creature = creature;
	}

	@Override
	public void onCleanup() {
		super.onCleanup();
	}

	public void reloadSkin() {
		entity.reloadSkin();
	}

	public void updateAppearance() {
		loader.reload(entity);
	}

	public void recreateSpatial() throws Exception {
		entity.unloadAndUnloadScene();
		entity.getSpatial().removeFromParent();
		createSpatial();
	}

	protected void modelLoaded() {
		// For sub-classes to overide and be notified of when the preview model
		// loads
	}

	private void createSpatial() {
		// Create the spatial
		entity = loader.create(creature);
		entity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
			public Void call() throws Exception {
				if (entity.getAnimControl() != null) {
					try {
						entity.getSpatial().addControl(entity.getAnimationHandler());
					} catch (UnsupportedOperationException uoe) {
						LOG.log(Level.SEVERE, "Failed to create animation handler.", uoe);
					}
				}
				modelLoaded();
				return null;
			}
		});

		// Now load for first time
		loader.reload(entity);

		this.app.getRootNode().attachChild(entity.getSpatial());
	}

	public AbstractSpawnEntity getSpatial() {
		return entity;
	}

	protected void setLightColour() {
		environmentLight.setAmbientColor(IceUI
				.getColourPreference(prefs, DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR,
						DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR_DEFAULT)
				.multLocal(prefs.getFloat(DesignConfig.CREATURE_TWEAK_LIGHT, DesignConfig.CREATURE_TWEAK_LIGHT_DEFAULT)));
	}
}
