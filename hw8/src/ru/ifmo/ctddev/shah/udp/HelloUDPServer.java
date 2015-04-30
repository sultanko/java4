package ru.ifmo.ctddev.shah.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Created on 28.04.15.
 *
 * @author sultan
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket datagramSocket;
    private Integer threadsCount;
    private ExecutorService threadPool;
    private ExecutorService taskManager;
    private BlockingQueue<DatagramPacket> queue;

    public static void printUsage() {
        System.out.println("port threadsCount");
    }

    public static void main(String[] args) {
        try ( HelloUDPServer server = new HelloUDPServer()) {
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        }
    }

    @Override
    public void start(int port, int threads) {
        System.err.println("SERVER");
        System.err.println("PORT: " + port + " THREADS: " + threads);
        try {
            this.datagramSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Cannot create socket " + e.getMessage());
            return;
        }
        this.threadsCount = threads;
        this.threadPool = Executors.newFixedThreadPool(threads);
        this.taskManager = Executors.newSingleThreadExecutor();
        queue = new LinkedBlockingQueue<>(threadsCount * 10);
        for (int i = 0; i < threadsCount; i++) {
            threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        final DatagramPacket datagramPacket = queue.take();
                        String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        received = "Hello, " + received;
                        DatagramPacket answer = new DatagramPacket(received.getBytes(), received.length(), datagramPacket.getAddress(), datagramPacket.getPort());
                        answer.setSocketAddress(datagramPacket.getSocketAddress());
                        try {
                            datagramSocket.send(answer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Thread.currentThread().interrupt();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        System.err.println("started");
        taskManager.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    datagramSocket.receive(datagramPacket);
                    queue.offer(datagramPacket);
                } catch (IOException e) {
                    System.err.println("MY " + e.getMessage());
//                    e.printStackTrace();
                }
            }
            Thread.currentThread().interrupt();
        });
    }

    @Override
    public void close() {
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        taskManager.shutdownNow();
        datagramSocket.close();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
