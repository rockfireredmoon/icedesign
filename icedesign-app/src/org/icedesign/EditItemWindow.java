package org.icedesign;

import java.io.File;
import java.util.prefs.Preferences;

import org.icelib.AttachableTemplate;
import org.icelib.Icelib;
import org.icescene.assets.Assets;

import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;

import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.layout.FillLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

public class EditItemWindow extends PersistentWindow {

	private ItemEditorPanel itemTextureEditor;

	public EditItemWindow(BaseScreen screen, Assets assets, Preferences prefs) {
		super(screen, DesignConfig.ATTACHMENT_FILES_EDITOR, 8, VAlign.Center, Align.Right, new Size(320, 300), true,
				SaveType.POSITION_AND_SIZE, prefs);

		setResizable(true);
		final BaseElement windowContent = getContentArea();
		windowContent.setLayoutManager(new FillLayout());
		itemTextureEditor = new ItemEditorPanel(assets, screen, prefs) {
			@Override
			protected void textureFileUpdated(File file) {
				onTextureFileUpdated();
			}
		};
		windowContent.addElement(itemTextureEditor);
	}

	protected void onTextureFileUpdated() {
	}

	public void setValue(AttachableTemplate def) {
		setWindowTitle(String.format("Item - %s", Icelib.getBaseFilename(def.getKey().getName())));
		itemTextureEditor.setValue(def);
		if (!isVisible()) {
			show();
		}
	}
}
