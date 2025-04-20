package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.tools.texturepacker.ImageProcessor;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * {@link ImageProcessor} used by the {@link MasterPacker}
 * <p>
 * Created on 2025-02-16.
 *
 * @author Alexander Winter
 */
public class MasterPackerImageProcessor extends ImageProcessor {
	private final Iterable<PackTarget> targets;
	private final AssetBundle bundle;
	private final TextureType textureType;
	private final File inputDir;

	public MasterPackerImageProcessor(TexturePacker.Settings settings,
	                                  Iterable<PackTarget> targets,
	                                  AssetBundle bundle,
									  TextureType textureType,
	                                  File inputDir) {
		super(settings);
		ensureNotNull(targets, "targets");
		ensureNotNull(bundle, "bundle");
		ensureNotNull(inputDir, "inputDir");
		ensureNotNull(textureType, "textureType");
		this.targets = targets;
		this.bundle = bundle;
		this.textureType = textureType;
		this.inputDir = inputDir;
	}

	@Override
	protected TexturePacker.Rect stripWhitespace(String name, BufferedImage source) {
		PackTarget target = null;

		for(PackTarget current : targets)
			if(Objects.equals(current.getBundleId(), bundle.getBundleId()))
				if(current.matches(bundle, textureType, inputDir, name)) {
					if(target != null)
						throw new IllegalStateException("Multiple PackTarget (" + target + ", " +
								current + ") matches the same input (" + name + ")");
					target = current;
				}
		if(target != null && target.stripWhitespace())
			return super.stripWhitespace(name, source);

		TexturePacker.Rect rect = new TexturePacker.Rect(source,
				0, 0, source.getWidth(), source.getHeight(), false);

		if(target == null)
			return rect;

		ExtendedRect extendedRect = new ExtendedRect(rect);
		extendedRect.leftExtension = target.getExtendLeft();
		extendedRect.rightExtension = target.getExtendRight();
		extendedRect.topExtension = target.getExtendTop();
		extendedRect.bottomExtension = target.getExtendBottom();
		return extendedRect;
	}
}
