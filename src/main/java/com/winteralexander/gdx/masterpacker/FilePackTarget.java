package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.StringUtil;
import com.winteralexander.gdx.utils.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * {@link PackTarget} that just copies a file
 * <p>
 * Created on 2024-06-14.
 *
 * @author Alexander Winter
 */
public class FilePackTarget implements PackTarget {
	private final String path, bundleId;
	private boolean keepStructure;

	public FilePackTarget(String path,
	                      String bundleId,
	                      boolean keepStructure) {
		ensureNotNull(path, "path");
		this.path = path;
		this.bundleId = bundleId;
		this.keepStructure = keepStructure;
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
		File dest = getDestinationFile(flatDir.getParentFile());
		FileUtil.ensureDirectory(dest.getParentFile());
		Files.copy(new File(baseDir, path).toPath(), dest.toPath(), REPLACE_EXISTING);
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
		return new File(baseDir, path).lastModified();
	}

	@Override
	public String toString() {
		return "FilePackTarget " + path;
	}

	public File getDestinationFile(File outDir) {
		if(keepStructure) {
			String[] outParts = outDir.getAbsolutePath()
					.replaceAll(Pattern.quote(File.separator) + "$", "")
					.replaceAll("^" + Pattern.quote(File.separator), "")
					.split(Pattern.quote(File.separator));
			String[] pathParts = path.split("/");
			int matchPos = Math.min(outParts.length, pathParts.length);
			while(!match(outParts, pathParts, matchPos))
				matchPos--;
			String[] newPath = new String[pathParts.length - matchPos];
			System.arraycopy(pathParts, matchPos, newPath, 0, pathParts.length - matchPos);
			return new File(outDir, StringUtil.join(newPath, File.separator));
		}

		return new File(outDir, new File(path).getName());
	}

	private boolean match(String[] outPath, String[] filePath, int offset) {
		for(int i = 0; i < offset; i++)
			if(!outPath[outPath.length - offset + i].equals(filePath[i]))
				return false;
		return true;
	}
}
