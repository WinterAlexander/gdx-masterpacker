package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.utils.ObjectMap;
import com.winteralexander.gdx.utils.Hash;
import com.winteralexander.gdx.utils.StringUtil;
import com.winteralexander.gdx.utils.io.FileUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static com.winteralexander.gdx.utils.CLIUtil.getParamValue;
import static com.winteralexander.gdx.utils.ObjectUtil.firstNonNull;
import static com.winteralexander.gdx.utils.io.FileUtil.ensureDirectory;
import static com.winteralexander.gdx.utils.io.SerializationUtil.readMap;
import static com.winteralexander.gdx.utils.io.SerializationUtil.writeMap;

/**
 * Application which packs assets automatically based on a config
 * <p>
 * Created on 2021-10-16.
 *
 * @author Alexander Winter
 */
public class MasterPacker {
	public static void main(String[] args) throws IOException {
		File bundleFile = new File(firstNonNull(getParamValue(args, "--bundle-list", "-b"),
				"bundles.bundlelist"));
		File targetsFile = new File(firstNonNull(getParamValue(args, "--pack-list", "-p"),
				"assets.packlist"));
		boolean resetCache = getParamValue(args, "--reset-cache", "-r") != null;

		File cacheDir = new File(firstNonNull(getParamValue(args, "--cache-dir", "-c"),
				"build"));
		File cacheFile = new File(cacheDir, "packer-" +
				Hash.sha256(bundleFile.getAbsolutePath()).substring(0, 8) + ".cache");

		ObjectMap<String, BundleCacheEntry> lastPacks = new ObjectMap<>();

		if(cacheFile.exists() && !resetCache)
			readMap(new BufferedInputStream(new FileInputStream(cacheFile)),
					String.class, BundleCacheEntry.class, lastPacks);

		List<AssetBundle> bundles = BundleList.parseFile(bundleFile);
		List<PackTarget> targets = PackingList.parseFile(targetsFile);

		File baseDir = new File("client/assets");

		for(AssetBundle bundle : bundles) {
			File inputDir = new File(baseDir, "gfx_src");
			ensureDirectory(inputDir);
			long bundleLastModification = -1L;
			int packTargetCount = 0;

			for(PackTarget target : targets)
				if(Objects.equals(target.getBundleId(), bundle.getBundleId())) {
					packTargetCount++;
					bundleLastModification = Math.max(bundleLastModification,
							target.lastModified(inputDir));
				}

			if(bundleLastModification != -1L
					&& lastPacks.containsKey(bundle.getBundleId())
					&& lastPacks.get(bundle.getBundleId()).lastModification > bundleLastModification
					&& lastPacks.get(bundle.getBundleId()).lastTargetCount == packTargetCount
					&& !anyDestinationMissing(bundle, targets, baseDir, inputDir)) {
				continue;
			}
			System.out.println("Packing bundle " + bundle.getBundleId());

			for(AssetResolution resolution : bundle.getOutRes()) {
				File resDir = new File(baseDir, resolution.getDirectory());

				File outDir = new File(resDir, bundle.getOutPath());
				File tmpFlat = new File(outDir, "tmp_f");
				File tmpNormal = new File(outDir, "tmp_n");
				File tmpPreshaded = new File(outDir, "tmp_p");

				ensureDirectory(tmpFlat);
				ensureDirectory(tmpNormal);
				ensureDirectory(tmpPreshaded);

				for(PackTarget target : targets)
					if(Objects.equals(target.getBundleId(), bundle.getBundleId()))
						target.process(bundle, resolution,
								inputDir, tmpFlat, tmpNormal, tmpPreshaded);

				Settings settings = new Settings();

				settings.paddingX = bundle.getPaddingX();
				settings.paddingY = bundle.getPaddingY();
				settings.filterMin = bundle.getMinFilter();
				settings.filterMag = bundle.getMagFilter();
				settings.maxWidth = settings.maxHeight = resolution.getAtlasMaxSize();

				if(bundle.getMaxWidth() != -1)
					settings.maxWidth = bundle.getMaxWidth();
				if(bundle.getMaxHeight() != -1)
					settings.maxHeight = bundle.getMaxHeight();

				settings.pot = bundle.isSquare();
				settings.useIndexes = false;
				settings.bleedIterations = 20;
				settings.stripWhitespaceX = settings.stripWhitespaceY = true;
				settings.square = bundle.isSquare();
				settings.grid = bundle.isGrid();

				String flatName = bundle.getAtlasName() != null
						? bundle.isShaded() ? bundle.getAtlasName() + "_flat" : bundle.getAtlasName()
						: bundle.isShaded() ? "flat" : outDir.getName();

				MasterPackerFileProcessor processor = new MasterPackerFileProcessor(settings,
						flatName, targets, bundle, inputDir);

				try {
					processor.setCurrentTextureType(TextureType.FLAT);
					processor.process(tmpFlat, outDir);

					postProcessAtlas(inputDir, outDir, TextureType.FLAT, bundle, targets);
				} catch(Exception ex) {
					throw new IOException("Failed to pack", ex);
				}

				if(bundle.isShaded()) {
					settings.stripWhitespaceX = settings.stripWhitespaceY = false;

					processor.setCurrentTextureType(TextureType.PRESHADED);
					TexturePacker.process(settings, tmpPreshaded.getAbsolutePath(),
							outDir.getAbsolutePath(),
							bundle.getAtlasName() != null ? bundle.getAtlasName() + "_preshaded" : "preshaded");

					postProcessAtlas(inputDir, outDir, TextureType.PRESHADED, bundle, targets);

					settings.paddingX = settings.paddingY = 0;

					processor.setCurrentTextureType(TextureType.NORMAL);
					TexturePacker.process(settings, tmpNormal.getAbsolutePath(),
							outDir.getAbsolutePath(),
							bundle.getAtlasName() != null ? bundle.getAtlasName() + "_normal" : "normal");

					postProcessAtlas(inputDir, outDir, TextureType.NORMAL, bundle, targets);
				}

				for(File tmp : tmpFlat.listFiles())
					tmp.delete();
				for(File tmp : tmpNormal.listFiles())
					tmp.delete();
				for(File tmp : tmpPreshaded.listFiles())
					tmp.delete();

				tmpFlat.delete();
				tmpNormal.delete();
				tmpPreshaded.delete();
			}

			lastPacks.put(bundle.getBundleId(), new BundleCacheEntry(System.currentTimeMillis(),
					packTargetCount));
		}

		try(OutputStream out = new BufferedOutputStream(new FileOutputStream(cacheFile))) {
			writeMap(out, lastPacks);
		}
	}

