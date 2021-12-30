import carlos.webcrawler.WebCrawler;
import carlos.webcrawler.WebCrawlerBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {

    static int max = 0;

    public static String sha256(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        var salt = "CS210+".getBytes(UTF_8);
        Objects.requireNonNull(md).update(salt);
        var data = md.digest(input.getBytes(UTF_8));
        var sb = new StringBuilder();
        for (byte datum : data)
            sb.append(Integer.toString((datum & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    public static int count (String a, String b) {
        int count = 0;
        for(int i = 0;i < a.length() && i < b.length(); i++)
            count += a.charAt(i) == b.charAt(i) ? 1 : 0;
        return count;
    }

    public static void main(String[] args) throws IOException {
        compareHashes(new WebCrawlerBuilder("https://en.wikipedia.org/wiki/Main_Page")
                .sentenceLimit(1_000_000_000)
                .withMinLength(3)
                .withMaxLength(10)
                .debugMode(true)
                .build()
                .start(5)
        );
    }

    private static void compareHashes(WebCrawler wc) throws IOException {
        try(var w = Files.newBufferedWriter(Paths.get("out.txt"))) {
            var sentences = wc.getSentences();
            wc.writeResultantSentencesToFile(sentences, Paths.get("result.txt"));
            IntStream.range(0, sentences.size()).parallel()
                    .forEach(i -> IntStream.range(i + 1, sentences.size()).parallel()
                            .forEach(j -> compareAndUpdate(w, sentences, i, j))
                    );
        }
    }

    private static void compareAndUpdate(BufferedWriter w, List<String> sentences, int i, int j) {
        String a = sentences.get(i), aHash = sha256(a), b = sentences.get(j), bHash = sha256(b);
        int count = count(aHash, bHash);
        if (count >= max) {
            max = writeToConsoleAndUpdate(a, b, count);
            writeToFile(w, max, a, b);
        }
    }

    private static int writeToConsoleAndUpdate(String a, String b, int count) {
        System.out.println(a);
        System.out.println(b);
        System.out.println(count);
        return count;
    }

    private static void writeToFile(BufferedWriter w, int max, String a, String b) {
        try {
            w.write(a);
            w.newLine();
            w.write(b);w.newLine();
            w.write(max + "\n--------------------------");
            w.newLine();
            w.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}