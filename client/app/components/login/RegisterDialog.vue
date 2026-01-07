<template>
  <div class="register-overlay" v-if="visible" @click.self="close">
    <div class="register-dialog">
      <div class="register-header">
        <h2>注册 ChatLibrary 账号</h2>
        <button class="close-btn" @click="close">×</button>
      </div>
      <div class="register-body">
        <div class="form-group">
          <label for="username"> 用户名</label>
          <input
            type="text"
            id="username"
            v-model="registerForm.username"
            placeholder="请输入用户名"
            maxlength="20"
            @input="handleUsernameInputChange"
          />
          <div class="input-tip" :class="getTipClass(usernameTip)">
            {{
              usernameTip ||
              "用户名长度3-20个字符，支持字母、数字、下划线和中文"
            }}
          </div>
        </div>
        <div class="form-group">
          <label for="account">邮箱</label>
          <input
            type="text"
            id="account"
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            maxlength="30"
            @input="handleEmailInputChange"
            @blur="handleEmailBlur"
          />
          <div class="input-tip" :class="getTipClass(emailTip)">
            {{ emailTip || "请输入正确的邮箱格式" }}
          </div>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="registerForm.password"
            placeholder="请输入密码"
            maxlength="30"
            @input="handlePasswordInputChange"
          />
          <div class="input-tip" :class="getTipClass(passwordTip)">
            {{ passwordTip || "密码长度6-30个字符，必须包含大小写字母和数字" }}
          </div>
          <div
            class="password-strength"
            v-if="registerForm.password.length > 0"
          >
            <div class="strength-bar" :class="passwordStrength"></div>
            <span class="strength-text">{{ passwordStrengthText }}</span>
          </div>
        </div>
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input
            type="password"
            id="confirmPassword"
            v-model="registerForm.confirmPassword"
            placeholder="请再次输入密码"
            @input="handleConfirmPasswordInputChange"
          />
          <div class="input-tip" :class="getTipClass(confirmPasswordTip)">
            {{ confirmPasswordTip || "请再次输入相同的密码" }}
          </div>
        </div>
        <div
          class="message"
          :class="isRegistered ? 'success' : ''"
          v-if="message"
        >
          {{ message }}
        </div>
      </div>
      <div class="register-footer">
        <button
          class="btn login-btn"
          @click="switchToLogin"
          :disabled="isLoading"
        >
          返回登录
        </button>
        <button
          class="btn register-submit-btn"
          @click="handleRegister"
          :disabled="isLoading"
        >
          <div class="button-content">
            <span v-if="isLoading" class="loading-spinner"></span>
            <span>{{ isLoading ? "注册中..." : "注册" }}</span>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  const userStore = useUserStore();
  
  // 提示信息
  const usernameTip = ref("");
  const emailTip = ref("");
  const passwordTip = ref("");
  const confirmPasswordTip = ref("");
  
  // 验证状态
  const usernameValid = ref(false);
  const emailValid = ref(false);
  const passwordValid = ref(false);
  const confirmPasswordValid = ref(false);
  
  const message = ref("");
  const isRegistered = ref(false);
  const isLoading = ref(false); // 添加加载状态变量
  
  const registerForm = reactive({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  
// 防抖函数
const debounce = (func: Function, wait: number) => {
  let timeout: ReturnType<typeof setTimeout>;
  return function executedFunction(...args: any[]) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(["close", "login"]);


// 密码强度计算
const passwordStrength = computed(() => {
  const password = registerForm.password;
  if (password.length === 0) return "";

  let score = 0;
  if (password.length >= 6) score++;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;

  if (score <= 2) return "weak";
  if (score <= 4) return "medium";
  return "strong";
});

const passwordStrengthText = computed(() => {
  switch (passwordStrength.value) {
    case "weak":
      return "弱";
    case "medium":
      return "中等";
    case "strong":
      return "强";
    default:
      return "";
  }
});

const getTipClass = (tipValue: string) => {
  if (!tipValue) return "";
  return "error";
};

const close = () => {
  emit("close");
  resetForm();
};

const handleRegister = async () => {
  // 如果已经在加载中，则不执行
  if (isLoading.value) return;

  isLoading.value = true;

  // 清空之前的错误信息
  message.value = "";
  // 执行完整验证
  const usernameError = validateUsername(registerForm.username.trim());
  const emailError = validateEmail(registerForm.email.trim());
  const passwordError = validatePassword(registerForm.password);
  const confirmPasswordError = validateConfirmPassword();

  // 更新提示信息
  usernameTip.value = usernameError;
  emailTip.value = emailError;
  passwordTip.value = passwordError;
  confirmPasswordTip.value = confirmPasswordError;

  // 检查基本字段是否为空
  if (!registerForm.username.trim()) {
    message.value = "请输入用户名";
    isLoading.value = false;
    return;
  }
  if (!registerForm.email.trim()) {
    message.value = "请输入邮箱";
    isLoading.value = false;
    return;
  }
  if (!registerForm.password.trim()) {
    message.value = "请输入密码";
    isLoading.value = false;
    return;
  }
  if (!registerForm.confirmPassword.trim()) {
    message.value = "请确认密码";
    isLoading.value = false;
    return;
  }

  // 检查验证状态
  if (
    !usernameValid.value ||
    !emailValid.value ||
    !passwordValid.value ||
    !confirmPasswordValid.value
  ) {
    message.value = "请修正表单中的错误后再提交";
    isLoading.value = false;
    return;
  }

  try {
    // 使用 userStore.register
    await userStore.register({
      username: registerForm.username.trim(),
      email: registerForm.email.trim(), 
      password: registerForm.password,
    });

    // 注册成功后自动登录
    message.value = "注册成功！";
    isRegistered.value = true;

    // 非阻塞式加载：先关闭弹窗，后台静默加载数据
    // 延时一点关闭以便展示成功消息
    setTimeout(() => {
      resetForm();
      close();
    }, 1000);
  } catch (error: any) {
    console.error("注册错误:", error);
    if (error.response?.status === 1001) {
      message.value = "该邮箱已被注册，请使用其他邮箱";
    } else if (error.response?.status === 400) {
      message.value = "请求参数错误，请检查输入信息";
    } else {
      message.value = error.message || "注册失败，请稍后重试";
    }
  } finally {
    isLoading.value = false;
  }
};

const switchToLogin = () => {
  message.value = "";
  emit("login");
};

// 用户名验证
const validateUsername = (username: string) => {
  if (username.length === 0) {
    usernameValid.value = false;
    return "";
  }

  if (username.length < 3) {
    usernameValid.value = false;
    return "用户名长度至少需要3个字符";
  }

  if (username.length > 20) {
    usernameValid.value = false;
    return "用户名长度不能超过20个字符";
  }

  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(username)) {
    usernameValid.value = false;
    return "用户名只能包含字母、数字、下划线和中文";
  }

  usernameValid.value = true;
  return "";
};

const handleUsernameInputChange = debounce(() => {
  const username = registerForm.username;
  if (username.length >= 3) {
    usernameTip.value = validateUsername(username);
  } else if (username.length > 0) {
    usernameTip.value = "用户名长度至少需要3个字符";
    usernameValid.value = false;
  } else {
    usernameTip.value = "";
    usernameValid.value = false;
  }
}, 300);

// 邮箱验证
const validateEmail = (email: string) => {
  if (email.length === 0) {
    emailValid.value = false;
    return "";
  }

  if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
    emailValid.value = false;
    return "请输入正确的邮箱格式";
  }

  emailValid.value = true;
  return "";
};

const handleEmailInputChange = debounce(() => {
  const email = registerForm.email;
  if (email.length > 0) {
    emailTip.value = validateEmail(email);
  } else {
    emailTip.value = "";
    emailValid.value = false;
  }
}, 300);

const handleEmailBlur = () => {
  if (registerForm.email) {
    emailTip.value = validateEmail(registerForm.email);
  }
};

// 密码验证
const validatePassword = (password: string) => {
  if (password.length === 0) {
    passwordValid.value = false;
    return "";
  }

  if (password.length < 6) {
    passwordValid.value = false;
    return "密码长度至少需要6个字符";
  }

  if (password.length > 30) {
    passwordValid.value = false;
    return "密码长度不能超过30个字符";
  }

  if (!/[A-Z]/.test(password)) {
    passwordValid.value = false;
    return "密码必须包含至少一个大写字母";
  }

  if (!/[a-z]/.test(password)) {
    passwordValid.value = false;
    return "密码必须包含至少一个小写字母";
  }

  if (!/[0-9]/.test(password)) {
    passwordValid.value = false;
    return "密码必须包含至少一个数字";
  }

  passwordValid.value = true;
  return "";
};

const handlePasswordInputChange = debounce(() => {
  const password = registerForm.password;

  // 渐进式验证：先检查长度，再检查复杂度
  if (password.length > 0 && password.length < 6) {
    passwordTip.value = "密码长度至少需要6个字符";
    passwordValid.value = false;
  } else if (password.length >= 6) {
    passwordTip.value = validatePassword(password);
  } else {
    passwordTip.value = "";
    passwordValid.value = false;
  }

  // 同时验证确认密码
  if (registerForm.confirmPassword) {
    validateConfirmPassword();
  }
}, 300);

// 确认密码验证
const validateConfirmPassword = () => {
  const confirmPassword = registerForm.confirmPassword;
  const password = registerForm.password;

  if (confirmPassword.length === 0) {
    confirmPasswordValid.value = false;
    return "";
  }

  if (confirmPassword !== password) {
    confirmPasswordValid.value = false;
    return "两次输入的密码不一致";
  }

  confirmPasswordValid.value = true;
  return "";
};

const handleConfirmPasswordInputChange = debounce(() => {
  confirmPasswordTip.value = validateConfirmPassword();
}, 300);

const resetForm = () => {
  registerForm.username = "";
  registerForm.email = "";
  registerForm.password = "";
  registerForm.confirmPassword = "";

  //提示信息以及验证状态更新
  usernameTip.value = "";
  emailTip.value = "";
  passwordTip.value = "";
  confirmPasswordTip.value = "";
  usernameValid.value = false;
  emailValid.value = false;
  passwordValid.value = false;
  confirmPasswordValid.value = false;
  message.value = "";
};
</script>

<style scoped>
.register-overlay {
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

.register-dialog {
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

.register-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
}

.register-header h2 {
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

.register-body {
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

input.error {
  border-color: #ef4444;
}

.message {
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.success {
  color: #10b981;
}

.register-footer {
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

.input-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 6px;
  transition: all 0.3s ease;
  min-height: 18px;
}

.input-tip.error {
  color: #ef4444;
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

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  height: 4px;
  width: 100px;
  border-radius: 2px;
  background-color: #e2e8f0;
  position: relative;
  overflow: hidden;
}

.strength-bar::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.strength-bar.weak::after {
  width: 33%;
  background-color: #ef4444;
}

.strength-bar.medium::after {
  width: 66%;
  background-color: #f59e0b;
}

.strength-bar.strong::after {
  width: 100%;
  background-color: #10b981;
}

.strength-text {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.strength-bar.weak + .strength-text {
  color: #ef4444;
}

.strength-bar.medium + .strength-text {
  color: #f59e0b;
}

.strength-bar.strong + .strength-text {
  color: #10b981;
}

.login-btn {
  background-color: transparent;
  color: #64748b;
  border: 1px solid transparent;
}

.login-btn:hover {
  color: #475569;
  background-color: #f1f5f9;
}

.register-submit-btn {
  background-color: #475569;
  color: white;
  min-width: 100px;
}

.register-submit-btn:hover {
  background-color: #334155;
  transform: translateY(-1px);
}

.register-submit-btn:disabled,
.login-btn:disabled {
  background-color: #94a3b8;
  cursor: not-allowed;
  transform: none;
  opacity: 1;
}

.login-btn:disabled {
  background-color: transparent;
  color: #cbd5e1;
}

.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 8px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Responsive */
@media (max-width: 640px) {
  .register-dialog {
    width: 100%;
    height: 100%;
    max-width: 100%;
    border-radius: 0;
    display: flex;
    flex-direction: column;
  }

  .register-body {
    flex: 1;
    overflow-y: auto;
  }

  .register-footer {
    padding-bottom: 40px;
  }
}
</style>
