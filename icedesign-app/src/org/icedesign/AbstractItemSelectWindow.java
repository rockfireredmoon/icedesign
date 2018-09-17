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
import org.iceui.controls.ElementStyle;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;

import icetone.controls.buttons.PushButton;
import icetone.controls.extras.Separator;
import icetone.controls.lists.ComboBox;
import icetone.controls.table.Table;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.Size;
import icetone.core.Element;
import icetone.core.ToolKit;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.ButtonWindow;

public abstract class AbstractItemSelectWindow extends ButtonWindow<Element> {

	private static final Logger LOG = Logger.getLogger(AbstractItemSelectWindow.class.getName());
	private PushButton btnCancel;
	private TextField nameFilter;
	private ComboBox<AttachmentPoint> attachmentPointFilter;
	private Preview preview;
	protected Table list;
	// @ServiceRef
	// protected static AttachableTemplates attachableTemplatesConfiguration;
	@ServiceRef
	protected static AttachableDef attachableDef;

	public AbstractItemSelectWindow(BaseScreen screen, String title, String okText) {
		super(screen, true);
		setDestroyOnHide(true);
		ElementStyle.warningColor(getDragBar());
		setWindowTitle(title);
		setButtonOkText(okText);
		setResizable(true);
		setMovable(false);
		sizeToContent();
		setModal(true);
		screen.showElement(this, ScreenLayoutConstraints.center);
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
		hide();
	}

	protected void doOnSelect(List<AttachableTemplate> itemDefinition) {
	}

	@Override
	protected void createButtons(BaseElement buttons) {
		super.createButtons(buttons);
		btnCancel = new PushButton(screen, "Cancel") {
			{
				setStyleClass("cancel");
			}
		};
		btnCancel.onMouseReleased(evt -> hide());
		btnCancel.setText("Cancel");
		buttons.addElement(btnCancel);
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
		container.setLayoutManager(
				new MigLayout(screen, "ins 0, wrap 2", "[shrink 0][grow]", "[shrink 0][shrink 0][fill, grow]"));
		// Name Filter
		container.addElement(new Label("Name Filter:", screen));
		nameFilter = new TextField(screen);
		nameFilter.onKeyboardReleased(evt -> refilter());
		container.addElement(nameFilter, "growx");

		// Attachment Point Filter
		container.addElement(new Label("Attachment Point Filter:", screen));
		attachmentPointFilter = new ComboBox<AttachmentPoint>(screen);
		attachmentPointFilter.addListItem("All", null);
		attachmentPointFilter.setSelectedIndex(0);
		for (AttachmentPoint ap : AttachmentPoint.values()) {
			attachmentPointFilter.addListItem(Icelib.toEnglish(ap, true), ap);
		}
		container.addElement(attachmentPointFilter, "growx");
		attachmentPointFilter.onChange(evt -> refilter());

		// Attachment List
		list = new Table(screen);
		list.onChanged(evt -> loadPreview());
		list.setHeadersVisible(false);
		list.addColumn("Attachment");
		list.setMaxDimensions(new Size(600, 300));
		refilter();
		container.addElement(list, "growx, growy, span 2");

		preview = new Preview(screen, new Size(200, 300));

		Element outer = new Element(screen);
		outer.setLayoutManager(new MigLayout(screen, "ins 0, wrap 3", "[fill, grow][][]", "[fill, grow]"));
		outer.addElement(container, "growy");
		outer.addElement(new Separator(screen, Orientation.VERTICAL));
		outer.addElement(preview, "growy");

		return outer;
	}

	private void loadPreview() {
		AttachableTemplate def = (AttachableTemplate) list.getSelectedRow().getValue();
		AttachmentItem ai = new AttachmentItem(def.getKey());
		final AttachmentItemEntity attSpatial = new AttachmentItemEntity(EntityContext.create(screen.getApplication()),
				ai);
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
		list.invalidate();
		list.removeAllRows();

		attachableDef.loadAll(ToolKit.get().getApplication().getAssetManager());

		String filterText = nameFilter.getText().trim();
		if (filterText.equals("")) {
			for (AttachableTemplate s : attachableDef.values()) {
				if (match(s)) {
					list.addListRow(s.getKey().getName(), s);
				}
			}
		} else if (filterText.startsWith("~")) {
			filterText = filterText.substring(1);
			for (AttachableTemplate s : attachableDef.values()) {
				if (filterText.matches(s.getKey().getName()) && match(s)) {
					list.addListRow(s.getKey().getName(), s);
				}
			}
		} else {
			String lowerFilterText = filterText.toLowerCase();
			for (AttachableTemplate s : attachableDef.values()) {
				if (s.getKey().getName().toLowerCase().contains(lowerFilterText) && match(s)) {
					list.addListRow(s.getKey().getName(), s);
				}
			}
		}
		list.validate();
	}

}
