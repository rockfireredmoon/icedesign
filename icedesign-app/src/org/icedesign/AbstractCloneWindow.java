package org.icedesign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.iceui.controls.ElementStyle;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.LineWrapMode;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.core.BaseScreen;
import icetone.core.layout.ScreenLayoutConstraints;
import icetone.extras.windows.InputBox;

public abstract class AbstractCloneWindow<T> extends InputBox {
	private static final Logger LOG = Logger.getLogger(AbstractCloneWindow.class.getName());

	protected T def;

	public AbstractCloneWindow(BaseScreen screen, T def) {
		super(screen, Vector2f.ZERO, false);
		setStyleClass("large");
		this.def = def;
		ElementStyle.normal(getDragBar(), true, false);
		getDragBar().setTextWrap(LineWrapMode.Word);
		setDestroyOnHide(true);
		ElementStyle.warningColor(getDragBar());
		sizeToContent();
		setWidth(300);
		setResizable(false);
		setMovable(false);
		setModal(true);
		screen.showElement(this, ScreenLayoutConstraints.center);
	}

	@Override
	public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
		hide();
	}

	@Override
	public void onButtonOkPressed(MouseButtonEvent evt, String text, boolean toggled) {
		hide();
		try {
			doCloneItem(def, text.replace(" ", "").replace("/", ""));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Failed to clone.", ex);
		}
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

	protected abstract void doCloneItem(final T def, String newName) throws IOException;
}
