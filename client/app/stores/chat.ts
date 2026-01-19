import { useChatApi } from "~/composables/api/useChatApi";
import { useStorage } from "@vueuse/core";
import { type ChatHistoryDTO, type ChatMessage } from "~/types";

export const useChatStore = defineStore("chat", () => {
  // --- 状态定义 ---
  const conversations = ref<ChatHistoryDTO[]>([]); // 会话列表
  const currentConversationId = ref<string | null>(null); // 当前选中会话ID
  const messages = ref<ChatMessage[]>([]); // 当前会话消息记录
  const isTyping = ref(false); // 是否正在输入/生成中
  const isThinking = ref(false); // AI 是否正在思考（等待首字）
  const messageQueue = ref<string[]>([]); // 消息队列（防止并发发送）
  const isProcessingQueue = ref(false); // 队列处理状态
  const pendingMessage = ref<string>(""); // 待发送消息（用于登录后恢复）

  const chatApi = useChatApi();
  const userStore = useUserStore();

  const guestConversations = useStorage<any[]>("guest-conversations", []);

  // 敏感词设置
  const sensitiveWords = [
    "暴力",
    "色情",
    "赌博",
    "毒品",
    "反动",
    "恐怖主义",
    "邪教",
    "诈骗",
  ];

  // 验证输入内容（敏感词过滤）
  const validateInput = (content: string): boolean => {
    if (!content.trim()) return false;
    if (sensitiveWords.some((word) => content.includes(word))) {
      // 本地模拟用户消息
      const userMsg: ChatMessage = {
        id: Date.now().toString(),
        conversationId: currentConversationId.value || "temp",
        role: "user",
        messageType: "USER",
        content: content,
        createTime: new Date().toISOString(),
      };
      messages.value.push(userMsg);
      // 本地模拟系统回复
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

  // --- Actions ---

  // 获取会话列表
  const fetchConversations = async () => {
    if (userStore.isAuthenticated && userStore.user?.id) {
      try {
        const res = await chatApi.getConversationList(userStore.user.id);
        if (res.code === 200) {
          let list = res.data;
          if (!Array.isArray(list)) {
            list = [];
          }
          // 按更新时间倒序排序
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
      // 访客模式使用本地存储
      conversations.value = guestConversations.value.sort((a, b) => {
        const timeA = a.updateTime ? new Date(a.updateTime).getTime() : 0;
        const timeB = b.updateTime ? new Date(b.updateTime).getTime() : 0;
        return timeB - timeA;
      });
    }
  };

  // 创建新会话
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

  // 删除会话
  const deleteConversation = async (conversationId: string) => {
    if (userStore.isAuthenticated) {
      try {
        const res = await chatApi.deleteConversation(conversationId);
        if (res.code === 200) {
          await fetchConversations();
          // 如果删除的是当前选中会话，清空当前状态
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

  // 选中会话并加载历史记录
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

  // 内部：处理单条消息发送逻辑
  const processMessage = async (content: string) => {
    // 如果没有选中会话，自动创建
    if (!currentConversationId.value) {
      await createConversation(content.substring(0, 20));
    }

    // 添加用户消息到界面
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
      // 预先添加 AI 消息占位
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

      // 调用流式接口
      await chatApi.sendMessage(
        currentConversationId.value!,
        content,
        userStore.user.id,
        {
          onMessage: (text) => {
            if (isThinking.value) isThinking.value = false;
            // 实时追加回复内容
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

  // 发送消息入口（加入队列）
  const sendMessage = async (content: string) => {
    if (!validateInput(content)) return;

    messageQueue.value.push(content);
    if (!isProcessingQueue.value) {
      processQueue();
    }
  };

  // 队列处理器
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

  // 清空所有状态（登出时调用）
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
    pendingMessage,
    fetchConversations,
    createConversation,
    deleteConversation,
    selectConversation,
    sendMessage,
    clearState,
  };
});
