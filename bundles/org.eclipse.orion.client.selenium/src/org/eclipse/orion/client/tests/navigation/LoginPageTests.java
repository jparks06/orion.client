package org.eclipse.orion.client.tests.navigation;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.LoginPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * @author jParks
 *
 */
public class LoginPageTests {

	private WebDriver driver;
	private LoginPage loginPage;

	@Before
	public void setup() throws Exception {
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		loginPage = LoginPage.navigateTo(driver);
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}

	@Test
	public void testPageExists() {
		assertTrue(driver.getTitle().contains("Login Page"));
	}

	@Test
	public void testValidLogin() {
		loginPage.selectOrionAccountLogin();
		loginPage.login("jparks", "pass");
		assertTrue(driver.getTitle().contains("Navigator"));
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}