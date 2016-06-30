package org.icedesign;

import java.util.Collection;
import java.util.logging.Logger;

import org.icelib.AttachmentItem;
import org.icescene.animation.AnimationSequence;
import org.iceui.controls.CancelButton;
import org.iceui.controls.FancyButtonWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.UIUtil;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.core.Element;
import icetone.core.ElementManager;

public abstract class PoseEditWindow extends FancyButtonWindow<Element> {

    private static final Logger LOG = Logger.getLogger(PoseEditWindow.class.getName());
    private CancelButton btnCancel;
    private PoseEditPanel editPanel;

    public PoseEditWindow(ElementManager screen) {
        super(screen, new Vector2f(15, 15), FancyWindow.Size.SMALL, true);
        setDestroyOnHide(false);
        setWindowTitle("Edit Pose");
        setButtonOkText("Save");
        pack(false);
        setIsResizable(false);
        setIsMovable(true);
        UIUtil.center(screen, this);
        screen.addElement(this);
    }

    protected abstract void onSave(AnimationSequence seq);

    @Override
    public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
        onSave(editPanel.getSequence());
        hideWithEffect();
    }

    protected void onAdd(AttachmentItem attachmentItem) {
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
