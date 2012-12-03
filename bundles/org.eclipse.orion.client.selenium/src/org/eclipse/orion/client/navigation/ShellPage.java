package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class ShellPage extends PageObject {

	public static final String SHELL_PAGE_URL = "http://localhost:8080/shell/shellPage.html#/file";
	public static final String SHELL_PAGE_TITLE = "Shell";

	public ShellPage(WebDriver driver) {
		super(driver);
	}

	public static ShellPage navigateTo(WebDriver driver) {
		driver.get(SHELL_PAGE_URL);
		return PageFactory.initElements(driver, ShellPage.class);
	}
}
