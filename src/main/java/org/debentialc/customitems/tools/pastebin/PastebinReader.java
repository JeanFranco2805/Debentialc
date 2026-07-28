package org.debentialc.customitems.tools.pastebin;

import org.bukkit.Bukkit;
import org.debentialc.service.CC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PastebinReader {
    private final static Pattern pastebinPattern = Pattern.compile("^https?://(?:www\\.)?pastebin\\.com/(?:raw/)?([a-zA-Z0-9]+)$");

    /**
     * Obtiene el contenido de un pastebin desde su URL
     * @param pastebinUrl URL del pastebin (puede ser raw o normal)
     * @return Lista de líneas del pastebin, o null si falla
     */
    public static List<String> getFromPastebin(String pastebinUrl) {
        Matcher matcher = pastebinPattern.matcher(pastebinUrl);
        if (!matcher.matches()) {
            return null;
        }
        String pastebinId = matcher.group(1);
        String rawPastebinUrl = "https://pastebin.com/raw/" + pastebinId;
        return downloadPastebinContent(rawPastebinUrl);
    }

    /**
     * Descarga el contenido de una URL de pastebin raw
     * @param pastebinUrl URL raw del pastebin
     * @return Lista de líneas, o null si falla
     */
    public static List<String> downloadPastebinContent(String pastebinUrl) {
        List<String> lines = new ArrayList<>();

        try {
            URL url = new URL(pastebinUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.replace("\r", "").trim();
                        if (line.isEmpty()) {
                            lines.add("");
                        } else {
                            lines.add(CC.translate(line));
                        }
                    }
                }
                return lines;
            } else {
                Bukkit.getLogger().log(Level.WARNING, "Pastebin Error! Server returned HTTP code: {0} for URL: {1}", new Object[]{responseCode, pastebinUrl});
                return null;
            }
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error getting pastebin content: " + e.getMessage(), e);
            return null;
        }
    }
}