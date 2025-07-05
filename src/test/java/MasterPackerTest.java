import com.winteralexander.gdx.masterpacker.MasterPacker;
import com.winteralexander.gdx.utils.io.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Unit test that tests the master packer
 * <p>
 * Created on 2025-07-03.
 *
 * @author Alexander Winter
 */
public class MasterPackerTest {
	@Test
	public void testPacking() throws IOException {
		if(new File("out/").exists())
			FileUtil.deleteRecursively(new File("out/"));

		long firstStart = System.nanoTime();
		MasterPacker.main("-b", "src/test/resources/bundles.bundlelist",
				"-p", "src/test/resources/assets.packlist",
				"-i", "src/test/resources/",
				"-o", "out/",
				"-c", "out/");
		long timeFirst = System.nanoTime() - firstStart;

		long secondStart = System.nanoTime();
		MasterPacker.main("-b", "src/test/resources/bundles.bundlelist",
				"-p", "src/test/resources/assets.packlist",
				"-i", "src/test/resources/",
				"-o", "out/",
				"-c", "out/");
		long timeSecond = System.nanoTime() - secondStart;

		// ensure packing is not redone (cache works)
		assertTrue(timeFirst > 10 * timeSecond);


	}
}
