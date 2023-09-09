package com.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
  public static void main(String[] args) {
    Pattern p = Pattern.compile("-?\\d+\\.?\\d+");
    Matcher m = p.matcher("3.00x850");
    while (m.find()) {
      System.out.println(m.group());
    }
  }
}
