package org.eclipse.orion.client.navigation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	@FindBy(id="registerButton")
	WebElement registerButton;
	@FindBy(id="create_login")
	WebElement createUserNameField;
	@FindBy(id="create_password")
	WebElement createPasswordField;
	@FindBy(id="create_passwordRetype")
	WebElement retypePasswordField;
	@FindBy(id="create_email")
	WebElement createEmailField;
	@FindBy(id="createButton")
	WebElement signupButton;
	@FindBy(id="hideRegisterButton")
	WebElement cancelSignupButton;

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

	public void orionAccountLogin(String userName, String password) {
		selectOrionAccountLogin();
		usernameField.clear();
		passwordField.clear();

		usernameField.sendKeys(userName);
		passwordField.sendKeys(password);

		loginFormSubmitButton.click();
	}
	
	public void registerUser(String userName, String password, String email){
		Logger logger = LoggerFactory.getLogger("test");
		logger.info("Trying to add user: "+ userName);
		registerButton.click();
		
		createUserNameField.clear();
		createPasswordField.clear();
		retypePasswordField.clear();
		createEmailField.clear();
		
		createUserNameField.sendKeys(userName);
		createPasswordField.sendKeys(password);
		retypePasswordField.sendKeys(password);
		createEmailField.sendKeys(email);
		
		signupButton.click();
		logger.info("Added user: "+ userName);
		(new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("home")));
	}
	
	public void deleteUser(String userName){
		driver.get("http://localhost:8080/profile/user-list.html");
		ManageUsersPage managePage = PageFactory.initElements(driver, ManageUsersPage.class);
		managePage.getUserFromList("test");
	}

}
