package org.icedesign;

import java.util.Collection;

import org.icelib.AttachmentItem;
import org.icescene.animation.AnimationSequence;

import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.PushButton;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.extras.windows.ButtonWindow;

public abstract class PoseEditWindow extends ButtonWindow<Element> {

	private PushButton btnCancel;
	private PoseEditPanel editPanel;

	public PoseEditWindow(BaseScreen screen) {
		super(screen, true);
		setDestroyOnHide(false);
		setWindowTitle("Edit Pose");
		setButtonOkText("Save");
		setResizable(false);
		setMovable(true);
		screen.showElement(this, ScreenLayoutConstraints.center);
	}

	protected abstract void onSave(AnimationSequence seq);

	@Override
	public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
		onSave(editPanel.getSequence());
		hide();
	}

	protected void onAdd(AttachmentItem attachmentItem) {
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

	@Override
	protected Element createContent() {
		editPanel = new PoseEditPanel(screen);
		return editPanel;
	}

	public void setAnimations(Collection<String> animations) {
		editPanel.setAnimations(animations);
	}

	public void setSequence(AnimationSequence sequence) {
		editPanel.setSequence(sequence);
	}
}
