package org.icedesign.app;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.io.FileUtils;
import org.icedesign.AbstractItemSelectWindow;
import org.icedesign.CloneItemWindow;
import org.icedesign.CreatureEditorAppState;
import org.icedesign.EditItemWindow;
import org.icedesign.NewItemWindow;
import org.icelib.Appearance;
import org.icelib.AttachableTemplate;
import org.icelib.Icelib;
import org.icelib.Persona;
import org.icelib.UndoManager;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.configuration.creatures.AbstractCreatureDefinition;
import org.icescene.controls.AnimationHandler;
import org.icescene.controls.AnimationRequest;
import org.icescene.entities.AbstractCreatureEntity;
import org.icescene.entities.EntityLoader;
import org.icescene.props.EntityFactory;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyDialogBox;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;
import org.iceui.controls.XScreen;
import org.iceui.controls.ZMenu;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icemoon.iceloader.ServerAssetManager;
import icetone.core.Container;
import icetone.core.Element.ZPriority;
import icetone.core.layout.mig.MigLayout;

public class MenuAppState extends IcemoonAppState<IcemoonAppState> {
	public enum ItemMenuActions {
		NEW_ITEM, DELETE_ITEM, EDIT_ITEM, CLONE_ITEM
	}

	private static final Logger LOG = Logger.getLogger(MenuAppState.class.getName());
	private Container layer;
	private final EntityLoader loader;
	private boolean cloning;
	private FancyButton tweak;
	private FancyButton options;
	private FancyButton exit;
	private final EntityFactory propFactory;
	private final Persona persona;
	private final UndoManager undoManager;
	private FancyButton items;
	private EditItemWindow itemTextureEditorWindow;

	public MenuAppState(UndoManager undoManager, EntityLoader loader, EntityFactory propFactory, Preferences prefs,
			Persona persona) {
		super(prefs);
		this.undoManager = undoManager;
		this.propFactory = propFactory;
		this.loader = loader;
		this.persona = persona;
	}

