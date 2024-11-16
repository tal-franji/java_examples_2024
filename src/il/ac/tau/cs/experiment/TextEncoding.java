package il.ac.tau.cs.experiment;

import java.nio.charset.StandardCharsets;

public class TextEncoding {
    public static String binDump(byte[] data) {
        StringBuilder dump = new StringBuilder();
        for (byte b: data) {
            dump.append(String.format("%02X ", b));
        }
        return dump.toString();
    }

    public static void main(String[] args) {
        String text = "hello";
        System.out.println("Hello in UTF-8");
        System.out.println(binDump(text.getBytes(StandardCharsets.UTF_8)));
        System.out.println("Hello in ASCII");
        System.out.println(binDump(text.getBytes(StandardCharsets.US_ASCII)));
        text = "שלום";
        System.out.println("Hebrew Shalom in UTF-8");
        System.out.println(binDump(text.getBytes(StandardCharsets.UTF_8)));
        System.out.println("Hebrew Shalom in ASCII");
        System.out.println(binDump(text.getBytes(StandardCharsets.US_ASCII)));

    }
}
