package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.graphics.Texture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.winteralexander.gdx.utils.CLIUtil.getArgsWithoutParams;
import static com.winteralexander.gdx.utils.CLIUtil.getParamValue;
import static com.winteralexander.gdx.utils.collection.CollectionUtil.range;
import static com.winteralexander.gdx.utils.math.NumberUtil.*;

/**
 * A file that defines the list of bundles used by the master packer
 * <p>
 * Created on 2021-10-17.
 *
 * @author Alexander Winter
 */
public class BundleList {
	public static List<AssetBundle> parseFile(File file) throws IOException {
		return Files.readAllLines(file.toPath())
				.stream()
				.map(line -> {
					if(line.trim().isEmpty() || line.startsWith("#"))
						return null;

					try {
						String[] parts = line.split("\\s+");
						String[] partsWithoutParams = getArgsWithoutParams(parts);

						float scale = tryParseFloat(getParamValue(parts, "--scale"), 1.0f);
						int padding = tryParseInt(getParamValue(parts, "--padding"), 10);
						int paddingX = tryParseInt(getParamValue(parts, "--padding-x"), padding);
						int paddingY = tryParseInt(getParamValue(parts, "--padding-y"), padding);
						boolean shaded = tryParseBoolean(getParamValue(parts, "--shaded"), true);
						boolean shrink = tryParseBoolean(getParamValue(parts, "--shrink"), false);
						boolean grid = tryParseBoolean(getParamValue(parts, "--grid"), false);
						String outResStr = getParamValue(parts, "--out-res");
						String minFilterStr = getParamValue(parts, "--min-filter");
						String magFilterStr = getParamValue(parts, "--mag-filter");
						String atlasName = getParamValue(parts, "--atlas-name");
						int maxHeight = tryParseInt(getParamValue(parts, "--max-width"), -1);
						int maxWidth = tryParseInt(getParamValue(parts, "--max-height"), -1);

						AssetResolution[] outRes;
						if(outResStr == null)
							outRes = AssetResolution.values;
						else
							outRes = Arrays.stream(outResStr.split(","))
									.map(s -> "_" + s.toUpperCase(Locale.ROOT))
									.map(AssetResolution::valueOf)
									.toArray(AssetResolution[]::new);

						Texture.TextureFilter minFilter, magFilter;

						if(minFilterStr == null)
							minFilter = Texture.TextureFilter.MipMapLinearLinear;
						else
							minFilter = Texture.TextureFilter.valueOf(minFilterStr);

						if(magFilterStr == null)
							magFilter = Texture.TextureFilter.Linear;
						else
							magFilter = Texture.TextureFilter.valueOf(magFilterStr);

						return new AssetBundle(partsWithoutParams[0],
								shaded,
								!shrink,
								grid,
								scale,
								paddingX,
								paddingY,
								maxWidth,
								maxHeight,
								minFilter,
								magFilter,
								atlasName,
								outRes,
								partsWithoutParams[1],
								range(partsWithoutParams, 2, partsWithoutParams.length));
					} catch(IllegalArgumentException | IndexOutOfBoundsException ex) {
						System.err.println("Invalid line in bundle list file: " + line);
						ex.printStackTrace();
					}

					return null;
				}).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
