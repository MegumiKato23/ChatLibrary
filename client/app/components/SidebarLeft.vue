<template>
  <div class="h-full flex flex-col bg-white border-r border-gray-200">
    <div class="p-4 border-b border-gray-100 bg-gray-50/50">
      <div
        v-if="userStore.isAuthenticated"
        class="flex items-center justify-between w-full"
      >
        <el-dropdown trigger="click" @command="handleCommand" class="w-full">
          <div class="flex items-center gap-3 cursor-pointer w-full">
            <el-avatar
              :size="36"
              class="!bg-slate-200 !text-slate-600 font-bold border border-white shadow-sm flex-shrink-0"
            >
              {{ userStore.user?.username?.charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="flex flex-col flex-1 overflow-hidden">
              <span class="font-semibold text-sm text-slate-700 truncate">{{
                userStore.user?.username
              }}</span>
              <span class="text-[10px] text-slate-400 text-left">在线</span>
            </div>
            <el-icon class="text-slate-400"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"
                >更改个人信息</el-dropdown-item
              >
              <el-dropdown-item command="password">更改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided class="text-red-500"
                >退出登录</el-dropdown-item
              >
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div v-else>
        <el-button
          type="primary"
          class="w-full !rounded-lg !shadow-sm"
          @click="userStore.showLoginModal"
          >登录 / 注册</el-button
        >
      </div>
    </div>

    <div class="p-4 flex gap-3">
      <el-button
        type="primary"
        class="flex-1 !rounded-lg !shadow-sm"
        @click="handleNewChat"
      >
        <el-icon class="mr-1"><Plus /></el-icon> 新对话
      </el-button>

      <el-upload
        class="flex-1"
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        :disabled="documentStore.isUploading"
        accept=".pdf,.docx,.txt,.md,.doc"
      >
        <el-button
          class="w-full !rounded-lg !bg-white !border-slate-200 !text-slate-600 hover:!text-primary hover:!border-primary hover:!bg-slate-50 transition-colors"
          :loading="documentStore.isUploading"
        >
          <el-icon class="mr-1" v-if="!documentStore.isUploading"
            ><Upload
          /></el-icon>
          上传
        </el-button>
      </el-upload>
    </div>

    <div v-if="documentStore.isUploading" class="px-4 mb-3">
      <div class="flex justify-between text-xs text-slate-500 mb-1">
        <span>上传中...</span>
        <span>{{ documentStore.uploadProgress }}%</span>
      </div>
      <el-progress
        :percentage="documentStore.uploadProgress"
        :show-text="false"
        :stroke-width="4"
        status="success"
      />
    </div>

    <el-tabs
      v-model="activeTab"
      class="flex-1 flex flex-col px-2 custom-tabs"
      stretch
    >
      <el-tab-pane label="对话" name="chats">
        <div class="overflow-y-auto h-[calc(100vh-220px)] px-2 pb-4 space-y-1">
          <div v-if="isLoading" class="space-y-3 mt-2">
            <el-skeleton :rows="2" animated v-for="i in 3" :key="i" />
          </div>

          <template v-else>
            <div
              v-for="conversation in chatStore.conversations || []"
              :key="conversation.id"
              class="group p-3 hover:bg-slate-50 cursor-pointer rounded-xl transition-all duration-200 border border-transparent hover:border-slate-100"
              :class="{
                '!bg-primary-light !border-primary-light/50':
                  conversation.id === chatStore.currentConversationId,
              }"
              @click="chatStore.selectConversation(conversation.id)"
            >
              <div class="flex justify-between items-start mb-1">
                <div
                  class="truncate font-medium text-sm text-slate-700"
                  :class="{
                    '!text-primary':
                      conversation.id === chatStore.currentConversationId,
                  }"
                >
                  {{ conversation.title || "新对话" }}
                </div>
                <el-icon
                  class="text-slate-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity"
                  @click.stop="openDeleteModal(conversation.id)"
                >
                  <Delete />
                </el-icon>
              </div>
              <div class="text-xs text-slate-400 flex items-center gap-1">
                <el-icon><Clock /></el-icon>
                {{ formatDate(conversation.updateTime) }}
              </div>
            </div>
            <el-empty
              v-if="chatStore.conversations.length === 0"
              description="暂无对话记录"
              :image-size="60"
            />
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane label="文档" name="files">
        <div class="overflow-y-auto h-[calc(100vh-220px)] px-2 pb-4 space-y-1">
          <div v-if="isLoading" class="space-y-3 mt-2">
            <el-skeleton :rows="2" animated v-for="i in 3" :key="i" />
          </div>

          <template v-else>
            <div
              v-for="doc in documentStore.documents"
              :key="doc.id"
              class="group p-3 hover:bg-slate-50 cursor-pointer rounded-xl transition-all duration-200 border border-transparent hover:border-slate-100"
              :class="{
                '!bg-primary-light !border-primary-light/50':
                  documentStore.currentDocument?.id === doc.id,
              }"
              @click="documentStore.selectDocument(doc)"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="flex items-center gap-2 overflow-hidden flex-1">
                  <div
                    class="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center flex-shrink-0 text-slate-500"
                  >
                    <el-icon v-if="doc.fileType === 'pdf'"
                      ><Document
                    /></el-icon>
                    <el-icon v-else-if="['doc', 'docx'].includes(doc.fileType)"
                      ><Document
                    /></el-icon>
                    <el-icon v-else><Document /></el-icon>
                  </div>
                  <div class="flex flex-col overflow-hidden">
                    <span
                      class="truncate text-sm font-medium text-slate-700"
                      :class="{
                        '!text-primary':
                          documentStore.currentDocument?.id === doc.id,
                      }"
                      >{{ doc.originalFilename }}</span
                    >
                    <span class="text-[10px] text-slate-400">{{
                      formatSize(doc.fileSize)
                    }}</span>
                  </div>
                </div>

                <el-icon
                  class="text-slate-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity p-1y"
                  @click.stop="openDeleteModal(doc.id, 'document')"
                  ><Delete
                /></el-icon>
              </div>
            </div>
            <el-empty
              v-if="documentStore.documents.length === 0"
              description="暂无上传文档"
              :image-size="60"
            />
          </template>
        </div>
      </el-tab-pane>
    </el-tabs>

    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="deleteModalVisible"
          class="delete-overlay"
          @click.self="cancelDelete"
        >
          <div class="delete-dialog">
            <div class="delete-header">
              <h3>{{ deleteType === "logout" ? "退出登录" : "删除提示" }}</h3>
              <button class="close-btn" @click="cancelDelete">×</button>
            </div>
            <div class="delete-body">
              <p>{{ deleteMessage }}</p>
            </div>
            <div class="delete-footer">
              <button class="btn cancel-btn" @click="cancelDelete">取消</button>
              <button class="btn confirm-btn" @click="confirmDelete">
                {{ deleteType === "logout" ? "确认" : "确定" }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <ChangePasswordDialog
      :visible="changePasswordVisible"
      @close="changePasswordVisible = false"
    />
    <EditProfileDialog
      :visible="editProfileVisible"
      @close="editProfileVisible = false"
      @success="editProfileVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import {
  Plus,
  Upload,
  Document,
  Delete,
  Clock,
  ArrowDown,
} from "@element-plus/icons-vue";
import ChangePasswordDialog from "~/components/user/ChangePasswordDialog.vue";
import EditProfileDialog from "~/components/user/EditProfileDialog.vue";

