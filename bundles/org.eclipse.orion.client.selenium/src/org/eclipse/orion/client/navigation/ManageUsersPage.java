/**
 * 
 */
package org.eclipse.orion.client.navigation;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageUsersPage extends PageObject {
	Logger logger = LoggerFactory.getLogger("ManageUsersPage");
	private static final String MANAGE_PAGE_URL = "http://localhost:8080/profile/user-list.html";

	@FindBy(id = "usersListinnerTree")
	WebElement usersList;
	
	@FindBy(xpath="/html/body/div/div/div/ul/li[3]/span")
	WebElement deleteButton;

	public ManageUsersPage(WebDriver driver) {
		super(driver);
	}

	public static ManageUsersPage goToManageUsersPage(WebDriver driver) {
		driver.get(MANAGE_PAGE_URL);
		return PageFactory.initElements(driver, ManageUsersPage.class);
	}

	public void getUserFromList(String userName) {
		usersList.findElement(By.linkText(userName)).click();
		(new WebDriverWait(driver, 10)).until(ExpectedConditions
				.presenceOfElementLocated(By.id("personalInformation_SectionTitle")));
	}

	public void deleteUser(String userName) throws InterruptedException {
		WebDriverWait webWait = new WebDriverWait(driver, 10);
		
		logger.debug("Trying to find user link" );				
		WebElement userLink = driver.findElement(By.linkText(userName));
		
		logger.debug("Trying to find parent" );
		WebElement parent = userLink.findElement(By.xpath("../../.."));
		
		logger.debug("Trying to find delete image" );
		WebElement deleteImage = parent.findElement(By.xpath("./td[2]/span/img"));

		logger.info("Trying to delete user: "+ userName );
		deleteImage.click();
		
		webWait.until(ExpectedConditions.alertIsPresent());
		// Click on OK button on popup.
		Alert deleteAlert = driver.switchTo().alert();
		deleteAlert.accept();
	}

	public void deleteUserFromProfilePage(String userName) {
		getUserFromList(userName);		
		
		logger.info("Trying to delete user" );

		//TODO: This does not work
		Actions action = new Actions(driver);
		action.click(deleteButton).perform();
		

		(new WebDriverWait(driver, 5)).until(ExpectedConditions
				.alertIsPresent());
		
//		// Click on OK button on popup.
//		Alert deleteAlert = driver.switchTo().alert();
//		deleteAlert.accept();
	}

}
