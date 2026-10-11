package com.machinesense.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends Telegram bot messages when a maintenance alert is triggered.
 *
 * Setup:
 *  1. Message @BotFather on Telegram → /newbot → get BOT_TOKEN
 *  2. Message your bot or add it to a group, then hit:
 *     https://api.telegram.org/bot<TOKEN>/getUpdates  → copy chat_id
 *  3. Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID env vars (or application.yml).
 *
 * If either value is blank the service silently skips sending (safe for dev).
 */
@Service
public class TelegramAlertService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertService.class);

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void sendAlert(String machineName, String machineCode,
                          String riskLevel, int healthScore,
                          double failureProbability, String failureType) {

        if (botToken.isBlank() || chatId.isBlank()) {
            log.info("Telegram not configured — skipping alert for {}", machineCode);
            return;
        }

        String emoji = "CRITICAL".equals(riskLevel) ? "🔴" : "🟡";
        String failureNote = (failureType != null && !failureType.isBlank())
                ? "\n⚠️ Predicted failure: *" + failureType + "*"
                : "";

        String text = String.format(
                "%s *MachineSense Alert* %s\n\n" +
                "🏭 Machine: *%s* (%s)\n" +
                "📊 Risk level: *%s*\n" +
                "❤️ Health score: *%d/100*\n" +
                "📈 Failure probability: *%.1f%%*" +
                "%s\n\n" +
                "👷 A maintenance ticket has been automatically created.\n" +
                "Please inspect the machine immediately.",
                emoji, emoji,
                machineName, machineCode,
                riskLevel,
                healthScore,
                failureProbability * 100,
                failureNote
        );

        sendMessage(text);
    }

    public void sendTestAlert(String machineName, String machineCode) {
        if (botToken.isBlank() || chatId.isBlank()) {
            log.info("Telegram not configured — skipping test alert for {}", machineCode);
            return;
        }

        String text = String.format(
                "🧪 *MachineSense Demo Alert*\n\n" +
                "🏭 Machine: *%s* (%s)\n" +
                "📊 Risk level: *WARNING*\n" +
                "❤️ Health score: *62/100*\n" +
                "📈 Failure probability: *35.0%%*\n" +
                "⚠️ Predicted failure: *Heat Dissipation*\n\n" +
                "👷 This is a simulated alert from the MachineSense demo.\n" +
                "Telegram alerts are working correctly! ✅",
                machineName, machineCode
        );

        sendMessage(text);
    }

    private void sendMessage(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                    botToken, chatId, encoded
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Telegram alert sent successfully");
            } else {
                log.warn("Telegram alert failed: HTTP {} — {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send Telegram alert: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
