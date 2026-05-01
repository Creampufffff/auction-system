package com.auction.app.service.impl;

import java.io.*;
import java.net.Socket;

public class ServerSocket {
    public ServerSocket() throws IOException {
        java.net.ServerSocket serverSocket = new java.net.ServerSocket(5000);
        System.out.println("Server dang khoi tao tai Port 5000");


        Socket clientSocket = serverSocket.accept();
        System.out.println("Khach ket noi: " + clientSocket);

        InputStream is = clientSocket.getInputStream();
        OutputStream os = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        PrintWriter writer = new PrintWriter(os, true);

        String line;
        while((line = reader.readLine()) != null){
            System.out.println("Client : "+ line);
            writer.println("Echo: "+line);
        }
        clientSocket.close();
    }
}
