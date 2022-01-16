package carlos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class HashComparator {
    private int max = 0;
    private final Path source;
    private final Path result = Paths.get("result.txt");

    public HashComparator(Path source) {
        this.source = source;
    }

    public static void main(String[] args) {
        new HashComparator(Path.of("")).compare();
    }

    public void compare() {
        try {
            List<String> sentences = List.of("It has spectacularly backfired.","Push to change it.");
            List<String> sha = sentences.stream().map(HashComparator::sha256).toList();
            compareHashes(sentences, sha);
            System.out.println("Finished!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String sha256(String input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        var salt = "CS210+".getBytes(UTF_8);
        Objects.requireNonNull(md).update(salt);
        var words = md.digest(input.getBytes(UTF_8));
        var sb = new StringBuilder();
        for (byte bit : words)
            sb.append(Integer.toString((bit & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    public int count (String a, String b) {
        return (int) IntStream.range(0, a.length()).parallel()
                .filter(i -> a.charAt(i) == b.charAt(i)).count();
    }

    private void compareAndUpdate(BufferedWriter w, List<String> sentences, List<String> sha, int i, int j) {
        String a = sentences.get(i), aHash = sha.get(i), b = sentences.get(j), bHash = sha.get(j);
        int count = count(aHash, bHash);
        if (count >= max) {
            max = writeToConsoleAndUpdate(a, b, count);
            writeToFile(w, a, b);
        }
    }

    private int writeToConsoleAndUpdate(String a, String b, int count) {
        System.out.println(a);
        System.out.println(b);
        System.out.println(count);
        return count;
    }

    private void writeToFile(BufferedWriter w, String a, String b) {
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

    private void compareHashes(List<String> sentences, List<String> sha) throws IOException {
        var option = Files.exists(result) ? APPEND : CREATE;
        try(var w = Files.newBufferedWriter(result, option)) {
            IntStream.range(0, sentences.size()).parallel()
                    .forEach(i -> IntStream.range(i + 1, sentences.size()).parallel()
                            .forEach(j -> compareAndUpdate(w, sentences, sha, i, j))
                    );
        }
    }
}
