package org.icedesign;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.io.IOUtils;
import org.icelib.Color;
import org.icelib.Icelib;
import org.icelib.RGB;
import org.icelib.XDesktop;
import org.icescene.Alarm;
import org.icescene.FileMonitor;
import org.icescene.HUDMessageAppState;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.configuration.ColorMapConfiguration;
import org.iceui.HPosition;
import org.iceui.IceUI;
import org.iceui.VPosition;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyInputBox;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.ImageTableCell;
import org.iceui.controls.SaveType;
import org.iceui.controls.UIUtil;
import org.iceui.controls.color.ColorFieldControl;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.lists.Table;
import icetone.core.Container;
import icetone.core.ElementManager;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

public abstract class AbstractTextureEditorPanel<T> extends Container {

	private static final Logger LOG = Logger.getLogger(AbstractTextureEditorPanel.class.getName());
	protected final Assets assets;
	private final FancyButton edit;
	private FileMonitor.Monitor monitor;
	private final FancyButton colourMap;
	private FancyPersistentWindow colourMapWindow;
	protected final Preferences pref;
	private Table colourMapTable;
	private ColorMapConfiguration editingColorMap;
	protected T value;
	protected final Table table;
	protected Container actions;
	private FancyButton folder;

	public AbstractTextureEditorPanel(final Assets assets, ElementManager screen, Preferences pref) {
		super(screen);

		this.pref = pref;
		this.assets = assets;

		table = new Table(screen) {
			@Override
			public void onChange() {
				setAvailable();
			}
		};
		table.getScrollBounds().setMinDimensions(new Vector2f(300, 300));
		table.setSelectionMode(Table.SelectionMode.ROW);
		table.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		table.addColumn("Image");
		table.addColumn("Type");
		table.setHeadersVisible(false);

		actions = new Container(screen);
		actions.setLayoutManager(new FlowLayout(0, BitmapFont.Align.Center));
		edit = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				Table.TableRow sel = table.getSelectedRow();
				String imgPath = (String) sel.getValue();
				try {
					File saveFile = new File(assets.getExternalAssetsFolder(), imgPath.replace('/', File.separatorChar));
					if (!saveFile.exists()) {
						File dir = saveFile.getParentFile();
						if (!dir.exists() && !dir.mkdirs()) {
							throw new IOException(String.format("Failed to create save directory %s.", dir));
						}
						LOG.info(String.format("Copying resource to %s for editing", saveFile));
						AssetManager mgr = screen.getApplication().getAssetManager();
						AssetInfo inf = mgr.locateAsset(new AssetKey<String>(imgPath));
						if (inf == null) {
							LOG.info(String.format("No existing resource %s, maybe the editor will create one.", saveFile));
						} else {
							InputStream in = inf.openStream();
							try {
								OutputStream out = new FileOutputStream(saveFile);
								try {
									IOUtils.copy(in, out);
								} finally {
									out.close();
								}
							} finally {
								in.close();
							}
						}
					}
					XDesktop.getDesktop().edit(saveFile);
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to edit image.", e);
				}
			}
		};
		edit.setText("Edit");
		edit.setToolTipText("Edit in external editor");
		actions.addChild(edit);

		colourMap = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				Table.TableRow sel = table.getSelectedRow();
				String imgPath = (String) sel.getValue();
				editColorMap(imgPath);
			}
		};
		colourMap.setText("Map");
		colourMap.setToolTipText("Edit the colour map for this texture");
		actions.addChild(colourMap);

		folder = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				File folder = assets.getExternalAssetFile(getDirectoryPath());
				try {
					Icelib.makeDir(folder);
					XDesktop.getDesktop().open(folder);
				} catch (Exception ex) {
					LOG.log(Level.SEVERE, String.format("Failed to open folder %s", folder), ex);
					HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
					if (hud != null) {
						hud.message(Level.SEVERE, String.format("Failed to open folder %s", folder), ex);
					}
				}
			}
		};
		folder.setText("Folder");
		folder.setToolTipText("Open the workspace folder that contains this item");
		actions.addChild(folder);
	}

	@Override
	public void controlCleanupHook() {
		super.controlCleanupHook();
		stopMonitoringFileChanges();
		if (colourMapWindow != null) {
			colourMapWindow.hideWithEffect();
		}
	}

	public void setValue(T value) {
		this.value = value;
		table.removeAllRows();
		updateMonitor();
	}

	protected void textureFileUpdated(File file) {
		((ServerAssetManager) app.getAssetManager()).clearCache();
	}

	protected abstract String getDirectoryPath();

	protected void updateMonitor() {

		// If the image is a local asset resource, watch it for changes
		String directoryPath = getDirectoryPath();
		try {
			stopMonitoringFileChanges();
			if (value != null) {
				final File file = new File(assets.getExternalAssetsFolder(), directoryPath);
				if (file.exists()) {
					monitor = ((IcesceneApp) app).getMonitor().monitorDirectory(file, new FileMonitor.Listener() {
						private Alarm.AlarmTask s;

						public void fileUpdated(final File file) {
							triggerReload(file);
						}

						public void fileCreated(File file) {
							triggerReload(file);
						}

						public void fileDeleted(File file) {
							triggerReload(file);
						}

						private void triggerReload(final File file) {
							// We can easily get multiple update events, so
							// defer the reload
							if (s != null) {
								s.cancel();
							}
							s = ((IcesceneApp) app).getAlarm().timed(new Callable<Void>() {
								public Void call() throws Exception {
									((ServerAssetManager) app.getAssetManager()).clearCache();
									setValue(value);
									textureFileUpdated(file);
									return null;
								}
							}, 1);
						}
					});
				}
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE, String.format("Failed to monitor for file changes of %s", directoryPath), e);
		}

		setAvailable();
	}

	private void editColorMap(String path) {
		final String colorMapPath = path + ".colormap";
		editingColorMap = new ColorMapConfiguration(app.getAssetManager(), colorMapPath);
		if (colourMapWindow == null) {
			colourMapWindow = new FancyPersistentWindow(screen, DesignConfig.COLOUR_MAP_EDITOR, 8, VPosition.BOTTOM,
					HPosition.RIGHT, new Vector2f(200, 300), FancyWindow.Size.SMALL, true, SaveType.POSITION_AND_SIZE, pref) {
				@Override
				protected void onCloseWindow() {
					colourMapWindow = null;
				}
			};
			colourMapWindow.setIsResizable(true);
			colourMapTable = new Table(screen) {
				@Override
				public void onChange() {
				}
			};
			colourMapTable.addColumn("Element");
			colourMapTable.addColumn("Colour");
			colourMapWindow.getContentArea().setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[fill, grow][]"));
			colourMapWindow.getContentArea().addChild(colourMapTable);

			Container actions = new Container(screen);
			actions.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));

			// Add
			FancyButton add = new FancyButton(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					FancyInputBox fib = new FancyInputBox(screen, colourMapWindow.getPosition(), FancyWindow.Size.LARGE, false) {
						@Override
						public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
							hideWindow();
						}

						@Override
						public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
							final Color colour = Color.WHITE;
							editingColorMap.add(text, colour);
							Table.TableRow row = new Table.TableRow(screen, colourMapTable, UIDUtil.getUID(),
									new Map.Entry<String, RGB>() {
										private RGB val = colour;

										public String getKey() {
											return text;
										}

										public RGB getValue() {
											return val;
										}

										public RGB setValue(RGB val) {
											this.val = val;
											return val;
										}
									});
							addColorMapRow(text, row, colour);
							hideWindow();
						}
					};
					fib.setWindowTitle("Add Colour");
					fib.setDestroyOnHide(true);
					fib.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
					fib.setButtonOkText("Add");
					fib.sizeToContent();
					fib.setWidth(300);
					fib.setIsResizable(false);
					fib.setIsMovable(false);
					UIUtil.center(screen, fib);
					screen.addElement(fib, null, true);
					fib.showAsModal(true);
				}
			};
			add.setText("Add");
			actions.addChild(add);

			// Remove
			FancyButton remove = new FancyButton(screen) {
				@SuppressWarnings("unchecked")
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					Table.TableRow row = colourMapTable.getSelectedRow();
					if (row != null) {
						Map.Entry<String, RGB> e = (Map.Entry<String, RGB>) row.getValue();
						editingColorMap.remove(e.getKey());
						colourMapTable.removeRow(row);
					}
				}
			};
			remove.setText("Remove");
			actions.addChild(remove);

			// Save
			FancyButton save = new FancyButton(screen) {
				@Override
				public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
					try {
						File extF = Icelib.makeParent(assets.getExternalAssetFile(colorMapPath));
						OutputStream out = new FileOutputStream(extF);
						try {
							editingColorMap.write(out, false);
						} finally {
							ColorMapConfiguration.resetCache();
							out.close();
						}
						colourMapWindow.hideWithEffect();
					} catch (IOException ioe) {
						LOG.log(Level.SEVERE, "Failed to write new colormap.", ioe);
					}
				}
			};
			save.setText("Save");
			actions.addChild(save);

			colourMapWindow.getContentArea().addChild(actions);

			screen.addElement(colourMapWindow, null, true);
		}
		colourMapTable.removeAllRows();
		for (Map.Entry<String, RGB> en : editingColorMap.getColors().entrySet()) {
			Table.TableRow row = new Table.TableRow(screen, colourMapTable, UIDUtil.getUID(), en);
			addColorMapRow(en.getKey(), row, en.getValue());
		}
		colourMapWindow.setWindowTitle(String.format("Colour Map - %s", Icelib.getBaseFilename(path)));
		colourMapWindow.showWithEffect();
	}

	protected void addImagePath(String name, String imagePath, String value) {
		if (app.getAssetManager().locateAsset(new AssetKey<String>(value)) == null) {
			LOG.warning(String.format("Resource %s does not exist, skipping", value));
			return;
		}

		// TODO Auto determine this somehow, or get from styles
		float cellHeight = 64f;

		// Image
		Table.TableCell imgCell = new ImageTableCell(screen, name, cellHeight, imagePath);
		Table.TableRow row = new Table.TableRow(screen, table, UIDUtil.getUID(), value);
		row.addChild(imgCell);

		// Name
		Table.TableCell nameCell = new Table.TableCell(screen, name, name);
		row.addChild(nameCell);

		table.addRow(row);
	}

	protected void setAvailable() {
		if (table.isAnythingSelected()) {
			Table.TableRow sel = table.getSelectedRow();
			String imgPath = (String) sel.getValue();
			colourMap.setIsEnabled(app.getAssetManager().locateAsset(new AssetKey<String>(imgPath + ".colormap")) != null);
			edit.setIsEnabled(true);
		} else {
			edit.setIsEnabled(false);
			colourMap.setIsEnabled(false);
		}
	}

	private void stopMonitoringFileChanges() {
		if (monitor != null) {
			monitor.stop();
			monitor = null;
		}
	}

	private void addColorMapRow(final String colourRegionName, Table.TableRow row, final RGB colour) {
		Table.TableCell nameCell = new Table.TableCell(screen, colourRegionName, colourRegionName);
		row.addChild(nameCell);

		Table.TableCell rgbCell = new Table.TableCell(screen, null, colour);
		ColorFieldControl rgb = new ColorFieldControl(screen, IceUI.toRGBA(colour)) {
			@Override
			protected void onChangeColor(ColorRGBA newColor) {
				editingColorMap.add(colourRegionName, IceUI.fromRGBA(newColor));
			}
		};
		rgbCell.setVAlign(BitmapFont.VAlign.Center);
		rgbCell.setHAlign(BitmapFont.Align.Center);
		rgbCell.addChild(rgb);
		row.addChild(rgbCell);
		colourMapTable.addRow(row);
	}
}
