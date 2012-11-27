package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage extends PageObject {

	public static final String LOGIN_HOME_URL = "http://localhost:8080";

	@FindBy(id = "orionLogin")
	WebElement orionAccountLoginButton;
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

	public void selectOrionAccountLogin() {
		orionAccountLoginButton.click();
	}

	public void login(String username, String password) {
		usernameField.clear();
		passwordField.clear();

		usernameField.sendKeys(username);
		passwordField.sendKeys(password);

		loginFormSubmitButton.click();
	}

}
