package org.icedesign;

import java.util.ArrayList;
import java.util.List;

import org.icelib.AbstractCreature;
import org.icelib.Appearance;
import org.icelib.AttachableTemplate;
import org.icelib.AttachmentItem;

import icetone.controls.lists.Table.SelectionMode;
import icetone.core.Element;
import icetone.core.ElementManager;

public class AddAttachmentWindow extends AbstractItemSelectWindow {

	private final AbstractCreature creature;

	public AddAttachmentWindow(ElementManager screen, AbstractCreature creature) {
		super(screen, "Select Attachment", "Select");
		this.creature = creature;
	}

	protected Element createContent() {
		Element el = super.createContent();
		list.setEnableKeyboardNavigation(true);
		list.setSelectionMode(SelectionMode.MULTIPLE_ROWS);
		return el;
	}

	@Override
	public void doOnSelect(List<AttachableTemplate> selection) {
		// Update the creature appearance
		Appearance appearance = creature.getAppearance();
		List<AttachmentItem> i = new ArrayList<AttachmentItem>();
		for (AttachableTemplate t : selection) {
			final AttachmentItem attachmentItem = new AttachmentItem(t.getKey(), null, null, null);
			appearance.addAttachment(attachmentItem);
			creature.setAppearance(appearance);
			i.add(attachmentItem);
		}
		onAdd(i);
	}

	protected void onAdd(List<AttachmentItem> attachmentItem) {
	}

}
