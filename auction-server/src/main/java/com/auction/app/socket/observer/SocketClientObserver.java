package com.auction.app.socket.observer;

import java.io.PrintWriter;

public class SocketClientObserver implements Observer<String> {
    private final PrintWriter writer;

    public SocketClientObserver(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onEvent(String event) {
        writer.println(event);
    }
}
