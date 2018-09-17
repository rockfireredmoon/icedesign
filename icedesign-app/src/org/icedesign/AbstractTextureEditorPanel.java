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
import org.icescene.FileMonitor;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.IcesceneApp;
import org.icescene.assets.Assets;
import org.icescene.configuration.ColorMapConfiguration;
import org.iceui.IceUI;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.ImageTableCell;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.input.event.MouseButtonEvent;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.buttons.PushButton;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.ToolKit;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.Alarm;
import icetone.extras.chooser.ColorFieldControl;
import icetone.extras.windows.InputBox;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

public abstract class AbstractTextureEditorPanel<T> extends StyledContainer {

	private static final Logger LOG = Logger.getLogger(AbstractTextureEditorPanel.class.getName());
	protected final Assets assets;
	private final PushButton edit;
	private FileMonitor.Monitor monitor;
	private final PushButton colourMap;
	private PersistentWindow colourMapWindow;
	protected final Preferences pref;
	private Table colourMapTable;
	private ColorMapConfiguration editingColorMap;
	protected T value;
	protected final Table table;
	protected StyledContainer actions;
	private PushButton folder;

	public AbstractTextureEditorPanel(final Assets assets, BaseScreen screen, Preferences pref) {
		super(screen);

		this.pref = pref;
		this.assets = assets;

		table = new Table(screen);
		table.onChanged(evt -> setAvailable());
		table.getScrollBounds().setMinDimensions(new Size(300, 300));
		table.setSelectionMode(Table.SelectionMode.ROW);
		table.setColumnResizeMode(Table.ColumnResizeMode.AUTO_FIRST);
		table.addColumn("Image");
		table.addColumn("Type");
		table.setHeadersVisible(false);

		actions = new StyledContainer(screen);
		actions.setLayoutManager(new FlowLayout(0, BitmapFont.Align.Center));
		edit = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		edit.onMouseReleased(evt -> {
			TableRow sel = table.getSelectedRow();
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
		});
		edit.setText("Edit");
		edit.setToolTipText("Edit in external editor");
		actions.addElement(edit);

		colourMap = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		colourMap.onMouseReleased(evt -> {
			TableRow sel = table.getSelectedRow();
			String imgPath = (String) sel.getValue();
			editColorMap(imgPath);
		});
		colourMap.setText("Map");
		colourMap.setToolTipText("Edit the colour map for this texture");
		actions.addElement(colourMap);

		folder = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		folder.onMouseReleased(evt -> {
			File folder = assets.getExternalAssetFile(getDirectoryPath());
			try {
				Icelib.makeDir(folder);
				XDesktop.getDesktop().open(folder);
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, String.format("Failed to open folder %s", folder), ex);
				HUDMessageAppState hud = ToolKit.get().getApplication().getStateManager()
						.getState(HUDMessageAppState.class);
				if (hud != null) {
					hud.message(Channel.ERROR, String.format("Failed to open folder %s", folder), ex);
				}
			}
		});
		folder.setText("Folder");
		folder.setToolTipText("Open the workspace folder that contains this item");
		actions.addElement(folder);
	}

	@Override
	public void controlCleanupHook() {
		super.controlCleanupHook();
		stopMonitoringFileChanges();
		if (colourMapWindow != null) {
			colourMapWindow.hide();
		}
	}

	public void setValue(T value) {
		this.value = value;
		table.removeAllRows();
		updateMonitor();
	}

	protected void textureFileUpdated(File file) {
		((ServerAssetManager) ToolKit.get().getApplication().getAssetManager()).clearCache();
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
					monitor = ((IcesceneApp) ToolKit.get().getApplication()).getMonitor().monitorDirectory(file,
							new FileMonitor.Listener() {
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
									// We can easily get multiple update events,
									// so
									// defer the reload
									if (s != null) {
										s.cancel();
									}
									s = ((IcesceneApp) ToolKit.get().getApplication()).getAlarm()
											.timed(new Callable<Void>() {
												public Void call() throws Exception {
													((ServerAssetManager) ToolKit.get().getApplication()
															.getAssetManager()).clearCache();
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
		editingColorMap = new ColorMapConfiguration(ToolKit.get().getApplication().getAssetManager(), colorMapPath);
		if (colourMapWindow == null) {
			colourMapWindow = new PersistentWindow(screen, DesignConfig.COLOUR_MAP_EDITOR, 8, VAlign.Bottom,
					Align.Right, new Size(200, 300), true, SaveType.POSITION_AND_SIZE, pref) {
				@Override
				protected void onCloseWindow() {
					colourMapWindow = null;
				}
			};
			colourMapWindow.setResizable(true);
			colourMapTable = new Table(screen);
			colourMapTable.addColumn("Element");
			colourMapTable.addColumn("Colour");
			colourMapWindow.getContentArea()
					.setLayoutManager(new MigLayout(screen, "wrap 1", "[fill, grow]", "[fill, grow][]"));
			colourMapWindow.getContentArea().addElement(colourMapTable);

			StyledContainer actions = new StyledContainer(screen);
			actions.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));

			// Add
			PushButton add = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			add.onMouseReleased(evt -> {
				InputBox fib = new InputBox(screen, colourMapWindow.getPixelPosition(), false) {
					{
						setStyleClass("large");
					}

					@Override
					public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
						hide();
					}

					@Override
					public void onButtonOkPressed(MouseButtonEvent evt, final String text, boolean toggled) {
						final Color colour = Color.WHITE;
						editingColorMap.add(text, colour);
						TableRow row = new TableRow(screen, colourMapTable, new Map.Entry<String, RGB>() {
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
						hide();
					}
				};
				fib.setWindowTitle("Add Colour");
				fib.setDestroyOnHide(true);
				ElementStyle.warningColor(fib.getDragBar());
				fib.setButtonOkText("Add");
				fib.sizeToContent();
				fib.setWidth(300);
				fib.setResizable(false);
				fib.setMovable(false);
				fib.setModal(true);
				screen.showElement(fib, ScreenLayoutConstraints.center);
			});
			add.setText("Add");
			actions.addElement(add);

			// Remove
			PushButton remove = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			remove.onMouseReleased(evt -> {
				TableRow row = colourMapTable.getSelectedRow();
				if (row != null) {
					@SuppressWarnings("unchecked")
					Map.Entry<String, RGB> e = (Map.Entry<String, RGB>) row.getValue();
					editingColorMap.remove(e.getKey());
					colourMapTable.removeRow(row);
				}
			});
			remove.setText("Remove");
			actions.addElement(remove);

			// Save
			PushButton save = new PushButton(screen) {
				{
					setStyleClass("fancy");
				}
			};
			save.onMouseReleased(evt -> {
				try {
					File extF = Icelib.makeParent(assets.getExternalAssetFile(colorMapPath));
					OutputStream out = new FileOutputStream(extF);
					try {
						editingColorMap.write(out, false);
					} finally {
						ColorMapConfiguration.resetCache();
						out.close();
					}
					colourMapWindow.hide();
				} catch (IOException ioe) {
					LOG.log(Level.SEVERE, "Failed to write new colormap.", ioe);
				}
			});
			save.setText("Save");
			actions.addElement(save);

			colourMapWindow.getContentArea().addElement(actions);

			screen.showElement(colourMapWindow);
		}
		colourMapTable.removeAllRows();
		for (Map.Entry<String, RGB> en : editingColorMap.getColors().entrySet()) {
			TableRow row = new TableRow(screen, colourMapTable, en);
			addColorMapRow(en.getKey(), row, en.getValue());
		}
		colourMapWindow.setWindowTitle(String.format("Colour Map - %s", Icelib.getBaseFilename(path)));
		colourMapWindow.show();
	}

	protected ImageTableCell addImagePath(String name, String imagePath, String value) {
		if (ToolKit.get().getApplication().getAssetManager().locateAsset(new AssetKey<String>(value)) == null) {
			LOG.warning(String.format("Resource %s does not exist, skipping", value));
			return null;
		}

		// TODO Auto determine this somehow, or get from styles
		float cellHeight = 64f;

		// Image
		ImageTableCell imgCell = new ImageTableCell(screen, name, cellHeight, imagePath);
		TableRow row = new TableRow(screen, table, value);
		row.addElement(imgCell);

		// Name
		TableCell nameCell = new TableCell(screen, name, name);
		row.addElement(nameCell);

		table.addRow(row);

		return imgCell;
	}

	protected void setAvailable() {
		if (table.isAnythingSelected()) {
			TableRow sel = table.getSelectedRow();
			String imgPath = (String) sel.getValue();
			colourMap.setEnabled(ToolKit.get().getApplication().getAssetManager()
					.locateAsset(new AssetKey<String>(imgPath + ".colormap")) != null);
			edit.setEnabled(true);
		} else {
			edit.setEnabled(false);
			colourMap.setEnabled(false);
		}
	}

	private void stopMonitoringFileChanges() {
		if (monitor != null) {
			monitor.stop();
			monitor = null;
		}
	}

	private void addColorMapRow(final String colourRegionName, TableRow row, final RGB colour) {
		TableCell nameCell = new TableCell(screen, colourRegionName, colourRegionName);
		row.addElement(nameCell);

		TableCell rgbCell = new TableCell(screen, null, colour);
		ColorFieldControl rgb = new ColorFieldControl(screen, IceUI.toRGBA(colour));
		rgb.onChange(evt -> editingColorMap.add(colourRegionName, IceUI.fromRGBA(evt.getNewValue())));
		rgbCell.setVAlign(BitmapFont.VAlign.Center);
		rgbCell.setHAlign(BitmapFont.Align.Center);
		rgbCell.addElement(rgb);
		row.addElement(rgbCell);
		colourMapTable.addRow(row);
	}
}
