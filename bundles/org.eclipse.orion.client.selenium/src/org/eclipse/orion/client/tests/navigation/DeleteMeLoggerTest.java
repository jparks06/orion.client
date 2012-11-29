/**
 * 
 */
package org.eclipse.orion.client.tests.navigation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jlee
 * 
 */
public class DeleteMeLoggerTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(DeleteMeLoggerTest.class);

		for (int i = 1; i < 10; i++) {
			logger.info(new java.util.Date().toString());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				logger.debug("Sleep error");
			}
			logger.info("{} seconds have passed {}", i*2, "Josh, you fool!");
		}

	}

}
