package org.icedesign;

import java.util.Iterator;
import java.util.prefs.Preferences;

import org.icelib.AttachableTemplate;
import org.icelib.AttachmentRibbon;
import org.icelib.Color;
import org.icelib.ColourPalette;
import org.icelib.Icelib;
import org.icelib.RGB;
import org.icescene.ServiceRef;
import org.icescene.configuration.ColourPalettes;
import org.iceui.IceUI;
import org.iceui.controls.ElementStyle;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.Spinner;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.StyledContainer;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.chooser.ColorFieldControl;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

public class AttachableDefEditor extends PersistentWindow {
	private Table particlesTable;
	private Table attachmentPointsTable;

	private Table colourTable;
	private AttachableTemplate value;
	private CheckBox ribbon;
	private CheckBox illuminated;
	private CheckBox animated;
	private CheckBox particle;
	private Spinner<Float> width;
	private Spinner<Float> offset;

	@ServiceRef
	private static ColourPalettes colourPalettes;

	public AttachableDefEditor(BaseScreen screen, Preferences pref) {
		super(screen, DesignConfig.COLOUR_MAP_EDITOR, 8, VAlign.Bottom, Align.Left, null, true,
				SaveType.POSITION_AND_SIZE, pref);

		setResizable(true);

		content.setLayoutManager(
				new MigLayout(screen, "wrap 2", "[fill, grow][]", "[][][][][][][][:104:][][:104:][][:104:][]"));

		// Illuminated
		illuminated = new CheckBox(screen);
		illuminated.setText("Illuminated");

		// Animated
		animated = new CheckBox(screen);
		animated.setText("Animated");

		// Particle
		particle = new CheckBox(screen);
		particle.setText("Particle");

		// Ribbon
		width = new Spinner<Float>(screen, Orientation.HORIZONTAL, true);
		width.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		offset = new Spinner<Float>(screen, Orientation.HORIZONTAL, true);
		offset.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		ribbon = new CheckBox(screen);
		ribbon.onChange(evt -> {
			offset.setEnabled(evt.getNewValue());
			width.setEnabled(evt.getNewValue());
		});
		ribbon.setText("Ribbon");

		// Colours
		colourTable = new Table(screen);
		colourTable.addColumn("Palette");
		colourTable.addColumn("Colour");

		// Particles
		particlesTable = new Table(screen);
		particlesTable.addColumn("Particle");

		// Attachment Points
		attachmentPointsTable = new Table(screen);
		attachmentPointsTable.addColumn("Attachment Point");

		// Add
		PushButton addColour = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		addColour.onMouseReleased(evt -> {
			ColourPalette pal = colourPalettes.get("rainbow");
			TableRow row = new TableRow(screen, colourTable, pal);
			addColorRow(pal, Color.WHITE, row);
		});
		addColour.setText("Add");

		// Remove
		PushButton removeColour = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		removeColour.onMouseReleased(evt -> {
			TableRow row = colourTable.getSelectedRow();
			if (row != null) {
				colourTable.removeRow(row);
			}
		});
		removeColour.setText("Remove");

		// Save
		PushButton save = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		save.onMouseReleased(evt -> {
			value.setAnimated(animated.isChecked());
			value.setIlluminated(illuminated.isChecked());
			value.setParticle(particle.isChecked());
			value.getColors().clear();
			if (ribbon.isChecked()) {
				AttachmentRibbon ribbon = new AttachmentRibbon();
				ribbon.setOffset((Float) offset.getSpinnerModel().getCurrentValue());
				ribbon.setWidth((Float) width.getSpinnerModel().getCurrentValue());
				value.setRibbon(ribbon);
			}
			// File extF = assets.getExternalAssetFile(colorMapPath);
			// try {
			// OutputStream out = new FileOutputStream(extF);
			// try {
			// editingColorMap.write(out, false);
			// } finally {
			// ColorMapConfiguration.resetCache();
			// out.close();
			// }
			hide();
			// } catch (IOException ioe) {
			// LOG.log(Level.SEVERE, "Failed to write new colormap.",
			// ioe);
			// }});
		});
		save.setText("Save");

		// Colour Actions
		StyledContainer colourActions = new StyledContainer(screen);
		colourActions.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow]", "[][]"));
		colourActions.addElement(addColour);
		colourActions.addElement(removeColour);

