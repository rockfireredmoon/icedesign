package org.icedesign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.icelib.AttachableTemplate;
import org.icelib.AttachmentPoint;
import org.icelib.EntityKey;
import org.icelib.EntityKey.Type;
import org.icelib.Icelib;
import org.icelib.XDesktop;
import org.icescene.HUDMessageAppState;
import org.icescene.ServiceRef;
import org.icescene.configuration.attachments.AttachableDef;
import org.icescene.configuration.attachments.AttachableTemplates;
import org.iceui.controls.AutocompleteTextField;
import org.iceui.controls.AutocompleteTextField.AutocompleteItem;
import org.iceui.controls.CancelButton;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButtonWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.LineWrapMode;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.menuing.MenuItem;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

public class NewItemWindow extends FancyButtonWindow<Element> {

	private static final Logger LOG = Logger.getLogger(NewItemWindow.class.getName());

	// protected ComboBox<AttachmentPoint> attachmentPoint;
	protected TextField item;
	private CancelButton btnCancel;
	private CheckBox openFolder;
	protected AutocompleteTextField<String> name;
	protected ComboBox<AttachableTemplate> template;
	protected String fullName;
	protected ComboBox<Type> templateType;
	protected ComboBox<Type> type;

	@ServiceRef
	protected static AttachableTemplates attachableTemplatesConfiguration;
	@ServiceRef
	protected static AttachableDef attachableDef;

	public NewItemWindow(ElementManager screen) {
		super(screen, UIDUtil.getUID(), Vector2f.ZERO, LUtil.LAYOUT_SIZE, FancyWindow.Size.SMALL, true);

		ElementStyle.normal(screen, getDragBar(), true, false);
		getDragBar().setTextWrap(LineWrapMode.Word);
		setDestroyOnHide(true);
		getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		sizeToContent();
		setWidth(330);
		setIsResizable(false);
		setIsMovable(false);
		setWindowTitle("New Item");
		setButtonOkText("Create");
		UIUtil.center(screen, this);
		screen.addElement(this, null, true);
		showAsModal(true);
	}

