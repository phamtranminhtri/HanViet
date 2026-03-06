package org.example.hanviet;

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class OcrService {
    private final CloseableHttpClient closeableHttpClient;
    private final HttpClient httpClient;
    private final String ocrSpaceApiKey;

    @Autowired
    public OcrService(CloseableHttpClient closeableHttpClient, HttpClient httpClient, String ocrSpaceApiKey) {
        this.closeableHttpClient = closeableHttpClient;
        this.httpClient = httpClient;
        this.ocrSpaceApiKey = ocrSpaceApiKey;
    }

//    public void compressImage(String inputFilePath, String outputFilePath) throws IOException {
//        File inputFile = new File(inputFilePath);
//        BufferedImage bufferedImage = ImageIO.read(inputFile);
//        Iterator<ImageWriter> imageWriterIterator = ImageIO.getImageWritersByFormatName("jpg");
//        ImageWriter imageWriter = imageWriterIterator.next();
//
//        File outputFile = new File(outputFilePath);
//        ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile);
//        imageWriter.setOutput(outputStream);
//
//        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
//        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//        imageWriteParam.setCompressionQuality(0.5F);
//
//        imageWriter.write(
//                null,
//                new IIOImage(bufferedImage, null, null),
//                imageWriteParam
//        );
//        outputStream.close();
//        imageWriter.dispose();
//    }

    public void compressImage(String inputFilePath, String outputFilePath) throws IOException {
        File inputFile = new File(inputFilePath);
        BufferedImage sourceImage = ImageIO.read(inputFile);

        // Create a new image with the correct color space (RGB)
        // This strips the Alpha channel that causes the "Bogus" error
        BufferedImage rgbImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        // Draw the source image onto the new RGB canvas
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(sourceImage, 0, 0, Color.WHITE, null); // Use White background for transparency
        g2d.dispose();

        Iterator<ImageWriter> imageWriterIterator = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter imageWriter = imageWriterIterator.next();

        File outputFile = new File(outputFilePath);
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile)) {
            imageWriter.setOutput(outputStream);

            ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
            imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imageWriteParam.setCompressionQuality(0.5F);

            // Write the NEW rgbImage, not the sourceImage
            imageWriter.write(
                    null,
                    new IIOImage(rgbImage, null, null),
                    imageWriteParam
            );
        } finally {
            imageWriter.dispose();
        }
    }

    public String parseOcr(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray parsedResults = jsonObject.getJSONArray("ParsedResults");
        JSONObject parsedResult = parsedResults.getJSONObject(0);
        return parsedResult.getString("ParsedText");
    }

    public List<String> parseHanviet(String html) {
        Document document = Jsoup.parse(html);
        Elements elements = document.select("span.hvres-goto-link");
        List<String> sounds = new ArrayList<>();
        for (int i = 0; i < elements.size() / 2; i++) {
            sounds.add(elements.get(i).text());
        }
        return sounds;
    }

    public String parseTranslation(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject responseData = jsonObject.getJSONObject("responseData");
        return responseData.getString("translatedText");
    }

    public String filterHanzi(String rawHanzi) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < rawHanzi.length(); i++) {
            int codePoint = rawHanzi.codePointAt(i);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                stringBuilder.append(rawHanzi.charAt(i));
            }
        }
        return stringBuilder.toString();
    }

    public String doOcrImage(String imagePath) throws IOException, ParseException {
        HttpEntity multiPartHttpEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", new File(imagePath))
                .addTextBody("scale", "true")
                .addTextBody("OCREngine", "3")
                .build();

        ClassicHttpRequest multiPartRequest = ClassicRequestBuilder
                .post("https://api.ocr.space/parse/image")
                .setHeader("apikey", ocrSpaceApiKey)
                .setEntity(multiPartHttpEntity)
                .build();

        CloseableHttpResponse httpResponse = closeableHttpClient.execute(multiPartRequest);

        return EntityUtils.toString(httpResponse.getEntity());
    }

    public String getHanviet(char hanzi) throws URISyntaxException, IOException, InterruptedException {
        String uri = "https://hvdic.thivien.net/whv/"
                + URLEncoder.encode(String.valueOf(hanzi), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public List<List<String>> getHanviets(String hanzis)
            throws URISyntaxException, IOException, InterruptedException
    {
        List<List<String>> hanviets = new ArrayList<>();
        for (char hanzi : hanzis.toCharArray()) {
            String hanvietHtml = getHanviet(hanzi);
            List<String> parsedHanviet = parseHanviet(hanvietHtml);
            hanviets.add(parsedHanviet);
        }
        return hanviets;
    }

    public String translate(String chineseText) throws URISyntaxException, IOException, InterruptedException {
        String language = URLEncoder.encode("zh|vi", StandardCharsets.UTF_8);
        String textEncoded = URLEncoder.encode(chineseText, StandardCharsets.UTF_8);
        String uri = "https://api.mymemory.translated.net/get?langpair=%s&q=%s".formatted(language, textEncoded);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