	private static boolean anyDestinationMissing(AssetBundle bundle,
	                                             Iterable<PackTarget> targets,
	                                             File baseDir,
	                                             File inputDir) {
		boolean hasFlatTarget = false;
		boolean hasNormalTarget = false;
		boolean hasPreshadedTarget = false;

		for(PackTarget target : targets)
			if(Objects.equals(target.getBundleId(), bundle.getBundleId())) {
				for(AssetResolution resolution : bundle.getOutRes()) {
					File resDir = new File(baseDir, resolution.getDirectory());
					File outDir = new File(resDir, bundle.getOutPath());

					if(target instanceof FilePackTarget) {
						if(!((FilePackTarget)target)
								.getDestinationFile(outDir)
								.exists()) {
							missingMessage("Destination of " + target, bundle);
							return true;
						}
					} else if(target instanceof AtlasPackTarget || target instanceof TexturePackTarget) {
						TextureType textureType = target instanceof AtlasPackTarget
								? ((AtlasPackTarget)target).getTextureType()
								: ((TexturePackTarget)target).getTextureType();
						switch(textureType) {
							case FLAT:
								hasFlatTarget = true;
								break;
							case NORMAL:
								hasNormalTarget = true;
								break;
							case PRESHADED:
								hasPreshadedTarget = true;
								break;
						}
					} else if(target instanceof DirectoryPackTarget) {
						hasFlatTarget |= ((DirectoryPackTarget)target).hasFlatTexture(inputDir);
						hasNormalTarget |= ((DirectoryPackTarget)target).hasNormalTexture(inputDir);
						hasPreshadedTarget |= ((DirectoryPackTarget)target).hasPreshadedTexture(inputDir);
					}
				}
			}

		for(AssetResolution resolution : bundle.getOutRes()) {
			File resDir = new File(baseDir, resolution.getDirectory());
			File outDir = new File(resDir, bundle.getOutPath());

			if(hasFlatTarget) {
				String flatName = bundle.getAtlasName() != null
						? bundle.isShaded()
							? bundle.getAtlasName() + "_flat"
							: bundle.getAtlasName()
						: bundle.isShaded()
							? "flat"
							: outDir.getName();

				if(!new File(outDir, flatName + ".atlas").exists()) {
					missingMessage(flatName + ".atlas", bundle);
					return true;
				}

				if(!new File(outDir, flatName + ".png").exists()) {
					missingMessage(flatName + ".png", bundle);
					return true;
				}
			}

			if(bundle.isShaded()) {
				if(hasPreshadedTarget) {
					String preshadedName = bundle.getAtlasName() != null
							? bundle.getAtlasName() + "_preshaded" : "preshaded";


					if(!new File(outDir, preshadedName + ".atlas").exists()) {
						missingMessage(preshadedName + ".atlas", bundle);
						return true;
					}

					if(!new File(outDir, preshadedName + ".png").exists()) {
						missingMessage(preshadedName + ".png", bundle);
						return true;
					}
				}

				if(hasNormalTarget) {
					String normalName = bundle.getAtlasName() != null
							? bundle.getAtlasName() + "_normal" : "normal";

					if(!new File(outDir, normalName + ".atlas").exists()) {
						missingMessage(normalName + ".atlas", bundle);
						return true;
					}

					if(!new File(outDir, normalName + ".png").exists()) {
						missingMessage(normalName + ".png", bundle);
						return true;
					}
				}
			}
		}
		return false;
	}

