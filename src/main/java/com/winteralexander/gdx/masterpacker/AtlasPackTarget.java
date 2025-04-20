package com.winteralexander.gdx.masterpacker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import static com.winteralexander.gdx.masterpacker.Downscalator.downscaleInPlace;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * An atlas target for packing (atlas to be packed with other atlas or textures)
 * <p>
 * Created on 2021-10-16.
 *
 * @author Alexander Winter
 */
public class AtlasPackTarget implements PackTarget {
	private final String path;
	private final TextureType textureType;
	private final String bundleId;
	private final float scale;
	private final boolean noDownscale;

	public AtlasPackTarget(String path,
	                       TextureType textureType,
	                       String bundleId,
	                       float scale,
	                       boolean noDownscale) {
		ensureNotNull(path, "path");
		ensureNotNull(textureType, "textureType");
		this.path = path;
		this.textureType = textureType;
		this.bundleId = bundleId;
		this.scale = scale;
		this.noDownscale = noDownscale;
	}

	@Override
	public String getBundleId() {
		return bundleId;
	}

	@Override
	public void process(AssetBundle bundle,
	                    AssetResolution resolution,
	                    File baseDir,
	                    File flatDir,
	                    File normalDir,
	                    File preshadedDir) throws IOException {
		String regionName = getRegionName(bundle);

		File outDir;

		switch(textureType) {
			case FLAT:
				outDir = flatDir;
				break;
			case NORMAL:
				outDir = normalDir;
				break;
			case PRESHADED:
				outDir = preshadedDir;
				break;
			default:
				throw new IllegalStateException("Invalid texture type " + textureType);
		}

		String png = path.substring(0, path.length() - 6) + ".png";

		File dest = new File(outDir, regionName + ".png");
		Files.copy(new File(baseDir, png).toPath(), dest.toPath(), REPLACE_EXISTING);
		downscaleInPlace(dest, bundle.getBaseScale() *
				scale * (noDownscale ? 1f : resolution.getScale()));
	}

	private String getRegionName(AssetBundle bundle) {
		String relPath = path;
		int curBest = 0;

		for(String basePath : bundle.getBasePaths())
			if(path.startsWith(basePath) && basePath.length() > curBest) {
				relPath = path.replaceFirst(Pattern.quote(basePath), "");
				curBest = basePath.length();
			}

		while(relPath.startsWith("/"))
			relPath = relPath.substring(1);

		return relPath.replace('/', '_')
				.replace("_flat.atlas", "")
				.replace("_normal.atlas", "")
				.replace("_preshaded.atlas", "")
				.replace(".atlas", "");
	}

	@Override
	public void postProcessAtlas(AssetBundle bundle,
	                             File baseDir,
	                             List<String> atlas,
	                             TextureType atlasType) throws IOException {
		if(atlasType != textureType)
			return;

		String regionName = getRegionName(bundle);

		List<String> subAtlas = Files.readAllLines(new File(baseDir, path).toPath());

		subAtlas = subAtlas.subList(5, subAtlas.size());

		insertSubAtlas(atlas, subAtlas, regionName);
	}

	private static void insertSubAtlas(List<String> atlas,
	                                   List<String> subAtlas,
	                                   String regionName) {
		for(int i = 0; i < atlas.size(); i++) {
			String line = atlas.get(i);
			int x = 0, y = 0;

			if(line.equals(regionName)) {
				do {
					if(atlas.get(i).startsWith("  xy:")) {
						String[] coords = atlas.get(i).split("xy:")[1].trim().split(",");
						x = Integer.parseInt(coords[0].trim());
						y = Integer.parseInt(coords[1].trim());
					}
					atlas.remove(i);
				} while(atlas.size() > i && atlas.get(i).startsWith("  "));

				for(int j = subAtlas.size(); j --> 0; ) {
					String subAtlasLine = subAtlas.get(j);

					if(!subAtlasLine.startsWith("  ")) {
						subAtlasLine = regionName + "_" + subAtlasLine;
					} else if(subAtlasLine.startsWith("  xy:")) {
						String[] coords = subAtlasLine.split("xy:")[1].trim().split(",");
						int subX = Integer.parseInt(coords[0].trim());
						int subY = Integer.parseInt(coords[1].trim());

						subAtlasLine = "  xy: " + (x + subX) + ", " + (y + subY);
					}

					atlas.add(i, subAtlasLine);
				}
				return;
			}
		}
	}

	@Override
	public boolean matches(AssetBundle bundle, TextureType textureType, File baseDir, String name) {
		return false;
	}

	@Override
	public boolean stripWhitespace() {
		return false;
	}

	@Override
	public int getExtendLeft() {
		return 0;
	}

	@Override
	public int getExtendRight() {
		return 0;
	}

	@Override
	public int getExtendTop() {
		return 0;
	}

	@Override
	public int getExtendBottom() {
		return 0;
	}

	@Override
	public long lastModified(File baseDir) {
		return Math.max(new File(baseDir, path).lastModified(),
				new File(baseDir, path.substring(0, path.length() - 6) + ".png").lastModified());
	}

	@Override
	public String toString() {
		return "AtlasPackTarget " + path;
	}

	public TextureType getTextureType() {
		return textureType;
	}
}
