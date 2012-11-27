package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class BasicNavigation extends PageObject {

	public static final String CLIENT_HOME_URL = "http://localhost:8080/navigate/table.html";

	public BasicNavigation(WebDriver driver) {
		super(driver);
	}

	public static BasicNavigation navigateTo(WebDriver driver) {
		driver.get(CLIENT_HOME_URL);
		return PageFactory.initElements(driver, BasicNavigation.class);
	}

}