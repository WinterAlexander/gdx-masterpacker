package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.winteralexander.gdx.utils.ReflectionUtil;

import java.awt.image.BufferedImage;

/**
 * {@link TexturePacker.Rect}
 * <p>
 * Created on 2025-02-16.
 *
 * @author Alexander Winter
 */
public class ExtendedRect extends TexturePacker.Rect {
	public int leftExtension = 0,
			rightExtension = 0,
			topExtension = 0,
			bottomExtension = 0;

	public ExtendedRect(TexturePacker.Rect other) {
		super(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR), 0, 0, 1, 1, false);
		ReflectionUtil.call(this, "set", other);
	}
}
