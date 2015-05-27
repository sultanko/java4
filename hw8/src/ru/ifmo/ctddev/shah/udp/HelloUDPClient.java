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
    private static final int WAIT_TIMEOUT = 100;
    private static final int BUFFER_SIZE = 65536;

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
        String host;
        Integer port;
        String prefix;
        Integer requests;
        Integer threads;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            prefix = args[2];
            requests = Integer.parseInt(args[3]);
            threads = Integer.parseInt(args[4]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
            printUsage();
            return;
        }
        client.start(host, port, prefix, requests, threads);
    }

    /**
     * Send requests to server on host:port. Sending threads * requests.
     * @param host url to connect
     * @param port port of server to connect
     * @param prefix prefix of request
     * @param requests count of requests in each thread
     * @param threads count of sending threads
     */
    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        InetAddress tmpAdress;
        try {
            tmpAdress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host " + e.getMessage());
            return;
        }

        final CountDownLatch downLatch = new CountDownLatch(threads);
        final ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        final InetAddress address = tmpAdress;
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            threadPool.submit(() -> {
                send(port, prefix, requests, downLatch, address, threadId);
            });
        }
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        threadPool.shutdown();
    }

    private void send(int port, String prefix, int requests, CountDownLatch downLatch, InetAddress address, int threadId) {
        DatagramSocket socket;
        final int MAX_BUF_SIZE;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(WAIT_TIMEOUT);
            MAX_BUF_SIZE = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Error creating socket " + e.getMessage());
            downLatch.countDown();
            return;
        }
        int requestSended = 0;
        String threadRequest = String.format("%s%d_", prefix, threadId);
        DatagramPacket answer = new DatagramPacket(new byte[MAX_BUF_SIZE], MAX_BUF_SIZE);
        while (requestSended < requests) {
            String request = String.format("%s%d", threadRequest, requestSended);
            String answerRequired = String.format("Hello, %s", request);
            DatagramPacket dp = new DatagramPacket(request.getBytes(), request.length(), address, port);
            try {
                socket.send(dp);
            } catch (IOException e) {
                System.err.println("Error while sending request" + e.getMessage());
                break;
            }
            try {
                socket.receive(answer);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                System.err.println("Error while receiving request" + e.getMessage());
                break;
            }
            String serverAnswer = new String(answer.getData(), 0, answer.getLength());
            if (serverAnswer.equals(answerRequired)) {
                System.out.println(request);
                System.out.println(new String(answer.getData(), 0, answer.getLength()));
                requestSended++;
            }
        }
        socket.close();
        downLatch.countDown();
    }

}
