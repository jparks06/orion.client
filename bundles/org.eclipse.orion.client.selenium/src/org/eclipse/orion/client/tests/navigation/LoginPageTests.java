package org.eclipse.orion.client.tests.navigation;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.LoginPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
		(new WebDriverWait(driver, 10)).until(ExpectedConditions.visibilityOf(loginPage.orionAccountLoginButton));
		assertTrue(driver.getTitle().contains(LoginPage.LOGIN_PAGE_TITLE));
	}

	@Test
	public void testValidLogin() {
	//	loginPage.selectOrionAccountLogin();
		loginPage.orionAccountLogin("jparks", "pass");
		(new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("home")));
		assertTrue(driver.getTitle().contains("Navigator"));
	}

	@Test
	public void testRegisterUser(){
		loginPage.registerUser("test", "password", "test@test.com");
		assertTrue(driver.getTitle().contains("Navigator"));
	}
	
	@Test
	public void deleteUser(){
		loginPage.orionAccountLogin("admin", "password");
		loginPage.deleteUser("test");
		assertTrue(driver.getTitle().contains("User Profile"));	
	}
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}