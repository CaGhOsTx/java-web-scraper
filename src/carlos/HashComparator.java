package carlos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HashComparator {

    public void compare(Path p) {
        try(var writer = Files.newBufferedWriter(Path.of("results.txt"))) {
            int max = 0;
            List<String> sentences = Files.lines(p).collect(Collectors.toSet()).stream().toList(),
                    sha = sentences.stream().map(HashComparator::sha256).toList();
            for(int i = 0; i < sentences.size(); i++) {
                for (int j = i + 1; j < sentences.size(); j++) {
                    int count = count(sha.get(i), sha.get(j));
                    if(count >= max) {
                        max = count;
                        System.out.println(sentences.get(i));
                        System.out.println(sentences.get(j));
                        System.out.println(count);
                        writer.write(sentences.get(i));writer.newLine();
                        writer.write(sentences.get(j));writer.newLine();
                        writer.write(count + "");writer.newLine();
                    }
                }
            }
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

    public static int count (String a, String b) {
        int count = 0;
        for(int i = 0;i < a.length() && i < b.length(); i++)
            count += a.charAt(i) == b.charAt(i) ? 1 : 0;
        return count;
    }
}
