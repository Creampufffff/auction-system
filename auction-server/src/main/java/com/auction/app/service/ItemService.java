package com.auction.app.service;

import com.app.common.entity.Item;

import java.util.List;

public interface ItemService {
    void saveItem(Item item); // tạo vật phẩm

    void deleteItem(String id);

    Item getById(String id); // tìm vật phẩm bằng id

    List<Item> getItemsList();
}