const userStore = useUserStore();
const chatStore = useChatStore();
const documentStore = useDocumentStore();

const activeTab = ref("chats");
const isLoading = ref(false);

const changePasswordVisible = ref(false);
const editProfileVisible = ref(false);

const handleCommand = (command: string) => {
  if (command === "logout") {
    openDeleteModal("", "logout");
  } else if (command === "password") {
    changePasswordVisible.value = true;
  } else if (command === "profile") {
    editProfileVisible.value = true;
  }
};

const deleteModalVisible = ref(false);
const itemToDeleteId = ref<string | null>(null);
const deleteType = ref<"conversation" | "document" | "logout">("conversation");

const deleteMessage = computed(() => {
  if (deleteType.value === "logout") return "确定要退出登录吗？";
  return deleteType.value === "conversation"
    ? "确定要退出当前会话吗？未保存的内容将会丢失"
    : "确定要删除此文档吗？此操作不可恢复";
});

const openDeleteModal = (
  id: string,
  type: "conversation" | "document" | "logout" = "conversation",
) => {
  itemToDeleteId.value = id;
  deleteType.value = type;
  deleteModalVisible.value = true;
};

const cancelDelete = () => {
  deleteModalVisible.value = false;
  itemToDeleteId.value = null;
};

const confirmDelete = async () => {
  if (deleteType.value === "logout") {
    await userStore.logout();
    deleteModalVisible.value = false;
    ElMessage.success("成功退出登录");
    return;
  }

  if (itemToDeleteId.value) {
    if (deleteType.value === "conversation") {
      await handleDeleteConversation(itemToDeleteId.value);
    } else {
      await handleDeleteDocument(itemToDeleteId.value);
    }
    deleteModalVisible.value = false;
    itemToDeleteId.value = null;
  }
};

