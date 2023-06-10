package com.jdaalba.words.io;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import scala.Tuple2;

import java.util.*;

public class Game {

    private final WebDriver driver;

    private String address;

    private final Actions actions;

    public Game(WebDriver driver, String address) {
        this.driver = driver;
        this.address = address;
        this.actions = new Actions(driver);
        init();
    }

    public void setAddress(String address) {
        this.address = address;
        driver.get(address);
    }

    public List<List<Tuple2<Character, String>>> apply(String word) {

        send(word);

        List<List<Tuple2<Character, String>>> l = new ArrayList<>();

        for (var i = 0; i < 6; i++) {

            for (var j = 0; j < 5; j++) {

                WebElement e = driver.findElements(By.xpath("//div[@id='board']/div"))
                        .get(i)
                        .findElements(By.className("react-card-flip"))
                        .get(j)
                        .findElement(By.className("react-card-flipper"))
                        .findElement(By.className("react-card-back"))
                        .findElement(By.tagName("div"));
                if (e.getText().length() == 0) {
                    System.out.println("@@@ Breaking @@@");
                    break;
                }
                if (j == 0) {
                    l.add(i, new ArrayList<>());
                }
                l.get(i).add(new Tuple2<>(e.getText().charAt(0), e.getCssValue("background-color")));
            }
        }
        return l;
    }

    private void send(String word) {
        Arrays.stream(word.toLowerCase().split(""))
                .flatMap(c -> driver.findElements(By.tagName("button")).stream()
                        .filter(e -> e.getAttribute("aria-label").equals(c)))
                .map(b -> new Actions(driver).click(b))
                .forEach(Actions::perform);

        driver.findElements(By.tagName("button")).stream()
                .filter(e -> Objects.equals(e.getAttribute("aria-label"), "procesar palabra"))
                .findFirst()
                .map(b -> new Actions(driver).click(b))
                .orElseThrow()
                .perform();
    }

    private void init() {
        driver.get(address);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        driver.findElements(By.tagName("button"))
                .stream()
                .filter(e -> Objects.equals(e.getAttribute("aria-label"), "Consentir"))
                .findFirst()
                .ifPresent(b -> actions.click(b).perform());

        driver.findElements(By.tagName("button")).stream()
                .filter(e -> Objects.equals(e.getText(), "¡JUGAR!"))
                .findFirst()
                .ifPresent(b -> actions.click(b).perform());
    }
}
