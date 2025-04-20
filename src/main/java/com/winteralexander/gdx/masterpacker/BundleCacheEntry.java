package com.winteralexander.gdx.masterpacker;

import com.winteralexander.gdx.utils.io.Serializable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.winteralexander.gdx.utils.io.StreamUtil.*;

/**
 * Stores the information of a bundle in the bundle cache, that is last modification date and how
 * many pack targets were in the bundle
 * <p>
 * Created on 2025-04-15.
 *
 * @author Alexander Winter
 */
public class BundleCacheEntry implements Serializable {
	public long lastModification = -1L;
	public int lastTargetCount = 0;

	public BundleCacheEntry() {}

	public BundleCacheEntry(long lastModification, int lastTargetCount) {
		this.lastModification = lastModification;
		this.lastTargetCount = lastTargetCount;
	}

	@Override
	public void readFrom(InputStream input) throws IOException {
		lastModification = readLong(input);
		lastTargetCount = readInt(input);
	}

	@Override
	public void writeTo(OutputStream output) throws IOException {
		writeLong(output, lastModification);
		writeInt(output, lastTargetCount);
	}
}
