package com.auction.app.service.impl;

import com.app.common.entity.Item;
import com.auction.app.repository.ItemDAO;
import com.auction.app.service.ItemService;

import java.util.List;

public class ItemServiceImpl implements ItemService {

    private final ItemDAO itemDAO;

    public ItemServiceImpl(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    @Override
    public void deleteItem(String id) {
        validateId(id);
        if (!itemDAO.delete(id)) {
            throw new IllegalArgumentException("Không tìm thấy item với id: " + id);
        }
    }

    @Override
    public Item getById(String id) {
        validateId(id);
        return itemDAO.findById(id);
    }

    @Override
    public List<Item> getItemsList() {
        return itemDAO.findAll();
    }

    @Override
    public void saveItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Vật phẩm không thể null");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Tên vật phẩm không thể để trống");
        }

        if (item.getStartDateString() == null || item.getStartDateString().isBlank()) {
            throw new IllegalArgumentException("Ngày không thể trống");
        }

        if (item.getEndDateString() == null || item.getEndDateString().isBlank()) {
            throw new IllegalArgumentException("Ngày không thể trống");
        }

        if (item.getStartPrice() < 0) {
            throw new IllegalArgumentException("Giá bắt đầu không thể nhỏ hơn 0");
        }

        if (item.getMinIncreasement() <= 0) {
            throw new IllegalArgumentException("Độ tăng nhỏ nhất phải lớn hơn 0");
        }

        if (!itemDAO.save(item)) {
            throw new IllegalStateException("Không lưu vật phẩm được");
        }
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID vật phẩm không thể trống");
        }
    }
}
