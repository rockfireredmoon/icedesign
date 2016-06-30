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
import org.iceui.HPosition;
import org.iceui.IceUI;
import org.iceui.VPosition;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;
import org.iceui.controls.color.ColorFieldControl;

import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.Spinner;
import icetone.controls.lists.Table;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.ElementManager;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;

public class AttachableDefEditor extends FancyPersistentWindow {
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

	public AttachableDefEditor(ElementManager screen, Preferences pref) {
		super(screen, DesignConfig.COLOUR_MAP_EDITOR, 8, VPosition.BOTTOM, HPosition.CENTER, LUtil.LAYOUT_SIZE,
				FancyWindow.Size.SMALL, true, SaveType.POSITION_AND_SIZE, pref);

		setIsResizable(true);

		content.setLayoutManager(new MigLayout(screen, "wrap 2", "[fill, grow][]", "[][][][][][][][:104:][][:104:][][:104:][]"));

		// Illuminated
		illuminated = new CheckBox(screen);
		illuminated.setLabelText("Illuminated");

		// Animated
		animated = new CheckBox(screen);
		animated.setLabelText("Animated");

		// Particle
		particle = new CheckBox(screen);
		particle.setLabelText("Particle");

		// Ribbon
		width = new Spinner<Float>(screen, Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
			}
		};
		width.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		offset = new Spinner<Float>(screen, Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
			}
		};
		offset.setSpinnerModel(new FloatRangeSpinnerModel(0.01f, 1000f, 0.1f, 1f));
		ribbon = new CheckBox(screen) {

			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				super.onButtonMouseLeftUp(evt, toggled);
				offset.setIsEnabled(getIsChecked());
				width.setIsEnabled(getIsChecked());
			}

		};
		ribbon.setLabelText("Ribbon");

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
		FancyButton addColour = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				ColourPalette pal = colourPalettes.get("rainbow");
				Table.TableRow row = new Table.TableRow(screen, colourTable, pal);
				addColorRow(pal, Color.WHITE, row);
			}
		};
		addColour.setText("Add");

		// Remove
		FancyButton removeColour = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				Table.TableRow row = colourTable.getSelectedRow();
				if (row != null) {
					colourTable.removeRow(row);
				}
			}
		};
		removeColour.setText("Remove");

		// Save
		FancyButton save = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				value.setAnimated(animated.getIsChecked());
				value.setIlluminated(illuminated.getIsChecked());
				value.setParticle(particle.getIsChecked());
				value.getColors().clear();
				if (ribbon.getIsChecked()) {
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
				hideWithEffect();
				// } catch (IOException ioe) {
				// LOG.log(Level.SEVERE, "Failed to write new colormap.",
				// ioe);
				// }
			}
		};
		save.setText("Save");

		// Colour Actions
		Container colourActions = new Container(screen);
		colourActions.setLayoutManager(new MigLayout(screen, "wrap 1", "[grow]", "[][]"));
		colourActions.addChild(addColour);
		colourActions.addChild(removeColour);

		// Actions
		Container actions = new Container(screen);
		actions.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));
		actions.addChild(save);

		// This
		content.addChild(illuminated, "span 2");
		content.addChild(animated, "span 2");
		content.addChild(particle, "span 2");
		content.addChild(ribbon, "span 2");
		Label label1 = new Label(screen);
		label1.setText("Width");
		width.setLabel(label1);
		content.addChild(label1, "gapleft 32");
		content.addChild(width);
		label1 = new Label(screen);
		label1.setText("Offset");
		offset.setLabel(label1);
		content.addChild(label1, "gapleft 32");
		content.addChild(offset);
		label1 = new Label(screen);
		label1.setText("Colours");
		ElementStyle.medium(screen, label1);
		content.addChild(label1, "span 2");
		content.addChild(colourTable, "growy");
		content.addChild(colourActions);
		label1 = new Label(screen);
		label1.setText("Particles");
		ElementStyle.medium(screen, label1);
		content.addChild(label1, "span 2");
		content.addChild(particlesTable, "growy, span 2");
		label1 = new Label(screen);
		label1.setText("Attachment Points");
		ElementStyle.medium(screen, label1);
		content.addChild(label1, "span 2");
		content.addChild(attachmentPointsTable, "growy, span 2");
		content.addChild(actions, "span 2, ay 0%");

		// Add to screen
		screen.addElement(this, null, true);

		// Set values
		showWithEffect();
	}

	private void addColorRow(final ColourPalette pal, RGB col, Table.TableRow row) {
		Table.TableCell nameCell = new Table.TableCell(screen, null, col);
		ComboBox<String> palette = new ComboBox<>(screen);
		for (String p : new String[] { "rainbow", "metal", "wood", "accent" }) {
			palette.addListItem(Icelib.toEnglish(p), p);
		}
		palette.setSelectedByValue(pal.getKey(), false);
		nameCell.setVAlign(BitmapFont.VAlign.Center);
		nameCell.setHAlign(BitmapFont.Align.Center);
		nameCell.addChild(palette);
		row.addChild(nameCell);

		Table.TableCell rgbCell = new Table.TableCell(screen, null, col);
		ColorFieldControl rgb = new ColorFieldControl(screen, IceUI.toRGBA(col));
		rgbCell.setVAlign(BitmapFont.VAlign.Center);
		rgbCell.setHAlign(BitmapFont.Align.Center);
		rgbCell.addChild(rgb);
		row.addChild(rgbCell);
		colourTable.addRow(row);
	}

	public void setValue(AttachableTemplate value) {
		setWindowTitle(String.format("Definition - %s", value.getKey().getName()));
		this.value = value;

		illuminated.setIsChecked(value.isIlluminated());
		particle.setIsChecked(value.isParticle());
		animated.setIsChecked(value.isAnimated());
		ribbon.setIsChecked(value.getRibbon() != null);
		if (value.getRibbon() != null) {
			width.getSpinnerModel().setValueFromString(String.valueOf(value.getRibbon().getWidth()));
			offset.getSpinnerModel().setValueFromString(String.valueOf(value.getRibbon().getOffset()));
		}

		colourTable. removeAllRows();

		Iterator<ColourPalette> p = value.getPalette().iterator();
		for (RGB col : value.getColors()) {
			ColourPalette pal = p.next();
			Table.TableRow row = new Table.TableRow(screen, colourTable, p);
			addColorRow(pal, col, row);
		}

		// Initial state
		offset.setIsEnabled(ribbon.getIsChecked());
		width.setIsEnabled(ribbon.getIsChecked());

	}
}
