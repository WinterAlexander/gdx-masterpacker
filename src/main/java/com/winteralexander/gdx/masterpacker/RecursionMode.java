package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.EnumConstantCache;

/**
 * A mode of recursion {@link DirectoryPackTarget} can be configured with
 * <p>
 * Created on 2021-10-19.
 *
 * @author Alexander Winter
 */
public enum RecursionMode {
	ENABLED,
	DISABLED,
	SUB_DIRS_ONLY
	;

	public static final RecursionMode[] values = EnumConstantCache.store(values());
}
