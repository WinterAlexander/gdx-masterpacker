package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.EnumConstantCache;

/**
 * Type of texture to be packed, each type gets its own output alias
 * <p>
 * Created on 2021-10-16.
 *
 * @author Alexander Winter
 */
public enum TextureType {
	FLAT, NORMAL, PRESHADED;

	public static final TextureType[] values = EnumConstantCache.store(values());
}