const handleNewChat = () => {
  chatStore.createConversation();
};

const beforeUpload = () => {
  if (!userStore.isAuthenticated) {
    userStore.showLoginModal();
    return false;
  }
  return true;
};

const handleUpload = async (options: any) => {
  try {
    await documentStore.uploadDocument(options.file);
    ElMessage.success("上传成功");
  } catch (error) {
    ElMessage.error("上传失败");
  }
};

const handleDeleteConversation = async (id: string) => {
  await chatStore.deleteConversation(id);
  ElMessage.success("对话已删除");
};

const handleDeleteDocument = async (id: string) => {
  await documentStore.deleteDocument(id);
  ElMessage.success("文档已删除");
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  // MM-DD HH:mm
  return `${(date.getMonth() + 1).toString().padStart(2, "0")}-${date
    .getDate()
    .toString()
    .padStart(2, "0")} ${date.getHours().toString().padStart(2, "0")}:${date
    .getMinutes()
    .toString()
    .padStart(2, "0")}`;
};

const formatSize = (bytes: number) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
};

onMounted(async () => {
  if (userStore.isAuthenticated) {
    isLoading.value = true;
    await Promise.all([
      chatStore.fetchConversations(),
      documentStore.fetchDocuments(),
    ]);
    isLoading.value = false;
  }
});
</script>

<style scoped>
/* Custom Scrollbar for lists */
.overflow-y-auto::-webkit-scrollbar {
  width: 4px;
}
.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: #e2e8f0;
  border-radius: 20px;
}

/* Tab Styles */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #f1f5f9;
}
:deep(.el-tabs__item) {
  font-weight: 500;
  color: #64748b;
}
:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}

/* Utility Class for Primary Light Background */
.bg-primary-light {
  background-color: var(--el-color-primary-light-9);
}
.text-primary {
  color: var(--el-color-primary);
}
.border-primary-light {
  border-color: var(--el-color-primary-light-5);
}

/* Delete Modal Styles */
.delete-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000; /* Higher than sidebar */
  transition: all 0.3s ease;
}

.delete-dialog {
  background-color: #ffffff;
  border-radius: 16px;
  width: 480px; /* 20% larger than typical 400px */
  max-width: 90%;
  color: #1e293b;
  box-shadow:
    0 10px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transform: scale(1);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.delete-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
}

.delete-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 24px;
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px;
  line-height: 1;
  border-radius: 4px;
}

.close-btn:hover {
  color: #475569;
  background-color: #f1f5f9;
}

.delete-body {
  padding: 24px;
  font-size: 16px;
  color: #475569;
  line-height: 1.5;
}

.delete-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 24px 24px;
}

.btn {
  padding: 10px 20px;
  border-radius: 4px; /* Requested 4px for confirm btn, applying to both for consistency or override */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.cancel-btn {
  background-color: #f1f5f9;
  color: #64748b;
}

.cancel-btn:hover {
  background-color: #e2e8f0;
  color: #475569;
}

.confirm-btn {
  background-color: white;
  border: 1px solid #ff5252;
  color: #ff5252;
  border-radius: 4px;
}

.confirm-btn:hover {
  /* Darken by ~20% */
  border-color: #cc0000;
  color: #cc0000;
  background-color: #fff5f5; /* Light red tint for hover background */
}

/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-enter-active .delete-dialog,
.fade-leave-active .delete-dialog {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-enter-from .delete-dialog,
.fade-leave-to .delete-dialog {
  transform: scale(0.95);
}
</style>
