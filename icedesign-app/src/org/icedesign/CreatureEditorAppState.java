package org.icedesign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.icelib.AbstractCreature;
import org.icelib.Appearance;
import org.icelib.Appearance.Body;
import org.icelib.Appearance.Gender;
import org.icelib.Appearance.Head;
import org.icelib.Appearance.Race;
import org.icelib.AttachableTemplate;
import org.icelib.AttachmentItem;
import org.icelib.AttachmentPoint;
import org.icelib.ClothingItem;
import org.icelib.ClothingTemplate;
import org.icelib.ClothingTemplateKey;
import org.icelib.EntityKey;
import org.icelib.Icelib;
import org.icelib.Persona;
import org.icelib.RGB;
import org.icelib.Region;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.ServiceRef;
import org.icescene.animation.AnimationOption;
import org.icescene.animation.AnimationSequence;
import org.icescene.assets.Assets;
import org.icescene.assets.ExtendedMaterialListKey.Lighting;
import org.icescene.configuration.ClothingDef;
import org.icescene.configuration.attachments.AttachableDef;
import org.icescene.configuration.attachments.AttachableTemplates;
import org.icescene.configuration.creatures.AbstractCreatureDefinition;
import org.icescene.configuration.creatures.ContentDef;
import org.icescene.configuration.creatures.CreatureKey;
import org.icescene.configuration.creatures.ModelDef;
import org.icescene.configuration.creatures.Skin;
import org.icescene.controls.AnimationHandler;
import org.icescene.controls.AnimationRequest;
import org.icescene.controls.CreatureAnimationControl;
import org.icescene.controls.Rotator;
import org.icescene.entities.AbstractLoadableEntity;
import org.icescene.entities.AbstractSpawnEntity;
import org.icescene.entities.EntityLoader;
import org.icescene.ogreparticle.OGREParticleConfiguration;
import org.icescene.ogreparticle.OGREParticleScript;
import org.icescene.props.EntityFactory;
import org.icesquirrel.interpreter.SquirrelInterpretedTable;
import org.icesquirrel.runtime.SquirrelArray;
import org.icesquirrel.runtime.SquirrelPrinter;
import org.icesquirrel.runtime.SquirrelTable;
import org.iceui.IceUI;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.TabPanelContent;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.Button;
import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.TabControl;
import icetone.controls.extras.Separator;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.SelectList;
import icetone.controls.lists.Slider;
import icetone.controls.lists.Spinner;
import icetone.controls.menuing.Menu;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.controls.text.AutocompleteItem;
import icetone.controls.text.AutocompleteSource;
import icetone.controls.text.AutocompleteTextField;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.Layout.LayoutType;
import icetone.core.Orientation;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.ToolKit;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.FillLayout;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.core.undo.UndoManager;
import icetone.core.undo.UndoableCommand;
import icetone.core.utils.Alarm.AlarmTask;
import icetone.extras.chooser.ColorButton;
import icetone.extras.chooser.ColorSelector;
import icetone.extras.util.ExtrasUtil;
import icetone.extras.windows.ButtonWindow;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

/**
 * Provides an enhanced 'Creature Tweak' that contains all of the features of
 * the official clients', but can save tweaks to up to 4 slots, and has live 3d
 * previews of the creature as well as attachments.
 * <p>
 * Also allows cloning of existing clothing sets, and the ability to edit the
 * textures in these sets. The assets produced are stored locally, and may be
 * latter integrated into the server.
 */
