package ru.ifmo.ctddev.shah.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created on 28.04.15.
 *
 * @author sultan
 */
public class HelloUDPServer implements HelloServer {
    private List<DatagramSocket> sockets;
    private List<ExecutorService> executorServices;
//    private BlockingQueue<DatagramPacket> queue;
    private static final String ANSWER = "Hello, ";
    private static final int BUFFER_SIZE = 65536;

    public HelloUDPServer() {
        sockets = new ArrayList<>();
        executorServices = new ArrayList<>();
    }

    private static void printUsage() {
        System.out.println("port threadsCount");
    }

    public static void main(String[] args) {
        Integer port;
        Integer threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
            printUsage();
            return;
        }
        try ( HelloUDPServer server = new HelloUDPServer()) {
            server.start(port, threads);
        }
    }

    /**
     * Create UDP-server on given port with given number of working threads.
     * @param port port on localhost
     * @param threads count of using threads
     */
    @Override
    public void start(final int port, final int threads) {
        DatagramSocket tmpSocket = null;
        ExecutorService tmpThreadPool = null;
        int num;
        synchronized (this) {
            try {
                tmpSocket = new DatagramSocket(port);
            } catch (SocketException e) {
                System.err.println("Cannot create socket " + e.getMessage());
                return;
            }
            tmpThreadPool = Executors.newFixedThreadPool(threads);
            sockets.add(tmpSocket);
            executorServices.add(tmpThreadPool);
            num = sockets.size() - 1;
//            queue = new LinkedBlockingQueue<>(threadsCount * 10);
        }
        final DatagramSocket datagramSocket = sockets.get(num);
        final ExecutorService threadPool = executorServices.get(num);
        for (int i = 0; i < threads; i++) {
            threadPool.submit(() -> {
                try {
                    final int MAX_BUF_SIZE = datagramSocket.getReceiveBufferSize();
                    final DatagramPacket datagramPacket =
                            new DatagramPacket(new byte[MAX_BUF_SIZE], MAX_BUF_SIZE);
                    while (!Thread.currentThread().isInterrupted()) {
                        datagramSocket.receive(datagramPacket);
                        String received = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        received = ANSWER + received;
                        DatagramPacket answer = new DatagramPacket(received.getBytes(), received.length(), datagramPacket.getAddress(), datagramPacket.getPort());
                        answer.setSocketAddress(datagramPacket.getSocketAddress());
                        datagramSocket.send(answer);
                    }
                    Thread.currentThread().interrupt();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    System.err.println("Error receiving message " + e.getMessage());
                }
            });
        }
/*        taskManager.submit(() -> {
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
        });*/
    }

    @Override
    public void close() {
        for (ExecutorService service : executorServices) {
            service.shutdownNow();
        }
        for (DatagramSocket socket : sockets) {
            socket.close();
        }
//        try {
//            Thread.sleep(SLEEP_TIMEOUT);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}
