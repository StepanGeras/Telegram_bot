FROM openjdk:21
WORKDIR /app
COPY build/libs/*.jar bot.jar
CMD ["java", "-jar", "bot.jar"]

