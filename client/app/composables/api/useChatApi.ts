import { type ChatHistoryDTO, type ChatMessage } from "~/types";

/**
 * 对话相关 API 接口
 * 对应后端 ChatController
 */
export const useChatApi = () => {
  const api = useApi();
  const config = useRuntimeConfig();
  const baseURL = config.public.apiBase as string;

  /**
   * 创建新会话
   * POST /ai/conversation?userId={userId}&title={title}
   * @param userId 用户ID
   * @param title 会话标题
   */
  const createConversation = (userId: string, title?: string) => {
    // 使用 query params
    return api.post<string>("/ai/conversation", null, {
      params: { userId, title },
    });
  };

  /**
   * 获取会话列表
   * GET /ai/conversations?userId={userId}
   * @param userId 用户ID
   */
  const getConversationList = (userId: string) => {
    return api.get<ChatHistoryDTO[]>("/ai/conversations", { userId });
  };

  /**
   * 获取会话消息历史
   * GET /ai/conversation/history/{historyId}
   * @param historyId 会话ID
   */
  const getConversationHistory = (historyId: string) => {
    return api.get<ChatMessage[]>("/ai/conversation/history/" + historyId);
  };

  /**
   * 删除会话
   * DELETE /ai/conversation/history/{historyId}
   * @param historyId 会话ID
   */
  const deleteConversation = (historyId: string) => {
    return api.del<void>("/ai/conversation/history/" + historyId);
  };

  /**
   * 发送消息（流式响应）
   * POST /ai/chat
   * @param chatId 会话ID
   * @param prompt 提示词
   * @param userId 用户ID
   * @param callbacks 回调函数
   */
  const sendMessage = async (
    chatId: string,
    prompt: string,
    userId: string,
    callbacks: {
      onMessage: (text: string) => void;
      onError: (err: any) => void;
      onComplete: () => void;
    }
  ) => {
    try {
      const url = `${baseURL}/ai/chat`;
      const body = {
        prompt,
        chatId,
        userId,
      };

      console.log("[ChatAPI] Sending message:", { url, body });

      const response = await fetch(url, {
        method: "POST",
        headers: {
          Accept: "text/event-stream",
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error(
          `[ChatAPI] HTTP error! status: ${response.status}, body: ${errorText}`
        );
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error("Response body is unavailable");
      }

      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk;

        // 处理 SSE 格式
        const lines = buffer.split("\n");
        // 保留最后一个可能不完整的行
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (line.trim() === "") continue;

          if (line.startsWith("data:")) {
            const data = line.substring(5);
            // 如果data为空或只包含空白字符，可能是心跳或空行，视情况处理
            if (data) {
              callbacks.onMessage(data);
            }
          } else {
            // 兼容非标准格式
            callbacks.onMessage(line);
          }
        }
      }

      // 处理剩余 buffer
      if (buffer.trim()) {
        if (buffer.startsWith("data:")) {
          callbacks.onMessage(buffer.substring(5));
        } else {
          callbacks.onMessage(buffer);
        }
      }

      callbacks.onComplete();
    } catch (error) {
      console.error("Stream Error:", error);
      callbacks.onError(error);
    }
  };

  return {
    createConversation,
    getConversationList,
    getConversationHistory,
    deleteConversation,
    sendMessage,
  };
};
