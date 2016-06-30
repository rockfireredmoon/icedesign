package org.icedesign;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icelib.AttachableTemplate;
import org.icelib.ClothingTemplate;
import org.icelib.ColourPalette;
import org.icelib.EntityKey;
import org.icelib.Icelib;
import org.icelib.RGB;
import org.icelib.Region;
import org.icescene.IcesceneApp;
import org.icescene.ServiceRef;
import org.icescene.assets.Assets;
import org.icescene.configuration.ClothingDef;
import org.icescene.configuration.ClothingTemplates;
import org.icescene.configuration.attachments.AttachableDef;
import org.icescene.configuration.attachments.AttachableTemplates;

import com.jme3.app.Application;

public class ItemWriter {

	@ServiceRef
	protected static AttachableTemplates attachableTemplatesConfiguration;
	@ServiceRef
	protected static AttachableDef attachableDef;

	@ServiceRef
	protected static ClothingTemplates clothingTemplates;
	@ServiceRef
	protected static ClothingDef clothingDef;

	private EntityKey newKey;
	private File assetsDir;
	private File itemDir;
	private List<AttachableTemplate> newAttachables = new ArrayList<>();
	private List<ClothingTemplate> newClothing = new ArrayList<>();

	public ItemWriter(Application app, EntityKey newKey) {
		this.newKey = newKey;

		Assets assets = ((IcesceneApp) app).getAssets();
		assetsDir = assets.getExternalAssetsFolder();

		// The folder for the item type
		File typeDir = new File(assetsDir, Icelib.toEnglish(newKey.getType()));

		// The item folder
		itemDir = new File(typeDir, String.format("%s-%s", Icelib.toEnglish(newKey.getType()), newKey.getItem()));
		if (!itemDir.exists()) {
			itemDir.mkdirs();
		}
	}

	public void addNewAttachable(AttachableTemplate newTemplate) {
		newAttachables.add(newTemplate);
	}

	public void addNewClothing(ClothingTemplate clothingTemplate) {
		newClothing.add(clothingTemplate);
	}

	public File getAssetsDir() {
		return assetsDir;
	}

	public File getItemDir() {
		return itemDir;
	}

