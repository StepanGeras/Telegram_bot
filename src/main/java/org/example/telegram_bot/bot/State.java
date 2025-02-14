package org.example.telegram_bot.bot;

import java.util.HashMap;
import java.util.Map;

public class State {

  private static final Map<Long, Integer> userSteps = new HashMap<>();

  public static void setStep(Long chatId, int step) {
    userSteps.put(chatId, step);
  }

  public static int getStep(Long chatId) {
    return userSteps.getOrDefault(chatId, 0);
  }

}
