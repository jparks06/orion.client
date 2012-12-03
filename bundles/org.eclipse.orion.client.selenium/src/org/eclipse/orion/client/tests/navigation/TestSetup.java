package org.eclipse.orion.client.tests.navigation;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.LoginPage;
import org.eclipse.orion.client.navigation.ManageUsersPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

/**
 * Test setup for the test suite.
 * 
 *
 */
public class TestSetup {
	
	private WebDriver driver;
	private LoginPage loginPage;
	private ManageUsersPage manageUsersPage;
	
	@Before
	public void setup() throws Exception {
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		loginPage = LoginPage.navigateTo(driver);
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test
	public void testSetup(){
		createTestUser();		
	}
	
	private void createTestUser(){
		loginPage.registerUser("test6", "test", "test6@email.com");
//		Select userDropdown = new Select(driver.findElement(By.id("userDropdown")));
//		userDropdown.selectByVisibleText("Sign Out");
	}
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}
