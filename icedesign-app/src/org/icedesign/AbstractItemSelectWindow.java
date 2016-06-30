package org.icedesign;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.AttachableTemplate;
import org.icelib.AttachmentItem;
import org.icelib.AttachmentPoint;
import org.icelib.Icelib;
import org.icescene.ServiceRef;
import org.icescene.configuration.attachments.AttachableDef;
import org.icescene.controls.Rotator;
import org.icescene.entities.AttachmentItemEntity;
import org.icescene.entities.EntityContext;
import org.iceui.controls.CancelButton;
import org.iceui.controls.FancyButtonWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;
import org.iceui.controls.XSeparator;

import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import icetone.controls.lists.ComboBox;
import icetone.controls.lists.Table;
import icetone.controls.lists.Table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;

public abstract class AbstractItemSelectWindow extends FancyButtonWindow<Element> {

	private static final Logger LOG = Logger.getLogger(AbstractItemSelectWindow.class.getName());
	private CancelButton btnCancel;
	private TextField nameFilter;
	private ComboBox<AttachmentPoint> attachmentPointFilter;
	private Preview preview;
	protected Table list;
	// @ServiceRef
	// protected static AttachableTemplates attachableTemplatesConfiguration;
	@ServiceRef
	protected static AttachableDef attachableDef;

	public AbstractItemSelectWindow(ElementManager screen, String title, String okText) {
		super(screen, new Vector2f(15, 15), FancyWindow.Size.SMALL, true);

		setDestroyOnHide(true);
		getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
		setWindowTitle(title);
		setButtonOkText(okText);
		setIsResizable(true);
		setIsMovable(false);
		sizeToContent();
		UIUtil.center(screen, this);
		screen.addElement(this, null, true);
		showAsModal(false);
	}

	@Override
	public final void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
		if (!list.isAnythingSelected()) {
			return;
		}
		List<AttachableTemplate> l = new ArrayList<AttachableTemplate>();
		for (TableRow r : list.getSelectedRows()) {
			l.add((AttachableTemplate) r.getValue());
		}
		doOnSelect(l);
		hideWindow();
	}

	protected void doOnSelect(List<AttachableTemplate> itemDefinition) {
	}

	@Override
	protected void createButtons(Element buttons) {
		super.createButtons(buttons);
		btnCancel = new CancelButton(screen, getUID() + ":btnCancel") {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				hideWindow();
			}
		};
		btnCancel.setText("Cancel");
		buttons.addChild(btnCancel);
		form.addFormElement(btnCancel);
	}

	protected boolean match(AttachableTemplate def) {
		AttachmentPoint value = attachmentPointFilter.getSelectedListItem().getValue();
		if (value == null) {
			return true;
		} else {
			// AttachableTemplateKey s = def.getKey();
			// AttachableTemplate t = attachableTemplatesConfiguration.get(s);
			return def.getAttachPoints().contains(value);
		}
	}

	@Override
	protected Element createContent() {
		Element container = new Element(screen);
		container.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[shrink 0][grow]", "[shrink 0][shrink 0][fill, grow]"));
		// Name Filter
		container.addChild(new Label("Name Filter:", screen));
		nameFilter = new TextField(screen) {
			@Override
			public void controlKeyPressHook(KeyInputEvent evt, String text) {
				refilter();
			}
		};
		container.addChild(nameFilter, "growx");
		// Attachment Point Filter
		container.addChild(new Label("Attachment Point Filter:", screen));
		attachmentPointFilter = new ComboBox<AttachmentPoint>(screen) {
			@Override
			public void onChange(int selectedIndex, AttachmentPoint value) {
				refilter();
			}
		};
		attachmentPointFilter.addListItem("All", null);
		for (AttachmentPoint ap : AttachmentPoint.values()) {
			attachmentPointFilter.addListItem(Icelib.toEnglish(ap, true), ap);
		}
		container.addChild(attachmentPointFilter, "growx");
		// Attachment List
		list = new Table(screen) {
			@Override
			public void onChange() {
				loadPreview();
			}
		};
		list.setHeadersVisible(false);
		list.addColumn("Attachment");
		list.setMaxDimensions(new Vector2f(600, 300));
		refilter();
		container.addChild(list, "growx, growy, span 2");

		preview = new Preview(screen, LUtil.LAYOUT_SIZE);
		
		Element outer = new Element(screen);
		outer.setLayoutManager(new MigLayout(screen, "ins 0, wrap 3", "[fill, grow][][]", "[fill, grow]"));
		outer.addChild(container, "growy");
		outer.addChild(new XSeparator(screen, Element.Orientation.VERTICAL));
		outer.addChild(preview, "growy");
		
		return outer;
	}

	private void loadPreview() {
		AttachableTemplate def = (AttachableTemplate) list.getSelectedRow().getValue();
		AttachmentItem ai = new AttachmentItem(def.getKey());
		final AttachmentItemEntity attSpatial = new AttachmentItemEntity(EntityContext.create(screen.getApplication()), ai);
		attSpatial.getSpatial().scale(0.5f);
		attSpatial.getSpatial().addControl(new Rotator(2f));
		preview.setPreview(attSpatial.getSpatial());
		preview.getAmbientLight().setColor(ColorRGBA.White.mult(15f));
		new Thread() {
			@Override
			public void run() {
				try {
					attSpatial.load();
					screen.getApplication().enqueue(new Callable<Void>() {
						public Void call() throws Exception {
							attSpatial.loadScene();
							return null;
						}
					});
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Failed to load preview model.", e);
				}
			}
		}.start();
	}

	private void refilter() {
		if (list == null) {
			return;
		}
		list.removeAllRows();

		attachableDef.loadAll(app.getAssetManager());

		String filterText = nameFilter.getText().trim();
		if (filterText.equals("")) {
			for (AttachableTemplate s : attachableDef.values()) {
				if (match(s)) {
					list.addListRow(s.getKey().getName(), s, false);
				}
			}
		} else if (filterText.startsWith("~")) {
			filterText = filterText.substring(1);
			for (AttachableTemplate s : attachableDef.values()) {
				if (filterText.matches(s.getKey().getName()) && match(s)) {
					list.addListRow(s.getKey().getName(), s, false);
				}
			}
		} else {
			String lowerFilterText = filterText.toLowerCase();
			for (AttachableTemplate s : attachableDef.values()) {
				if (s.getKey().getName().toLowerCase().contains(lowerFilterText) && match(s)) {
					list.addListRow(s.getKey().getName(), s, false);
				}
			}
		}
		list.pack();
	}

}
