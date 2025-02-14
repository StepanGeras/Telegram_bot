package org.example.telegram_bot.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.example.telegram_bot.entity.User;
import org.springframework.stereotype.Service;

@Service
public class WordService {

  public File generateWordDocument(User user) throws Exception {
    XWPFDocument document = new XWPFDocument();

    XWPFParagraph title = document.createParagraph();
    title.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun titleRun = title.createRun();
    titleRun.setText("Персональные данные");
    titleRun.setBold(true);
    titleRun.setFontSize(16);

    createParagraph(document, "ФИО: " + user.getFullName());
    createParagraph(document, "Дата рождения: " + user.getBirthDate());
    createParagraph(document, "Пол: " + user.getGender());

    if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
      createParagraph(document, "Фото:");
      insertImage(document, user.getImageUrl());
    } else {
      createParagraph(document, "Фото: Нет");
    }

    File file = File.createTempFile("user_" + user.getId(), ".docx");
    try (FileOutputStream out = new FileOutputStream(file)) {
      document.write(out);
    }
    document.close();

    return file;
  }

  private void createParagraph(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setText(text);
    run.setFontSize(14);
  }

  private void insertImage(XWPFDocument document, String imageUrl) {
    try (InputStream inputStream = new URL(imageUrl).openStream()) {
      XWPFParagraph imageParagraph = document.createParagraph();
      XWPFRun imageRun = imageParagraph.createRun();

      File tempImage = File.createTempFile("image", ".jpg");
      try (FileOutputStream outputStream = new FileOutputStream(tempImage)) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
      }

      imageRun.addPicture(new FileInputStream(tempImage), XWPFDocument.PICTURE_TYPE_JPEG,
          tempImage.getName(), Units.toEMU(150), Units.toEMU(150));
      tempImage.delete();
    } catch (Exception e) {
      createParagraph(document, "Ошибка загрузки фото");
    }
  }

}
