package com.example.eventfinder.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for storing and retrieving raw HTML from web scraping.
 * Keeps local copies for offline re-parsing and reduces repeated network requests.
 */
@Service
public class HtmlStorage {
    private static final Logger logger = LoggerFactory.getLogger(HtmlStorage.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${scraper.html-storage-path:./scraper-data}")
    private String storagePath;

    /**
     * Save raw HTML content with a timestamp
     * Format: {storagePath}/{website}_{date}.html
     */
    public void saveHtml(String website, String html) throws IOException {
        String filename = generateFilename(website);
        Path filepath = Paths.get(storagePath, filename);

        // Ensure directory exists
        Files.createDirectories(filepath.getParent());

        // Save HTML
        Files.write(filepath, html.getBytes(StandardCharsets.UTF_8));
        logger.info("Saved HTML for {} to: {}", website, filepath.toAbsolutePath());
    }

    /**
     * Load HTML from disk if it exists
     */
    public String loadHtml(String website) throws IOException {
        String filename = generateFilename(website);
        Path filepath = Paths.get(storagePath, filename);

        if (Files.exists(filepath)) {
            String html = Files.readString(filepath, StandardCharsets.UTF_8);
            logger.info("Loaded cached HTML for {} from: {}", website, filepath.toAbsolutePath());
            return html;
        }

        return null;
    }

    /**
     * Check if cached HTML exists for today
     */
    public boolean exists(String website) {
        String filename = generateFilename(website);
        Path filepath = Paths.get(storagePath, filename);
        return Files.exists(filepath);
    }

    /**
     * Delete old HTML files (older than X days)
     */
    public void cleanup(int daysOld) {
        try {
            File dir = new File(storagePath);
            if (!dir.exists()) {
                return;
            }

            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }

            long now = System.currentTimeMillis();
            long cutoff = now - (daysOld * 24L * 60L * 60L * 1000L);

            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoff) {
                    if (file.delete()) {
                        logger.info("Deleted old HTML file: {}", file.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up old HTML files", e);
        }
    }

    /**
     * Generate filename based on website and today's date
     */
    private String generateFilename(String website) {
        String date = LocalDate.now().format(DATE_FORMAT);
        return website.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + date + ".html";
    }

    /**
     * List all stored HTML files
     */
    public File[] listStoredHtml() {
        File dir = new File(storagePath);
        if (!dir.exists()) {
            return new File[]{};
        }
        return dir.listFiles((d, name) -> name.endsWith(".html"));
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
}
