package ru.ifmo.ctddev.shah.udp;

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
public class HelloUDPClient implements AutoCloseable {
    private final DatagramSocket datagramSocket;
//    private final SocketAddress socketAddress;
    private final InetAddress address;
    private final Integer port;
    private final String prefix;
    private final Integer threadCount;
    private final Integer requestCount;
    private final ThreadLocal<Integer> requestSended;
    private final ThreadLocal<DatagramSocket> socket;
    private final CountDownLatch downLatch;
    private final ExecutorService threadPool;

    public HelloUDPClient(InetAddress datagramSocket, Integer port, String prefix, Integer threadsCount, Integer requestCount) throws SocketException {
//        this.socketAddress = new InetSocketAddress(datagramSocket, port);
//        System.err.println(this.socketAddress);
//        this.datagramSocket = new DatagramSocket(port, datagramSocket);
        this.datagramSocket = null;
        this.address = datagramSocket;
        this.port = port;
        this.prefix = prefix;
        this.threadCount = threadsCount;
        this.requestCount = requestCount;
        this.requestSended = new ThreadLocal<>();
        this.downLatch = new CountDownLatch(threadsCount);
        this.threadPool = Executors.newFixedThreadPool(threadsCount);
        socket = new ThreadLocal<>();
    }

    private void send() throws SocketException {
        for (int i = 0; i < threadCount; i++) {
            threadPool.submit(() -> {
                try {
                    socket.set(new DatagramSocket());
                } catch (SocketException e) {
                    e.printStackTrace();
                    return;
                }
                requestSended.set(0);
                while (requestSended.get() < requestCount) {
                    String request = prefix + ThreadId.get() + "_" + requestSended.get();
                    DatagramPacket dp = new DatagramPacket(request.getBytes(), request.length(), address, port);
                    try {
                        socket.get().send(dp);
                        System.err.println("Sended");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    DatagramPacket answer = new DatagramPacket(new byte[dp.getLength() + 10], dp.getLength() + 10);
                    try {
                        socket.get().receive(answer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(request);
                    System.out.println(new String(answer.getData()));
                    requestSended.set(requestSended.get() + 1);
                }
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

    public static void printUsage() {
        System.out.println("Ip-adress port prefix theardsCount requestsCount");
    }

    public static boolean checkUsage(String[] args) {
        if (args == null || args.length != 5) {
            printUsage();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String domainName = args[0];
        Integer port = Integer.valueOf(args[1]);
        InetAddress ia;
        try {
            ia = InetAddress.getByName(domainName);
        } catch (UnknownHostException e) {
            printUsage();
            System.err.println("Unknown host " + e.getMessage());
            return;
        }
        try (HelloUDPClient client =
                     new HelloUDPClient(ia, port, args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]))) {
            client.send();
        } catch (SocketException e) {
            System.err.println("Cannot create socket");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        threadPool.shutdownNow();
        while (downLatch.getCount() > 0) {
            downLatch.countDown();
        }
    }
}
