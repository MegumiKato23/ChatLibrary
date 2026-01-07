import { useChatApi } from "~/composables/api/useChatApi";
import { useStorage } from "@vueuse/core";
import { type ChatHistoryDTO, type ChatMessage } from "~/types";

export const useChatStore = defineStore("chat", () => {
  const conversations = ref<ChatHistoryDTO[]>([]);
  const currentConversationId = ref<string | null>(null);
  const messages = ref<ChatMessage[]>([]);
  const isTyping = ref(false);
  const isThinking = ref(false);
  const messageQueue = ref<string[]>([]);
  const isProcessingQueue = ref(false);
  
  const chatApi = useChatApi();
  const userStore = useUserStore();
  
  const guestConversations = useStorage<any[]>("guest-conversations", []);

  // 敏感词设置
  const sensitiveWords = ["", ""]; 

  const validateInput = (content: string): boolean => {
    if (!content.trim()) return false;
    if (sensitiveWords.some((word) => content.includes(word))) {
      const userMsg: ChatMessage = {
        id: Date.now().toString(),
        conversationId: currentConversationId.value || "temp",
        role: "user",
        messageType: "USER",
        content: content,
        createTime: new Date().toISOString(),
      };
      messages.value.push(userMsg);
      messages.value.push({
        id: (Date.now() + 1).toString(),
        conversationId: currentConversationId.value || "temp",
        role: "assistant",
        messageType: "ASSISTANT",
        content: "您的输入包含敏感词，请修改后重试。",
        createTime: new Date().toISOString(),
      });
      return false;
    }
    return true;
  };

  const fetchConversations = async () => {
    if (userStore.isAuthenticated && userStore.user?.id) {
      try {
        const res = await chatApi.getConversationList(userStore.user.id);
        if (res.code === 200) {
          let list = res.data;
          if (!Array.isArray(list)) {
            list = [];
          }

          conversations.value = list.sort((a, b) => {
            const timeA = a.updateTime ? new Date(a.updateTime).getTime() : 0;
            const timeB = b.updateTime ? new Date(b.updateTime).getTime() : 0;
            return timeB - timeA;
          });
        }
      } catch (error: any) {
        throw new Error("获取对话列表失败", error);
      }
    } else {
      console.log("[ChatStore] Guest mode or not authenticated");
      conversations.value = guestConversations.value.sort((a, b) => {
        const timeA = a.updateTime ? new Date(a.updateTime).getTime() : 0;
        const timeB = b.updateTime ? new Date(b.updateTime).getTime() : 0;
        return timeB - timeA;
      });
    }
  };

  const createConversation = async (title: string = "New Chat") => {
    if (userStore.isAuthenticated && userStore.user?.id) {
      try {
        const res = await chatApi.createConversation(userStore.user.id, title);
        if (res.code === 200) {
          await fetchConversations();
          currentConversationId.value = res.data;
          messages.value = [];
        }
      } catch (error: any) {
        throw new Error("创建对话失败", error);
      }
    } else {
      userStore.showLoginModal();
      throw new Error("请先登录");
    }
  };

  const deleteConversation = async (conversationId: string) => {
    if (userStore.isAuthenticated) {
      try {
        const res = await chatApi.deleteConversation(conversationId);
        if (res.code === 200) {
          await fetchConversations();
          if (currentConversationId.value === conversationId) {
            currentConversationId.value = null;
            messages.value = [];
          }
        }
      } catch (error: any) {
        throw new Error("删除对话失败", error);
      }
    }
  };

  const selectConversation = async (conversationId: string) => {
    currentConversationId.value = conversationId;
    if (userStore.isAuthenticated) {
      try {
        const res = await chatApi.getConversationHistory(conversationId);
        if (res.code === 200) {
          let list = res.data;
          if (!Array.isArray(list)) list = [];
          messages.value = list;
        }
      } catch (error: any) {
        throw new Error("获取对话历史失败", error);
      }
    }
  };

  const processMessage = async (content: string) => {
    if (!currentConversationId.value) {
      await createConversation(content.substring(0, 20));
    }

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      conversationId: currentConversationId.value!,
      role: "user",
      messageType: "USER",
      content,
      createTime: new Date().toISOString(),
    };
    messages.value.push(userMsg);

    isTyping.value = true;
    isThinking.value = true;

    if (userStore.isAuthenticated && userStore.user?.id) {
      const aiMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        conversationId: currentConversationId.value!,
        role: "assistant",
        messageType: "ASSISTANT",
        content: "",
        createTime: new Date().toISOString(),
      };
      messages.value.push(aiMsg);
      const aiMessageIndex = messages.value.length - 1;

      await chatApi.sendMessage(
        currentConversationId.value!,
        content,
        userStore.user.id,
        {
          onMessage: (text) => {
            if (isThinking.value) isThinking.value = false;
            if (messages.value[aiMessageIndex]) {
              messages.value[aiMessageIndex].content += text;
            }
          },
          onError: (err) => {
            console.error("Chat error", err);
            isTyping.value = false;
            isThinking.value = false;
            if (messages.value[aiMessageIndex]) {
              messages.value[aiMessageIndex].content +=
                "\n[Error: 消息生成失败]";
            }
          },
          onComplete: () => {
            isTyping.value = false;
            isThinking.value = false;
          },
        }
      );
    } else {
      userStore.showLoginModal();
      isTyping.value = false;
      isThinking.value = false;
    }
  };

  const sendMessage = async (content: string) => {
    if (!validateInput(content)) return;

    messageQueue.value.push(content);
    if (!isProcessingQueue.value) {
      processQueue();
    }
  };

  const processQueue = async () => {
    isProcessingQueue.value = true;
    while (messageQueue.value.length > 0) {
      const content = messageQueue.value.shift();
      if (content) {
        await processMessage(content);
      }
    }
    isProcessingQueue.value = false;
  };

  const clearState = () => {
    conversations.value = [];
    currentConversationId.value = null;
    messages.value = [];
    isTyping.value = false;
    isThinking.value = false;
    messageQueue.value = [];
    isProcessingQueue.value = false;
  };

  return {
    conversations,
    currentConversationId,
    messages,
    isTyping,
    isThinking,
    fetchConversations,
    createConversation,
    deleteConversation,
    selectConversation,
    sendMessage,
    clearState,
  };
});
