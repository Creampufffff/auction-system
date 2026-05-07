package com.auction.app.socket.observer;

public interface Observer<T> {
    void onEvent(T event);
}
