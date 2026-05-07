package com.auction.client.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SocketClientService {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 5000);
        BufferedReader networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        PrintWriter networkOut = new PrintWriter(socket.getOutputStream(), true);

        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

        String userInput;
        while((userInput = userIn.readLine()) != null){
            networkOut.println(userInput);
            System.out.println(networkIn.readLine());
        }
        socket.close();
    }
}
