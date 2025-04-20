package com.winteralexander.gdx.masterpacker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static com.winteralexander.gdx.masterpacker.Downscalator.downscaleInPlace;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A texture to be packed by the master packer
 * <p>
 * Created on 2021-10-16.
 *
 * @author Alexander Winter
 */
public class TexturePackTarget implements PackTarget {
	private final String path;
	private final TextureType textureType;
	private final String bundleId;
	private final float scale;
	private final boolean stripWhitespace, noDownscale;
	private final int extendLeft, extendRight, extendTop, extendBottom;

	public TexturePackTarget(String path,
	                         TextureType textureType,
	                         String bundleId,
	                         float scale,
	                         boolean stripWhitespace,
	                         boolean noDownscale,
							 int extendLeft,
							 int extendRight,
							 int extendTop,
							 int extendBottom) {
		ensureNotNull(path, "path");
		ensureNotNull(textureType, "textureType");
		this.path = path;
		this.textureType = textureType;
		this.bundleId = bundleId;
		this.scale = scale;
		this.stripWhitespace = stripWhitespace;
		this.noDownscale = noDownscale;
		this.extendLeft = extendLeft;
		this.extendRight = extendRight;
		this.extendTop = extendTop;
		this.extendBottom = extendBottom;
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
		String relPath = path;
		int curBest = 0;

		for(String basePath : bundle.getBasePaths())
			if(path.startsWith(basePath) && basePath.length() > curBest) {
				relPath = path.replaceFirst(Pattern.quote(basePath), "");
				curBest = basePath.length();
			}

		while(relPath.startsWith("/"))
			relPath = relPath.substring(1);

		String name = relPath.replace('/', '_')
				.replace("_f.png", ".png")
				.replace("_n.png", ".png");

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

		File dest = new File(outDir, name);
		Files.copy(new File(baseDir, path).toPath(), dest.toPath(), REPLACE_EXISTING);
		extendInPlace(dest, extendLeft, extendRight, extendTop, extendBottom);
		downscaleInPlace(dest, bundle.getBaseScale() * scale *
				(noDownscale ? 1f : resolution.getScale()));
	}

	@Override
	public boolean matches(AssetBundle bundle, TextureType textureType, File baseDir, String name) {
		if(textureType != this.textureType)
			return false;

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
				.replace("_n.png", "")
				.replace("_f.png", "")
				.replace(".png", "")
				.equals(name);
	}

	@Override
	public boolean stripWhitespace() {
		return stripWhitespace;
	}

	public static void extendInPlace(File file,
	                                 int extendLeft,
	                                 int extendRight,
	                                 int extendTop,
	                                 int extendBottom) throws IOException {
		if(extendLeft == 0 && extendRight == 0 && extendTop == 0 && extendBottom == 0)
			return;

		BufferedImage img = ImageIO.read(file);

		int w = img.getWidth();
		int h = img.getHeight();
		int fullW = w + extendLeft + extendRight;
		int fullH = h + extendTop + extendBottom;

		BufferedImage extended = new BufferedImage(fullW, fullH, img.getType());

		Graphics g = extended.getGraphics();
		g.drawImage(img, extendLeft, extendTop, null);

		if(extendLeft > 0)
			g.drawImage(img,
					0, extendTop, extendLeft, h + extendTop,
					0, 0, 1, h,
					null);

		if(extendRight > 0)
			g.drawImage(img,
					extendLeft + w, extendTop, fullW, h + extendTop,
					w - 1, 0, w, h,
					null);

		if(extendTop > 0)
			g.drawImage(img,
					extendLeft, 0, w + extendLeft, extendTop,
					0, 0, w, 1,
					null);

		if(extendBottom > 0)
			g.drawImage(img,
					extendLeft, h + extendTop, w + extendLeft, fullH,
					0, h - 1, w, h,
					null);

		if(extendTop > 0 && extendLeft > 0)
			g.drawImage(img,
					0, 0, extendLeft, extendTop,
					0, 0, 1, 1,
					null);

		if(extendTop > 0 && extendRight > 0)
			g.drawImage(img,
					w + extendLeft, 0, fullW, extendTop,
					w - 1, 0, w, 1,
					null);

		if(extendBottom > 0 && extendLeft > 0)
			g.drawImage(img,
					0, h + extendTop, extendLeft, fullH,
					0, h - 1, 1, h,
					null);

		if(extendBottom > 0 && extendRight > 0)
			g.drawImage(img,
					w + extendLeft, h + extendTop, fullW, fullH,
					w - 1, h - 1, w, h,
					null);

		ImageIO.write(extended, "png", file);
	}

	@Override
	public int getExtendLeft() {
		return extendLeft;
	}

	@Override
	public int getExtendRight() {
		return extendRight;
	}

	@Override
	public int getExtendTop() {
		return extendTop;
	}

	@Override
	public int getExtendBottom() {
		return extendBottom;
	}

	@Override
	public long lastModified(File baseDir) {
		return new File(baseDir, path).lastModified();
	}

	@Override
	public String toString() {
		return "TexturePackTarget " + path;
	}

	public TextureType getTextureType() {
		return textureType;
	}
}
