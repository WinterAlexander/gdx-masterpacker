package com.winteralexander.gdx.masterpacker;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A target for texture packing
 * <p>
 * Created on 2021-10-16.
 *
 * @author Alexander Winter
 */
public interface PackTarget {
	/**
	 * @return ID of the bundle for which this pack target belongs too
	 */
	String getBundleId();

	/**
	 * Process this pack target in the packing of a bundle
	 *
	 * @param bundle bundle to pack
	 * @param resolution resolution being packed at the moment
	 * @param baseDir
	 * @param flatDir
	 * @param normalDir
	 * @param preshadedDir
	 * @throws IOException
	 */
	void process(AssetBundle bundle,
				 AssetResolution resolution,
	             File baseDir,
	             File flatDir,
	             File normalDir,
	             File preshadedDir) throws IOException;

	default void postProcessAtlas(AssetBundle bundle,
	                              File baseDir,
	                              List<String> atlas,
	                              TextureType atlasType) throws IOException {}

	/**
	 * Checks if this pack target matches the specified region output name, used
	 * to identify which pack target created a region
	 *
	 * @param bundle bundle for this pack target
	 * @param textureType type of texture currently being packed
	 * @param baseDir base directory in which packing takes place
	 * @param name name of region
	 * @return true if region matches, otherwise false
	 */
	boolean matches(AssetBundle bundle, TextureType textureType, File baseDir, String name);

	/**
	 * @return true if regions created by this pack target should be stripped of
	 * their whitespace
	 */
	boolean stripWhitespace();

	int getExtendLeft();

	int getExtendRight();

	int getExtendTop();

	int getExtendBottom();

	/**
	 * Retrieves the last modification of this pack target's input in the provided directory
	 *
	 * @param baseDir input directory in which to check for last modification
	 * @return last modification date of any files affected by this pack target
	 */
	long lastModified(File baseDir);
}
