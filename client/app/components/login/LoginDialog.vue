<template>
  <div>
    <div class="login-overlay" v-if="visible" @click.self="close">
      <div class="login-dialog">
        <div class="login-header">
          <h2>登录到 ChatLibrary</h2>
          <button class="close-btn" @click="close">×</button>
        </div>
        <div class="login-body">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
              type="text"
              id="username"
              v-model="loginForm.username"
              placeholder="请输入用户名"
              @input="handleInput"
            />
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input
              type="password"
              id="password"
              v-model="loginForm.password"
              placeholder="请输入密码"
              @input="handleInput"
            />
          </div>
          <div class="error-message" v-if="errorMessage">
            {{ errorMessage }}
          </div>
        </div>
        <div class="login-footer">
          <button
            type="button"
            class="btn register-btn"
            @click="switchToRegister"
          >
            注册账号
          </button>
          <button
            type="button"
            class="btn login-submit-btn"
            @click="handleLogin"
            :disabled="isLoading"
          >
            {{ isLoading ? "登录中..." : "登录" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(["close", "register"]);

const userStore = useUserStore();
const chatStore = useChatStore();
const documentStore = useDocumentStore();

const errorMessage = ref("");
const isLoading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
});

const close = () => {
  emit("close");
  // 清空表单和错误信息
  loginForm.username = "";
  loginForm.password = "";
  errorMessage.value = "";
};

const loadData = async () => {
  await Promise.allSettled([
    chatStore.fetchConversations(),
    documentStore.fetchDocuments(),
  ]);
};

const handleLogin = async () => {
  // 1. 表单基本验证
  if (!loginForm.username.trim()) {
    errorMessage.value = "请输入用户名";
    return;
  }
  if (!loginForm.password.trim()) {
    errorMessage.value = "请输入密码";
    return;
  }

  isLoading.value = true;
  errorMessage.value = "";

  try {
    // 2. 调用 Store 登录方法
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
    });

    // 3. 验证登录状态
    if (userStore.isAuthenticated) {
      ElMessage.success("登录成功，欢迎回来");
      close();
      loadData();
    } else {
      throw new Error("登录状态异常，请重试");
    }
  } catch (e: any) {
    // 错误处理
    errorMessage.value = "用户名或密码错误";
  } finally {
    isLoading.value = false;
    if (!errorMessage.value) {
      resetForm();
    }
  }
};

const resetForm = () => {
  loginForm.password = "";
  errorMessage.value = "";
};

const handleInput = () => {
  errorMessage.value = "";
};

const switchToRegister = () => {
  emit("register");
};
</script>

<style scoped>
/* ... existing styles ... */
.register-btn:active {
  transform: scale(0.95);
  background-color: #f1f5f9;
}

.login-overlay {
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
  z-index: 1000;
  transition: all 0.3s ease;
}

.login-dialog {
  background-color: #ffffff;
  border-radius: 16px;
  width: 400px;
  max-width: 90%;
  color: #1e293b;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transform: scale(1);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.login-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
}

.login-header h2 {
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

.login-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

input {
  width: 100%;
  padding: 12px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #1e293b;
  font-size: 14px;
  transition: all 0.2s;
}

input:focus {
  outline: none;
  border-color: #475569;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(71, 85, 105, 0.1);
}

.error-message {
  color: #ef4444;
  font-size: 13px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  animation: shake 0.3s ease-in-out;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-2px);
  }
  75% {
    transform: translateX(2px);
  }
}

.login-footer {
  display: flex;
  justify-content: space-between;
  padding: 0 24px 24px;
  margin-top: -8px;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.register-btn {
  background-color: transparent;
  color: #64748b;
  border: 1px solid transparent;
}

.register-btn:hover {
  color: #475569;
  background-color: #f1f5f9;
}

.login-submit-btn {
  background-color: #475569;
  color: white;
  min-width: 100px;
}

.login-submit-btn:hover {
  background-color: #334155;
  transform: translateY(-1px);
}

.login-submit-btn:disabled {
  background-color: #94a3b8;
  cursor: not-allowed;
  transform: none;
}

/* Toast Styles - Updated for Light Theme */
.login-toast {
  position: fixed;
  top: 24px;
  right: 50%;
  transform: translateX(50%);
  z-index: 9999;
  animation: slideInDown 0.3s ease-out, slideOutUp 0.3s ease-in 2.7s forwards;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1),
    0 4px 6px -2px rgba(0, 0, 0, 0.05);
  font-size: 14px;
  font-weight: 500;
  min-width: 300px;
}

.toast-content.success {
  border-left: 4px solid #10b981;
  color: #1e293b;
}

.toast-content.error {
  border-left: 4px solid #ef4444;
  color: #1e293b;
}

.toast-icon {
  font-size: 18px;
  color: #10b981;
}

.toast-content.error .toast-icon {
  color: #ef4444;
}

.toast-close {
  margin-left: auto;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
}

.toast-close:hover {
  color: #475569;
}

@keyframes slideInDown {
  from {
    transform: translateX(50%) translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateX(50%) translateY(0);
    opacity: 1;
  }
}

@keyframes slideOutUp {
  from {
    transform: translateX(50%) translateY(0);
    opacity: 1;
  }
  to {
    transform: translateX(50%) translateY(-20px);
    opacity: 0;
  }
}

/* Responsive */
@media (max-width: 640px) {
  .login-dialog {
    width: 100%;
    height: 100%;
    max-width: 100%;
    border-radius: 0;
    display: flex;
    flex-direction: column;
  }

  .login-body {
    flex: 1;
  }

  .login-footer {
    padding-bottom: 40px;
  }
}
</style>
