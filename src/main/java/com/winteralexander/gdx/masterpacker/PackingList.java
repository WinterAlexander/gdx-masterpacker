package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.ObjectUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.winteralexander.gdx.utils.CLIUtil.getArgsWithoutParams;
import static com.winteralexander.gdx.utils.CLIUtil.getParamValue;
import static com.winteralexander.gdx.utils.math.NumberUtil.tryParseFloat;
import static com.winteralexander.gdx.utils.math.NumberUtil.tryParseInt;

/**
 * Represents a file that lists packing target
 * <p>
 * Created on 2021-10-17.
 *
 * @author Alexander Winter
 */
public class PackingList {
	public static List<PackTarget> parseFile(File file) throws IOException {
		return Files.readAllLines(file.toPath())
				.stream()
				.map(line -> {
					if(line.trim().isEmpty() || line.startsWith("#"))
						return null;

					try {
						String[] parts = line.split("\\s+");
						String[] paramlessParts = getArgsWithoutParams(parts);

						RecursionMode recursionMode = tryParseRecursionMode(
								getParamValue(parts, "--recursion"),
								RecursionMode.ENABLED);
						float scale = tryParseFloat(getParamValue(parts, "--scale"), 1.0f);
						boolean stripWhitespace = getParamValue(parts, "--strip") != null;
						boolean noDownscale = getParamValue(parts, "--nodownscale") != null;
						boolean keepStructure = getParamValue(parts, "--keep-structure") != null;
						int extendLeft = tryParseInt(getParamValue(parts, "--extend-left"), -1);
						int extendRight = tryParseInt(getParamValue(parts, "--extend-right"), -1);
						int extendTop = tryParseInt(getParamValue(parts, "--extend-top"), -1);
						int extendBottom = tryParseInt(getParamValue(parts, "--extend-bottom"), -1);
						int extendX = tryParseInt(getParamValue(parts, "--extend-x"), -1);
						int extendY = tryParseInt(getParamValue(parts, "--extend-y"), -1);
						int extend = tryParseInt(getParamValue(parts, "--extend"), -1);
						extendLeft = ObjectUtil.firstNonNegative(extendLeft, extendX, extend, 0);
						extendRight = ObjectUtil.firstNonNegative(extendRight, extendX, extend, 0);
						extendTop = ObjectUtil.firstNonNegative(extendTop, extendY, extend, 0);
						extendBottom = ObjectUtil.firstNonNegative(extendBottom, extendY, extend, 0);

						if(paramlessParts[0].equalsIgnoreCase("texture"))
							return new TexturePackTarget(paramlessParts[1],
									TextureType.valueOf(paramlessParts[2].toUpperCase(Locale.ROOT)),
									paramlessParts[3],
									scale,
									stripWhitespace,
									noDownscale,
									extendLeft,
									extendRight,
									extendTop,
									extendBottom);
						else if(paramlessParts[0].equalsIgnoreCase("atlas"))
							return new AtlasPackTarget(paramlessParts[1],
									TextureType.valueOf(paramlessParts[2].toUpperCase(Locale.ROOT)),
									paramlessParts[3],
									scale,
									noDownscale,
									extendLeft,
									extendRight,
									extendTop,
									extendBottom);
						else if(paramlessParts[0].equalsIgnoreCase("directory"))
							return new DirectoryPackTarget(recursionMode,
									paramlessParts[1],
									paramlessParts[2],
									scale,
									stripWhitespace,
									noDownscale,
									extendLeft,
									extendRight,
									extendTop,
									extendBottom);
						else if(paramlessParts[0].equalsIgnoreCase("file"))
							return new FilePackTarget(paramlessParts[1],
									paramlessParts[2],
									keepStructure);
					} catch(IllegalArgumentException | IndexOutOfBoundsException ex) {
						System.err.println("Invalid line in packing list file: " + line);
						ex.printStackTrace();
					}

					return null;
				}).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public static RecursionMode tryParseRecursionMode(String input, RecursionMode defaultValue) {
		if(input == null)
			return defaultValue;
		try {
			return RecursionMode.valueOf(input.toUpperCase(Locale.ROOT).replace('-', '_'));
		} catch(IllegalArgumentException ex) {
			return defaultValue;
		}
	}
}
