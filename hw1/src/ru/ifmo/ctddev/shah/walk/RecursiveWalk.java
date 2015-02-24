package ru.ifmo.ctddev.shah.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args != null && args.length == 2 && args[0] != null && args[1] != null) {
            runHashFiles(args[0], args[1]);
        } else {
            System.err.println("Invalid usage");
        }
    }

    public static void runHashFiles(String nameToRead, String nameToWrite) {

        File fileToRead = new File(nameToRead);
        File fileToWrite = new File(nameToWrite);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead), "UTF-8"));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWrite), "UTF-8"))) {
            String text;
            while ((text = reader.readLine()) != null) {
                    recursiveWalk(text, writer);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    public static void recursiveWalk(final String filename, final BufferedWriter writer) throws IOException {
        Path path = Paths.get(filename);

        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    int hash = 0;

                    try {
                        hash = hashFile(file.toString());
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }

                    writer.write(String.format("%08x", hash) + " " + file.toString());
                    writer.newLine();

                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    writer.write(String.format("%08x", 0) + " " + file.toString());
                    writer.newLine();
                    return super.visitFileFailed(file, exc);
                }
            });
        } else {
            int hash = 0;

            try {
                hash = hashFile(filename);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            writer.write(String.format("%08x", hash) + " " + filename);
            writer.newLine();
        }
    }

    public static int hashFile(String filename) throws IOException {
        FileInputStream is = new FileInputStream(new File(filename));
        int hash = 0x811c9dc5;
        int prime = 0x01000193;
        int b;

        while ((b = is.read()) != -1) {
            hash *= prime;
            hash ^= (b & 0xff);
        }

        return hash;
    }
}
