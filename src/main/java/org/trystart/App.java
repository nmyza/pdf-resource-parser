package org.trystart;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class App {
    private static int THREAD_COUNT = 8;
    public static void main(String[] args) throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(THREAD_COUNT);
        for (String url: loadURLs()) {
            if(StringUtils.isNoneBlank(url)) {
                service.execute(() -> {
                    try {
                        parse(url, "pdf_" + parseFileNameFrom(url));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        service.shutdown();
    }

    private static String parseFileNameFrom(String url) throws IOException {
        String [] decodedURL = URLDecoder.decode(url, "UTF-8").split("/");
        String name = decodedURL[decodedURL.length - 1];
        return name.replaceAll(".pdf", "").replaceAll("\\?", "_");
    }

    private static List<String> loadURLs() throws Exception {
        List<String> result = null;
        URL urlName = Thread.currentThread().getContextClassLoader().getResource("URLs.txt");
        if (Objects.nonNull(urlName)) {
            URI fileName = urlName.toURI();
            try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
                result = stream.collect(Collectors.toList());
            }
        }
        return result;
    }

    private static void parse(String url, String fileName) throws IOException {
        PDDocument pdDoc = loadPDF(url);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdDoc).replaceAll(" ", ",");
        pdDoc.close();
        Files.write(Paths.get("out/" + fileName + ".csv"), text.getBytes());
    }

    private static PDDocument loadPDF(String url)  throws IOException {
        URLConnection connection = (new URL(url)).openConnection();
        connection.addRequestProperty(USER_AGENT, "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        connection.connect();
        return PDDocument.load(connection.getInputStream());
    }
}