		// Actions
		StyledContainer actions = new StyledContainer(screen);
		actions.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
		actions.addElement(save);

		// This
		content.addElement(illuminated, "span 2");
		content.addElement(animated, "span 2");
		content.addElement(particle, "span 2");
		content.addElement(ribbon, "span 2");
		Label label1 = new Label(screen);
		label1.setText("Width");
		width.setLabel(label1);
		content.addElement(label1, "gapleft 32");
		content.addElement(width);
		label1 = new Label(screen);
		label1.setText("Offset");
		offset.setLabel(label1);
		content.addElement(label1, "gapleft 32");
		content.addElement(offset);
		label1 = new Label(screen);
		label1.setText("Colours");
		ElementStyle.medium(label1);
		content.addElement(label1, "span 2");
		content.addElement(colourTable, "growy");
		content.addElement(colourActions);
		label1 = new Label(screen);
		label1.setText("Particles");
		ElementStyle.medium(label1);
		content.addElement(label1, "span 2");
		content.addElement(particlesTable, "growy, span 2");
		label1 = new Label(screen);
		label1.setText("Attachment Points");
		ElementStyle.medium(label1);
		content.addElement(label1, "span 2");
		content.addElement(attachmentPointsTable, "growy, span 2");
		content.addElement(actions, "span 2, ay 0%");

		// Add to screen
		screen.showElement(this);
	}

	private void addColorRow(final ColourPalette pal, RGB col, TableRow row) {
		TableCell nameCell = new TableCell(screen, null, col);
		ComboBox<String> palette = new ComboBox<>(screen);
		for (String p : new String[] { "rainbow", "metal", "wood", "accent" }) {
			palette.addListItem(Icelib.toEnglish(p), p);
		}
		palette.setSelectedByValue(pal.getKey());
		nameCell.setVAlign(BitmapFont.VAlign.Center);
		nameCell.setHAlign(BitmapFont.Align.Center);
		nameCell.addElement(palette);
		row.addElement(nameCell);

		TableCell rgbCell = new TableCell(screen, null, col);
		ColorFieldControl rgb = new ColorFieldControl(screen, IceUI.toRGBA(col));
		rgbCell.setVAlign(BitmapFont.VAlign.Center);
		rgbCell.setHAlign(BitmapFont.Align.Center);
		rgbCell.addElement(rgb);
		row.addElement(rgbCell);
		colourTable.addRow(row);
	}

	public void setValue(AttachableTemplate value) {
		setWindowTitle(String.format("Definition - %s", value.getKey().getName()));
		this.value = value;

		illuminated.setChecked(value.isIlluminated());
		particle.setChecked(value.isParticle());
		animated.setChecked(value.isAnimated());
		ribbon.setChecked(value.getRibbon() != null);
		if (value.getRibbon() != null) {
			width.getSpinnerModel().setValueFromString(String.valueOf(value.getRibbon().getWidth()));
			offset.getSpinnerModel().setValueFromString(String.valueOf(value.getRibbon().getOffset()));
		}

		colourTable.removeAllRows();

		Iterator<ColourPalette> p = value.getPalette().iterator();
		for (RGB col : value.getColors()) {
			ColourPalette pal = p.next();
			TableRow row = new TableRow(screen, colourTable, p);
			addColorRow(pal, col, row);
		}

		// Initial state
		offset.setEnabled(ribbon.isChecked());
		width.setEnabled(ribbon.isChecked());

	}
}
