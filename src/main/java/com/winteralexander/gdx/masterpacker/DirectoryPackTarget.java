package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.collection.CollectionUtil;
import com.winteralexander.gdx.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static com.winteralexander.gdx.masterpacker.Downscalator.downscaleInPlace;
import static com.winteralexander.gdx.masterpacker.TexturePackTarget.extendInPlace;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A directory of textures to be automatically packed
 * <p>
 * Created on 2021-10-19.
 *
 * @author Alexander Winter
 */
public class DirectoryPackTarget implements PackTarget {
	private final String path;
	private final RecursionMode recursionMode;
	private final String bundleId;
	private final float scale;
	private final boolean stripWhitespace, noDownscale;
	private final int extendLeft, extendRight, extendTop, extendBottom;

	public DirectoryPackTarget(RecursionMode recursionMode,
	                           String path,
	                           String bundleId,
	                           float scale,
	                           boolean stripWhitespace,
	                           boolean noDownscale,
	                           int extendLeft,
	                           int extendRight,
	                           int extendTop,
	                           int extendBottom) {
		ensureNotNull(recursionMode, "recursionMode");
		ensureNotNull(path, "path");
		this.recursionMode = recursionMode;
		this.path = path;
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
		File file = new File(baseDir, path);

		if(recursionMode != RecursionMode.ENABLED) {
			for(File child : file.listFiles())
				if((recursionMode == RecursionMode.DISABLED) != child.isDirectory())
					process(child, bundle, resolution, baseDir, flatDir, normalDir, preshadedDir);
		} else
			process(file, bundle, resolution, baseDir, flatDir, normalDir, preshadedDir);
	}

	private void process(File file,
	                     AssetBundle bundle,
	                     AssetResolution resolution,
	                     File baseDir,
	                     File flatDir,
	                     File normalDir,
	                     File preshadedDir) throws IOException {
		if(file.isDirectory()) {
			for(File children : file.listFiles())
				process(children, bundle, resolution, baseDir, flatDir, normalDir, preshadedDir);
			return;
		}

		File atlas = new File(file.getAbsolutePath().replace(".png", ".atlas"));

		if(atlas.exists())
			return;

		String baseRel = baseDir.toURI().relativize(file.toURI()).getPath();
		String relPath = baseRel;
		int curBest = 0;

		for(String basePath : bundle.getBasePaths())
			if(baseRel.startsWith(basePath) && curBest < basePath.length()) {
				relPath = baseRel.replaceFirst(Pattern.quote(basePath), "");
				curBest = basePath.length();
			}

		while(relPath.startsWith("/"))
			relPath = relPath.substring(1);

		String name = relPath.replace('/', '_')
				.replace("_f.png", ".png")
				.replace("_n.png", ".png");

		File flatVersion = new File(file.getParent(), file.getName().replace(".png", "_f.png"));
		File dest;

		if(file.getName().endsWith("_n.png"))
			dest = new File(normalDir, name);

		else if(file.getName().endsWith("_f.png")
				|| file.getName().endsWith(".png") && !flatVersion.exists())
			dest = new File(flatDir, name);

		else if(file.getName().endsWith(".png") && flatVersion.exists())
			dest = new File(preshadedDir, name);
		else
			return;

		Files.copy(file.toPath(), dest.toPath(), REPLACE_EXISTING);
		extendInPlace(dest, extendLeft, extendRight, extendTop, extendBottom);
		downscaleInPlace(dest, bundle.getBaseScale() * scale *
				(noDownscale ? 1f : resolution.getScale()));
	}

	@Override
	public boolean matches(AssetBundle bundle, TextureType textureType, File baseDir, String name) {
		return recurMatches(bundle, textureType, baseDir, new File(baseDir, path), name, 0);
	}

	private boolean recurMatches(AssetBundle bundle, TextureType textureType,
	                             File baseDir, File file, String name,
	                             int depth) {
		if(recursionMode == RecursionMode.DISABLED && depth > 1)
			return false;

		if(file.isDirectory()) {
			for(File children : file.listFiles())
				if(recurMatches(bundle, textureType, baseDir, children, name, depth + 1))
					return true;
			return false;
		}

		if(recursionMode == RecursionMode.SUB_DIRS_ONLY && depth == 1)
			return false;

		File atlas = new File(file.getAbsolutePath().replace(".png", ".atlas"));

		if(atlas.exists())
			return false;

		String baseRel = baseDir.toURI().relativize(file.toURI()).getPath();
		String relPath = baseRel;
		int curBest = 0;

		for(String basePath : bundle.getBasePaths())
			if(baseRel.startsWith(basePath) && curBest < basePath.length()) {
				relPath = baseRel.replaceFirst(Pattern.quote(basePath), "");
				curBest = basePath.length();
			}

		while(relPath.startsWith("/"))
			relPath = relPath.substring(1);


		if(relPath.endsWith("_n") && textureType != TextureType.NORMAL)
			return false;

		if(relPath.endsWith("_f") && textureType != TextureType.FLAT)
			return false;

		String regionName = relPath.replace('/', '_')
				.replace("_f.png", "")
				.replace("_n.png", "")
				.replace(".png", "");

		return regionName.equals(name);
	}

	@Override
	public boolean stripWhitespace() {
		return stripWhitespace;
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
		File directory = new File(baseDir, path);
		if(recursionMode == RecursionMode.ENABLED)
			return FileUtil.getLastModifiedRecursively(directory);

		long lastModified = directory.lastModified();
		for(File child : directory.listFiles()) {
			if((recursionMode == RecursionMode.DISABLED) != child.isDirectory())
				lastModified = Math.max(lastModified, FileUtil.getLastModifiedRecursively(child));
		}

		return lastModified;
	}

	public boolean hasFlatTexture(File baseDir) {
		return CollectionUtil.any(FileUtil.recurse(new File(baseDir, path)),
				f -> f.getName().endsWith(".png")
						&& !f.getName().endsWith("_n.png"));
	}

	public boolean hasNormalTexture(File baseDir) {
		return CollectionUtil.any(FileUtil.recurse(new File(baseDir, path)),
				f -> f.getName().endsWith("_n.png"));
	}

	public boolean hasPreshadedTexture(File baseDir) {
		return CollectionUtil.any(FileUtil.recurse(new File(baseDir, path)),
				f -> f.getName().endsWith(".png")
						&& !f.getName().endsWith("_f.png")
						&& !f.getName().endsWith("_n.png")
						&& new File(f.getParent(), f.getName().replace(".png", "_f.png")).exists());
	}

	@Override
	public String toString() {
		return "DirectoryPackTarget " + path;
	}
}
