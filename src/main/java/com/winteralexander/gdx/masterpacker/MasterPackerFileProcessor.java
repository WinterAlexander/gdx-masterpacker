package com.winteralexander.gdx.masterpacker;

import com.badlogic.gdx.tools.texturepacker.ImageProcessor;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePackerFileProcessor;
import com.badlogic.gdx.utils.Array;
import com.winteralexander.gdx.utils.ReflectionUtil;

import java.io.File;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * {@link TexturePackerFileProcessor} used by the {@link MasterPacker}
 * <p>
 * Created on 2025-04-16.
 *
 * @author Alexander Winter
 */
public class MasterPackerFileProcessor extends TexturePackerFileProcessor {
	private final Iterable<PackTarget> targets;
	private final AssetBundle bundle;
	private final File inputDir;

	private TextureType currentTextureType = null;

	public MasterPackerFileProcessor(TexturePacker.Settings defaultSettings,
	                                 String packFileName,
	                                 Iterable<PackTarget> targets,
	                                 AssetBundle bundle,
	                                 File inputDir) {
		super(defaultSettings, packFileName, null);
		ensureNotNull(targets, "targets");
		ensureNotNull(bundle, "bundle");
		ensureNotNull(inputDir, "inputDir");
		this.targets = targets;
		this.bundle = bundle;
		this.inputDir = inputDir;
	}

	Array<TexturePacker.Rect> rects;
	Array<TexturePacker.Page> pages;

	@Override
	protected TexturePacker newTexturePacker(File root, TexturePacker.Settings settings) {
		TexturePacker packer = new TexturePacker(root, settings) {
			@Override
			protected ImageProcessor newImageProcessor(Settings settings) {
				return new MasterPackerImageProcessor(settings, targets, bundle,
						currentTextureType, inputDir);
			}
		};
		TexturePacker.Packer oldPacker = ReflectionUtil.get(packer, "packer");
		packer.setPacker(new TexturePacker.Packer() {
			@Override
			public Array<TexturePacker.Page> pack(Array<TexturePacker.Rect> array) {
				rects = array;
				pages = oldPacker.pack(array);
				return pages;
			}

			@Override
			public Array<TexturePacker.Page> pack(TexturePacker.ProgressListener progressListener,
			                                      Array<TexturePacker.Rect> array) {
				rects = array;
				pages = oldPacker.pack(progressListener, array);
				return pages;
			}
		});
		packer.setProgressListener(new TexturePacker.ProgressListener() {
			@Override
			public void start(float portion) {
				if(portion == 0.01f) {
					for(TexturePacker.Page page : pages) {
						for(TexturePacker.Rect rect : page.outputRects) {
							TexturePacker.Rect matching = null;
							for(TexturePacker.Rect other : rects)
								if(rect.name.equals(other.name)) {
									matching = other;
									break;
								}

							if(matching instanceof ExtendedRect) {
								rect.x += ((ExtendedRect)matching).leftExtension;
								rect.y -= ((ExtendedRect)matching).topExtension;

								rect.originalWidth -= ((ExtendedRect)matching).leftExtension + ((ExtendedRect)matching).rightExtension;
								rect.originalHeight -= ((ExtendedRect)matching).topExtension + ((ExtendedRect)matching).bottomExtension;
								rect.regionWidth -= ((ExtendedRect)matching).leftExtension + ((ExtendedRect)matching).rightExtension;
								rect.regionHeight -= ((ExtendedRect)matching).topExtension + ((ExtendedRect)matching).bottomExtension;

							}
						}
					}
				}

				super.start(portion);
			}

			@Override
			public void progress(float v) {}
		});
		return packer;
	}

	public void setCurrentTextureType(TextureType currentTextureType) {
		this.currentTextureType = currentTextureType;
	}
}
