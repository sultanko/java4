package ru.ifmo.ctddev.shah.udp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 28.04.15.
 *
 * @author sultan
 */
public class HelloUDPClient implements HelloClient {
//    private final SocketAddress socketAddress;
    private InetAddress address;
    private Integer port;
    private String prefix;
    private Integer threadCount;
    private Integer requestCount;
    private CountDownLatch downLatch;
    private ExecutorService threadPool;

    public static void printUsage() {
        System.out.println("Ip-adress port prefix requestsCount theardsCount ");
    }

    public static boolean checkUsage(String[] args) {
        if (args == null || args.length != 5) {
            printUsage();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        HelloUDPClient client = new HelloUDPClient();
        client.start(args[0], Integer.parseInt(args[1]), args[2],
                Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }

    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        try {
            this.address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host " + e.getMessage());
            return;
        }
        this.threadCount = threads;
        this.requestCount = requests;
        this.downLatch = new CountDownLatch(threadCount);
        this.threadPool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threadPool.submit(() -> {
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    socket.setSoTimeout(100);
                } catch (SocketException e) {
                    downLatch.countDown();
                    return;
                }
                int requestSended = 0;
                while (requestSended < requestCount) {
                    String request = prefix + threadId + "_" + requestSended;
                    DatagramPacket dp = new DatagramPacket(request.getBytes(), request.length(), address, port);
                    try {
                        socket.send(dp);
                    } catch (IOException e) {
                        System.err.println("Error while sending request" + e.getMessage());
                        break;
                    }
                    DatagramPacket answer = new DatagramPacket(new byte[dp.getLength() + 1024], dp.getLength() + 1024);
                    try {
                        socket.receive(answer);
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        System.err.println("Error while receiving request" + e.getMessage());
                        break;
                    }
//                    System.out.println(request);
//                    System.out.println(new String(answer.getData(), 0, answer.getLength()));
                    requestSended++;
                }
                socket.close();
                downLatch.countDown();
                System.err.println(downLatch.getCount());
            });
        }
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