public class CreatureEditorAppState extends IcemoonAppState<IcemoonAppState<?>>
		implements PreferenceChangeListener, CreatureAnimationControl.Listener, AnimationHandler.Listener {

	final static CreatureKey BIPED_KEY = new CreatureKey("Biped", null);
	final static CreatureKey PROP_KEY = new CreatureKey("Prop", null);

	private static final Logger LOG = Logger.getLogger(CreatureEditorAppState.class.getName());
	private PushButton clone;
	private Preview previewPanel;
	private boolean sync;
	private final UndoManager undoManager;

	public enum Type {

		GENERAL, EAR_SIZE, TAIL_SIZE, SIZE, SKIN
	}

	public interface Listener {

		void stopAnimate();

		void animate(AnimationRequest request);

		void updateModels(CreatureEditorAppState tweak);

		void updateAppearance(CreatureEditorAppState tweak, Type type);

		void typeChanged(CreatureEditorAppState tweak, Appearance.Name newType);

		void animationSpeedChange(float newSpeed);
	}

	private Spinner<Float> size;
	private ComboBox<Gender> gender;
	private ComboBox<Body> bodyType;
	private ComboBox<Race> race;
	private ComboBox<Head> head;
	private List<Listener> listeners = new ArrayList<>();
	private Map<Region, ArmourColourBar> colorChoosers = new HashMap<>();
	private Map<Region, ComboBox<ClothingTemplate>> assetChoosers = new HashMap<>();
	private Map<Region, PushButton> editButtons = new HashMap<>();
	private AbstractCreature creature;
	private TextField name;
	private Table bodyTemplate;
	private BaseElement bipedDetails;
	private TabControl tabs;
	private Spinner<Float> earSize;
	private Spinner<Float> tailSize;
	private AbstractCreatureDefinition creatureDefinition;
	private BaseElement skinPanel;
	private PosePanel posePanel;
	private TextField bodyFilter;
	private TextField clothingFilter;
	private BaseElement clothing;
	private Label clothingNotSupportedMessage;
	private Table attachments;
	private ComboBox<AttachmentPoint> attachmentPoint;
	private TextField effect;
	private PushButton chooseEffect;
	private AttachmentColourBar attachmentColours;
	private BaseElement attachmentDetails;
	private ComboBox<ClothingTemplate> clothingSet;
	private AttachableTemplate attachableTemplate;
	private TextField prop;
	private final EntityFactory propFactory;
	private Label skinNotSupportedMessage;
	private Label attachmentsNotSupportedMessage;
	private BaseElement attachmentsPanel;
	private Label poseNotSupportedMessage;
	private PushButton preview;
	private final EntityLoader spawnLoader;
	private AbstractSpawnEntity previewEntity;
	private StyledContainer previewContainerPanel;
	private Slider<Float> light;
	private AbstractSpawnEntity target;
	private CheckBox syncWithSelection;
	private PersistentWindow clothingTextureEditorWindow;
	private ClothingTextureEditorPanel clothingTextureEditor;
	private final Assets assets;
	private PushButton cloneAttachment;
	private PushButton editAttachment;
	private CreatureKey creatureKey;

	protected PersistentWindow window;
	private AlarmTask bodyFilterTimer;
	@ServiceRef
	protected static AttachableTemplates attachableTemplatesConfiguration;
	@ServiceRef
	protected static ContentDef contentDef;
	@ServiceRef
	protected static ModelDef modelDef;
	@ServiceRef
	protected static ClothingDef clothingDef;
	@ServiceRef
	protected static AttachableDef attachableDef;

	public CreatureEditorAppState(UndoManager undoManager, Preferences pref, EntityFactory propFactory,
			EntityLoader spawnLoader, Assets assets, Persona persona) {
		super(pref);
		addPrefKeyPattern(DesignConfig.CREATURE_TWEAK + ".*");

		this.undoManager = undoManager;
		this.assets = assets;
		this.propFactory = propFactory;
		this.spawnLoader = spawnLoader;

		setPreviewCreature(persona);
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public final void setPreviewCreature(AbstractCreature creature) {
		this.creature = creature;
		getDefinition();
		if (isInitialized()) {
			updateForms();
		}
	}

	public boolean isSyncWithSelection() {
		return sync;
	}

	public void setSyncWithSelection(boolean sync) {
		if (this.sync != sync) {
			this.sync = sync;
			if (syncWithSelection != null) {
				if (target == null) {
					syncWithSelection.runAdjusting(() -> syncWithSelection.setChecked(sync));
				} else {
					syncWithSelection.setChecked(sync);
				}
			}
		}
	}

	public void setTargetCreatureSpatial(AbstractSpawnEntity target) {
		this.target = target;
		if (syncWithSelection != null) {
			syncWithSelection.setVisible(target != null);
			// if (target == null) {
			// syncWithSelection.setIsCheckedNoCallback(target != null);
			// }
			if (syncWithSelection.isChecked()) {
				loadFromTargetAppearance();
			}
		}
	}

	@Override
	public void postInitialize() {

		assetManager = app.getAssetManager();
		screen = ((IcesceneApp) app).getScreen();

		window = new PersistentWindow(screen, DesignConfig.CREATURE_TWEAK, 8, VAlign.Top, Align.Left, null, false,
				SaveType.POSITION_AND_SIZE, prefs) {
			{
				setStyleClass("large");
			}

			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				app.getStateManager().detach(CreatureEditorAppState.this);
			}
		};

		// Window management (if available)
		window.setMinimizable(true);

		// window.setIsResizable(false);
		window.setWindowTitle("Creature Tweak");

		// The window content contains the editor area and the toggleable
		// preview area
		final BaseElement windowContent = window.getContentArea();
		windowContent.setLayoutManager(new BorderLayout());

		// The editor area
		final StyledContainer editorArea = new StyledContainer(screen);
		editorArea.setLayoutManager(
				new MigLayout(screen, "fill, wrap 3", "[shrink 0][grow][shrink 0]", "[shrink 0][grow][shrink 0]"));
		windowContent.addElement(editorArea, Border.CENTER);

		Label l = new Label(screen);
		l.setText("Name:");
		editorArea.addElement(l);

		name = new TextField(screen);
		editorArea.addElement(name, "growx");

		preview = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		preview.onMouseReleased(evt -> {
			prefs.putBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW, !prefs.getBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW,
					DesignConfig.CREATURE_TWEAK_PREVIEW_DEFAULT));
		});
		preview.setText("Preview");
		preview.setButtonIconAlign(BitmapFont.Align.Right);
		ElementStyle.arrowButton(preview, Border.EAST);
		editorArea.addElement(preview, "");

		tabs = new TabControl(screen);
		tabs.setUseSlideEffect(true);
		editorArea.addElement(tabs, "growx, growy, span 3");

		tabs.addTab("Body", createBody());
		tabs.addTab("Skin", createSkin());
		tabs.addTab("Armour", createClothing());
		tabs.addTab("Attachments", createAttachments());
		tabs.addTab("Pose", createPose());

		// Buttons
		BaseElement buttons = new BaseElement(screen);
		buttons.setLayoutManager(new MigLayout(screen, "wrap 3", "[][]push[]", "[]"));
		editorArea.addElement(buttons, "growx, span 3");

		// Load appearance
		PushButton load = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		load.onMouseReleased(evt -> showLoadMenu(evt.getX(), evt.getY()));
		load.setText("Load");
		load.setToolTipText("Load creature appearance from clipboard, selection or slot");
		buttons.addElement(load);

		// Save appearance
		PushButton save = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		save.onMouseReleased(evt -> showSaveMenu(evt.getX(), evt.getY()));
		save.setText("Save");
		save.setToolTipText("Save creature appearance to clipboard, selection or slot");
		buttons.addElement(save);

		// Automatically synchronize with selection
		syncWithSelection = new CheckBox(screen);
		syncWithSelection.setChecked(sync);
		syncWithSelection.onChange(evt -> {
			if (!evt.getSource().isAdjusting() && evt.getNewValue()) {
				loadFromTargetAppearance();
			}
		});
		syncWithSelection.setText("Synchronise with selection");
		syncWithSelection.setVisible(target != null);
		buttons.addElement(syncWithSelection);

		updateForms();
		checkPreview(false);

		screen.addElement(window);

	}

	@Override
	public void sequenceStarted(CreatureAnimationControl control, AnimationSequence seq) {
		posePanel.setActive(control.isAnyActive());
	}

	@Override
	public void sequenceStopped(CreatureAnimationControl control, AnimationSequence seq) {
		LOG.info(String.format("Sequence %s stopped", seq.getName()));
		posePanel.setActive(control.isAnyActive());
	}

	@Override
	public void animationStarted(AnimationOption name) {
		posePanel.setActive(true);
	}

	@Override
	public void animationChanged(AnimationOption name) {
		posePanel.setSelectedPreset(name);
	}

	@Override
	public void animationStopped() {
		posePanel.setActive(false);
	}

	@Override
	protected void handlePrefUpdateSceneThread(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(DesignConfig.CREATURE_TWEAK_PREVIEW)) {
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					checkPreview(true);
					return null;
				}
			});
		} else if (evt.getKey().equals(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT)) {
			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {
					if (previewContainerPanel != null) {
						final float val = prefs.getFloat(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT,
								DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT_DEFAULT);
						previewPanel.getAmbientLight().setColor(ColorRGBA.White.mult(val));
						light.runAdjusting(() -> light.setSelectedValue(val));
					}
					return null;
				}
			});
		}
	}

	protected void checkPreview(boolean resizeWindow) {
		if (previewContainerPanel == null
				&& prefs.getBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW, DesignConfig.CREATURE_TWEAK_PREVIEW_DEFAULT)) {
			// Viewport border
			previewPanel = new Preview(screen, new Size(200, 300));
			float lightAmt = prefs.getFloat(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT,
					DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT_DEFAULT);
			previewPanel.getAmbientLight().setColor(ColorRGBA.White.mult(lightAmt));

			createCreatureSpatial();

			// Rotator Buttons
			Button rotateLeft = new Button(screen);
			rotateLeft.setStyleClass("rotate-button rotate-left");
			rotateLeft.onMousePressed(evt -> previewEntity.getSpatial().addControl(new Rotator(-3f)));
			rotateLeft.onMouseReleased(evt -> previewEntity.getSpatial().removeControl(Rotator.class));
			Button rotateRight = new Button(screen);
			rotateRight.setStyleClass("rotate-button rotate-right");
			rotateRight.onMousePressed(evt -> previewEntity.getSpatial().addControl(new Rotator(3f)));
			rotateRight.onMouseReleased(evt -> previewEntity.getSpatial().removeControl(Rotator.class));
			StyledContainer rotatorButtons = new StyledContainer(screen);
			rotatorButtons.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
			rotatorButtons.addElement(rotateLeft);
			rotatorButtons.addElement(rotateRight);

			// Tools
			light = new Slider<Float>(screen, Orientation.HORIZONTAL);
			light.onChanged((evt) -> {
				if(!evt.getSource().isAdjusting())
					prefs.putFloat(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT, evt.getNewValue());
			});
			light.setToolTipText("Light");
			light.setSliderModel(new FloatRangeSliderModel(0.1f, 20f, lightAmt, 0.5f));
			light.setLockToStep(true);

			// Preview container panel (adds vertical separator)
			previewContainerPanel = new StyledContainer(screen);
			previewContainerPanel.setLayoutManager(new MigLayout(screen, "", "[][]", "[][][]"));
			previewContainerPanel.addElement(new Separator(screen, Orientation.VERTICAL), "spany, growy");
			previewContainerPanel.addElement(light, "growx, wrap, pushy");
			previewContainerPanel.addElement(previewPanel, "wrap");
			previewContainerPanel.addElement(rotatorButtons, "ax 50%, pushy");

			ElementStyle.arrowButton(preview, Border.WEST);

			// Window
			window.getContentArea().addElement(previewContainerPanel, Border.EAST);
			if (resizeWindow) {
				Vector2f pSz = previewContainerPanel.calcPreferredSize();
				window.setDimensions(pSz.x + window.getWidth(), window.getHeight());
				window.layoutChildren();
				ExtrasUtil.saveWindowPositionAndSize(prefs, window, DesignConfig.CREATURE_TWEAK);
			}

			//
			spawnLoader.reload(previewEntity);
		} else if (previewContainerPanel != null && !prefs.getBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW,
				DesignConfig.CREATURE_TWEAK_PREVIEW_DEFAULT)) {
			final float targetWidth = window.getWidth() - previewContainerPanel.getWidth();
			window.getContentArea().removeElement(previewContainerPanel);
			ElementStyle.arrowButton(preview, Border.EAST);
			if (resizeWindow) {
				window.setDimensions(targetWidth, window.getHeight());
				ExtrasUtil.saveWindowPositionAndSize(prefs, window, DesignConfig.CREATURE_TWEAK);
			}
			previewContainerPanel = null;
			light = null;
			previewPanel = null;
			previewEntity = null;
		}
	}

	@Override
	protected void onCleanup() {
		super.onCleanup();
		window.setDestroyOnHide(true);
		window.hide();
	}

	private TabPanelContent createPose() {

		LOG.info("Creating poses");

		TabPanelContent tpc = new TabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(0, 0));

		// Pose
		posePanel = new PosePanel(screen) {
			@Override
			protected void onStartPose() {
				final AnimationOption preset = posePanel.getPreset();
				LOG.info(String.format("Starting pose %s", preset));

				AnimationRequest request = new AnimationRequest(preset);
				if (posePanel.isForceLoop())
					request.setLoop(true);

				if (syncWithSelection.isChecked()) {
					fireAnimationSequence(request);
				} else {
					if (previewEntity != null) {
						AnimationHandler<?, ?> control = previewEntity.getSpatial().getControl(AnimationHandler.class);
						control.play(request);
					}
				}
			}

			@Override
			protected void onSpeedChange(float newSpeed) {
				if (previewEntity != null) {
					previewEntity.getSpatial().getControl(AnimationHandler.class).setSpeed(newSpeed);
				}

				if (syncWithSelection.isChecked()) {
					fireAnimationSpeedChange(newSpeed);
				}
			}

			@Override
			protected void onStopPose() {
				if (previewEntity != null) {
					previewEntity.getSpatial().getControl(AnimationHandler.class).stop();
				}
				if (syncWithSelection.isChecked()) {
					fireStopAnimation();
				}
			}

			@Override
			protected void onStartAnimation() {
				if (previewEntity != null) {
					previewEntity.getSpatial().getControl(AnimationHandler.class)
							.play(new AnimationRequest(posePanel.getPreset()));
				}

				// TODO
			}
		};

		// Not supported messages
		poseNotSupportedMessage = new Label("Pose is not supported on this creature type", screen);
		poseNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addElement(posePanel, Border.CENTER);
		tpc.addElement(poseNotSupportedMessage, Border.CENTER);

		return tpc;
	}

	private TabPanelContent createSkin() {

		TabPanelContent tpc = new TabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		skinPanel = new BaseElement(screen);
		skinPanel.setAsContainerOnly();
		skinPanel.setLayoutManager(new MigLayout(screen, "wrap 4"));

		// Not supported messages
		skinNotSupportedMessage = new Label("Skin is not supported on this creature type", screen);
		skinNotSupportedMessage.setTextAlign(BitmapFont.Align.Left);

		// Tab content
		tpc.addElement(skinPanel, Border.CENTER);
		tpc.addElement(skinNotSupportedMessage, Border.CENTER);
		return tpc;
	}

	private void createClothingRow(String label, final Region type, BaseElement tabPanel) {
		tabPanel.addElement(new Label(label, screen));

		final ArmourColourBar colorBar = new ArmourColourBar(screen, type, creature) {
			@Override
			protected void onUpdate() {
				tweakUpdated(CreatureEditorAppState.Type.GENERAL);
			}
		};

		// Asset
		ComboBox<ClothingTemplate> asset = new ComboBox<ClothingTemplate>(screen);
		asset.onChange(evt -> {
			if (!evt.getSource().isAdjusting()) {

				// Update the creature appearance
				Appearance appearance = creature.getAppearance();
				Appearance.ClothingList clothingList = appearance.getClothing();

				if (type == null) {
					clothingList.clear();

					// Clothing set
					if (evt.getNewValue() == null) {
						// Set everything to None
						for (Map.Entry<Region, ComboBox<ClothingTemplate>> b : assetChoosers.entrySet()) {
							b.getValue().runAdjusting(() -> b.getValue().setSelectedIndex(0));
							colorChoosers.get(b.getKey()).setDefinition(null);
							editButtons.get(b.getKey()).setEnabled(false);
						}
					} else {
						// Set everything to one particular set
						final Map<Region, String> regions = evt.getNewValue().getRegions();
						List<Region> remainingRegions = new ArrayList<Region>(regions.keySet());

						// Regions that do exist in this set
						for (Region r : regions.keySet()) {
							ComboBox<ClothingTemplate> c = assetChoosers.get(r);
							c.runAdjusting(() -> c.setSelectedByValue(evt.getNewValue()));
							remainingRegions.remove(r);
							clothingList
									.add(new ClothingItem(r.toClothingType(), evt.getNewValue().getKey(), null, null));
							colorChoosers.get(r).setDefinition(evt.getNewValue());
							setEditAvailable(c, editButtons.get(r));
						}

						// Regions that don't exist in this set
						for (Region r : remainingRegions) {
							ComboBox<ClothingTemplate> c = assetChoosers.get(r);
							colorChoosers.get(r).setDefinition(null);
							c.runAdjusting(() -> c.setSelectedIndex(0));
							editButtons.get(r).setEnabled(false);
						}
					}

				} else {
					// Items
					Appearance.ClothingType clothingType = type.toClothingType();
					ClothingItem it = clothingList.getItemForType(clothingType);
					if (evt.getNewValue() == null) {
						if (it != null) {
							clothingList.remove(it);
						}
					} else {
						if (it == null) {
							it = new ClothingItem(clothingType, evt.getNewValue().getKey(), null, null);
							clothingList.add(it);
						} else {
							it.setKey(evt.getNewValue().getKey());
						}
					}

					// Rebuild colours
					colorBar.setDefinition(evt.getNewValue());

					setEditAvailable(asset, editButtons.get(type));

				}
				clone.setEnabled(clothingSet != null
						&& clothingSet.getSelectedListItem().getValue() instanceof ClothingTemplate);
				appearance.setClothing(clothingList);
				creature.setAppearance(appearance);
				tweakUpdated(CreatureEditorAppState.Type.GENERAL);
			}
		});
		// asset.addListItem("None", 0);
		asset.getMenu().setMaxDimensions(new Size(Float.MAX_VALUE, 300));

		// clothingDef.loadAll(assetManager);
		// for (ClothingDefinition def : Icelib.sort(clothingDef.values())) {
		// if (type == null || def.getRegions().containsKey(type)) {
		// asset.addListItem(def.getKey() == null ? "NULL" :
		// def.getKey().getName(), def);
		// }
		// }
		if (type != null) {
			assetChoosers.put(type, asset);
		} else {
			clothingSet = asset;
		}
		tabPanel.addElement(asset);

		// Color chooser
		if (type == null) {
			clone = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			clone.onMouseReleased(evt -> {
				// ClothingTemplate sel =
				// asset.getSelectedListItem().getValue();
				// if (sel != null) {
				// CloneItemWindow cw = new CloneItemWindow(screen) {
				// protected void onCreate(AttachableTemplate newDef, File
				// assetsDir) throws IOException {
				// super.onCreate(newDef, assetsDir);
				// Appearance appearance = creature.getAppearance();
				// appearance.removeAttachment(sel);
				// sel.setKey(newDef.getKey());
				// appearance.addAttachment(sel);
				// updateForms();
				// recreateAndFireUpdateModels();
				// }
				// };
				// cw.setItem(clothingDef.get(sel.getKey()));
				// AttachmentPoint node = sel.getNode();
				// if (node == null) {
				// if (attachableTemplate != null) {
				// node = attachableTemplate.getAttachPoints().get(0);
				// }
				// }
				// cw.setAttachmentPoint(node);
				// }

				final ClothingTemplate value = asset.getSelectedListItem().getValue();
				if (value instanceof ClothingTemplate) {
					ClothingTemplate def = (ClothingTemplate) value;

					List<ClothingTemplate> defs = new ArrayList<ClothingTemplate>();
					for (Region region : Region.values()) {
						ComboBox<ClothingTemplate> lb = assetChoosers.get(region);
						if (lb != null && lb.getSelectIndex() > 0) {
							ClothingTemplate t = lb.getSelectedListItem().getValue();
							defs.add(t);
						}
					}

					new CloneClothingWindow(screen, def.getKey(), defs) {

						protected void onDoCloneItem(final ClothingTemplate def, final ClothingTemplate newDef,
								String newName) throws IOException {
							// Now update the creature tweak window forms
							refilterClothing();
							clothingSet.setSelectedByValue(newDef);

						}
					};
				}
			});
			clone.setEnabled(clothingSet != null && clothingSet.getSelectIndex() > -1
					&& clothingSet.getSelectedListItem().getValue() instanceof ClothingTemplate);
			clone.setText("Clone");
			clone.setToolTipText("Clone this clothing set for texture editing");
			tabPanel.addElement(clone, "span 4");
		} else {
			tabPanel.addElement(colorBar);
			colorChoosers.put(type, colorBar);

			// Copy
			final PushButton copyButton = new PushButton(screen);
			copyButton.onMouseReleased(evt -> {
				SquirrelTable outter = new SquirrelTable();
				SquirrelTable inner = new SquirrelTable();
				final ClothingTemplate def = (ClothingTemplate) asset.getSelectedListItem().getValue();
				inner.put("type", def.getKey().getType());
				SquirrelArray cols = new SquirrelArray();
				for (RGB r : creature.getAppearance().getClothing().getItemForType(type.toClothingType()).getColors()) {
					cols.add(Icelib.toHexString(r));
				}
				inner.put("colors", cols);
				outter.put("c", inner);
				ToolKit.get().setClipboardText(SquirrelPrinter.format(outter));
			});
			copyButton.setToolTipText("Copy appearance to clipboard");
			copyButton.setText("C");
			ElementStyle.normal(copyButton);
			tabPanel.addElement(copyButton);
			copyButton.setVisible(type != null);

			// Paste
			final PushButton pasteButton = new PushButton(screen);
			pasteButton.onMouseReleased(evt -> {
				String clipText = ToolKit.get().getClipboardText();
				if (clipText != null) {
					try {
						SquirrelTable eo = SquirrelInterpretedTable.table(clipText);
						SquirrelTable inner = (SquirrelTable) eo.get("c");
						if (inner == null) {
							throw new Exception("Content doesn't appear to be clothing item appearance string.");
						}
						String atype = (String) inner.get("type");
						if (atype == null) {
							throw new Exception("Content does not contain an asset type.");
						}
						// ENotation.EArray colors = (ENotation.EArray)
						// inner.get("colors");
						ClothingTemplate def = clothingDef.get(new EntityKey(atype));
						if (def == null) {
							throw new Exception(String.format("Clothing item %s does not exist.", atype));
						}
						asset.setSelectedByValue(def);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to parse clipboard content for clothing item apppearance.", e);
					}
				}
			});
			pasteButton.setToolTipText("Paste appearance from clipboard");
			pasteButton.setText("P");
			ElementStyle.normal(pasteButton);
			tabPanel.addElement(pasteButton);
			pasteButton.setVisible(type != null);

			// Edit
			final PushButton editButton = new PushButton(screen);
			editButton.onMouseReleased(evt -> {
				final Object value = asset.getSelectedListItem().getValue();
				if (value instanceof ClothingTemplate) {
					final ClothingTemplate def = (ClothingTemplate) value;
					edit(def, type);
				}
			});
			editButtons.put(type, editButton);
			setEditAvailable(asset, editButton);
			editButton.setToolTipText("Edit this item");
			editButton.setText("E");
			ElementStyle.normal(editButton);
			tabPanel.addElement(editButton);
			editButton.setVisible(type != null);
		}

		refilterAssets(asset, "", type);

	}

	private void setEditAvailable(ComboBox<ClothingTemplate> asset, PushButton editButton) {
		ClothingTemplate sel = asset.getSelectedValue();
		editButton.setEnabled(sel != null && assets
				.getExternalAssetFile(
						String.format("%s/%s", Icelib.toEnglish(sel.getKey().getType()), sel.getKey().getItemName()))
				.exists());
	}

	private void editItem(AttachableTemplate def) {

		EditItemWindow itemTextureEditorWindow = ((BaseScreen) screen).getElementByClass(EditItemWindow.class);
		if (itemTextureEditorWindow == null) {
			itemTextureEditorWindow = new EditItemWindow(screen, ((IcesceneApp) app).getAssets(), prefs) {
				@Override
				protected void onCloseWindow() {
					screen.removeElement(this);
				}

				@Override
				protected void onTextureFileUpdated() {
					recreateAndFireUpdateModels();
				}
			};
			screen.showElement(itemTextureEditorWindow);
		}
		itemTextureEditorWindow.setValue(def);
	}

	private void edit(ClothingTemplate def, Region region) {
		if (clothingTextureEditorWindow == null) {
			clothingTextureEditorWindow = new PersistentWindow(screen, DesignConfig.CLOTHING_TEXTURE_EDITOR, 8,
					VAlign.Center, Align.Right, null, true, SaveType.POSITION_AND_SIZE, prefs) {
				@Override
				protected void onCloseWindow() {
					clothingTextureEditorWindow = null;
					screen.removeElement(this);
				}
			};
			clothingTextureEditorWindow.setResizable(true);
			final BaseElement windowContent = clothingTextureEditorWindow.getContentArea();
			windowContent.setLayoutManager(new FillLayout());
			clothingTextureEditor = new ClothingTextureEditorPanel(assets, screen, prefs) {
				@Override
				protected void textureFileUpdated(File file) {
					recreateAndFireUpdateModels();
				}
			};
			windowContent.addElement(clothingTextureEditor);
			screen.showElement(clothingTextureEditorWindow, ScreenLayoutConstraints.center);
		}
		clothingTextureEditorWindow.setWindowTitle(String.format("Textures - %s", def.getKey().getName()));
		clothingTextureEditor.setClothingDefinition(def, region);
		if (!clothingTextureEditorWindow.isVisible()) {
			clothingTextureEditorWindow.show();
		}
	}

	private TabPanelContent createClothing() {
		TabPanelContent tpc = new TabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		clothing = new BaseElement(screen);
		clothing.setAsContainerOnly();
		clothing.setLayoutManager(new MigLayout(screen, "wrap 2", "[shrink 0][grow]", "[]push[]push"));

		// Filter
		clothing.addElement(new Label("Filter", screen));
		clothingFilter = new TextField(screen);
		clothingFilter.onMouseReleased(evt -> refilterClothing());
		clothing.addElement(clothingFilter, "growx");

		// Clothing
		BaseElement c = new BaseElement(screen);
		c.setLayoutManager(new MigLayout(screen, "wrap 6, gap 2, ins 0",
				"[shrink 0][120::,grow]4[180:180:][::16,shrink 0][::16,shrink 0][::16,shrink 0]", "[]"));
		clothing.addElement(c, "growx, growy, span 2");

		createClothingRow("Armor Set", null, c);
		for (Region region : Region.values()) {
			createClothingRow(Icelib.toEnglish(region), region, c);
		}

		// Not supported messages
		clothingNotSupportedMessage = new Label("Armour is not supported on this creature type", screen);
		clothingNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addElement(clothing, Border.CENTER);
		tpc.addElement(clothingNotSupportedMessage, Border.CENTER);

		return tpc;
	}

	private void refilterClothing() {
		String filterText = clothingFilter.getText().toLowerCase();
		boolean changed = false;
		if (refilterAssets(clothingSet, filterText, null)) {
			changed = true;
		}
		for (Map.Entry<Region, ComboBox<ClothingTemplate>> en : assetChoosers.entrySet()) {
			ComboBox<ClothingTemplate> asset = en.getValue();
			Region type = en.getKey();
			if (refilterAssets(asset, filterText, type)) {
				changed = true;
			}
		}
		LOG.info("Changed = " + changed);

		if (changed) {
			updateForms();
			recreateAndFireUpdateModels();
		}
	}

	private boolean refilterAssets(ComboBox<ClothingTemplate> asset, String filterText, Region type) {
		LOG.info(String.format("Refiltering available clothing using '%s' as filter for region %s", filterText, type));
		Object sel = asset.getSelectIndex() > -1 ? asset.getSelectedListItem().getValue() : null;
		asset.invalidate();
		asset.runAdjusting(() -> {
			asset.removeAllListItems();
			asset.addListItem("None", null);
		});
		int found = sel != null && sel.equals(0) ? 0 : -1;
		int index = 1;
		clothingDef.loadAll(assetManager);
		for (ClothingTemplate def : Icelib.sort(clothingDef.values())) {
			ClothingTemplateKey key = def.getKey();
			if (key != null && key.getName() != null) {
				if ((filterText.equals("") || (key.getName().toLowerCase().contains(filterText)))
						&& (type == null || def.getRegions().containsKey(type))) {
					asset.addListItem(key.getName(), def);
					if (key.getName().equals(sel)) {
						found = index;
					}
					index++;
				}
			}
		}
		if (found == -1) {
			asset.runAdjusting(() -> asset.setSelectedIndex(Math.min(1, asset.getListItems().size())));
		}
		asset.validate();
		return found > 0;
	}

	private TabPanelContent createBody() {

		StyledContainer top = new StyledContainer(screen);
		top.setLayoutManager(new MigLayout(screen, "ins 0", "[][grow,fill][]", "[]"));
		top.addElement(new Label("Filter", screen));
		bodyFilter = new TextField(screen);
		bodyFilter.onChange(evt -> {
			if (bodyFilterTimer != null)
				bodyFilterTimer.cancel();
			bodyFilterTimer = ToolKit.get().getAlarm().timed(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					refilterBody();
					return null;
				}
			}, 1f);
		});
		top.addElement(bodyFilter);
		PushButton clear = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		clear.onMouseReleased(evt -> {
			bodyFilter.setText("");
			refilterBody();
		});
		clear.setText("Clear");
		top.addElement(clear);

		// Left side
		bodyTemplate = new Table(screen);
		bodyTemplate.setMinDimensions(new Size(200, 200));
		bodyTemplate.setEnableKeyboardNavigation(true);
		bodyTemplate.addColumn("Body Template");
		bodyTemplate.setHeadersVisible(false);
		bodyTemplate.setSelectionMode(Table.SelectionMode.ROW);
		refilterBody();
		bodyTemplate.onChanged(evt -> {
			if (!evt.getSource().isAdjusting()) {

				// Determine type and template to use
				TableRow row = evt.getSource().getSelectedRow();
				CreatureKey c = (CreatureKey) row.getValue();
				if (undoManager == null) {
					selectCreature(c);
				} else {
					undoManager.storeAndExecute(new SelectTypeCommand(c));
				}
			}
		});

		// Right side
		bipedDetails = new BaseElement(screen);
		bipedDetails.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[][]"));
		bipedDetails.setAsContainerOnly();

		// Size
		Label label = new Label(screen);
		label.setText("Prop");
		bipedDetails.addElement(label);
		prop = new AutocompleteTextField<String>(screen, new AutocompleteSource<String>() {
			public List<AutocompleteItem<String>> getItems(String text) {
				text = text.toLowerCase();
				List<AutocompleteItem<String>> l = new ArrayList<>();
				for (String p : propFactory.getAllPropResources()) {
					String base = Icelib.getBasename(Icelib.getFilename(p));
					if (base.toLowerCase().contains(text)) {
						l.add(new AutocompleteItem<String>(base, base));
						if (l.size() > 100) {
							break;
						}
					}
				}
				Collections.sort(l);
				return l;
			}
		}) {
			@Override
			protected void onChange(String value) {
				if (undoManager == null) {
					creature.getAppearance().setProp(value);
					recreateAndFireUpdateModels();
				} else {
					undoManager.storeAndExecute(new ChangePropCommand(value));
				}
			}
		};
		prop.setToolTipText("Type in partial prop name and press CTRL+SPACE to autocomplete");
		prop.setLabel(label);
		bipedDetails.addElement(prop, "growx");

		// Size
		label = new Label(screen);
		label.setText("Size");
		bipedDetails.addElement(label);
		size = new Spinner<Float>(screen, Orientation.HORIZONTAL, true);
		size.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		size.onChange(evt -> {
			if (undoManager == null) {
				try {
					creature.getAppearance().setSize(evt.getNewValue());
					tweakUpdated(CreatureEditorAppState.Type.SIZE);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to update size.", e);
				}
			} else {
				undoManager.storeAndExecute(new ChangeSizeCommand(evt.getNewValue()));
			}
		});
		size.setLabel(label);
		bipedDetails.addElement(size);

		// Ear Size
		label = new Label(screen);
		label.setText("Ears");
		bipedDetails.addElement(label);
		earSize = new Spinner<Float>(screen, Orientation.HORIZONTAL, true);
		earSize.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 10f, 0.1f, 1f));
		earSize.onChange(evt -> {
			if (undoManager == null) {
				try {
					creature.getAppearance().setEarSize(evt.getNewValue());
					tweakUpdated(CreatureEditorAppState.Type.EAR_SIZE);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to update ear size.", e);
				}
			} else {
				undoManager.storeAndExecute(new ChangeEarSizeCommand(evt.getNewValue()));
			}
		});
		earSize.setLabel(label);
		bipedDetails.addElement(earSize);

		// Tail Size
		label = new Label(screen);
		label.setText("Tail");
		bipedDetails.addElement(label);

		tailSize = new Spinner<Float>(screen, Orientation.HORIZONTAL, true);
		tailSize.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 10f, 0.1f, 1f));
		tailSize.onChange(evt -> {
			if (undoManager == null) {
				try {
					creature.getAppearance().setTailSize(evt.getNewValue());
					tweakUpdated(CreatureEditorAppState.Type.TAIL_SIZE);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to update tail size.", e);
				}
			} else {
				undoManager.storeAndExecute(new ChangeTailSizeCommand(evt.getNewValue()));
			}
		});
		tailSize.setLabel(label);
		bipedDetails.addElement(tailSize);

		// Gender
		label = new Label(screen);
		label.setText("Gender");
		bipedDetails.addElement(label);
		gender = new ComboBox<Gender>(screen);
		for (Appearance.Gender g : Appearance.Gender.values()) {
			gender.addListItem(Icelib.toEnglish(g), g);
		}
		gender.onChange(evt -> {
			if (!evt.getSource().isAdjusting()) {
				if (undoManager == null) {
					creature.getAppearance().setGender(evt.getNewValue());
					recreateAndFireUpdateModels();
				} else {
					undoManager.storeAndExecute(new ChangeGenderCommand(evt.getNewValue()));
				}
			}
		});
		gender.setLabel(label);
		bipedDetails.addElement(gender);

		// Body Type
		label = new Label(screen);
		label.setText("Body Type");
		bipedDetails.addElement(label);
		bodyType = new ComboBox<Body>(screen);
		for (Appearance.Body g : Appearance.Body.values()) {
			bodyType.addListItem(Icelib.toEnglish(g), g);
		}
		bodyType.onChange(evt -> {
			if (!evt.getSource().isAdjusting()) {
				if (undoManager == null) {
					creature.getAppearance().setBody(evt.getNewValue());
					recreateAndFireUpdateModels();
				} else {
					undoManager.storeAndExecute(new ChangeBodyCommand(evt.getNewValue()));
				}
			}
		});
		bodyType.setLabel(label);
		bipedDetails.addElement(bodyType);

		// Race
		label = new Label(screen);
		label.setText("Race");
		bipedDetails.addElement(label);
		race = new ComboBox<Race>(screen);
		for (Appearance.Race g : Appearance.Race.values()) {
			race.addListItem(Icelib.toEnglish(g), g);
		}
		race.onChange(evt -> {
			if (!evt.getSource().isAdjusting()) {
				if (undoManager == null) {
					creature.getAppearance().setRace(evt.getNewValue());
					recreateAndFireUpdateModels();
				} else {
					undoManager.storeAndExecute(new ChangeRaceCommand(evt.getNewValue()));
				}
			}
		});
		race.setLabel(label);
		bipedDetails.addElement(race);

		// Head
		label = new Label(screen);
		label.setText("Head");
		bipedDetails.addElement(label);
		head = new ComboBox<Head>(screen);
		for (Appearance.Head g : Appearance.Head.values()) {
			head.addListItem(Icelib.toEnglish(g), g);
		}
		head.onChange(evt -> {
			if (!evt.getSource().isAdjusting()) {
				if (undoManager == null) {
					creature.getAppearance().setHead(evt.getNewValue());
					recreateAndFireUpdateModels();
				} else {
					undoManager.storeAndExecute(new ChangeHeadCommand(evt.getNewValue()));
				}
			}
		});
		head.setLabel(label);
		bipedDetails.addElement(head);

		// This

		TabPanelContent tpc = new TabPanelContent(screen);
		tpc.setLayoutManager(new MigLayout(screen, "fill", "[grow, fill][shrink 0]", "[shrink 0][grow]"));
		tpc.addElement(top, "span 2, wrap");
		tpc.addElement(bodyTemplate, "growy");
		tpc.addElement(bipedDetails);

		return tpc;
	}

	private void refilterBody() {
		List<CreatureKey> kk = new ArrayList<CreatureKey>();
		kk.add(BIPED_KEY);
		kk.add(PROP_KEY);
		kk.addAll(CreatureKey.getAll("Horde", assetManager));
		kk.addAll(CreatureKey.getAll("Boss", assetManager));
		bodyTemplate.invalidate();
		bodyTemplate.removeAllRows();
		for (CreatureKey k : kk) {
			if (bodyMatches(k)) {
				TableRow row = new TableRow(screen, bodyTemplate, k);
				TableCell cell = new TableCell(screen, k.getText(), k);
				row.addElement(cell);
				bodyTemplate.addRow(row);
			}
		}
		bodyTemplate.validate();
	}

	private boolean bodyMatches(CreatureKey k) {
		String filterText = bodyFilter.getText().toLowerCase();
		return filterText.equals("") || k.getText().toLowerCase().contains(filterText);
	}

	private void selectCreature(CreatureKey key) {
		this.creatureKey = key;

		// Create a brand new appearance
		Appearance newAppearance = new Appearance();

		Appearance.Name newType = Appearance.Name.C2;
		if (key == null)
			key = BIPED_KEY;

		if (key == BIPED_KEY) {
			newAppearance.setSize(1f);
			newAppearance.setRace(Appearance.Race.HART);
			newAppearance.setHead(Appearance.Head.NORMAL);
			newAppearance.setBody(Appearance.Body.NORMAL);
			newAppearance.setGender(Appearance.Gender.MALE);
		} else if (key == PROP_KEY) {
			newType = Appearance.Name.P1;
			newAppearance.setProp("Prop-Chair");
		} else {
			newType = Appearance.Name.N4;
			newAppearance.setBodyTemplate(key.getFullName());
		}

		// Update appearance
		newAppearance.setName(newType);
		creature.setAppearance(newAppearance);

		// Update preview spatial, and inform listeners if synching
		if (syncWithSelection.isChecked()) {
			// If synching, we expect something to call {@link
			// CreatureEditorAppState#setTargetCreatureSpatial}
			// which will update the local preview model as well
			target.getCreature().setAppearance(newAppearance.clone());
			for (Listener l : listeners) {
				l.typeChanged(CreatureEditorAppState.this, newType);
			}
		} else {
			unloadAndRecreateCreatureSpatial();

			// Now update the creature tweak window forms
			getDefinition();
			updateForms();
		}
	}

	private AttachmentItem getSelectedAttachmentItem() {
		int r = attachments.getSelectedRowIndex();
		if (r > -1) {
			final List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
			final AttachmentItem item = attachmentItems.get(r);
			return item;
		}
		return null;
	}

	private void updateAttachmentDetails() {
		LOG.info("Updating attachments");
		TableRow row = attachments.getSelectedRow();
		AttachmentPoint node = null;
		String effectName = null;
		attachableTemplate = null;
		if (row != null) {
			final AttachmentItem item = (AttachmentItem) row.getValue();
			node = item.getNode();
			attachableTemplate = attachableDef.get(item.getKey());
			if (node == null) {
				if (attachableTemplate != null) {
					node = attachableTemplate.getAttachPoints().get(0);
				}
			}
			effectName = item.getEffect();
			final AttachmentPoint fNode = node;
			attachmentPoint.runAdjusting(() -> attachmentPoint.setSelectedByValue(fNode));
			if (attachableTemplate.isParticle()) {
				attachmentColours.setVisible(false);
			} else {
				attachmentColours.setVisible(true);
				attachmentColours.setAttachment(item);
			}
			attachmentDetails.setVisible(true);
			cloneAttachment.setEnabled(true);
			editAttachment.setEnabled(app.getAssets().getExternalAssetFile(item.getKey().getPath()).exists());
		} else {
			attachmentDetails.setVisible(false);
			attachmentColours.setVisible(false);
			cloneAttachment.setEnabled(false);
			editAttachment.setEnabled(false);
		}
		attachmentPoint.setEnabled(node != null);
		chooseEffect.setEnabled(node != null);
		if (effectName != null) {
			effect.setText(effectName);
		} else {
			effect.setText("");
		}
	}

	private TabPanelContent createAttachments() {
		LOG.info("Creating attachments");

		TabPanelContent tpc = new TabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		attachmentsPanel = new BaseElement(screen);
		attachmentsPanel.setAsContainerOnly();
		attachmentsPanel.setLayoutManager(new MigLayout(screen, "fill, wrap 2", "[grow][shrink 0]", "[grow]"));

		// Attachments

		attachments = new Table(screen);
		attachments.onChanged(evt -> {
			if (!evt.getSource().isAdjusting()) {
				updateAttachmentDetails();
			}
		});
		attachments.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		attachments.addColumn("Attachment");
		attachments.addColumn("Point");

		// Sequences buttons
		BaseElement attachmentActions = new BaseElement(screen);
		attachmentActions.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow]", "[][][][][]"));

		// Add new attachment
		PushButton add = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		add.onMouseReleased(evt -> addAttachment());
		add.setText("Add");
		attachmentActions.addElement(add, "growx");

		// Remove attachment
		PushButton remove = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		remove.onMouseReleased(evt -> {

			List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
			List<Integer> rows = attachments.getSelectedRowIndexes();
			Collections.sort(rows);
			Collections.reverse(rows);
			for (int r : rows) {
				attachmentItems.remove(r);
			}
			creature.getAppearance().setAttachments(attachmentItems);
			recreateAndFireUpdateModels();
			updateForms();
		});
		remove.setText("Remove");
		attachmentActions.addElement(remove, "growx");

		// Copy attachment
		PushButton copy = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		copy.onMouseReleased(evt -> {
			AttachmentItem sel = getSelectedAttachmentItem();
			if (sel != null) {
				ToolKit.get().setClipboardText(sel.toSerializable().toString());
			}
		});
		copy.setText("Copy");
		attachmentActions.addElement(copy, "growx");

		// Paste
		PushButton paste = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		paste.onMouseReleased(evt -> {
			int r = attachments.getSelectedRowIndex();
			if (r > -1) {
				final List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
				try {
					SquirrelTable eo = SquirrelInterpretedTable.table(ToolKit.get().getClipboardText());
					AttachmentItem newItem = AttachmentItem.createAttachment(eo);
					attachmentItems.set(r, newItem);
					creature.getAppearance().setAttachments(attachmentItems);
					updateForms();
					recreateAndFireUpdateModels();
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to parse creature appearance.", e);
				}
			}
		});
		paste.setText("Paste");
		attachmentActions.addElement(paste, "growx");

		// Attachment point
		final List<AttachmentPoint> asList = Arrays.asList(AttachmentPoint.values());
		Collections.sort(asList, new Comparator<AttachmentPoint>() {
			public int compare(AttachmentPoint o1, AttachmentPoint o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		attachmentPoint = new ComboBox<AttachmentPoint>(screen, asList.toArray(new AttachmentPoint[0]));
		attachmentPoint.onChange(evt -> {
			int r = attachments.getSelectedRowIndex();
			if (r > -1) {
				final List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
				final AttachmentItem item = attachmentItems.get(r);
				AttachmentPoint sel = attachmentPoint.getSelectIndex() > -1
						? (AttachmentPoint) attachmentPoint.getSelectedListItem().getValue() : null;
				item.setNode(sel);
				creature.getAppearance().setAttachments(attachmentItems);
				recreateAndFireUpdateModels();
				updateForms();
				attachments.setSelectedRowIndex(r);
			}
		});
		for (AttachmentPoint ap : asList) {
			attachmentPoint.addListItem(Icelib.toEnglish(ap, true), ap);
		}

		// Effect
		effect = new TextField(screen);

		chooseEffect = new PushButton(screen);
		chooseEffect.onMouseReleased(evt -> selectParticle());
		chooseEffect.setText("..");
		effect.setEnabled(false);

		// Clone
		cloneAttachment = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		cloneAttachment.onMouseReleased(evt -> {
			AttachmentItem sel = getSelectedAttachmentItem();
			if (sel != null) {
				CloneItemWindow cw = new CloneItemWindow(screen) {
					protected void onCreate(AttachableTemplate newDef, File assetsDir) throws IOException {
						super.onCreate(newDef, assetsDir);
						Appearance appearance = creature.getAppearance();
						appearance.removeAttachment(sel);
						sel.setKey(newDef.getKey());
						appearance.addAttachment(sel);
						updateForms();
						recreateAndFireUpdateModels();
					}
				};
				cw.setItem(attachableDef.get(sel.getKey()));
				AttachmentPoint node = sel.getNode();
				if (node == null) {
					if (attachableTemplate != null) {
						node = attachableTemplate.getAttachPoints().get(0);
					}
				}
				cw.setAttachmentPoint(node);
			}
		});
		cloneAttachment.setText("Clone");
		cloneAttachment.setToolTipText("Clone this item to a local copy so you can edit it.");

		// Edit
		editAttachment = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		editAttachment.onMouseReleased(evt -> {
			AttachmentItem sel = getSelectedAttachmentItem();
			if (sel != null) {
				AttachableTemplate def = attachableDef.get(sel.getKey());
				editItem(def);
			}
		});
		editAttachment.setText("Edit");
		editAttachment.setToolTipText(
				"Edit this items textures, models and colour maps. Only enabled when you are working on a cloned item.");

		// Item actions
		StyledContainer itemActions = new StyledContainer(screen);
		itemActions.setLayoutManager(new MigLayout(screen, "", "push[][]push", "[]"));
		itemActions.addElement(cloneAttachment);
		itemActions.addElement(editAttachment);

		// Colours
		attachmentColours = new AttachmentColourBar(screen, creature) {
			@Override
			protected void onUpdate() {
				List<AttachmentItem> items = creature.getAppearance().getAttachments();
				int attachIndex = indexOfAttachment(getAttachment());
				items.set(attachIndex, getAttachment());
				creature.getAppearance().setAttachments(items);

				recreateAndFireUpdateModels();
				updateForms();
				attachments.setSelectedRowIndex(indexOfAttachment(getAttachment()));
			}
		};

		// Details
		attachmentDetails = new BaseElement(screen);
		attachmentDetails.setLayoutManager(
				new MigLayout(screen, "fill, wrap 4", "[shrink 0][grow][shrink 0][:200:]", "[][][][]"));
		attachmentDetails.addElement(new Label("Point:", screen));
		attachmentDetails.addElement(attachmentPoint, "span 2, growx");
		attachmentDetails.addElement(new Label("Colours", screen), "ax 50%, wrap");
		attachmentDetails.addElement(new Label("Effect:", screen));
		attachmentDetails.addElement(effect, "growx");
		attachmentDetails.addElement(chooseEffect);
		attachmentDetails.addElement(attachmentColours, "ax 50%");
		attachmentDetails.addElement(itemActions, "span 4, growx");

		// This
		attachmentsPanel.addElement(attachments, "growx, growy");
		attachmentsPanel.addElement(attachmentActions);
		attachmentsPanel.addElement(attachmentDetails, "span 2, growx");

		// Not supported messages
		attachmentsNotSupportedMessage = new Label("Attachments are not supported on this creature type", screen);
		attachmentsNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addElement(attachmentsPanel, Border.CENTER);
		tpc.addElement(attachmentsNotSupportedMessage, Border.CENTER);
		return tpc;
	}

	private void showSaveMenu(float x, float y) {
		Menu<Integer> subMenu = new Menu<>(screen);
		subMenu.onChanged((evt) -> {
			int s = evt.getNewValue().getValue();
			if (s == -1) {
				ToolKit.get().setClipboardText(creature.getAppearance().toString());
				info("Saved appearance to clipboard");
			} else if (s == 0) {
				try {
					target.getCreature().getAppearance().parse(creature.getAppearance().toString());
				} catch (IOException ex) {
					// Should not happen
					throw new RuntimeException(ex);
				}
				info("Saved appearance to selection");
				fireUpdateModels();
			} else {
				Preferences slotNode = getSlotPreferenceNode(s);
				slotNode.put("appearance", creature.getAppearance().toString());
				info(String.format("Saved appearance to selection %d", s));
			}
		});
		subMenu.addMenuItem("Save to clipboard", -1);
		if (target != null && !syncWithSelection.isChecked()) {
			subMenu.addMenuItem("Save to selected creature", 0);
		}
		for (int i = 1; i <= DesignConstants.ALLOWED_SAVEABLE_TWEAK_SETS; i++) {
			subMenu.addMenuItem("Save to slot " + i, i);
		}
		screen.addElement(subMenu);
		subMenu.showMenu(null, x, y);
	}

	private Preferences getSlotPreferenceNode(int s) {
		return prefs.node("slots").node(String.valueOf(s));
	}

	private void showLoadMenu(float x, float y) {
		Menu<Integer> subMenu = new Menu<>(screen);
		subMenu.onChanged((evt) -> {
			int s = evt.getNewValue().getValue();
			try {
				if (s == -1) {
					parseAppearanceString(ToolKit.get().getClipboardText());
					info("Loaded appearance from clipboard");
				} else if (s == 0) {
					loadFromTargetAppearance();
					info("Loaded appearance from target");
				} else {
					parseAppearanceString(getSlotPreferenceNode(s).get("appearance", ""));
					info(String.format("Loaded appearance from slot %d", s));
				}

				if (syncWithSelection.isChecked()) {
					for (Listener l : listeners) {
						l.updateAppearance(CreatureEditorAppState.this, Type.GENERAL);
					}
				}

			} catch (Exception ex) {
				error(String.format("Failed to parse creature appearance.", ex.getMessage()));
				LOG.log(Level.SEVERE, "Failed to parse creature appearance.", ex);
			}
		});
		subMenu.addMenuItem("Load from clipboard", -1);
		if (target != null && !syncWithSelection.isChecked()) {
			subMenu.addMenuItem("Load from selected creature", 0);
		}
		for (int i = 1; i <= DesignConstants.ALLOWED_SAVEABLE_TWEAK_SETS; i++) {
			Preferences p = getSlotPreferenceNode(i);
			String appString = p.get("appearance", "");
			if (!appString.equals("")) {
				subMenu.addMenuItem("Load from slot " + i, i);
			}
		}
		screen.addElement(subMenu);
		subMenu.showMenu(null, x, y);
	}

	private void loadFromTargetAppearance() {
		LOG.info(String.format("Loading appearance from %s", target));
		try {
			parseAppearanceString(target.getCreature().getAppearance().toString());
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Failed to parse creature appearance.", ex);
		}
	}

	private void parseAppearanceString(final String appString) throws IOException {
		LOG.info(String.format("Parsing '%s'", appString));
		creature.setAppearance(new Appearance(appString));
		// clothingFilter.setText("");
		getDefinition();
		updateForms();
		unloadAndRecreateCreatureSpatial();
	}

	private int indexOfAttachment(AttachmentItem attachmentItem) {
		int idx = 0;
		for (TableRow r : attachments.getRows()) {
			if (r.getValue().equals(attachmentItem)) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	private void addAttachment() {
		new AddAttachmentWindow(screen, creature) {
			@Override
			protected void onAdd(List<AttachmentItem> sel) {
				updateForms();
				attachments.setSelectedRowIndex(indexOfAttachment(sel.get(0)));
				recreateAndFireUpdateModels();
			}
		};
	}

	private void selectParticle() {
		final ButtonWindow<Element> dialog = new ButtonWindow<Element>(screen, true) {
			private PushButton btnCancel;
			private SelectList<String> list;
			private TextField nameFilter;
			private ComboBox<String> typeFilter;
			private AlarmTask refilterTask;

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				int r = attachments.getSelectedRowIndex();
				if (r > -1) {
					final List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
					final AttachmentItem item = attachmentItems.get(r);
					String sel = list.getSelectedValue();
					item.setEffect(sel);
					creature.getAppearance().setAttachments(attachmentItems);
					recreateAndFireUpdateModels();
					updateForms();
					attachments.setSelectedRowIndex(r);
				}
				hide();
			}

			@Override
			protected void createButtons(BaseElement buttons) {
				btnCancel = new PushButton(screen, "Cancel") {
					{
						setStyleClass("cancel");
					}
				};
				btnCancel.onMouseReleased(evt -> hide());
				btnCancel.setText("Cancel");
				buttons.addElement(btnCancel);
				form.addFormElement(btnCancel);
				super.createButtons(buttons);
			}

			@Override
			protected Element createContent() {

				Element container = new Element(screen);
				container.setLayoutManager(new MigLayout(screen, "wrap 2, fill", "[][]", "[][][][]"));

				container.addElement(new Label("Name Filter:", screen));
				nameFilter = new TextField(screen);
				nameFilter.onChange(evt -> {

					if (refilterTask != null)
						refilterTask.cancel();
					refilterTask = ToolKit.get().getAlarm().timed(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							refilter();
							return null;
						}
					}, 1);
				});
				container.addElement(nameFilter, "growx");

				container.addElement(new Label("Type Filter:", screen));
				typeFilter = new ComboBox<String>(screen);
				typeFilter.onChange(evt -> {
					if (!evt.getSource().isAdjusting()) {
						if (refilterTask != null)
							refilterTask.cancel();
						refilter();
					}
				});
				Set<String> particleFiles = ((ServerAssetManager) app.getAssetManager())
						.getAssetNamesMatching(".*\\.particle");
				for (String s : particleFiles) {
					typeFilter.addListItem(Icelib.toEnglish(Icelib.getBaseFilename(s)), s);
				}
				typeFilter.runAdjusting(() -> typeFilter.setSelectedByValue("Particles/attachable.particle"));
				container.addElement(typeFilter, "growx");

				list = new SelectList<String>(screen);
				refilter();
				container.addElement(list, "span 2");
				return container;

			}

			private void refilter() {
				final Object value = typeFilter.getSelectedListItem().getValue();
				System.out.println("refiltering " + value);
				if (list == null || typeFilter.getSelectIndex() == -1) {
					return;
				}
				list.invalidate();
				list.removeAllListItems();
				String filterText = nameFilter.getText().trim();
				OGREParticleConfiguration cfg = OGREParticleConfiguration.get(assetManager, (String) value);
				for (Map.Entry<String, OGREParticleScript> en : cfg.getBackingObject().entrySet()) {
					String k = en.getValue().getName();
					if (filterText.equals("") || (filterText.startsWith("~") && k.matches(filterText.substring(1))
							|| k.toLowerCase().contains(filterText.toLowerCase()))) {
						list.addListItem(k, k);
					}
				}
				list.validate();
			}
		};
		dialog.setDestroyOnHide(true);
		ElementStyle.warningColor(dialog.getDragBar());
		dialog.setWindowTitle("Select Particle Effect");
		dialog.setButtonOkText("Select");
		dialog.setResizable(false);
		dialog.setMovable(false);
		dialog.setModal(true);
		screen.showElement(dialog, ScreenLayoutConstraints.center);

	}

	private void tweakUpdated(Type type) {
		if (syncWithSelection.isChecked()) {
			if (previewEntity != null) {
				switch (type) {
				case SIZE:
				case TAIL_SIZE:
				case EAR_SIZE:
					previewEntity.updateSize();
					break;
				case SKIN:
					previewEntity.reloadSkin();
					break;
				case GENERAL:
					break;
				}
			}
			switch (type) {
			case TAIL_SIZE:
				target.getCreature().getAppearance().setTailSize(creature.getAppearance().getTailSize());
				break;
			case EAR_SIZE:
				target.getCreature().getAppearance().setEarSize(creature.getAppearance().getEarSize());
				break;
			case SIZE:
				target.getCreature().getAppearance().setSize(creature.getAppearance().getSize());
				break;
			default:
				try {
					String string = creature.getAppearance().toString();
					target.getCreature().getAppearance().parse(string);
				} catch (Exception ex) {
					LOG.log(Level.SEVERE, "Invalid appeareance.", ex);
					error("Invalid appearance.", ex);
				}
				break;
			}
			for (Listener l : listeners) {
				l.updateAppearance(this, type);
			}
		} else {
			if (previewEntity != null) {
				switch (type) {
				case SIZE:
				case TAIL_SIZE:
				case EAR_SIZE:
					previewEntity.updateSize();
					break;
				case SKIN:
					previewEntity.reloadSkin();
					break;
				default:
					spawnLoader.reload(previewEntity);
					break;
				}
			}
		}
	}

	private void fireUpdateModels() {
		for (Listener l : listeners) {
			l.updateModels(this);
		}
	}

	private void fireStopAnimation() {
		if (syncWithSelection.isChecked()) {
			for (Listener l : listeners) {
				l.stopAnimate();
			}
		}
	}

	private void fireAnimationSequence(AnimationRequest request) {
		if (syncWithSelection.isChecked()) {
			for (Listener l : listeners) {
				l.animate(request);
			}
		}
	}

	private void fireAnimationSpeedChange(float newSpeed) {
		if (syncWithSelection.isChecked()) {
			for (Listener l : listeners) {
				l.animationSpeedChange(newSpeed);
			}
		}
	}

	private void getDefinition() {
		/*
		 * Get the static creature definition
		 */
		if (creature.getAppearance().getName().equals(Appearance.Name.C2)) {
			creatureDefinition = contentDef
					.get(new CreatureKey("Biped", Icelib.toEnglish(creature.getAppearance().getRace()) + "_"
							+ Icelib.toEnglish(creature.getAppearance().getGender())));
		} else if (creature.getAppearance().getName().equals(Appearance.Name.N4)) {
			String bodyTemplate = creature.getAppearance().getBodyTemplate();
			creatureDefinition = modelDef.get(new CreatureKey(bodyTemplate));
		} else {
			creatureDefinition = null;
		}
	}

	private void updateForms() {
		LOG.info("Updating forms");
		Appearance appearance = creature == null ? new Appearance() : creature.getAppearance();

		// Common stuff
		name.runAdjusting(() -> name.setText(creature.getDisplayName()));
		size.runAdjusting(() -> size.setSelectedValue(appearance.getSize()));

		if (appearance.getName().equals(Appearance.Name.C2)) {
			bodyTemplate.runAdjusting(() -> bodyTemplate.setSelectedRowObjects(Arrays.asList(BIPED_KEY)));

			// Biped only stuff
			attachmentsPanel.show();
			earSize.show();
			tailSize.show();
			gender.show();
			head.show();
			bodyType.show();
			race.show();
			clothing.show();
			posePanel.show();
			prop.hide();

			clothingNotSupportedMessage.hide();
			skinNotSupportedMessage.hide();
			attachmentsNotSupportedMessage.hide();
			poseNotSupportedMessage.hide();

			tailSize.runAdjusting(() -> tailSize.setSelectedValue(appearance.getTailSize()));
			earSize.runAdjusting(() -> earSize.setSelectedValue(appearance.getEarSize()));
			gender.runAdjusting(() -> gender.setSelectedByValue(appearance.getGender()));
			head.runAdjusting(() -> head.setSelectedByValue(appearance.getHead()));
			bodyType.runAdjusting(() -> bodyType.setSelectedByValue(appearance.getBody()));
			race.runAdjusting(() -> race.setSelectedByValue(appearance.getRace()));
		} else if (appearance.getName().equals(Appearance.Name.P1)) {
			bodyTemplate.runAdjusting(() -> bodyTemplate.setSelectedRowObjects(Arrays.asList(PROP_KEY)));
			prop.setText(appearance.getProp());

			// Biped only stuff
			earSize.hide();
			tailSize.hide();
			gender.hide();
			head.hide();
			bodyType.hide();
			race.hide();
			clothing.hide();
			attachmentsPanel.hide();
			posePanel.hide();
			prop.show();

			clothingNotSupportedMessage.show();
			skinNotSupportedMessage.show();
			attachmentsNotSupportedMessage.show();
			poseNotSupportedMessage.show();

		} else {
			bodyTemplate.runAdjusting(() -> bodyTemplate
					.setSelectedRowObjects(Arrays.asList(new CreatureKey(appearance.getBodyTemplate()))));
			earSize.hide();
			tailSize.hide();
			gender.hide();
			head.hide();
			bodyType.hide();
			race.hide();
			attachmentsPanel.show();
			posePanel.show();

			clothingNotSupportedMessage.show();
			skinNotSupportedMessage.hide();
			attachmentsNotSupportedMessage.hide();
			poseNotSupportedMessage.hide();
			clothing.hide();
			prop.hide();
		}

		// Now build the skin elements
		skinPanel.removeAllChildren();
		if (creatureDefinition != null) {
			for (final Map.Entry<String, Skin> en : creatureDefinition.getSkin().entrySet()) {
				Label cc = new Label(screen);
				cc.setText(en.getValue().getDisplayName());
				cc.setToolTipText(en.getKey());
				RGB color = en.getValue().getDefaultColour();
				Appearance.SkinElement skinElement = appearance.getSkinElement(en.getKey());
				if (skinElement != null) {
					color = skinElement.getColor();
				}
				ColorButton cfc = new ColorButton(screen, IceUI.toRGBA(color), false, true) {
					@Override
					protected void onChangeColor(ColorRGBA newColor) {
						creature.getAppearance().addOrUpdateSkinElement(
								new Appearance.SkinElement(en.getKey(), IceUI.fromRGBA(newColor)));
						tweakUpdated(Type.SKIN);
					}
				};
				cfc.setTabs(ColorSelector.ColorTab.values());
				cfc.setPalette(IceUI.toRGBAList(en.getValue().getPalette()));
				skinPanel.addElement(cfc);
				skinPanel.addElement(cc);
				cfc.setLabel(cc);
			}

			// Armour
			List<Region> remaining = new ArrayList<Region>(Arrays.asList(Region.values()));
			for (ClothingItem item : appearance.getClothing()) {
				Region region = item.getType().toEquipType().toRegion();
				ComboBox<ClothingTemplate> combo = assetChoosers.get(region);
				combo.runAdjusting(() -> combo.setSelectedByValue(clothingDef.get(item.getKey())));
				remaining.remove(region);
			}
			for (Region r : remaining) {
				ComboBox<ClothingTemplate> combo = assetChoosers.get(r);
				combo.setSelectedIndex(0);
			}
		}

		// Attachments
		List<Object> selectedAttachments = attachments.getSelectedObjects();
		attachments.invalidate();
		attachments.removeAllRows();
		for (AttachmentItem ai : appearance.getAttachments()) {
			TableRow row = new TableRow(screen, attachments, ai);
			row.addCell(ai.getKey().getName(), ai.getKey());
			AttachmentPoint node = ai.getNode();
			if (node == null) {
				AttachableTemplate temp = attachableDef.get(ai.getKey());
				if (attachableTemplate != null) {
					node = temp.getAttachPoints().get(0);
				}
			}
			row.addCell(Icelib.toEnglish(node), node);
			attachments.addRow(row);
		}
		attachments.runAdjusting(() -> attachments.setSelectedRowObjects(selectedAttachments));
		attachments.validate();
		updateAttachmentDetails();

		window.getContentArea().dirtyLayout(false, LayoutType.boundsChange());
		window.getContentArea().layoutChildren();
	}

	public void setAnimations(Collection<String> anims, Collection<AnimationOption> presets) {
		posePanel.setPresets(presets);
		posePanel.setAnimations(anims);
		LOG.info("Setting anims: " + anims);
		if (anims != null) {
			posePanel.show();
		} else {
			posePanel.hide();
		}
		posePanel.layoutChildren();
	}

	private void recreateAndFireUpdateModels() {
		if (syncWithSelection.isChecked()) {
			try {
				target.getCreature().getAppearance().parse(creature.getAppearance().toString());
				fireUpdateModels();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			unloadAndRecreateCreatureSpatial();
		}
	}

	private void unloadAndRecreateCreatureSpatial() {
		removeCreatureSpatial();
		createCreatureSpatial();
		spawnLoader.reload(previewEntity);
	}

	private void removeCreatureSpatial() {
		if (previewEntity != null) {
			spawnLoader.remove(previewEntity);
		}
	}

	private void createCreatureSpatial() {
		LOG.info("Recreating");

		// Create the entity
		previewEntity = spawnLoader.create(creature);
		previewEntity.setFixedLighting(Lighting.UNLIT);
		previewEntity.getSpatial().setLocalTranslation(0, -1.65f, 0);
		previewEntity.getSpatial().scale(0.25f);
		previewEntity.getSpatial().rotate(0, FastMath.DEG_TO_RAD * 25f, 0);
		previewEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_LOADED, new Callable<Void>() {
			public Void call() throws Exception {
				if (previewEntity.getAnimControl() != null) {
					LOG.info("Adding animation");
					AnimationHandler<?, ?> animHandler = previewEntity.getAnimationHandler();
					animHandler.setSpeed(posePanel.getSpeed());
					previewEntity.getSpatial().addControl(animHandler);
					animHandler.addListener(CreatureEditorAppState.this);
					posePanel.setPresets(animHandler.getAnimations().values());
					posePanel.setAnimations(animHandler.getEntity().getDefinition().getAnimations().keySet());
					if (animHandler.isAnimating()) {
						posePanel.setSelectedPreset(animHandler.getActive().getName());
						posePanel.setActive(true);
					} else {
						posePanel.setActive(false);

					}

				} else {
					posePanel.setActive(false);
					posePanel.setPresets(null);
					posePanel.setAnimations(null);
				}
				return null;
			}
		});
		previewEntity.invoke(AbstractLoadableEntity.When.AFTER_SCENE_UNLOADED, new Callable<Void>() {
			public Void call() throws Exception {
				AnimationHandler<?, ?> animHandler = previewEntity.getAnimationHandler();
				if (animHandler != null) {
					animHandler.removeListener(CreatureEditorAppState.this);
				}
				return null;
			}
		});

		if (previewPanel != null) {
			previewPanel.setPreview(previewEntity.getSpatial());
		}

	}

	@SuppressWarnings("serial")
	abstract class AbstractSizeCommand implements UndoableCommand {

		private final float size;
		private float oldSize;

		public AbstractSizeCommand(float size) {
			this.size = size;
		}

		abstract void updateSize(float size);

		abstract float getOldSize();

		public void undoCommand() {
			try {
				updateSize(oldSize);
				tweakUpdated(CreatureEditorAppState.Type.SIZE);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Failed to update size.", e);
			}
		}

		public void doCommand() {
			try {
				oldSize = getOldSize();
				updateSize(size);
				tweakUpdated(CreatureEditorAppState.Type.SIZE);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Failed to update size.", e);
			}
		}
	}

	@SuppressWarnings("serial")
	class ChangeEarSizeCommand extends AbstractSizeCommand {

		public ChangeEarSizeCommand(float size) {
			super(size);
		}

		@Override
		void updateSize(float size) {
			CreatureEditorAppState.this.earSize
					.runAdjusting(() -> CreatureEditorAppState.this.earSize.setSelectedValue(size));
			creature.getAppearance().setEarSize(size);
		}

		@Override
		float getOldSize() {
			return creature.getAppearance().getEarSize();
		}
	}

	@SuppressWarnings("serial")
	class ChangeTailSizeCommand extends AbstractSizeCommand {

		public ChangeTailSizeCommand(float size) {
			super(size);
		}

		@Override
		void updateSize(float size) {
			CreatureEditorAppState.this.tailSize
					.runAdjusting(() -> CreatureEditorAppState.this.tailSize.setSelectedValue(size));
			creature.getAppearance().setTailSize(size);
		}

		@Override
		float getOldSize() {
			return creature.getAppearance().getTailSize();
		}
	}

	@SuppressWarnings("serial")
	class ChangeSizeCommand extends AbstractSizeCommand {

		public ChangeSizeCommand(float size) {
			super(size);
		}

		@Override
		void updateSize(float size) {
			CreatureEditorAppState.this.size
					.runAdjusting(() -> CreatureEditorAppState.this.size.setSelectedValue(size));
			creature.getAppearance().setSize(size);
		}

		@Override
		float getOldSize() {
			return creature.getAppearance().getSize();
		}
	}

	@SuppressWarnings("serial")
	class ChangeGenderCommand implements UndoableCommand {

		private final Appearance.Gender gender;
		private Appearance.Gender oldGender;

		public ChangeGenderCommand(Appearance.Gender gender) {
			this.gender = gender;
		}

		public void undoCommand() {
			creature.getAppearance().setGender(oldGender);
			recreateAndFireUpdateModels();
		}

		public void doCommand() {
			oldGender = creature.getAppearance().getGender();
			creature.getAppearance().setGender(gender);
			recreateAndFireUpdateModels();
		}
	}

	@SuppressWarnings("serial")
	class ChangeRaceCommand implements UndoableCommand {

		private final Appearance.Race race;
		private Appearance.Race oldRace;

		public ChangeRaceCommand(Appearance.Race race) {
			this.race = race;
		}

		public void undoCommand() {
			creature.getAppearance().setRace(oldRace);
			recreateAndFireUpdateModels();
		}

		public void doCommand() {
			oldRace = creature.getAppearance().getRace();
			creature.getAppearance().setRace(race);
			recreateAndFireUpdateModels();
		}
	}

	@SuppressWarnings("serial")
	class ChangeHeadCommand implements UndoableCommand {

		private final Appearance.Head head;
		private Appearance.Head oldHead;

		public ChangeHeadCommand(Appearance.Head head) {
			this.head = head;
		}

		public void undoCommand() {
			creature.getAppearance().setHead(oldHead);
			recreateAndFireUpdateModels();
		}

		public void doCommand() {
			oldHead = creature.getAppearance().getHead();
			creature.getAppearance().setHead(head);
			recreateAndFireUpdateModels();
		}
	}

	@SuppressWarnings("serial")
	class ChangeBodyCommand implements UndoableCommand {

		private final Appearance.Body body;
		private Appearance.Body oldBody;

		public ChangeBodyCommand(Appearance.Body body) {
			this.body = body;
		}

		public void undoCommand() {
			creature.getAppearance().setBody(oldBody);
			recreateAndFireUpdateModels();
		}

		public void doCommand() {
			oldBody = creature.getAppearance().getBody();
			creature.getAppearance().setBody(body);
			recreateAndFireUpdateModels();
		}
	}

	@SuppressWarnings("serial")
	class ChangePropCommand implements UndoableCommand {

		private final String prop;
		private String oldProp;

		public ChangePropCommand(String prop) {
			this.prop = prop;
		}

		public void undoCommand() {
			creature.getAppearance().setProp(oldProp);
			recreateAndFireUpdateModels();
		}

		public void doCommand() {
			oldProp = creature.getAppearance().getProp();
			creature.getAppearance().setProp(prop);
			recreateAndFireUpdateModels();
		}
	}

	class SelectTypeCommand implements UndoableCommand {

		private static final long serialVersionUID = 1L;
		private final CreatureKey type;
		private CreatureKey oldType;

		public SelectTypeCommand(CreatureKey type) {
			this.type = type;
		}

		public void undoCommand() {
			selectCreature(oldType);
		}

		public void doCommand() {
			oldType = creatureKey;
			selectCreature(type);
		}
	}
}
