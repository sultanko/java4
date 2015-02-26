package ru.ifmo.ctddev.shah.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    private static final int BLOCK_SIZE = 1024;
    private static byte[] array = null;

    public static void main(String[] args) {
        if (args != null && args.length == 2 && args[0] != null && args[1] != null) {
            runHashFiles(args[0], args[1]);
        } else {
            System.err.println("Invalid usage");
        }
    }

    public static void runHashFiles(String nameToRead, String nameToWrite) {


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(nameToRead)), "UTF-8"));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(nameToWrite)), "UTF-8"))) {
            String text;
            array = new byte[BLOCK_SIZE];
            while ((text = reader.readLine()) != null) {
                try {
                    recursiveWalk(text, writer);
                } catch (IOException e) {
                    System.err.println("Error while walking: " + e.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Can't open input/output file: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unsupported encoding of file " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error while reading file" + e.getMessage());
        } finally {
            array = null;
        }
    }


    public static void recursiveWalk(final String filename, final BufferedWriter writer) throws IOException {
        Path path = Paths.get(filename);

        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    checkFile(file.toString(), writer);

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
            checkFile(filename, writer);
        }
    }

    public static void checkFile(final String filename, final BufferedWriter writer) throws IOException{
        int hash = 0;

        try {
            hash = hashFile(filename);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        writer.write(String.format("%08x", hash) + " " + filename);
        writer.newLine();

    }

    public static int hashFile(String filename) throws IOException {
        FileInputStream is = new FileInputStream(new File(filename));
        int hash = 0x811c9dc5;
        int prime = 0x01000193;
        int readedBytes;

        while ((readedBytes = is.read(array, 0, BLOCK_SIZE)) != -1) {
            for (int i = 0; i < readedBytes; i++) {
                hash *= prime;
                hash ^= (array[i] & 0xff);
            }
        }

        return hash;
    }
}
