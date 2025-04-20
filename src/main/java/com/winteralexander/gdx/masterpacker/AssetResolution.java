package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.EnumConstantCache;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * Resolution at which an asset can be in or exported at
 * <p>
 * Created on 2021-10-19.
 *
 * @author Alexander Winter
 */
public enum AssetResolution {
	_720P("gfx_720", 2048, 1f / 3f),
	_1080P("gfx_1080", 4096, 0.5f),
	_4K("gfx", 8192, 1f),
	;

	public static final AssetResolution[] values = EnumConstantCache.store(values());

	private final String directory;
	private final int atlasMaxSize;
	private final float scale;

	AssetResolution(String directory, int atlasMaxSize, float scale) {
		ensureNotNull(directory, "directory");
		this.directory = directory;
		this.atlasMaxSize = atlasMaxSize;
		this.scale = scale;
	}

	public String getDirectory() {
		return directory;
	}

	public int getAtlasMaxSize() {
		return atlasMaxSize;
	}

	public float getScale() {
		return scale;
	}
}
