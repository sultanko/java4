package sample;

/**
 * Created on 20.05.15.
 *
 * @author sultan
 */

import javafx.util.Pair;

import javax.swing.*;
import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.*;
import java.lang.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class FileWalker {

    private static final int BUFFER_SIZE = 1024;

    private final BlockingQueue<Pair<Path, Path>> queue = new LinkedBlockingQueue<>();
    private final AtomicLong copiedBytest = new AtomicLong();
    private final AtomicLong totalSize = new AtomicLong();
    private final ExecutorService threadScheldure = Executors.newFixedThreadPool(1);
    private static final int workersCount = 1;
    private final ExecutorService threadsWorkers = Executors.newFixedThreadPool(workersCount);

    public FileWalker() {
        for (int i = 0; i < workersCount; ++i) {
            threadsWorkers.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Pair<Path, Path> pathCur = null;
                    try {
                        pathCur = queue.take();
                    } catch (InterruptedException e) {
                        return;
                    }
                    System.err.println(pathCur.getValue());
                    try {
                        copyFile(pathCur.getKey(), pathCur.getValue());
                    } catch (ClosedByInterruptException ignored) {
                        Thread.currentThread().interrupt();
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "Error while copying files " + e.getMessage());
                        });
                    }
                }
            });
        }
    }

    public void copyFile(final Path dest, final Path src) throws IOException {
        Path destDir = dest.getParent();
        if (!Files.exists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (FileAlreadyExistsException ignored) {
            }
        }
        try {
            Files.createFile(dest);
        } catch (FileAlreadyExistsException ignored) {
        }
        try (InputStream in = Files.newInputStream(src);
                OutputStream out = Files.newOutputStream(dest)) {
            byte[] bytes = new byte[BUFFER_SIZE];
            while (in.read(bytes) != -1) {
                copiedBytest.addAndGet(bytes.length);
                out.write(bytes);
            }
        }
    }

    public void recursiveWalk(final Path pathDest, final Path pathSrc) throws IOException {
        copiedBytest.set(0);
        totalSize.set(0);
        final Queue<Pair<Path, Path>> currentFileList = new ArrayDeque<>();

        if (Files.isDirectory(pathSrc)) {
            Files.walkFileTree(pathSrc, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult result = super.visitFile(file, attrs);
                    if (!Files.isDirectory(file)) {
                        Path pathRem = pathDest.resolve(pathSrc.relativize(file));
                        currentFileList.add(new Pair<>(pathRem, file));
                        totalSize.addAndGet(Files.size(file));
                    }
                    return result;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return super.visitFileFailed(file, exc);
                }
            });
        } else {
            currentFileList.add(new Pair<>(pathDest, pathSrc));
        }
        queue.addAll(currentFileList);
    }

    private void copyDirsPrivate(final String src, final String dest) throws IOException {

        final Path pathSrc = Paths.get(src);
        final Path pathDest = Paths.get(dest);
        if (pathSrc.equals(pathDest)) {
            throw new IOException("Destination folder is the same");
        }

        recursiveWalk(pathDest, pathSrc);
    }

    public void copyDirs(final String src, final String dest) throws IOException {
        threadScheldure.submit(() -> {
            try {
                copyDirsPrivate(src, dest);
            } catch (ClosedByInterruptException ignored) {
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Error while copying files " + e.getMessage());
                });
            }
        });
    }

    public long getCopiedBytest() {
        return copiedBytest.get();
    }

    public double getPercent() {
        return ((double)copiedBytest.get())/totalSize.get();
    }

    public void close() throws Exception {
        queue.clear();
        threadsWorkers.shutdownNow();
        threadScheldure.shutdownNow();
    }

    public long getTotalSize() {
        return totalSize.get();
    }
}
