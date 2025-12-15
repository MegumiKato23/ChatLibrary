package com.zg.ai.repository;

import java.util.List;

public interface ChatHistoryRepository {
    /**
     *
     * @param chatId {@link String} 会话ID
     * @author 周广
     */
    void save(String chatId);

    /**
     *
     * @param chatId {@link String} 会话ID
     * @author 周广
     */
    void delete(String chatId);

    /**
     *
     * @param userId {@link String} 用户ID
     * @return {@link List< String>} 会话ID列表
     * @author 周广
     */
    List<String> getChatIds(String userId);
}
