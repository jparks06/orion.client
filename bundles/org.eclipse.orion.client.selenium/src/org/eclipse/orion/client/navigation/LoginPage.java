package org.eclipse.orion.client.navigation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage extends PageObject {

	public static final String LOGIN_HOME_URL = "http://localhost:8080";
	public static final String LOGIN_PAGE_TITLE = "Login Page";

	@FindBy(id = "orionLogin")
	public WebElement orionAccountLoginButton;
	@FindBy(id = "login")
	WebElement usernameField;
	@FindBy(id = "password")
	WebElement passwordField;
	@FindBy(id = "loginButton")
	WebElement loginFormSubmitButton;

	public LoginPage(WebDriver driver) {
		super(driver);
	}

	public static LoginPage navigateTo(WebDriver driver) {
		driver.get(LOGIN_HOME_URL);
		return PageFactory.initElements(driver, LoginPage.class);
	}

	public void orionAccountLogin(String username, String password) {
		orionAccountLoginButton.click();
		usernameField.clear();
		passwordField.clear();

		usernameField.sendKeys(username);
		passwordField.sendKeys(password);

		loginFormSubmitButton.click();
		
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("home")));
	}
}
