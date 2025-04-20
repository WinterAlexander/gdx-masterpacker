package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.graphics.Texture;

import static com.winteralexander.gdx.utils.Validation.*;
import static com.winteralexander.gdx.utils.collection.CollectionUtil.toArray;

/**
 * A bundle of assets to be grouped together in a texture atlas. Multiple bundles
 * can be put into an atlas if they fit
 * <p>
 * Created on 2021-10-17.
 *
 * @author Alexander Winter
 */
public class AssetBundle {
	private final String bundleId;
	private final boolean shaded, square;
	private final float baseScale;
	private final int paddingX, paddingY;
	private final Texture.TextureFilter minFilter;
	private final Texture.TextureFilter magFilter;
	private final String atlasName;
	private final AssetResolution[] outRes;
	private final String outPath;
	private final String[] basePaths;

	public AssetBundle(String bundleId,
					   boolean shaded,
					   boolean square,
					   float baseScale,
					   int paddingX,
					   int paddingY,
					   Texture.TextureFilter minFilter,
					   Texture.TextureFilter magFilter,
					   String atlasName,
	                   AssetResolution[] outRes,
	                   String outPath,
	                   String... basePaths) {
		ensureNotNull(basePaths, "basePaths");
		ensureNotEmpty(outRes, "outRes");
		ensureNoneNull(outRes, "outRes");
		ensureNotNull(outPath, "outPath");
		this.bundleId = bundleId;
		this.shaded = shaded;
		this.square = square;
		this.baseScale = baseScale;
		this.paddingX = paddingX;
		this.paddingY = paddingY;
		this.minFilter = minFilter;
		this.magFilter = magFilter;
		this.atlasName = atlasName;
		this.outRes = outRes;
		this.outPath = outPath;
		this.basePaths = basePaths;
	}

	public AssetBundle(String bundleId,
					   boolean shaded,
					   boolean square,
					   float baseScale,
					   int paddingX,
					   int paddingY,
					   Texture.TextureFilter minFilter,
					   Texture.TextureFilter magFilter,
					   String atlasName,
	                   AssetResolution[] outRes,
	                   String outPath,
	                   Iterable<String> basePaths) {
		this(bundleId,
				shaded,
				square,
				baseScale,
				paddingX,
				paddingY,
				minFilter,
				magFilter,
				atlasName,
				outRes,
				outPath,
				toArray(String.class, basePaths));
	}

	public String getBundleId() {
		return bundleId;
	}

	public boolean isShaded() {
		return shaded;
	}

	public boolean isSquare() {
		return square;
	}

	public Texture.TextureFilter getMinFilter() {
		return minFilter;
	}

	public Texture.TextureFilter getMagFilter() {
		return magFilter;
	}

	public String getAtlasName() {
		return atlasName;
	}

	public AssetResolution[] getOutRes() {
		return outRes;
	}

	public String getOutPath() {
		return outPath;
	}

	public String[] getBasePaths() {
		return basePaths;
	}

	public float getBaseScale() {
		return baseScale;
	}

	public int getPaddingX() {
		return paddingX;
	}

	public int getPaddingY() {
		return paddingY;
	}
}