	private static void missingMessage(String name, AssetBundle bundle) {
		System.out.println(name + " is missing, thus bundle " + bundle.getBundleId() +
				" will be repacked.");
	}

	private static void postProcessAtlas(File inputDir,
	                                     File outDir,
	                                     TextureType textureType,
	                                     AssetBundle bundle,
	                                     List<PackTarget> targets) throws IOException {
		String name;

		switch(textureType) {
			case FLAT:
				name = bundle.getAtlasName() != null
					? bundle.isShaded() ? bundle.getAtlasName() + "_flat" : bundle.getAtlasName()
					: bundle.isShaded() ? "flat" : outDir.getName();
				break;
			case NORMAL:
				name = bundle.getAtlasName() != null
						? bundle.getAtlasName() + "_normal" : "normal";
				break;
			case PRESHADED:
				name = bundle.getAtlasName() != null
						? bundle.getAtlasName() + "_preshaded" : "preshaded";
				break;

			default:
				throw new IllegalArgumentException("Unrecognized texture type: " + textureType);
		}

		File atlas = new File(outDir, name + ".atlas");

		if(!atlas.exists())
			return;

		List<String> atlasContent = Files.readAllLines(atlas.toPath());

		for(PackTarget target : targets)
			if(Objects.equals(target.getBundleId(), bundle.getBundleId()))
				target.postProcessAtlas(bundle, inputDir, atlasContent, textureType);

		if(!atlas.delete() || !atlas.createNewFile())
			throw new IOException("Failed to delete and recreate atlas file " + name +
					".atlas for bundle " + bundle.getBundleId());
		FileWriter writer = new FileWriter(atlas);

		writer.write(StringUtil.join(atlasContent, "\n"));
		writer.close();
	}

}
