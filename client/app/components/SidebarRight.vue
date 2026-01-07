<template>
  <div class="h-full flex flex-col bg-white">
    <div
      class="p-4 border-b border-gray-100 bg-white/80 backdrop-blur-sm sticky top-0 z-10 flex justify-between items-center shadow-sm"
    >
      <div class="font-semibold text-slate-800 truncate text-base">
        {{
          (chatStore.conversations || []).find(
            (s) => s.id === chatStore.currentConversationId
          )?.title || "新对话"
        }}
      </div>
      <div class="flex items-center gap-2">
        <div
          class="text-[10px] text-slate-400 font-mono bg-slate-50 px-2 py-1 rounded-md"
          v-if="chatStore.currentConversationId"
        >
          {{ chatStore.currentConversationId.substring(0, 8) }}
        </div>
        <el-button
          circle
          size="small"
          :icon="Delete"
          @click="handleClearHistory"
          v-if="chatStore.messages && chatStore.messages.length > 0"
          title="清空历史"
        />
      </div>
    </div>

    <div
      class="flex-1 overflow-y-auto p-4 space-y-6 scroll-smooth"
      ref="messagesContainer"
    >
      <div
        v-if="!chatStore.messages || chatStore.messages.length === 0"
        class="h-full flex flex-col items-center justify-center text-slate-400 gap-4"
      >
        <div
          class="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center"
        >
          <el-icon :size="32" class="text-slate-300"><ChatDotRound /></el-icon>
        </div>
        <p class="text-sm">开始一个新的对话...</p>
      </div>

      <div
        v-for="(msg, index) in chatStore.messages || []"
        :key="index"
        class="flex w-full group"
        :class="msg.messageType === 'USER' ? 'justify-end' : 'justify-start'"
      >
        <div
          v-if="msg.messageType !== 'USER'"
          class="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold mr-3 flex-shrink-0 shadow-sm mt-1"
        >
          AI
        </div>

        <div
          class="max-w-[85%] rounded-2xl px-5 py-3.5 text-sm leading-relaxed shadow-sm transition-all"
          :class="[
            msg.messageType === 'USER'
              ? 'bg-slate-800 text-white rounded-br-sm'
              : 'bg-white border border-slate-100 text-slate-700 rounded-bl-sm shadow-sm',
          ]"
        >
          <div v-if="msg.messageType === 'ASSISTANT'">
            <div v-if="hasThink(msg.content)" class="mb-2">
              <details class="group/think">
                <summary
                  class="list-none cursor-pointer text-xs text-slate-400 hover:text-primary transition-colors flex items-center gap-1 select-none"
                >
                  <el-icon
                    class="transition-transform group-open/think:rotate-90"
                    ><ArrowRight
                  /></el-icon>
                  <span>思考过程</span>
                </summary>
                <div
                  class="mt-2 pl-4 border-l-2 border-slate-100 text-slate-500 text-xs whitespace-pre-wrap leading-relaxed animate-in fade-in slide-in-from-top-1"
                >
                  {{ getThink(msg.content) }}
                </div>
              </details>
            </div>
            <div
              class="markdown-body"
              v-html="renderMarkdown(getAnswer(msg.content))"
            ></div>
          </div>
          <div v-else class="whitespace-pre-wrap">{{ msg.content }}</div>
        </div>

        <div
          v-if="msg.messageType === 'USER'"
          class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 text-xs font-bold ml-3 flex-shrink-0 shadow-sm mt-1"
        >
          Me
        </div>
      </div>

      <div
        v-if="chatStore.isThinking"
        class="flex justify-start w-full animate-pulse"
      >
        <div
          class="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold mr-3 flex-shrink-0 shadow-sm mt-1 opacity-50"
        >
          AI
        </div>
        <div
          class="bg-white border border-slate-100 rounded-2xl rounded-bl-sm px-5 py-3.5 shadow-sm flex items-center gap-2"
        >
          <span
            class="w-2 h-2 bg-slate-400 rounded-full animate-bounce"
            style="animation-delay: 0s"
          ></span>
          <span
            class="w-2 h-2 bg-slate-400 rounded-full animate-bounce"
            style="animation-delay: 0.2s"
          ></span>
          <span
            class="w-2 h-2 bg-slate-400 rounded-full animate-bounce"
            style="animation-delay: 0.4s"
          ></span>
          <span class="text-xs text-slate-400 ml-2">正在思考...</span>
        </div>
      </div>
    </div>

    <div class="p-4 border-t border-gray-100 bg-white relative z-20">
      <div class="relative">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入消息..."
          class="custom-textarea !text-base"
          resize="none"
          @keydown.enter.prevent="handleSend($event as KeyboardEvent)"
          :disabled="chatStore.isTyping"
        />
        <div class="absolute right-2 bottom-2">
          <el-button
            type="primary"
            circle
            :icon="Position"
            @click="handleSend()"
            :loading="chatStore.isTyping"
            :disabled="!inputMessage.trim()"
            class="!w-8 !h-8 !min-h-0"
          />
        </div>
      </div>
      <div class="text-center mt-2 text-[10px] text-slate-300">
        AI 生成的内容可能不准确，请核实重要信息。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ChatDotRound,
  Position,
  Delete,
  ArrowRight,
} from "@element-plus/icons-vue";
import MarkdownIt from "markdown-it";

