package org.example.telegram_bot.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.TelegramBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class Config {

  @Bean
  public TelegramBotsApi telegramBotsApi(TelegramBot bot) throws Exception {
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
    telegramBotsApi.registerBot((LongPollingBot) bot);
    return telegramBotsApi;
  }

}
