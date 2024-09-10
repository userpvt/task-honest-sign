package com.app;


import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final AtomicInteger currentRequestCount = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;

        // Сбрасываем счетчик запросов по истечении времени
        scheduler.scheduleAtFixedRate(() -> {
            currentRequestCount.set(0);
        }, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    // Метод для создания документа
    public synchronized String createDocument(Document document, String signature) throws IOException, InterruptedException {
        // Ожидание, если лимит запросов достигнут
        while (currentRequestCount.get() >= requestLimit) {
            wait(timeUnit.toMillis(1)); // Блокируем поток до освобождения лимита
        }

        // Инкремент количества запросов
        currentRequestCount.incrementAndGet();

        // Отправка запроса
        Gson gson = new Gson();
        String requestBody = gson.toJson(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        notifyAll(); // Уведомляем другие потоки после завершения запроса
        return response.body();
    }

    // Внутренний класс документа
    public static class Document {
        private final String participantInn;
        private final String docId;
        private final String docStatus = "DRAFT";
        private final String docType = "LP_INTRODUCE_GOODS";
        private final boolean importRequest = true;
        private final String ownerInn;
        private final String producerInn;
        private final String productionDate;
        private final String productionType;
        private final Product[] products;
        private final String regDate;
        private final String regNumber;

        public Document(String participantInn, String docId, String ownerInn, String producerInn,
                        String productionDate, String productionType, Product[] products, String regDate, String regNumber) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public static class Product {
            private final String certificateDocument;
            private final String certificateDocumentDate;
            private final String certificateDocumentNumber;
            private final String ownerInn;
            private final String producerInn;
            private final String productionDate;
            private final String tnvedCode;
            private final String uitCode;
            private final String uituCode;

            public Product(String certificateDocument, String certificateDocumentDate, String certificateDocumentNumber,
                           String ownerInn, String producerInn, String productionDate, String tnvedCode,
                           String uitCode, String uituCode) {
                this.certificateDocument = certificateDocument;
                this.certificateDocumentDate = certificateDocumentDate;
                this.certificateDocumentNumber = certificateDocumentNumber;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.productionDate = productionDate;
                this.tnvedCode = tnvedCode;
                this.uitCode = uitCode;
                this.uituCode = uituCode;
            }
        }
    }
}
