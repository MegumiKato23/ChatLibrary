package com.zg.ai.repository;

import com.zg.ai.mapper.ChatHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InSqlChatHistoryRepository implements ChatHistoryRepository {
    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Override
    public void save(String chatId) {}

    private boolean exists(String chatId) {
        return false;
    }

    @Override
    public void delete(String chatId) {}

    @Override
    public List<String> getChatIds(String userId) {
        return null;
    }
}
