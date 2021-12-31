package carlos.webcrawler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class HashComparator {
    private int max = 0;
    private Path p;
    private Path result = Paths.get("result.txt");

    public HashComparator(Path p) {
        this.p = p;
    }

    public void start() throws IOException {
        List<String> a = List.of(""), b = List.of("");
        var br = Files.lines(p).limit(10_000);
        int i = 0, j = 0;
        while(!a.isEmpty()) {
            br = Files.lines(p).limit(10_000).skip(i);
            a = br.toList();
            printCompareSingle(i, a.size());
            i += a.size();
            compareHashes(a);
            while(!b.isEmpty()) {
                br = Files.lines(p).limit(10_000).skip(i + j);
                b = br.toList();
                printCompareSingle(i + j, i + j + b.size());
                j += b.size();
                compareHashes(b);
                System.out.println("Comparing both");
                compareHashes(a,b);
            }
        }
        System.out.println("Finished!");
    }

    private void printCompareSingle(int i, int j) {
        System.out.println("Comparing range " + i + "-" + j);
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
        var data = md.digest(input.getBytes(UTF_8));
        var sb = new StringBuilder();
        for (byte datum : data)
            sb.append(Integer.toString((datum & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    public int count (String a, String b) {
        int count = 0;
        for(int i = 0;i < a.length() && i < b.length(); i++)
            count += a.charAt(i) == b.charAt(i) ? 1 : 0;
        return count;
    }

    private void compareAndUpdate(BufferedWriter w, List<String> first, int i, int j) {
        String a = first.get(i), aHash = sha256(a), b = first.get(j), bHash = sha256(b);
        int count = count(aHash, bHash);
        if (count >= max) {
            max = writeToConsoleAndUpdate(a, b, count);
            writeToFile(w, a, b);
        }
    }

    private void compareAndUpdate(BufferedWriter w, List<String> first, List<String> second, int i, int j) {
        String a = first.get(i), aHash = sha256(a), b = second.get(j), bHash = sha256(b);
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

    private void compareHashes(List<String> first) throws IOException {
        var option = Files.exists(result) ? APPEND : CREATE;
        try(var w = Files.newBufferedWriter(result, option)) {
            IntStream.range(0, first.size()).parallel()
                    .forEach(i -> IntStream.range(i + 1, first.size()).parallel()
                            .forEach(j -> compareAndUpdate(w, first, i, j))
                    );
        }
    }

    private void compareHashes(List<String> first, List<String> second) throws IOException {
        var option = Files.exists(result) ? APPEND : CREATE;
        try(var w = Files.newBufferedWriter(result, option)) {
            IntStream.range(0, first.size()).parallel()
                    .forEach(i -> IntStream.range(i + 1, second.size()).parallel()
                            .forEach(j -> compareAndUpdate(w, first, second, i, j))
                    );
        }
    }
}
