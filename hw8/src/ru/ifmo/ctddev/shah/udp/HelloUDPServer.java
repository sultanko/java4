package ru.ifmo.ctddev.shah.udp;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Created on 28.04.15.
 *
 * @author sultan
 */
public class HelloUDPServer implements AutoCloseable {
    private final DatagramSocket datagramSocket;
    private final Integer threadsCount;
    private final ExecutorService threadPool;
    private final ExecutorService taskManager;
    private final BlockingQueue<DatagramPacket> queue;

    public HelloUDPServer(Integer port, Integer threadsCount) throws UnknownHostException, SocketException {
        System.err.println("SERVER");
        this.datagramSocket = new DatagramSocket(port);
        this.threadsCount = threadsCount;
        this.threadPool = Executors.newFixedThreadPool(threadsCount);
        this.taskManager = Executors.newSingleThreadExecutor();
        queue = new LinkedBlockingQueue<>(threadsCount * 2);
        for (int i = 0; i < threadsCount; i++) {
            threadPool.submit(() -> {
                try {
                    while (true) {
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private void start() {
        System.err.println("started");
        while (true) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                datagramSocket.receive(datagramPacket);
                System.err.println("Received");
                if (queue.add(datagramPacket)) {
                    System.err.println("Added");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void printUsage() {
        System.out.println("port threadsCount");
    }

    public static void main(String[] args) {
        try ( HelloUDPServer server = new HelloUDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]))) {
            server.start();
        } catch (UnknownHostException | SocketException e) {
            printUsage();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        threadPool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