	public void write() throws IOException {

		List<AttachableTemplate> currentTemplates = new ArrayList<>();
		List<ClothingTemplate> currentClothingTemplates = new ArrayList<>();

		// The definition file (contains multiple items)
		File scriptFile = new File(itemDir, String.format("%s-%s.js", Icelib.toEnglish(newKey.getType()), newKey.getItem()));

		// Get all the attachable items that exist currently in the script
		// for
		// this
		// type/name
		for (AttachableTemplate templ : attachableDef.values()) {
			if (templ.getKey().getType().equals(newKey.getType()) && templ.getKey().getItem().equals(newKey.getItem())) {
				currentTemplates.add(templ);
			}
		}

		// Get all the clothing items that exist currently in the script for
		// this
		// type/name
		for (ClothingTemplate templ : clothingDef.values()) {
			if (templ.getKey().getType().equals(newKey.getType()) && templ.getKey().getItem().equals(newKey.getItem())) {
				currentClothingTemplates.add(templ);
			}
		}

		//
		currentTemplates.addAll(newAttachables);
		currentClothingTemplates.addAll(newClothing);

		// Write the templates to the script file
		PrintWriter pw = new PrintWriter(new FileWriter(scriptFile));
		try {
			pw.println("__ColourPalettes = __ColourPalettes;");

			if (!currentTemplates.isEmpty()) {
				pw.println("__AttachableTemplates = __AttachableTemplates;");
				pw.println("__AttachableDef = __AttachableDef;");
			}
			if (!currentClothingTemplates.isEmpty()) {
				pw.println("__ClothingDef = __ClothingDef;");
				pw.println("__ClothingTemplates = __ClothingTemplates;");
			}

			pw.println("with (JavaImporter(org.icelib.beans)) {");

			// Write the clothing templates
			for (ClothingTemplate cdef : currentClothingTemplates) {
				ClothingTemplate ctempl = cdef.getDelegate();
				if (ctempl == null) {
					pw.println(String.format("\t__ClothingDef[\"%s-%s\"] = {", Icelib.toEnglish(cdef.getKey().getType()),
							newKey.getItem()));
					pw.println("\t\tpalette : [ " + getPaletteString(cdef.getPalette()) + " ],");
					pw.println("\t\tcolors : [ " + getColorString(cdef.getColors()) + " ],");
					pw.println("\t\tregions : { \n" + getRegionsString(cdef.getRegions()) + " }");
					pw.println("\r};");
				} else {

					pw.println(String.format("\t__ClothingDef[\"%s-%s\"] = ObjectMapping.delegate(__ClothingTemplates[\"%s\"], {",
							Icelib.toEnglish(cdef.getKey().getType()), newKey.getItem(), ctempl.getKey().getTemplate()));

					if (!Icelib.listEqualsNoOrder(cdef.getPalette(), ctempl.getPalette())) {
						pw.println("\t\tpalette : [ " + getPaletteString(cdef.getPalette()) + " ],");
					}
					if (!Icelib.listEqualsNoOrder(cdef.getColors(), ctempl.getColors())) {
						pw.println("\t\tcolors : [ " + getColorString(cdef.getColors()) + " ],");
					}
					if (!Icelib.mapEqualsNoOrder(cdef.getRegions(), ctempl.getRegions())) {
						pw.println("\t\tregions : { \n" + getRegionsString(cdef.getRegions()) + "\n\t\t}");
					}
					pw.println("\t});");
				}

				// Make the template publically available
				clothingDef.put(cdef.getKey(), cdef);
			}

			// Write the attachment templates
			for (AttachableTemplate def : currentTemplates) {

				AttachableTemplate templ = def.getDelegate();

				pw.println(String.format("\t__AttachableDef[\"%s-%s-%s\"] = ObjectMapping.delegate(",
						Icelib.toEnglish(def.getKey().getType()), newKey.getItem(), def.getKey().getTemplate()));
				pw.println(String.format("\t\t__AttachableTemplates[\"%s.%s\"], {", Icelib.toEnglish(templ.getKey().getType()),
						templ.getKey().getItem()));

				// Palette
				List<ColourPalette> palette = def.getPalette();
				pw.println("\t\t\tpalette : [ " + getPaletteString(palette) + " ],");

				// Colours
				pw.print("\t\t\tcolors : [ " + getColorString(def.getColors()) + " ]");
				if (def.isAnimated()) {
					pw.print(",\n\t\t\tanimated : true");
				}
				if (def.isIlluminated()) {
					pw.print(",\n\t\t\tilluminated : true");
				}
				if (def.isParticle())
					pw.print(",\n\t\t\tilluminated : true");
				if (!def.getParticles().isEmpty()) {
					StringBuilder bui = new StringBuilder();
					for (String p : def.getParticles()) {
						if (bui.length() > 0)
							bui.append(",\n");
						bui.append("\t\t\t\t\"");
						bui.append(p);
						bui.append("\"");
					}
					pw.print(",\n\t\t\tparticles : [ " + bui.toString() + "]");
				}
				pw.println();
				pw.println("\t});");

				// Make the template publicly available
				attachableDef.put(def.getKey(), def);
			}

			pw.println("};");
			pw.flush();
		} finally {
			pw.close();
		}
	}

	protected String getRegionsString(Map<Region, String> regions) {
		StringBuilder bui = new StringBuilder();
		for (Map.Entry<Region, String> en : regions.entrySet()) {
			if (bui.length() > 0)
				bui.append(",\n");
			bui.append(String.format("\t\t\t%s : \"%s\"", en.getKey().name().toLowerCase(), en.getValue()));
		}
		return bui.toString();
	}

	protected String getColorString(List<RGB> colors) {
		StringBuilder bui = new StringBuilder();
		for (RGB r : colors) {
			if (bui.length() > 0) {
				bui.append(", ");
			}
			bui.append("\"");
			bui.append(Icelib.toHexNumber(r));
			bui.append("\"");
		}
		return bui.toString();
	}

	protected String getPaletteString(List<ColourPalette> palette) {
		StringBuilder bui = new StringBuilder();
		for (ColourPalette p : palette) {
			if (bui.length() > 0) {
				bui.append(", ");
			}
			bui.append("__ColourPalettes.");
			bui.append(p.getKey());
		}
		return bui.toString();
	}
}