const chatStore = useChatStore();
const inputMessage = ref("");
const messagesContainer = ref<HTMLElement | null>(null);
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
});

const hasThink = (content: string) => {
  return content.includes("<think>");
};

const getThink = (content: string) => {
  const match = content.match(/<think>([\s\S]*?)(?:<\/think>|$)/);
  return match && match[1] ? match[1].trim() : "";
};

const getAnswer = (content: string) => {
  if (content.includes("</think>")) {
    return content.replace(/<think>[\s\S]*?<\/think>/, "").trim();
  } else {
    return content.replace(/<think>[\s\S]*/, "").trim();
  }
};

const renderMarkdown = (text: string) => {
  return md.render(text || "");
};

const handleSend = async (e?: KeyboardEvent) => {
  if (e && e.shiftKey) return; 

  if (!inputMessage.value.trim() || chatStore.isTyping) return;

  const content = inputMessage.value;
  inputMessage.value = "";

  await chatStore.sendMessage(content);
};

const handleClearHistory = () => {
  ElMessageBox.confirm("确定要清空当前对话历史吗？", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      if (chatStore.currentConversationId) {
        chatStore.deleteConversation(chatStore.currentConversationId);
      }
    })
    .catch(() => {});
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

watch(() => chatStore.messages?.length, scrollToBottom);
watch(
  () => chatStore.messages,
  () => {
    scrollToBottom();
  },
  { deep: true }
);
</script>

<style scoped>
/* Custom Input Styles */
:deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 40px 12px 16px;
  background-color: #f8fafc;
  border-color: #e2e8f0;
  transition: all 0.2s;
  box-shadow: none !important;
}
:deep(.el-textarea__inner:focus) {
  background-color: #fff;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-8) !important;
}

/* Markdown Styles */
:deep(.markdown-body) {
  font-size: 0.875rem;
  color: #334155;
}
:deep(.markdown-body p) {
  margin-bottom: 0.75em;
}
:deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}
:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 1.5em;
  margin-bottom: 0.75em;
  list-style-type: disc;
}
:deep(.markdown-body pre) {
  background-color: #f1f5f9;
  padding: 0.75rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  font-family: monospace;
  margin-bottom: 0.75em;
  border: 1px solid #e2e8f0;
}
:deep(.markdown-body code) {
  background-color: #f1f5f9;
  padding: 0.2em 0.4em;
  border-radius: 0.25em;
  font-family: monospace;
  font-size: 0.85em;
  color: #ef4444;
}
:deep(.markdown-body pre code) {
  background-color: transparent;
  padding: 0;
  color: inherit;
  font-size: 1em;
}
:deep(.markdown-body a) {
  color: var(--el-color-primary);
  text-decoration: underline;
}
</style>
