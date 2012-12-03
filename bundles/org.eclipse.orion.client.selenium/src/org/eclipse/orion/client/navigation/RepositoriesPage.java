package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class RepositoriesPage extends PageObject {

	public static final String REPOSITORIES_PAGE_URL = "http://localhost:8080/git/git-repository.html";
	public static final String REPOSITORIES_PAGE_TITLE = "Git";
	
	public RepositoriesPage(WebDriver driver) {
		super(driver);
	}

	public static RepositoriesPage navigateTo(WebDriver driver) {
		driver.get(REPOSITORIES_PAGE_URL);
		return PageFactory.initElements(driver, RepositoriesPage.class);
	}
}
