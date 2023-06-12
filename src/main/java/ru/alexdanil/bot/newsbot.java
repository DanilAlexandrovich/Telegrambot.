package ru.alexdanil.bot;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class newsbot extends TelegramLongPollingBot {

    private final String apiKey = "4fd4a504bfe94c51b5eb9f6a6d4702b6";
    private final int pageSize = 5;
    private int currentPage = 1;
    private List<MyMessageSender.Article> articles;

    @Override

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String query = message.getText();

            if (query.equals("/start")) {
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendTextMessage(chatId, "Привет! Я бот для новостей, выбери тему:");
                sendTopicsKeyboard(chatId);
            } else if (query.equals("/next") && currentPage * pageSize < articles.size()) {
                currentPage++;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            } else if (query.equals("/prev") && currentPage > 1) {
                currentPage--;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            } else {
                articles = getNewsArticles(query);
                currentPage = 1;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String data = query.getData();
            String chatId = query.getMessage().getChatId().toString();

            if (data.equals("sport") || data.equals("politics") || data.equals("economy") || data.equals("local_news")) {
                articles = getNewsArticles(data);
                currentPage = 1;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            } else if (data.equals("/prev") && currentPage > 1) {
                currentPage--;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            } else if (data.equals("/next") && currentPage * pageSize < articles.size()) {
                currentPage++;
                MyMessageSender sender = new MyMessageSender(this);
                sender.sendArticlesPage(chatId, articles, pageSize, currentPage);
            }
        }

    }

    private void sendTopicsKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Создаем ряд кнопок для каждой темы
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton sportButton = new KeyboardButton("Спорт");
        sportButton.setRequestContact(false);
        row1.add(sportButton);

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton politicsButton = new KeyboardButton("Политика");
        politicsButton.setRequestContact(false);
        row2.add(politicsButton);

        KeyboardRow row3 = new KeyboardRow();
        KeyboardButton economyButton = new KeyboardButton("Экономика");
        economyButton.setRequestContact(false);
        row3.add(economyButton);

        KeyboardRow row4 = new KeyboardRow();
        KeyboardButton localNewsButton = new KeyboardButton("СВО");
        localNewsButton.setRequestContact(false);
        row4.add(localNewsButton);

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);

        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setKeyboard(keyboardRows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выбери тему:");
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private List<MyMessageSender.Article> getNewsArticles(String query) {
        // Создаем строку URL-адреса запроса к News API
        String apiUrl = "https://newsapi.org/v2/everything?q=" + query + "&apiKey=" + apiKey;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();

            // Парсим JSON-ответ и извлекаем статьи
            Gson gson = new Gson();
            MyMessageSender.NewsApiResponse newsApiResponse = gson.fromJson(responseBody, MyMessageSender.NewsApiResponse.class);
            return newsApiResponse.getArticles();

        } catch (IOException e) {
            System.out.println("Ошибка при выполнении HTTP-запроса к News API: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return "AMDAnewsbot";
    }

    @Override
    public String getBotToken() {
        return "6203454012:AAFMRQGplWnE_bRIjZ4zedEfDzFLfq_D9io";
    }
}

class MyMessageSender {
    private final newsbot bot;

    public MyMessageSender(newsbot bot) {
        this.bot = bot;
    }

    public void sendTextMessage(String chatId, String text) {
        int maxMessageSize = 4000; // максимальный размер сообщения
        List<String> chunks = new ArrayList<>();

        // разбиваем текст на куски
        for (int i = 0; i < text.length(); i += maxMessageSize) {
            String chunk = text.substring(i);
            if (chunk.length() > maxMessageSize) {
                chunk = chunk.substring(0, maxMessageSize);
            }
            chunks.add(chunk);
        }

        // отправляем каждый кусок в виде отдельного сообщения
        for (String chunk : chunks) {
            SendMessage sendMessage = new SendMessage(chatId, chunk);
            try {
                bot.execute(sendMessage);
                System.out.println("Отправлено сообщение \"" + chunk + "\" в чат " + chatId);
            } catch (TelegramApiException e) {
                System.out.println("Не удалось отправить сообщение \"" + chunk + "\" в чат " + chatId);
                e.printStackTrace();
            }
        }
    }

    public void sendHtmlMessage(String chatId, String message) {
        int maxMessageSize = 4000; // ограничение на максимальный размер сообщения

        if (message.length() <= maxMessageSize) {
            SendMessage sendMessage = new SendMessage(chatId, message);
            sendMessage.setParseMode(ParseMode.HTML);
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println("Ошибка при отправке сообщения с HTML: " + e.getMessage());
            }
        } else {
            List<String> messageChunks = splitStringIntoChunks(message, maxMessageSize);

            for (String chunk : messageChunks) {
                SendMessage sendMessage = new SendMessage(chatId, chunk);
                sendMessage.setParseMode(ParseMode.HTML);
                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException e) {
                    System.out.println("Ошибка при отправке сообщения с HTML: " + e.getMessage());
                }
            }
        }
    }

    private List<String> splitStringIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
            chunks.add(chunk);
        }

        return chunks;
    }

    class NewsApiResponse {
        private List<Article> articles;

        public NewsApiResponse(List<Article> articles) {
            this.articles = articles;
        }

        public List<Article> getArticles() {
            return articles;
        }
    }

    public void sendArticlesPage(String chatId, List<Article> articles, int pageSize, int page) {
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, articles.size());

        StringBuilder messageBuilder = new StringBuilder("Вот главные заголовки:\n\n");
        for (int i = startIndex; i < endIndex; i++) {
            Article article = articles.get(i);
            messageBuilder.append("<a href=\"")
                    .append(article.getUrl())
                    .append("\">")
                    .append(article.getTitle())
                    .append("</a>\n\n");
        }

        boolean hasPrevPage = page > 1;
        boolean hasNextPage = endIndex < articles.size();

        // Создаем инлайн-клавиатуру с кнопками "Prev" и "Next"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        if (hasPrevPage) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("Prev");
            prevButton.setCallbackData("/prev");
            row.add(prevButton);
        }

        if (hasNextPage) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Next");
            nextButton.setCallbackData("/next");
            row.add(nextButton);
        }

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        // Отправляем сообщение с инлайн-клавиатурой
        SendMessage sendMessage = new SendMessage(chatId, messageBuilder.toString());
        sendMessage.setParseMode(ParseMode.HTML);
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    public static class Article {
        private final String title;
        private final String url;

        public Article(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}