	@Override
	protected void postInitialize() {

		layer = new Container(screen);
		layer.setLayoutManager(new MigLayout(screen, "fill", "push[][][][]push", "[]push"));

		// Terrain
		tweak = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				toggleTweak();
			}
		};
		tweak.setText("Tweak");
		tweak.setToolTipText("Open Creature Tweak window");
		layer.addChild(tweak);

		// Terrain
		items = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				ZMenu menu = createItemsMenu();
				menu.showMenu(null, getAbsoluteX(), getAbsoluteY() - menu.getHeight());
			}
		};
		items.setText("Items");
		items.setIsEnabled(false);
		items.setToolTipText("WILL PROVIDE DIRECT ACCESS TO ITEMS - NOT COMPLETE");
		layer.addChild(items);

		// Options
		options = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				toggleOptions();
			}
		};
		options.setText("Options");
		options.setToolTipText("Show application options");
		layer.addChild(options);

		// Exit
		exit = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				exitApp();
			}
		};
		exit.setText("Exit");
		exit.setToolTipText("Exit Icedesign");
		layer.addChild(exit);

		//
		app.getLayers(ZPriority.MENU).addChild(layer);
	}

	@Override
	protected void onCleanup() {
		app.getLayers(ZPriority.MENU).removeChild(layer);
	}

	private void toggleOptions() {
		final OptionsAppState state = stateManager.getState(OptionsAppState.class);
		if (state == null) {
			stateManager.attach(new OptionsAppState(prefs));
		} else {
			stateManager.detach(state);
		}
	}

	public void edit(final AttachableTemplate def) {
		EditItemWindow itemTextureEditorWindow = ((XScreen) screen).getElementByClass(EditItemWindow.class);
		if (itemTextureEditorWindow == null) {
			itemTextureEditorWindow = new EditItemWindow(screen, ((IcesceneApp) app).getAssets(), prefs) {
				@Override
				protected void onCloseWindow() {
					screen.removeElement(this);
				}

				@Override
				protected void onTextureFileUpdated() {
					PreviewAppState pas = app.getStateManager().getState(PreviewAppState.class);
					if (pas != null) {
						try {
							pas.recreateSpatial();
						} catch (Exception e) {
							LOG.log(Level.SEVERE, "Failed to reload preview.", e);
						}
					}

					CreatureEditorAppState cas = app.getStateManager().getState(CreatureEditorAppState.class);
					if (cas != null) {
						try {
							cas.setTargetCreatureSpatial(pas.getSpatial());
						} catch (Exception e) {
							LOG.log(Level.SEVERE, "Failed to reload editor.", e);
						}
					}
				}
			};
			screen.addElement(itemTextureEditorWindow, null, true);
		}
		itemTextureEditorWindow.setValue(def);
	}

	protected void toggleTweak() {
		CreatureEditorAppState cas = app.getStateManager().getState(CreatureEditorAppState.class);
		if (cas == null) {
			app.getStateManager().attach(new CreatureEditorAppState(undoManager, prefs, propFactory, loader,
					MenuAppState.this.app.getAssets(), persona) {
				private IcesceneApp.AppListener appListener;
				private CreatureEditorAppState.Listener previewListener;

				@Override
				public void postInitialize() {
					super.postInitialize();

					UIUtil.center(screen, window);
					// Watch for app getting resized, center the
					// window
					this.app.addListener(appListener = new IcesceneApp.AppListener() {
						public void reshape(int w, int h) {
							UIUtil.center(screen, window);
						}
					});

					final PreviewAppState previewAppState = app.getStateManager().getState(PreviewAppState.class);
					addListener(previewListener = new CreatureEditorAppState.Listener() {
						public void updateAppearance(CreatureEditorAppState tweak, CreatureEditorAppState.Type type) {
							switch (type) {
							case SIZE:
							case TAIL_SIZE:
							case EAR_SIZE:
								previewAppState.getSpatial().updateSize();
								break;
							case SKIN:
								previewAppState.reloadSkin();
								break;
							default:
								previewAppState.updateAppearance();
								break;
							}
						}

						public void updateModels(CreatureEditorAppState tweak) {
							try {
								previewAppState.recreateSpatial();
							} catch (Exception ex) {
								LOG.log(Level.SEVERE, "Failed to recreate spatial.", ex);
							}
						}

						public void typeChanged(CreatureEditorAppState tweak, Appearance.Name newType) {
							try {
								previewAppState.recreateSpatial();
							} catch (Exception ex) {
								LOG.log(Level.SEVERE, "Failed to recreate spatial.", ex);
							}
						}

						public void stopAnimate() {
							AnimationHandler<? extends AbstractCreatureDefinition, AbstractCreatureEntity<? extends AbstractCreatureDefinition>> animationHandler = previewAppState
									.getSpatial().getAnimationHandler();
							if (animationHandler != null) {
								animationHandler.stop();
							}
						}

						@Override
						public void animate(AnimationRequest request) {
							AnimationHandler<? extends AbstractCreatureDefinition, AbstractCreatureEntity<? extends AbstractCreatureDefinition>> animationHandler = previewAppState
									.getSpatial().getAnimationHandler();
							if (animationHandler != null) {
								animationHandler.play(request);
							}

						}

						@Override
						public void animationSpeedChange(float newSpeed) {
							AnimationHandler<? extends AbstractCreatureDefinition, AbstractCreatureEntity<? extends AbstractCreatureDefinition>> animationHandler = previewAppState
									.getSpatial().getAnimationHandler();
							if (animationHandler != null) {
								animationHandler.setSpeed(newSpeed);
							}
						}
					});
					setSyncWithSelection(true);
					setTargetCreatureSpatial(previewAppState.getSpatial());
				}

				@Override
				protected void onCleanup() {
					super.onCleanup();
					removeListener(previewListener);
					this.app.removeListener(appListener);
				}
			});
		} else {
			app.getStateManager().detach(cas);
		}
	}

	private void delete(final AttachableTemplate def) {
		final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				Assets assets = ((IcesceneApp) app).getAssets();
				File dir = assets.getExternalAssetFile(String.format("Items/%s", def.getKey().getName()));
				FileUtils.deleteQuietly(dir);
				// ItemDefinitionManager defs =
				// ItemDefinitionManager.get(assetManager);
				// defs.remove(def);
				// File itemDef =
				// assets.getExternalAssetFile(ItemDefinitionManager.DEFAULT_RESOURCE_PATH);
				// try {
				// FileOutputStream fos = new FileOutputStream(itemDef);
				// LOG.info(String.format("Copying existing clothing definition
				// to %s",
				// itemDef));
				// try {
				// defs.write(fos);
				// } finally {
				// fos.close();
				// }
				// } catch (Exception e) {
				// error("Failed to write item definitions. ", e);
				// LOG.log(Level.SEVERE,
				// String.format("Failed to write item definitions %s",
				// itemDef), e);
				// }

				error("NOT IMPLEMENTED");
				((ServerAssetManager) app.getAssetManager()).clearCache();
				hideWindow();
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle(String.format("Delete %s", def.getKey().getName()));
		dialog.setButtonOkText("Delete");
		dialog.setMsg(String.format(
				"This will remove the item '%s' from your local workspace. Are you sure you wish to do this, it cannot be recovered once deleted?",
				def.getKey().getName()));
		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		dialog.sizeToContent();
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);
	}

	private ZMenu createItemsMenu() {
		ZMenu menu = new ZMenu(screen) {
			@Override
			public void onItemSelected(ZMenu.ZMenuItem item) {
				if (item.getValue().equals(ItemMenuActions.DELETE_ITEM)) {
					new AbstractItemSelectWindow(screen, "Delete Item", "Delete") {
						@Override
						protected boolean match(AttachableTemplate def) {
							return super.match(def) && ((IcesceneApp) app).getAssets()
									.getExternalAssetFile(String.format("Items/%s", def.getKey().getName())).exists();
						}

						@Override
						protected void doOnSelect(List<AttachableTemplate> def) {
							for (AttachableTemplate t : def)
								delete(t);
						}
					};
				} else if (item.getValue().equals(ItemMenuActions.EDIT_ITEM)) {
					new AbstractItemSelectWindow(screen, "Edit Item", "Edit") {

						@Override
						protected boolean match(AttachableTemplate def) {
							return super.match(def) && ((IcesceneApp) app).getAssets()
									.getExternalAssetFile(String.format("Items/%s", def.getKey().getName())).exists();
						}

						@Override
						protected void doOnSelect(List<AttachableTemplate> def) {
							edit(def.get(0));
						}
					};
				} else if (item.getValue().equals(ItemMenuActions.CLONE_ITEM)) {
					new AbstractItemSelectWindow(screen, "Clone Item", "Clone") {

						@Override
						protected void doOnSelect(List<AttachableTemplate> def) {
							new CloneItemWindow(screen).setItem(def.get(0));
						}
					};
				} else if (item.getValue().equals(ItemMenuActions.NEW_ITEM)) {
					new NewItemWindow(screen);
				}
			}
		};
		for (ItemMenuActions n : ItemMenuActions.values()) {
			menu.addMenuItem(Icelib.toEnglish(n), n);
		}

		// Show menu
		screen.addElement(menu);
		return menu;
	}

	private void exitApp() {
		final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), FancyWindow.Size.LARGE, true) {
			@Override
			public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}

			@Override
			public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
				app.stop();
			}
		};
		dialog.setDestroyOnHide(true);
		dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		dialog.setWindowTitle("Confirm Exit");
		dialog.setButtonOkText("Exit");
		dialog.setMsg("Are you sure you wish to exit? Make sure you have saved!");

		dialog.setIsResizable(false);
		dialog.setIsMovable(false);
		dialog.sizeToContent();
		UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
		dialog.showAsModal(true);
	}
}
