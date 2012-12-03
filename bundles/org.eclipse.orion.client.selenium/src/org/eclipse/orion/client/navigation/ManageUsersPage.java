/**
 * 
 */
package org.eclipse.orion.client.navigation;

import java.util.List;

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

public class ManageUsersPage extends PageObject{
	Logger logger = LoggerFactory.getLogger("ManageUsersPage");
	private static final String MANAGE_PAGE_URL = "http://localhost:8080/profile/user-list.html";
	
	@FindBy(id="usersListinnerTree")
	WebElement usersList;
	
	public ManageUsersPage(WebDriver driver) {
		super(driver);
	}
	
	public static ManageUsersPage goToManageUsersPage(WebDriver driver){
		driver.get(MANAGE_PAGE_URL);
		return PageFactory.initElements(driver, ManageUsersPage.class);
	}
	
	public void getUserFromList(String userName){
		usersList.findElement(By.linkText(userName)).click();
		(new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("personalInformation_SectionTitle")));		
	}
	
	public void deleteUser(String userName){				
		List<WebElement> tableRow = usersList.findElements(By.tagName("tr"));
		WebElement deleteButton = null;
		for(WebElement element : tableRow){
			List<WebElement> tableCells = element.findElements(By.tagName("td"));
			for(WebElement cell : tableCells){
				if(cell.getText().equals("test")){
					deleteButton = tableCells.get(tableCells.indexOf(cell)+1);
					break;
				}
			}
		}
		logger.info("Trying to delete: "+ userName);
		deleteButton.findElement(By.xpath("./span/img")).click();

		//Click on OK button on popup.
		Alert deleteAlert = driver.switchTo().alert();
		deleteAlert.accept();			
	}
	
	public void deleteUserFromProfilePage(String userName){
		getUserFromList(userName);
		
		//WebElement deleteButton = driver.findElement(By.xpath("/html/body/div/div/div/ul/li[3]/span"));
		(new Actions(driver).click(driver.findElement(By.xpath("/html/body/div/div/div/ul/li[3]/span")))).perform();
		(new WebDriverWait(driver, 5)).until(ExpectedConditions.alertIsPresent());
	}

}