	public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
		hideWindow();
	}

	@Override
	public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
		HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);

		// Create the key for the new item
		EntityKey newKey = new EntityKey();
		newKey.setType(type.getSelectedListItem().getValue());
		newKey.setItem(name.getText());
		newKey.setTemplate(item.getText());

		/* Create and add the new item to the list we are going to write. Also give sub-classes
		 * a chance to hook in and configure  this item (e.g. cloning)
		 */
		AttachableTemplate newItem = createItem(newKey);
		newItem.setDelegate(template.getSelectedListItem().getValue());
		configureNewItem(newItem);

		if (attachableDef.containsKey(newKey)) {
			hud.message(Level.SEVERE, String.format("%s already exists.", newKey.getName()));
		} else {
			try {
				ItemWriter itemWriter = new ItemWriter(app, newKey);
				itemWriter.addNewAttachable(newItem);
				itemWriter.write();

				// Create item file
				try {
					if (openFolder.getIsChecked()) {
						XDesktop.getDesktop().open(itemWriter.getItemDir());
					}
					onCreate(newItem, itemWriter.getAssetsDir());
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to create item.", e);
					hud.message(Level.SEVERE, "Failed to create item.", e);
				}

				//
				hideWindow();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void configureNewItem(AttachableTemplate newItem) {
	}

	protected AttachableTemplate createItem(EntityKey newKey) {
		return new AttachableTemplate(newKey);
	}

	protected void copy(File assetDir, String sourcePath, String targetPath) throws IOException, FileNotFoundException {
		LOG.info(String.format("Cloning %s to %s", sourcePath, targetPath));
		try {
			AssetInfo info = screen.getApplication().getAssetManager().locateAsset(new AssetKey<String>(sourcePath));
			InputStream in = info.openStream();
			try {
				File file = new File(assetDir, targetPath.replace('/', File.separatorChar));
				File dir = file.getParentFile();
				if (!dir.exists() && !dir.mkdirs()) {
					throw new IOException(String.format("Failed to create directory %s.", dir));
				}
				LOG.info(String.format("Writing %s", file.getAbsolutePath()));
				OutputStream out = new FileOutputStream(file);
				try {
					IOUtils.copy(in, out);
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
		} catch (AssetNotFoundException anfe) {
			LOG.warning(String.format("Could not find asset %s to copy.", sourcePath));
		}
	}

	protected void onCreate(AttachableTemplate newDef, File assetsDir) throws IOException {
		HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
		hud.message(Level.INFO, String.format("Created %s.", name.getText()));
	}

	@Override
	protected Element createContent() {
		Element contentArea = new Element(screen);
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 2", "[shrink 0][fill, grow]", "[][][][][]"));

		// Template Type
		contentArea.addChild(new Label("Template Type", screen));
		contentArea.addChild(templateType = new ComboBox<EntityKey.Type>(screen, EntityKey.Type.values()) {
			@Override
			public void onChange(int selectedIndex, EntityKey.Type value) {
				rebuildTemplates();
				reselectAttachmentPoint();
			}
		});

		// NOTE, only one template type is actually used currently
		templateType.setSelectedByValue(EntityKey.Type.ITEM, false);
		templateType.setIsEnabled(false);

		// Type
		contentArea.addChild(new Label("Type", screen));
		contentArea.addChild(type = new ComboBox<EntityKey.Type>(screen, EntityKey.Type.values()) {
			@Override
			public void onChange(int selectedIndex, EntityKey.Type value) {
				rebuildName();
			}
		});

		// Template
		contentArea.addChild(new Label("Template", screen));
		contentArea.addChild(template = new ComboBox<AttachableTemplate>(screen) {
			@Override
			public void onChange(int selectedIndex, AttachableTemplate value) {
				reselectAttachmentPoint();
			}
		});
		rebuildTemplates();

		// Name
		contentArea.addChild(new Label("Name", screen));
		name = new AutocompleteTextField<String>(screen, new AutocompleteTextField.AutocompleteSource<String>() {
			public List<AutocompleteItem<String>> getItems(String text) {
				text = text.toLowerCase();
				List<AutocompleteItem<String>> l = new ArrayList<>();
				attachableDef.loadAll(app.getAssetManager());
				for (AttachableTemplate def : attachableDef.values()) {
					String part = def.getKey().getName();
					int idx = part.lastIndexOf('-');
					if (idx != -1) {
						part = part.substring(idx + 1);
					}
					AutocompleteItem<String> e = new AutocompleteItem<String>(part, part);
					if (part.toLowerCase().contains(text) && !l.contains(e)) {
						l.add(e);
					}
				}
				Collections.sort(l);
				return l;
			}
		}) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				rebuildName();
			}

			@Override
			protected void onChange(String value) {
				rebuildName();
			}
		};
		contentArea.addChild(name);

		// Attach point
		// contentArea.addChild(new Label("Attachment point", screen));
		// contentArea.addChild(attachmentPoint = new
		// ComboBox<AttachmentPoint>(screen));
		// for (AttachmentPoint p :
		// Icelib.sort(Arrays.asList(AttachmentPoint.values()))) {
		// attachmentPoint.addListItem(Icelib.toEnglish(p), p, false, false);
		// }
		// attachmentPoint.pack(false);

		contentArea.addChild(new Label("Item", screen));
		contentArea.addChild(item = new TextField(screen), "growx");

		// Open folder
		openFolder = new CheckBox(screen);
		rebuildName();
		openFolder.setToolTipText("Open the folder where the files for this item should " + "be placed.");
		contentArea.addChild(openFolder, "span 2, growx");
		return contentArea;
	}

	protected void rebuildTemplates() {
		template.removeAllListItems();
		for (AttachableTemplate p : Icelib.sort(new ArrayList<AttachableTemplate>(attachableTemplatesConfiguration.values()))) {
			if (p.getKey().getType().equals(templateType.getSelectedListItem().getValue())) {
				template.addListItem(p.getKey().getItem(), p, false, false);
			}
		}
		template.pack(false);
	}

	public void setAttachmentPoint(AttachmentPoint point) {
		// this.attachmentPoint.setSelectedByValue(point, true);
	}

	@Override
	public void createButtons(Element buttons) {
		super.createButtons(buttons);
		btnCancel = new CancelButton(screen, getUID() + ":btnCancel") {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				onButtonCancelPressed(evt, toggled);
			}
		};
		btnCancel.setText("Cancel");
		buttons.addChild(btnCancel);
		form.addFormElement(btnCancel);
		rebuildName();
	}

	protected void reselectAttachmentPoint() {
		// AttachableTemplate value = template.getSelectIndex() > -1 ?
		// template.getSelectedListItem().getValue() : null;
		// if (attachmentPoint != null && value != null) {
		// List<AttachmentPoint> points = value.getAttachPoints();
		// if (points != null) {
		// attachmentPoint.setSelectedByValue(points.get(0), true);
		// }
		// }
	}

	protected void rebuildName() {
		if (openFolder != null && name != null && btnOk != null) {
			MenuItem<AttachableTemplate> selectedListItem = template.getSelectedListItem();
			if (selectedListItem == null) {
				openFolder.setLabelText("No template selected");
				btnOk.setIsEnabled(false);
			} else {
				fullName = Icelib.toEnglish(type.getSelectedListItem().getValue()) + "-" + name.getText();
				// fullName = ((AttachableTemplate)
				// selectedListItem.getValue()).getKey().getItemName() + "-" +
				// name.getText();
				openFolder.setLabelText(String.format("Open folder '%s'", fullName));
				btnOk.setIsEnabled(name.getText().length() > 0);
			}
		}
	}
}
