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
import org.icelib.UndoManager;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.ServiceRef;
import org.icescene.animation.AnimationOption;
import org.icescene.animation.AnimationSequence;
import org.icescene.assets.Assets;
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
import org.iceui.HPosition;
import org.iceui.IceUI;
import org.iceui.VPosition;
import org.iceui.XTabPanelContent;
import org.iceui.controls.AutocompleteTextField;
import org.iceui.controls.AutocompleteTextField.AutocompleteItem;
import org.iceui.controls.AutocompleteTextField.AutocompleteSource;
import org.iceui.controls.CancelButton;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyButtonWindow;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;
import org.iceui.controls.TabPanelContent;
import org.iceui.controls.UIUtil;
import org.iceui.controls.XScreen;
import org.iceui.controls.XSeparator;
import org.iceui.controls.ZMenu;
import org.iceui.controls.color.ColorButton;
import org.iceui.controls.color.XColorSelector;

import com.jme3.font.BitmapFont;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.SelectList;
import icetone.controls.lists.Slider;
import icetone.controls.lists.Spinner;
import icetone.controls.lists.Table;
import icetone.controls.lists.Table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.controls.windows.TabControl;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.Screen;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.FillLayout;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

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
	private FancyButton clone;
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
	private boolean adjusting;
	private Map<Region, ArmourColourBar> colorChoosers = new HashMap<>();
	private Map<Region, ComboBox<ClothingTemplate>> assetChoosers = new HashMap<>();
	private Map<Region, ButtonAdapter> editButtons = new HashMap<>();
	private AbstractCreature creature;
	private TextField name;
	private Table bodyTemplate;
	private Element bipedDetails;
	private TabControl tabs;
	private Spinner<Float> earSize;
	private Spinner<Float> tailSize;
	private AbstractCreatureDefinition creatureDefinition;
	private Element skinPanel;
	private PosePanel posePanel;
	private TextField bodyFilter;
	private TextField clothingFilter;
	private Element clothing;
	private Label clothingNotSupportedMessage;
	private Table attachments;
	private ComboBox<AttachmentPoint> attachmentPoint;
	private TextField effect;
	private ButtonAdapter chooseEffect;
	private AttachmentColourBar attachmentColours;
	private Element attachmentDetails;
	private ComboBox<ClothingTemplate> clothingSet;
	private AttachableTemplate attachableTemplate;
	private TextField prop;
	private final EntityFactory propFactory;
	private Label skinNotSupportedMessage;
	private Label attachmentsNotSupportedMessage;
	private Element attachmentsPanel;
	private Label poseNotSupportedMessage;
	private FancyButton preview;
	private final EntityLoader spawnLoader;
	private AbstractSpawnEntity previewEntity;
	private Container previewContainerPanel;
	private Slider<Float> light;
	private AbstractSpawnEntity target;
	private CheckBox syncWithSelection;
	private FancyPersistentWindow clothingTextureEditorWindow;
	private ClothingTextureEditorPanel clothingTextureEditor;
	private final Assets assets;
	private FancyButton cloneAttachment;
	private FancyButton editAttachment;
	private CreatureKey creatureKey;

	protected FancyPersistentWindow window;
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

	public CreatureEditorAppState(UndoManager undoManager, Preferences pref, EntityFactory propFactory, EntityLoader spawnLoader,
			Assets assets, Persona persona) {
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
					syncWithSelection.setIsCheckedNoCallback(sync);
				} else {
					syncWithSelection.setIsChecked(sync);
				}
			}
		}
	}

	public void setTargetCreatureSpatial(AbstractSpawnEntity target) {
		this.target = target;
		if (syncWithSelection != null) {
			syncWithSelection.setIsVisible(target != null);
			// if (target == null) {
			// syncWithSelection.setIsCheckedNoCallback(target != null);
			// }
			if (syncWithSelection.getIsChecked()) {
				loadFromTargetAppearance();
			}
		}
	}

	@Override
	public void postInitialize() {

		assetManager = app.getAssetManager();
		screen = ((IcesceneApp) app).getScreen();

		window = new FancyPersistentWindow(screen, DesignConfig.CREATURE_TWEAK, 8, VPosition.TOP, HPosition.LEFT, LUtil.LAYOUT_SIZE,
				FancyWindow.Size.LARGE, false, SaveType.POSITION_AND_SIZE, prefs) {
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
		final Element windowContent = window.getContentArea();
		windowContent.setLayoutManager(new BorderLayout());

		// The editor area
		final Container editorArea = new Container(screen);
		editorArea.setLayoutManager(
				new MigLayout(screen, "fill, wrap 3", "[shrink 0][grow][shrink 0]", "[shrink 0][grow][shrink 0]"));
		windowContent.addChild(editorArea, BorderLayout.Border.CENTER);

		Label l = new Label(screen);
		l.setText("Name:");
		editorArea.addChild(l);

		name = new TextField(screen);
		editorArea.addChild(name, "growx");

		preview = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				prefs.putBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW,
						!prefs.getBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW, DesignConfig.CREATURE_TWEAK_PREVIEW_DEFAULT));
			}
		};
		preview.setText("Preview");
		preview.setButtonIconAlign(BitmapFont.Align.Right);
		ElementStyle.arrowButton((Screen) screen, preview, "Right");
		editorArea.addChild(preview, "");

		tabs = new TabControl(screen);
		tabs.setUseSlideEffect(true);
		editorArea.addChild(tabs, "growx, growy, span 3");

		adjust(true);

		tabs.addTab("Body", createBody());
		tabs.addTab("Skin", createSkin());
		tabs.addTab("Armour", createClothing());
		tabs.addTab("Attachments", createAttachments());
		tabs.addTab("Pose", createPose());

		// Buttons
		Element buttons = new Element(screen);
		buttons.setLayoutManager(new MigLayout(screen, "wrap 3", "[][]push[]", "[]"));
		editorArea.addChild(buttons, "growx, span 3");

		// Load appearance
		FancyButton load = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				showLoadMenu(evt.getX(), evt.getY());
			}
		};
		load.setText("Load");
		load.setToolTipText("Load creature appearance from clipboard, selection or slot");
		buttons.addChild(load);

		// Save appearance
		FancyButton save = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				showSaveMenu(evt.getX(), evt.getY());
			}
		};
		save.setText("Save");
		save.setToolTipText("Save creature appearance to clipboard, selection or slot");
		buttons.addChild(save);

		// Automatically synchronize with selection
		syncWithSelection = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				if (toggled) {
					loadFromTargetAppearance();
				}
			}
		};
		syncWithSelection.setLabelText("Synchronise with selection");
		syncWithSelection.setIsCheckedNoCallback(sync);
		syncWithSelection.setIsVisible(target != null);
		buttons.addChild(syncWithSelection);

		adjust(false);

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
						light.setSelectedValue(val);
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
			previewPanel = new Preview(screen);
			float lightAmt = prefs.getFloat(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT,
					DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT_DEFAULT);
			previewPanel.getAmbientLight().setColor(ColorRGBA.White.mult(lightAmt));

			createCreatureSpatial();

			// Rotator Buttons
			ButtonAdapter rotateLeft = new ButtonAdapter(screen, screen.getStyle("RotateLeftButton").getVector2f("defaultSize")) {
				@Override
				public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggled) {
					previewEntity.getSpatial().addControl(new Rotator(-3f));
				}

				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					previewEntity.getSpatial().removeControl(Rotator.class);
				}
			};
			rotateLeft.setStyles("RotateLeftButton");
			ButtonAdapter rotateRight = new ButtonAdapter(screen, screen.getStyle("RotateRightButton").getVector2f("defaultSize")) {
				@Override
				public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggled) {
					previewEntity.getSpatial().addControl(new Rotator(3f));
				}

				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					previewEntity.getSpatial().removeControl(Rotator.class);
				}
			};
			rotateRight.setStyles("RotateRightButton");
			Container rotatorButtons = new Container(screen);
			rotatorButtons.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
			rotatorButtons.addChild(rotateLeft);
			rotatorButtons.addChild(rotateRight);

			// Tools
			light = new Slider<Float>(screen, Slider.Orientation.HORIZONTAL, true) {
				@Override
				public void onChange(Float value) {
					prefs.putFloat(DesignConfig.CREATURE_TWEAK_PREVIEW_LIGHT, (Float) value);
				}
			};
			light.setToolTipText("Light");
			light.setSliderModel(new FloatRangeSliderModel(0.1f, 20f, lightAmt, 0.5f));
			light.setLockToStep(true);

			// Preview container panel (adds vertical separator)
			previewContainerPanel = new Container(screen);
			previewContainerPanel.setLayoutManager(new MigLayout(screen, "", "[][]", "[][][]"));
			previewContainerPanel.addChild(new XSeparator(screen, Element.Orientation.VERTICAL), "spany, growy");
			previewContainerPanel.addChild(light, "growx, wrap, pushy");
			previewContainerPanel.addChild(previewPanel, "wrap");
			previewContainerPanel.addChild(rotatorButtons, "ax 50%, pushy");

			ElementStyle.arrowButton((Screen) screen, preview, "Left");

			// Window
			window.getContentArea().addChild(previewContainerPanel, BorderLayout.Border.EAST);
			if (resizeWindow) {
				Vector2f pSz = LUtil.getContainerPreferredDimensions(previewContainerPanel);
				LUtil.setDimensions(window, pSz.x + window.getWidth(), window.getHeight());
				window.layoutChildren();
				UIUtil.saveWindowPositionAndSize(prefs, window, DesignConfig.CREATURE_TWEAK);
			}

			//
			spawnLoader.reload(previewEntity);
		} else if (previewContainerPanel != null
				&& !prefs.getBoolean(DesignConfig.CREATURE_TWEAK_PREVIEW, DesignConfig.CREATURE_TWEAK_PREVIEW_DEFAULT)) {
			final float targetWidth = window.getWidth() - previewContainerPanel.getWidth();
			window.getContentArea().removeChild(previewContainerPanel);
			ElementStyle.arrowButton((Screen) screen, preview, "Right");
			if (resizeWindow) {
				LUtil.setDimensions(window, targetWidth, window.getHeight());
				window.layoutChildren();
				UIUtil.saveWindowPositionAndSize(prefs, window, DesignConfig.CREATURE_TWEAK);
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
		window.hideWithEffect();
	}

	private void adjust(boolean a) {
		if (a == adjusting) {
			throw new IllegalStateException("Already adjusting.");
		}
		this.adjusting = a;
	}

	private XTabPanelContent createPose() {

		LOG.info("Creating poses");

		XTabPanelContent tpc = new XTabPanelContent(screen);
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

				if (syncWithSelection.getIsChecked()) {
					fireAnimationSequence(request);
				}
				else {
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

				if (syncWithSelection.getIsChecked()) {
					fireAnimationSpeedChange(newSpeed);
				}
			}

			@Override
			protected void onStopPose() {
				if (previewEntity != null) {
					previewEntity.getSpatial().getControl(AnimationHandler.class).stop();
				}
				if (syncWithSelection.getIsChecked()) {
					fireStopAnimation();
				}
			}

			@Override
			protected void onStartAnimation() {
				if (previewEntity != null) {
					previewEntity.getSpatial().getControl(AnimationHandler.class).play(new AnimationRequest(posePanel.getPreset()));
				}

				// TODO
			}
		};

		// Not supported messages
		poseNotSupportedMessage = new Label("Pose is not supported on this creature type", screen);
		poseNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addChild(posePanel, BorderLayout.Border.CENTER);
		tpc.addChild(poseNotSupportedMessage, BorderLayout.Border.CENTER);

		return tpc;
	}

	private TabPanelContent createSkin() {

		XTabPanelContent tpc = new XTabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		skinPanel = new Element(screen);
		skinPanel.setAsContainerOnly();
		skinPanel.setLayoutManager(new MigLayout(screen, "wrap 4"));

		// Not supported messages
		skinNotSupportedMessage = new Label("Skin is not supported on this creature type",
				screen)/* {
						
						@Override
						public void setPosition(Vector2f position) {
						// TODO Auto-generated method stub
						super.setPosition(position);
						System.out.println("setPosition " + position);
						}
						
						@Override
						public void setPosition(float x, float y) {
						// TODO Auto-generated method stub
						super.setPosition(x, y);
						System.out.println("setPosition " + x + ", " + y);
						}
						
						@Override
						public void setX(float x) {
						// TODO Auto-generated method stub
						super.setX(x);
						System.out.println("setX " + x);
						}
						
						@Override
						public void setY(float y) {
						// TODO Auto-generated method stub
						super.setY(y);
						System.out.println("setY " + y);
						}
						
						}*/;
		skinNotSupportedMessage.setTextAlign(BitmapFont.Align.Left);

		// Tab content
		tpc.addChild(skinPanel, BorderLayout.Border.CENTER);
		tpc.addChild(skinNotSupportedMessage, BorderLayout.Border.CENTER);
		return tpc;
	}

	private void createClothingRow(String label, final Region type, Element tabPanel) {
		tabPanel.addChild(new Label(label, screen));

		final ArmourColourBar colorBar = new ArmourColourBar(screen, type, creature) {
			@Override
			protected void onUpdate() {
				tweakUpdated(CreatureEditorAppState.Type.GENERAL);
			}
		};

		// Asset
		final ComboBox<ClothingTemplate> asset = new ComboBox<ClothingTemplate>(screen) {
			@Override
			public void onChange(int selectedIndex, ClothingTemplate value) {
				if (!adjusting) {

					final ClothingTemplate asset = value instanceof ClothingTemplate ? (ClothingTemplate) value : null;

					// Update the creature appearance
					Appearance appearance = creature.getAppearance();
					Appearance.ClothingList clothingList = appearance.getClothing();

					if (type == null) {
						clothingList.clear();

						// Clothing set
						if (asset == null) {
							// Set everything to None
							for (Map.Entry<Region, ComboBox<ClothingTemplate>> b : assetChoosers.entrySet()) {
								b.getValue().setSelectedIndex(0);
								colorChoosers.get(b.getKey()).setDefinition(null);
								editButtons.get(b.getKey()).setIsEnabled(false);
							}
						} else {
							// Set everything to one particular set
							final Map<Region, String> regions = asset.getRegions();
							List<Region> remainingRegions = new ArrayList<Region>(regions.keySet());

							// Regions that do exist in this set
							for (Region r : regions.keySet()) {
								ComboBox<ClothingTemplate> c = assetChoosers.get(r);
								c.setSelectedByValue(asset, false);
								remainingRegions.remove(r);
								clothingList.add(new ClothingItem(r.toClothingType(), asset.getKey(), null, null));
								colorChoosers.get(r).setDefinition(asset);
								setEditAvailable(c, editButtons.get(r));
							}

							// Regions that don't exist in this set
							for (Region r : remainingRegions) {
								ComboBox<ClothingTemplate> c = assetChoosers.get(r);
								colorChoosers.get(r).setDefinition(null);
								c.setSelectedIndex(0);
								editButtons.get(r).setIsEnabled(false);
							}
						}

					} else {
						// Items
						Appearance.ClothingType clothingType = type.toClothingType();
						ClothingItem it = clothingList.getItemForType(clothingType);
						if (selectedIndex == 0) {
							if (it != null) {
								clothingList.remove(it);
							}
						} else {
							if (it == null) {
								it = new ClothingItem(clothingType, asset.getKey(), null, null);
								clothingList.add(it);
							} else {
								it.setKey(asset.getKey());
							}
						}

						// Rebuild colours
						colorBar.setDefinition(asset);

						setEditAvailable(this, editButtons.get(type));

					}
					clone.setIsEnabled(
							clothingSet != null && clothingSet.getSelectedListItem().getValue() instanceof ClothingTemplate);
					appearance.setClothing(clothingList);
					creature.setAppearance(appearance);
					tweakUpdated(CreatureEditorAppState.Type.GENERAL);
				}
			}
		};
		// asset.addListItem("None", 0);
		asset.getMenu().setMaxDimensions(new Vector2f(Float.MAX_VALUE, 300));

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
		tabPanel.addChild(asset);

		// Color chooser
		if (type == null) {
			clone = new FancyButton(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
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
						System.out.println(">>" + def.getKey());

						List<ClothingTemplate> defs = new ArrayList<ClothingTemplate>();
						for (Region region : Region.values()) {
							ComboBox<ClothingTemplate> lb = assetChoosers.get(region);
							if (lb != null && lb.getSelectIndex() > 0) {
								ClothingTemplate t = lb.getSelectedListItem().getValue();
								defs.add(t);
							}
						}

						new CloneClothingWindow(screen, def.getKey(), defs) {

							protected void onDoCloneItem(final ClothingTemplate def, final ClothingTemplate newDef, String newName)
									throws IOException {
								// Now update the creature tweak window forms
								refilterClothing();
								clothingSet.setSelectedByValue(newDef, true);

							}
						};
					}
				}
			};
			clone.setIsEnabled(clothingSet != null && clothingSet.getSelectIndex() > -1
					&& clothingSet.getSelectedListItem().getValue() instanceof ClothingTemplate);
			clone.setText("Clone");
			clone.setToolTipText("Clone this clothing set for texture editing");
			tabPanel.addChild(clone, "span 4");
		} else {
			tabPanel.addChild(colorBar);
			colorChoosers.put(type, colorBar);

			// Copy
			final ButtonAdapter copyButton = new ButtonAdapter(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {

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
					screen.setClipboardText(SquirrelPrinter.format(outter));
				}
			};
			copyButton.setToolTipText("Copy appearance to clipboard");
			copyButton.setText("C");
			ElementStyle.small(copyButton);
			tabPanel.addChild(copyButton);
			copyButton.setIsVisible(type != null);

			// Paste
			final ButtonAdapter pasteButton = new ButtonAdapter(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					String clipText = screen.getClipboardText();
					if (clipText != null) {
						try {
							SquirrelTable eo = SquirrelInterpretedTable.table(clipText);
							SquirrelTable inner = (SquirrelTable) eo.get("c");
							if (inner == null) {
								throw new Exception("Content doesn't appear to be clothing item appearance string.");
							}
							String type = (String) inner.get("type");
							if (type == null) {
								throw new Exception("Content does not contain an asset type.");
							}
							// ENotation.EArray colors = (ENotation.EArray)
							// inner.get("colors");
							ClothingTemplate def = clothingDef.get(new EntityKey(type));
							if (def == null) {
								throw new Exception(String.format("Clothing item %s does not exist.", type));
							}
							asset.setSelectedByValue(def, true);
						} catch (Exception e) {
							LOG.log(Level.SEVERE, "Failed to parse clipboard content for clothing item apppearance.", e);
						}
					}
				}
			};
			pasteButton.setToolTipText("Paste appearance from clipboard");
			pasteButton.setText("P");
			ElementStyle.small(pasteButton);
			tabPanel.addChild(pasteButton);
			pasteButton.setIsVisible(type != null);

			// Edit
			final ButtonAdapter editButton = new ButtonAdapter(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					final Object value = asset.getSelectedListItem().getValue();
					if (value instanceof ClothingTemplate) {
						final ClothingTemplate def = (ClothingTemplate) value;
						edit(def, type);
					}
				}
			};
			editButtons.put(type, editButton);
			setEditAvailable(asset, editButton);
			editButton.setToolTipText("Edit this item");
			editButton.setText("E");
			ElementStyle.small(editButton);
			tabPanel.addChild(editButton);
			editButton.setIsVisible(type != null);
		}

		refilterAssets(asset, "", type);

	}

	private void setEditAvailable(ComboBox<ClothingTemplate> asset, ButtonAdapter editButton) {
		ClothingTemplate sel = asset.getSelectedValue();
		editButton
				.setIsEnabled(sel != null && assets
						.getExternalAssetFile(
								String.format("%s/%s", Icelib.toEnglish(sel.getKey().getType()), sel.getKey().getItemName()))
						.exists());
	}

	private void editItem(AttachableTemplate def) {

		EditItemWindow itemTextureEditorWindow = ((XScreen) screen).getElementByClass(EditItemWindow.class);
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
			screen.addElement(itemTextureEditorWindow, null, true);
		}
		itemTextureEditorWindow.setValue(def);
	}

	private void edit(ClothingTemplate def, Region region) {
		if (clothingTextureEditorWindow == null) {
			clothingTextureEditorWindow = new FancyPersistentWindow(screen, DesignConfig.CLOTHING_TEXTURE_EDITOR, 8,
					VPosition.MIDDLE, HPosition.RIGHT, LUtil.LAYOUT_SIZE, FancyWindow.Size.SMALL, true, SaveType.POSITION_AND_SIZE,
					prefs) {
				@Override
				protected void onCloseWindow() {
					clothingTextureEditorWindow = null;
					screen.removeElement(this);
				}
			};
			clothingTextureEditorWindow.setIsResizable(true);
			final Element windowContent = clothingTextureEditorWindow.getContentArea();
			windowContent.setLayoutManager(new FillLayout());
			clothingTextureEditor = new ClothingTextureEditorPanel(assets, screen, prefs) {
				@Override
				protected void textureFileUpdated(File file) {
					recreateAndFireUpdateModels();
				}
			};
			windowContent.addChild(clothingTextureEditor);
			clothingTextureEditorWindow.sizeToContent();
			UIUtil.center(screen, clothingTextureEditorWindow);
			screen.addElement(clothingTextureEditorWindow, null, true);
		}
		clothingTextureEditorWindow.setWindowTitle(String.format("Textures - %s", def.getKey().getName()));
		clothingTextureEditor.setClothingDefinition(def, region);
		if (!clothingTextureEditorWindow.getIsVisible()) {
			clothingTextureEditorWindow.showWithEffect();
		}
	}

	private XTabPanelContent createClothing() {
		XTabPanelContent tpc = new XTabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		clothing = new Element(screen);
		clothing.setAsContainerOnly();
		clothing.setLayoutManager(new MigLayout(screen, "wrap 2", "[shrink 0][grow]", "[]push[]push"));

		// Filter
		clothing.addChild(new Label("Filter", screen));
		clothingFilter = new TextField(screen) {
			@Override
			public void controlKeyReleaseHook(KeyInputEvent evt, String text) {
				refilterClothing();
			}
		};
		clothing.addChild(clothingFilter, "growx");

		// Clothing
		Element c = new Element(screen);
		c.setLayoutManager(new MigLayout(screen, "wrap 6, gap 2, ins 0",
				"[shrink 0][120::,grow]4[180:180:][::16,shrink 0][::16,shrink 0][::16,shrink 0]", "[]"));
		clothing.addChild(c, "growx, growy, span 2");

		createClothingRow("Armor Set", null, c);
		for (Region region : Region.values()) {
			createClothingRow(Icelib.toEnglish(region), region, c);
		}

		// Not supported messages
		clothingNotSupportedMessage = new Label("Armour is not supported on this creature type", screen);
		clothingNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addChild(clothing, BorderLayout.Border.CENTER);
		tpc.addChild(clothingNotSupportedMessage, BorderLayout.Border.CENTER);

		return tpc;
	}

	private void refilterClothing() {
		adjust(true);
		String filterText = clothingFilter.getText().toLowerCase();
		boolean changed = false;
		try {
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
		} finally {
			adjust(false);
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
		asset.removeAllListItems();
		asset.addListItem("None", null, false, false);
		int found = sel != null && sel.equals(0) ? 0 : -1;
		int index = 1;
		clothingDef.loadAll(assetManager);
		for (ClothingTemplate def : Icelib.sort(clothingDef.values())) {
			ClothingTemplateKey key = def.getKey();
			if (key != null && key.getName() != null) {
				if ((filterText.equals("") || (key.getName().toLowerCase().contains(filterText)))
						&& (type == null || def.getRegions().containsKey(type))) {
					asset.addListItem(key.getName(), def, false, false);
					if (key.getName().equals(sel)) {
						found = index;
					}
					index++;
				}
			}
		}
		if (found == -1) {
			asset.setSelectedIndex(Math.min(1, asset.getListItems().size()));
		}
		asset.pack(false);
		return found > 0;
	}

	private XTabPanelContent createBody() {

		Container top = new Container(screen);
		top.setLayoutManager(new MigLayout(screen, "ins 0", "[][grow,fill][]", "[]"));
		top.addChild(new Label("Filter", screen));
		bodyFilter = new TextField(screen) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				super.controlKeyPressHook(evt, text);
				refilterBody();
			}
		};
		top.addChild(bodyFilter);
		FancyButton clear = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				bodyFilter.setText("");
				refilterBody();
			}
		};
		clear.setText("Clear");
		top.addChild(clear);

		// Left side
		bodyTemplate = new Table(screen) {
			@Override
			public void onChange() {
				if (!adjusting) {

					// Determine type and template to use
					TableRow row = getSelectedRow();
					CreatureKey c = (CreatureKey) row.getValue();
					if (undoManager == null) {
						selectCreature(c);
					} else {
						undoManager.storeAndExecute(new SelectTypeCommand(c));
					}
				}
			}
		};
		bodyTemplate.setMinDimensions(new Vector2f(200, 200));
		bodyTemplate.setEnableKeyboardNavigation(true);
		bodyTemplate.addColumn("Body Template");
		bodyTemplate.setHeadersVisible(false);
		bodyTemplate.setSelectionMode(Table.SelectionMode.ROW);
		refilterBody();

		// Right side
		bipedDetails = new Element(screen);
		bipedDetails.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[][]"));
		bipedDetails.setAsContainerOnly();

		// Size
		Label label = new Label(screen);
		label.setText("Prop");
		bipedDetails.addChild(label);
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
		bipedDetails.addChild(prop, "growx");

		// Size
		label = new Label(screen);
		label.setText("Size");
		bipedDetails.addChild(label);
		size = new Spinner<Float>(screen, Spinner.Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				if (undoManager == null) {
					try {
						creature.getAppearance().setSize((Float) value);
						tweakUpdated(CreatureEditorAppState.Type.SIZE);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to update size.", e);
					}
				} else {
					undoManager.storeAndExecute(new ChangeSizeCommand((Float) value));
				}
			}
		};
		size.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		size.setLabel(label);
		bipedDetails.addChild(size);

		// Ear Size
		label = new Label(screen);
		label.setText("Ears");
		bipedDetails.addChild(label);
		earSize = new Spinner<Float>(screen, Spinner.Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				if (undoManager == null) {
					try {
						creature.getAppearance().setEarSize(value);
						tweakUpdated(CreatureEditorAppState.Type.EAR_SIZE);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to update ear size.", e);
					}
				} else {
					undoManager.storeAndExecute(new ChangeEarSizeCommand(value));
				}

			}
		};
		earSize.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 10f, 0.1f, 1f));
		earSize.setLabel(label);
		bipedDetails.addChild(earSize);

		// Tail Size
		label = new Label(screen);
		label.setText("Tail");
		bipedDetails.addChild(label);

		tailSize = new Spinner<Float>(screen, Spinner.Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				if (undoManager == null) {
					try {
						creature.getAppearance().setTailSize(value);
						tweakUpdated(CreatureEditorAppState.Type.TAIL_SIZE);
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to update tail size.", e);
					}
				} else {
					undoManager.storeAndExecute(new ChangeTailSizeCommand(value));
				}
			}
		};
		tailSize.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 10f, 0.1f, 1f));
		tailSize.setLabel(label);
		bipedDetails.addChild(tailSize);

		// Gender
		label = new Label(screen);
		label.setText("Gender");
		bipedDetails.addChild(label);
		gender = new ComboBox<Gender>(screen) {
			@Override
			public void onChange(int selectedIndex, Gender value) {
				if (!adjusting) {
					if (undoManager == null) {
						creature.getAppearance().setGender(value);
						recreateAndFireUpdateModels();
					} else {
						undoManager.storeAndExecute(new ChangeGenderCommand(value));
					}
				}
			}
		};
		for (Appearance.Gender g : Appearance.Gender.values()) {
			gender.addListItem(Icelib.toEnglish(g), g);
		}
		gender.setLabel(label);
		bipedDetails.addChild(gender);

		// Body Type
		label = new Label(screen);
		label.setText("Body Type");
		bipedDetails.addChild(label);
		bodyType = new ComboBox<Body>(screen) {
			@Override
			public void onChange(int selectedIndex, Body value) {
				if (!adjusting) {
					if (undoManager == null) {
						creature.getAppearance().setBody((Appearance.Body) value);
						recreateAndFireUpdateModels();
					} else {
						undoManager.storeAndExecute(new ChangeBodyCommand((Appearance.Body) value));
					}
				}
			}
		};
		for (Appearance.Body g : Appearance.Body.values()) {
			bodyType.addListItem(Icelib.toEnglish(g), g);
		}
		bodyType.setLabel(label);
		bipedDetails.addChild(bodyType);

		// Race
		label = new Label(screen);
		label.setText("Race");
		bipedDetails.addChild(label);
		race = new ComboBox<Race>(screen) {
			@Override
			public void onChange(int selectedIndex, Race value) {
				if (!adjusting) {
					if (undoManager == null) {
						creature.getAppearance().setRace(value);
						recreateAndFireUpdateModels();
					} else {
						undoManager.storeAndExecute(new ChangeRaceCommand(value));
					}
				}
			}
		};
		for (Appearance.Race g : Appearance.Race.values()) {
			race.addListItem(Icelib.toEnglish(g), g);
		}
		race.setLabel(label);
		bipedDetails.addChild(race);

		// Head
		label = new Label(screen);
		label.setText("Head");
		bipedDetails.addChild(label);
		head = new ComboBox<Head>(screen) {
			@Override
			public void onChange(int selectedIndex, Head value) {
				if (!adjusting) {
					if (undoManager == null) {
						creature.getAppearance().setHead((Appearance.Head) value);
						recreateAndFireUpdateModels();
					} else {
						undoManager.storeAndExecute(new ChangeHeadCommand((Appearance.Head) value));
					}
				}
			}
		};
		for (Appearance.Head g : Appearance.Head.values()) {
			head.addListItem(Icelib.toEnglish(g), g);
		}
		head.setLabel(label);
		bipedDetails.addChild(head);

		// This

		XTabPanelContent tpc = new XTabPanelContent(screen);
		tpc.setLayoutManager(new MigLayout(screen, "fill", "[grow, fill][shrink 0]", "[shrink 0][grow]"));
		tpc.addChild(top, "span 2, wrap");
		tpc.addChild(bodyTemplate, "growy");
		tpc.addChild(bipedDetails);

		return tpc;
	}

	private void refilterBody() {
		List<CreatureKey> kk = new ArrayList<CreatureKey>();
		kk.add(BIPED_KEY);
		kk.add(PROP_KEY);
		kk.addAll(CreatureKey.getAll("Horde", assetManager));
		kk.addAll(CreatureKey.getAll("Boss", assetManager));
		bodyTemplate.removeAllRows();

		for (CreatureKey k : kk) {
			if (bodyMatches(k)) {
				TableRow row = new Table.TableRow(screen, bodyTemplate, UIDUtil.getUID(), k);
				Table.TableCell cell = new Table.TableCell(screen, k.getText(), k);
				row.addChild(cell, null, false, false);
				bodyTemplate.addRow(row, false);
			}
		}
		bodyTemplate.pack();
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
		if (syncWithSelection.getIsChecked()) {
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
			attachmentPoint.setSelectedByValue(node, false);
			if (attachableTemplate.isParticle()) {
				attachmentColours.setIsVisible(false);
			} else {
				attachmentColours.setIsVisible(true);
				attachmentColours.setAttachment(item);
			}
			attachmentDetails.setIsVisible(true);
			cloneAttachment.setIsEnabled(true);
			editAttachment.setIsEnabled(app.getAssets().getExternalAssetFile(item.getKey().getPath()).exists());
		} else {
			attachmentDetails.setIsVisible(false);
			attachmentColours.setIsVisible(false);
			cloneAttachment.setIsEnabled(false);
			editAttachment.setIsEnabled(false);
		}
		attachmentPoint.setIsEnabled(node != null);
		chooseEffect.setIsEnabled(node != null);
		if (effectName != null) {
			effect.setText(effectName);
		} else {
			effect.setText("");
		}
	}

	private XTabPanelContent createAttachments() {
		LOG.info("Creating attachments");

		XTabPanelContent tpc = new XTabPanelContent(screen);
		tpc.setLayoutManager(new BorderLayout(8, 8));

		attachmentsPanel = new Element(screen);
		attachmentsPanel.setAsContainerOnly();
		attachmentsPanel.setLayoutManager(new MigLayout(screen, "fill, wrap 2", "[grow][shrink 0]", "[grow]"));

		// Attachments

		attachments = new Table(screen) {
			@Override
			public void onChange() {
				if (!adjusting) {
					updateAttachmentDetails();
				}
			}
		};
		attachments.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		attachments.addColumn("Attachment");
		attachments.addColumn("Point");

		// Sequences buttons
		Element attachmentActions = new Element(screen);
		attachmentActions.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow]", "[][][][][]"));

		// Add new attachment
		ButtonAdapter add = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				addAttachment();
			}
		};
		add.setText("Add");
		attachmentActions.addChild(add, "growx");

		// Remove attachment
		ButtonAdapter remove = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
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
			}
		};
		remove.setText("Remove");
		attachmentActions.addChild(remove, "growx");

		// Copy attachment
		ButtonAdapter copy = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				AttachmentItem sel = getSelectedAttachmentItem();
				if (sel != null) {
					screen.setClipboardText(sel.toSerializable().toString());
				}
			}
		};
		copy.setText("Copy");
		attachmentActions.addChild(copy, "growx");

		// Paste
		ButtonAdapter paste = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				int r = attachments.getSelectedRowIndex();
				if (r > -1) {
					final List<AttachmentItem> attachmentItems = creature.getAppearance().getAttachments();
					try {
						SquirrelTable eo = SquirrelInterpretedTable.table(screen.getClipboardText());
						AttachmentItem newItem = AttachmentItem.createAttachment(eo);
						attachmentItems.set(r, newItem);
						creature.getAppearance().setAttachments(attachmentItems);
						updateForms();
						recreateAndFireUpdateModels();
					} catch (Exception e) {
						LOG.log(Level.SEVERE, "Failed to parse creature appearance.", e);
					}
				}
			}
		};
		paste.setText("Paste");
		attachmentActions.addChild(paste, "growx");

		// Attachment point
		final List<AttachmentPoint> asList = Arrays.asList(AttachmentPoint.values());
		Collections.sort(asList, new Comparator<AttachmentPoint>() {
			public int compare(AttachmentPoint o1, AttachmentPoint o2) {
				return o1.name().compareTo(o2.name());
			}
		});
		attachmentPoint = new ComboBox<AttachmentPoint>(screen, asList.toArray(new AttachmentPoint[0])) {
			@Override
			public void onChange(int selectedIndex, AttachmentPoint value) {
				if (!adjusting) {
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
				}
			}
		};
		for (AttachmentPoint ap : asList) {
			attachmentPoint.addListItem(Icelib.toEnglish(ap, true), ap, false, false);
		}
		attachmentPoint.pack(false);

		// Effect
		effect = new TextField(screen);

		chooseEffect = new ButtonAdapter(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				selectParticle();
			}
		};
		chooseEffect.setText("..");
		effect.setIsEnabled(false);

		// Clone
		cloneAttachment = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
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
			}
		};
		cloneAttachment.setText("Clone");
		cloneAttachment.setToolTipText("Clone this item to a local copy so you can edit it.");

		// Edit
		editAttachment = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				AttachmentItem sel = getSelectedAttachmentItem();
				if (sel != null) {
					AttachableTemplate def = attachableDef.get(sel.getKey());
					editItem(def);
				}
			}
		};
		editAttachment.setText("Edit");
		editAttachment.setToolTipText(
				"Edit this items textures, models and colour maps. Only enabled when you are working on a cloned item.");

		// Item actions
		Container itemActions = new Container(screen);
		itemActions.setLayoutManager(new MigLayout(screen, "", "push[][]push", "[]"));
		itemActions.addChild(cloneAttachment);
		itemActions.addChild(editAttachment);

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
		attachmentDetails = new Element(screen);
		attachmentDetails.setLayoutManager(new MigLayout(screen, "fill, wrap 4", "[shrink 0][grow][shrink 0][:200:]", "[][][][]"));
		attachmentDetails.addChild(new Label("Point:", screen));
		attachmentDetails.addChild(attachmentPoint, "span 2, growx");
		attachmentDetails.addChild(new Label("Colours", screen), "ax 50%, wrap");
		attachmentDetails.addChild(new Label("Effect:", screen));
		attachmentDetails.addChild(effect, "growx");
		attachmentDetails.addChild(chooseEffect);
		attachmentDetails.addChild(attachmentColours, "ax 50%");
		attachmentDetails.addChild(itemActions, "span 4, growx");

		// This
		attachmentsPanel.addChild(attachments, "growx, growy");
		attachmentsPanel.addChild(attachmentActions);
		attachmentsPanel.addChild(attachmentDetails, "span 2, growx");

		// Not supported messages
		attachmentsNotSupportedMessage = new Label("Attachments are not supported on this creature type", screen);
		attachmentsNotSupportedMessage.setTextAlign(BitmapFont.Align.Center);

		// Tab content
		tpc.addChild(attachmentsPanel, BorderLayout.Border.CENTER);
		tpc.addChild(attachmentsNotSupportedMessage, BorderLayout.Border.CENTER);
		return tpc;
	}

	private void showSaveMenu(float x, float y) {
		ZMenu subMenu = new ZMenu(screen) {
			@Override
			protected void onItemSelected(ZMenu.ZMenuItem item) {
				int s = (Integer) item.getValue();
				if (s == -1) {
					screen.setClipboardText(creature.getAppearance().toString());
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
			}
		};
		subMenu.addMenuItem("Save to clipboard", -1);
		if (target != null && !syncWithSelection.getIsChecked()) {
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
		ZMenu subMenu = new ZMenu(screen) {
			@Override
			protected void onItemSelected(ZMenu.ZMenuItem item) {
				super.onItemSelected(item);
				int s = (Integer) item.getValue();
				try {
					if (s == -1) {
						parseAppearanceString(screen.getClipboardText());
						info("Loaded appearance from clipboard");
					} else if (s == 0) {
						loadFromTargetAppearance();
						info("Loaded appearance from target");
					} else {
						parseAppearanceString(getSlotPreferenceNode(s).get("appearance", ""));
						info(String.format("Loaded appearance from slot %d", s));
					}

					if (syncWithSelection.getIsChecked()) {
						for (Listener l : listeners) {
							l.updateAppearance(CreatureEditorAppState.this, Type.GENERAL);
						}
					}

				} catch (Exception ex) {
					error(String.format("Failed to parse creature appearance.", ex.getMessage()));
					LOG.log(Level.SEVERE, "Failed to parse creature appearance.", ex);
				}
			}
		};
		subMenu.addMenuItem("Load from clipboard", -1);
		if (target != null && !syncWithSelection.getIsChecked()) {
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
		final FancyButtonWindow<Element> dialog = new FancyButtonWindow<Element>(screen, new Vector2f(15, 15),
				FancyWindow.Size.SMALL, true) {
			private CancelButton btnCancel;
			private SelectList<String> list;
			private TextField nameFilter;
			private ComboBox<String> typeFilter;
			private boolean adjusting;

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
				hideWindow();
			}

			@Override
			protected void createButtons(Element buttons) {
				btnCancel = new CancelButton(screen, getUID() + ":btnCancel") {
					@Override
					public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
						hideWindow();
					}
				};
				btnCancel.setText("Cancel");
				buttons.addChild(btnCancel);
				form.addFormElement(btnCancel);
				super.createButtons(buttons);
			}

			@Override
			protected Element createContent() {

				adjust(true);
				try {

					Element container = new Element(screen);
					container.setLayoutManager(new MigLayout(screen, "wrap 2, fill", "[][]", "[][][][]"));

					container.addChild(new Label("Name Filter:", screen));
					nameFilter = new TextField(screen) {
						@Override
						public void controlKeyPressHook(KeyInputEvent evt, String text) {
							refilter();
						}
					};
					container.addChild(nameFilter, "growx");

					container.addChild(new Label("Type Filter:", screen));
					typeFilter = new ComboBox<String>(screen) {
						@Override
						public void onChange(int selectedIndex, String value) {
							if (!adjusting) {
								refilter();
							}
						}
					};
					Set<String> particleFiles = ((ServerAssetManager) app.getAssetManager()).getAssetNamesMatching(".*\\.particle");
					for (String s : particleFiles) {
						typeFilter.addListItem(Icelib.toEnglish(Icelib.getBaseFilename(s)), s);
					}
					typeFilter.setSelectedByValue("Particles/attachable.particle", false);
					container.addChild(typeFilter, "growx");

					list = new SelectList<String>(screen);
					refilter();
					container.addChild(list, "span 2");
					return container;
				} finally {
					adjust(false);
				}

			}

			private void refilter() {
				final Object value = typeFilter.getSelectedListItem().getValue();
				if (list == null || typeFilter.getSelectIndex() == -1) {
					return;
				}
				list.removeAllListItems();
				String filterText = nameFilter.getText().trim();
				OGREParticleConfiguration cfg = OGREParticleConfiguration.get(assetManager, (String) value);
				filterSection(cfg, (String) value, filterText);

			}

			private void filterSection(OGREParticleConfiguration cfg, String sectionName, String filterText) {
				for (Map.Entry<String, OGREParticleScript> en : cfg.getBackingObject().entrySet()) {
					String k = en.getValue().getName();
					if (filterText.equals("") || (filterText.startsWith("~") && k.matches(filterText.substring(1))
							|| k.toLowerCase().contains(filterText.toLowerCase()))) {
						list.addListItem(k, k);
					}
				}
				list.pack();
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle("Select Particle Effect");
		dialog.setButtonOkText("Select");
		dialog.sizeToContent();
		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);

	}

	private void tweakUpdated(Type type) {
		if (syncWithSelection.getIsChecked()) {
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
		if (syncWithSelection.getIsChecked()) {
			for (Listener l : listeners) {
				l.stopAnimate();
			}
		}
	}

	private void fireAnimationSequence(AnimationRequest request) {
		if (syncWithSelection.getIsChecked()) {
			for (Listener l : listeners) {
				l.animate(request);
			}
		}
	}

	private void fireAnimationSpeedChange(float newSpeed) {
		if (syncWithSelection.getIsChecked()) {
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
			creatureDefinition = contentDef.get(new CreatureKey("Biped", Icelib.toEnglish(creature.getAppearance().getRace()) + "_"
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
		adjust(true);

		// Update the forms
		try {

			// Common stuff
			name.setText(creature.getDisplayName());
			size.setSelectedValue(appearance.getSize());

			if (appearance.getName().equals(Appearance.Name.C2)) {
				bodyTemplate.setSelectedRowObjects(Arrays.asList(BIPED_KEY));

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

				tailSize.setSelectedValue(appearance.getTailSize());
				earSize.setSelectedValue(appearance.getEarSize());
				gender.setSelectedByValue(appearance.getGender(), false);
				head.setSelectedByValue(appearance.getHead(), false);
				bodyType.setSelectedByValue(appearance.getBody(), false);
				race.setSelectedByValue(appearance.getRace(), false);
			} else if (appearance.getName().equals(Appearance.Name.P1)) {
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
				try {
					bodyTemplate.setSelectedRowObjects(Arrays.asList(new CreatureKey(appearance.getBodyTemplate())));
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to select body template.", e);
				}
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
							creature.getAppearance()
									.addOrUpdateSkinElement(new Appearance.SkinElement(en.getKey(), IceUI.fromRGBA(newColor)));
							tweakUpdated(Type.SKIN);
						}
					};
					cfc.setTabs(XColorSelector.ColorTab.values());
					cfc.setPalette(IceUI.toRGBAList(en.getValue().getPalette()));
					skinPanel.addChild(cfc);
					skinPanel.addChild(cc);
					cfc.setLabel(cc);
				}

				// Armour
				List<Region> remaining = new ArrayList<Region>(Arrays.asList(Region.values()));
				for (ClothingItem item : appearance.getClothing()) {
					Region region = item.getType().toEquipType().toRegion();
					ArmourColourBar colourBar = colorChoosers.get(region);
					ComboBox<ClothingTemplate> combo = assetChoosers.get(region);
					combo.setSelectedByValue(clothingDef.get(item.getKey()), true);
					remaining.remove(region);
				}
				for (Region r : remaining) {
					ComboBox<ClothingTemplate> combo = assetChoosers.get(r);
					combo.setSelectedIndexWithCallback(0);
				}
			}

			// Attachments
			List<Object> selectedAttachments = attachments.getSelectedObjects();
			attachments.removeAllRows();
			for (AttachmentItem ai : appearance.getAttachments()) {
				Table.TableRow row = new Table.TableRow(screen, attachments, ai);
				row.addCell(ai.getKey().getName(), ai.getKey());
				AttachmentPoint node = ai.getNode();
				if (node == null) {
					AttachableTemplate temp = attachableDef.get(ai.getKey());
					if (attachableTemplate != null) {
						node = temp.getAttachPoints().get(0);
					}
				}
				row.addCell(Icelib.toEnglish(node), node);
				attachments.addRow(row, false);
			}
			attachments.setSelectedRowObjects(selectedAttachments);
			attachments.pack();
			updateAttachmentDetails();

			window.getContentArea().dirtyLayout(true);
			window.getContentArea().layoutChildren();

		} finally {
			adjust(false);
		}
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
		if (syncWithSelection.getIsChecked()) {
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
	abstract class AbstractSizeCommand implements UndoManager.UndoableCommand {

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
			creature.getAppearance().setSize(size);
		}

		@Override
		float getOldSize() {
			return creature.getAppearance().getSize();
		}
	}

	@SuppressWarnings("serial")
	class ChangeGenderCommand implements UndoManager.UndoableCommand {

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
	class ChangeRaceCommand implements UndoManager.UndoableCommand {

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
	class ChangeHeadCommand implements UndoManager.UndoableCommand {

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
	class ChangeBodyCommand implements UndoManager.UndoableCommand {

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
	class ChangePropCommand implements UndoManager.UndoableCommand {

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

	class SelectTypeCommand implements UndoManager.UndoableCommand {

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
