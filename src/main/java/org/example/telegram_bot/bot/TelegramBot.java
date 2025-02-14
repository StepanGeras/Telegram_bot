package org.example.telegram_bot.bot;

import java.util.Comparator;
import java.util.List;
import org.example.telegram_bot.entity.User;
import org.example.telegram_bot.service.UserService;
import org.example.telegram_bot.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class TelegramBot extends TelegramLongPollingBot {

  private static final String CONSENT_YES = "consent_yes";
  private static final String GENDER_MALE = "gender_male";
  private static final String GENDER_FEMALE = "gender_female";

  @Value("${bot.username}")
  private String BOT_USERNAME;

  @Value("${bot.token}")
  private String BOT_TOKEN;

  private final UserService userService;
  private final WordService wordService;

  @Autowired
  public TelegramBot(UserService userService, WordService wordService) {
    this.userService = userService;
    this.wordService = wordService;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      handleTextMessage(update);
    } else if (update.hasCallbackQuery()) {
      handleCallbackQuery(update);
    } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
      handlePhoto(update.getMessage());
    }
  }

  private void handleTextMessage(Update update) {
    String chatId = update.getMessage().getChatId().toString();
    String text = update.getMessage().getText();
    int step = State.getStep(Long.parseLong(chatId));

    if (text.startsWith("/start")) {
      String utm = text.replace("/start", "").trim();
      User user = new User();
      user.setId(Long.valueOf(chatId));
      user.setUtm(utm);
      userService.save(user);
    }

    switch (step) {
      case 0 -> askConsent(chatId);
      case 1 -> askFullName(chatId, text);
      case 2 -> askBirthDate(chatId, text);
      case 3 -> askGender(chatId);
      case 4 -> handlePhoto(update.getMessage());
    }
  }

  private void handleCallbackQuery(Update update) {
    String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
    String data = update.getCallbackQuery().getData();

    if (CONSENT_YES.equals(data)) {
      User user = userService.findById(Long.valueOf(chatId));
      user.setConsent(true);
      userService.save(user);
      sendTextMessage(chatId, "Введите ваше ФИО:");
      State.setStep(Long.parseLong(chatId), 1);
    } else if (GENDER_MALE.equals(data) || GENDER_FEMALE.equals(data)) {
      User user = userService.findById(Long.valueOf(chatId));
      user.setGender(GENDER_MALE.equals(data) ? "Мужской" : "Женский");
      userService.save(user);

      sendTextMessage(chatId, "Загрузите вашу фотографию:");
      State.setStep(Long.parseLong(chatId), 4);
    }
  }

  private void askConsent(String chatId) {
    SendMessage message = new SendMessage(chatId, "Согласны на обработку персональных данных?");
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    List<InlineKeyboardButton> row = List.of(
        InlineKeyboardButton.builder().text("✅ Согласен").callbackData("consent_yes").build(),
        InlineKeyboardButton.builder().text("📜 Подробнее").url("https://example.com").build()
    );
    markup.setKeyboard(List.of(row));
    message.setReplyMarkup(markup);
    executeMessage(message);
  }

  private void askFullName(String chatId, String text) {
    if (text.trim().split(" ").length < 2) {
      sendTextMessage(chatId, "Введите ФИО полностью (Фамилия Имя):");
      return;
    }
    User user = userService.findById(Long.valueOf(chatId));
    user.setFullName(text);
    userService.save(user);

    sendTextMessage(chatId, "Введите дату рождения (в формате dd.MM.yyyy):");
    State.setStep(Long.parseLong(chatId), 2);
  }

  private void askBirthDate(String chatId, String text) {
    if (!text.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
      sendTextMessage(chatId, "Неверный формат. Введите дату рождения (dd.MM.yyyy):");
      return;
    }
    User user = userService.findById(Long.valueOf(chatId));
    user.setBirthDate(text);
    userService.save(user);
    askGender(chatId);
  }

  private void askGender(String chatId) {
    SendMessage message = new SendMessage(chatId, "Выберите пол:");
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    List<InlineKeyboardButton> row = List.of(
        InlineKeyboardButton.builder().text("👨 Мужской").callbackData("gender_male").build(),
        InlineKeyboardButton.builder().text("👩 Женский").callbackData("gender_female").build()
    );
    markup.setKeyboard(List.of(row));
    message.setReplyMarkup(markup);
    executeMessage(message);
  }

  private void handlePhoto(Message message) {
    Long userId = message.getFrom().getId();
    List<PhotoSize> photos = message.getPhoto();

    PhotoSize bestPhoto = photos.stream()
                                .max(Comparator.comparing(PhotoSize::getFileSize))
                                .orElse(null);

    if (bestPhoto == null)
      return;

    try {
      GetFile getFile = new GetFile();
      getFile.setFileId(bestPhoto.getFileId());
      File file = execute(getFile);

      String filePath = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

      User user = userService.findById(userId);
      user.setImageUrl(filePath);
      userService.save(user);

      sendTextMessage(userId.toString(), "Ваше фото сохранено!");
      State.setStep(userId, 5);

      java.io.File wordFile = wordService.generateWordDocument(user);

      sendDocument(userId.toString(), wordFile);
    }
    catch (Exception e) {
      sendTextMessage(userId.toString(), "Ошибка при обработке фото. Попробуйте снова.");
    }
  }

  private void sendDocument(String chatId, java.io.File wordFile) {
    SendDocument sendDocument = new SendDocument();
    sendDocument.setChatId(chatId);
    sendDocument.setDocument(new InputFile(wordFile));

    try {
      execute(sendDocument);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendTextMessage(String chatId, String text) {
    SendMessage message = new SendMessage(chatId, text);
    executeMessage(message);
  }

  private void executeMessage(SendMessage message) {
    try {
      execute(message);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getBotUsername() {
    return BOT_USERNAME;
  }

  @Override
  public String getBotToken() {
    return BOT_TOKEN;
  }

}
