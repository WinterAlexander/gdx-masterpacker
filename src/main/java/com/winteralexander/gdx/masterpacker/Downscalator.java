package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.tools.texturepacker.ColorBleedEffect;
import com.badlogic.gdx.utils.ObjectMap;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import com.winteralexander.gdx.utils.CLIUtil;
import com.winteralexander.gdx.utils.Hash;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static com.winteralexander.gdx.utils.CLIUtil.getParamValue;
import static com.winteralexander.gdx.utils.ObjectUtil.firstNonNull;
import static com.winteralexander.gdx.utils.io.SerializationUtil.readMap;
import static com.winteralexander.gdx.utils.io.SerializationUtil.writeMap;

/**
 * Downscales assets from 4K to 1080p and 720p
 * <p>
 * Created on 2021-01-15.
 *
 * @author Alexander Winter
 */
public class Downscalator {
	public static void main(String[] args) throws IOException {
		ObjectMap<String, Long> lastDownscales = new ObjectMap<>();

		File listFile = new File(firstNonNull(getParamValue(args, "--list", "-l"),
				"downscalator.list"));
		File cacheDir = new File(firstNonNull(getParamValue(args, "--cache-dir", "-c"),
				"build"));

		File cache = new File(cacheDir, "downscalator-" +
				Hash.sha256(listFile.getAbsolutePath()).substring(0, 8) + ".cache");

		if(cache.exists())
			readMap(new BufferedInputStream(new FileInputStream(cache)),
					String.class, Long.class, lastDownscales);

		for(AssetResolution resolution : AssetResolution.values) {
			File baseDir = new File("client/assets/gfx_src");
			File outDir = new File(baseDir.getParentFile(), resolution.getDirectory());

			List<DownscalatorEntry> paths = Files.readAllLines(listFile.toPath()).stream()
					.map(s -> s.split(" "))
					.map(s -> new DownscalatorEntry(
							CLIUtil.getArgsWithoutParams(s)[0],
							CLIUtil.getParamValue(s, "--nodownscale") == null))
					.collect(Collectors.toList());

			process(baseDir,
					baseDir,
					outDir,
					resolution,
					paths,
					lastDownscales);

			try(OutputStream output = new BufferedOutputStream(new FileOutputStream(cache))) {
				writeMap(output, lastDownscales);
			}
		}
	}

	private static DownscalatorEntry matchAnyPath(List<DownscalatorEntry> paths,
	                                              String input) {
		for(DownscalatorEntry entry : paths) {
			String path = entry.path;
			if(path.equals(input) || path.startsWith(input))
				return entry;

			if(path.endsWith("*") && input.startsWith(path.substring(0, path.length() - 1)))
				return entry;

			if(path.contains("**")) {
				int index = path.indexOf("**");
				int endIndex = path.lastIndexOf("**") + 2;
				if(input.startsWith(path.substring(0, index))
				&& input.endsWith(path.substring(endIndex)))
					return entry;
			}
		}

		return null;
	}

	public static void process(File baseDir,
	                           File file,
	                           File outDir,
	                           AssetResolution size,
	                           List<DownscalatorEntry> paths,
	                           ObjectMap<String, Long> lastDownscales) throws IOException {
		if(file.isDirectory()) {
			for(File inner : file.listFiles())
				process(baseDir, inner, outDir, size, paths, lastDownscales);
			return;
		}

		String relative = baseDir.toURI().relativize(file.toURI()).getPath();

		DownscalatorEntry entry = matchAnyPath(paths, relative);
		if(entry == null || !entry.doDownscale && size != AssetResolution._4K)
			return;

		if(file.isFile() && !file.getName().endsWith(".png"))
			return;

		File outFile = new File(outDir, relative);

		long lastDownscale = lastDownscales.get(outFile.getAbsolutePath(), -1L);

		if(lastDownscale >= file.lastModified() && outFile.exists()) {
			return;
		}

		try {
			outFile.getParentFile().mkdirs();
			BufferedImage img = ImageIO.read(file);

			if(img == null)
				return; // not an image

			BufferedImage copy = createResizedCopy(img,
					Math.round(img.getWidth() * size.getScale()),
					Math.round(img.getHeight() * size.getScale()));
			ImageIO.write(copy, "png", outFile);
			System.out.println("Resized " + file.getAbsolutePath());
			lastDownscales.put(outFile.getAbsolutePath(), System.currentTimeMillis());
		} catch(Exception ex) {
			System.out.println("Didn't work with " + file.getAbsolutePath());
			ex.printStackTrace();
		}
	}

	public static BufferedImage createResizedCopy(BufferedImage source,
	                                              int destWidth,
	                                              int destHeight) {
		ResampleOp resampleOp = new ResampleOp(destWidth, destHeight);
		resampleOp.setFilter(ResampleFilters.getBiCubicFilter());
		return resampleOp.filter(source, null);
	}

	public static void downscaleInPlace(File file, float scale) throws IOException {
		if(scale == 1f)
			return;

		BufferedImage img = ImageIO.read(file);

		if(img.getWidth() == 1 || img.getHeight() == 1)
			return;

		img = new ColorBleedEffect().processImage(img, 20);
		BufferedImage copy = createResizedCopy(img,
				Math.round(img.getWidth() * scale),
				Math.round(img.getHeight() * scale));
		ImageIO.write(copy, "png", file);
	}

	public static class DownscalatorEntry {
		public final String path;
		public final boolean doDownscale;

		public DownscalatorEntry(String path, boolean doDownscale) {
			this.path = path;
			this.doDownscale = doDownscale;
		}
	}
}
