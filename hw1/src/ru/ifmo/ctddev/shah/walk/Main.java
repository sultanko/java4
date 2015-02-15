package ru.ifmo.ctddev.shah.walk;

import javafx.util.Pair;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        List<String> fileList = new ArrayList<String>();
        File fileToRead = new File("in.txt");
        File fileToWrite = new File("out.txt");
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead), "UTF-8"));
            String text = null;

            while ((text = reader.readLine()) != null) {
                fileList.add(text);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
            }
        }


        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWrite), "UTF-8"));

            for (String filename : fileList) {
                List<Pair<Integer, String>> resultList = null;
                try {
                    resultList = recursiveWalk(filename);
                } catch (FileNotFoundException e ){
                    System.err.println(e.getMessage());
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                } finally {
                    if (resultList != null) {
                        for (Pair<Integer, String> item : resultList) {
                            writer.write(String.format("%08x", item.getKey()) + " " + item.getValue());
                            writer.newLine();
                        }
                    } else {
                        writer.write(String.format("%08x", 0) + " " + filename);
                        writer.newLine();
                    }
                }
            }
            writer.close();
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Can't close output file");
                }
            }
        }
    }

    public static List< Pair<Integer, String> > recursiveWalk(final String filename) throws IOException {
        final List<Pair<Integer, String>> result = new ArrayList<Pair<Integer, String>>();
        Path path = Paths.get(filename);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    int hash = 0;
                    try {
                        hash = hashFile(file.toString());
                    } catch (IOException ignored) {
                    }
                    result.add(new Pair<Integer, String>(hash, file.toString()));
                    return super.visitFile(file, attrs);
                }
            });
        } else {
            result.add(new Pair<Integer, String>(hashFile(filename), filename));
        }
        return result;
    }

    public static int hashFile(String filename) throws IOException {
        FileInputStream is = new FileInputStream(new File(filename));
        int b = 0;
        int hash = 0x811c9dc5;
        int prime = 0x01000193;

        while ((b = is.read()) != -1) {
            hash *= prime;
            hash ^= b;
        }

        return hash;
    }
}
