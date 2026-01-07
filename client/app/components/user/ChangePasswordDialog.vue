<template>
  <Teleport to="body">
    <Transition name="fade-scale">
      <div v-if="visible" class="dialog-overlay" @click.self="close">
        <div class="dialog-box">
          <div class="dialog-header">
            <h3>更改密码</h3>
            <button class="close-btn" @click="close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-group">
              <label>当前密码</label>
              <input
                type="password"
                v-model="form.oldPassword"
                placeholder="请输入当前密码"
                @input="handleOldPasswordInput"
              />
              <div class="input-tip" :class="getTipClass(oldPasswordTip)">
                {{ oldPasswordTip }}
              </div>
            </div>
            <div class="form-group">
              <label>新密码</label>
              <input
                type="password"
                v-model="form.newPassword"
                placeholder="请输入新密码"
                maxlength="30"
                @input="handleNewPasswordInput"
              />
              <div class="input-tip" :class="getTipClass(newPasswordTip)">
                {{
                  newPasswordTip ||
                  "密码长度8-30个字符，必须包含大小写字母和数字"
                }}
              </div>
              <div class="password-strength" v-if="form.newPassword.length > 0">
                <div class="strength-bar" :class="passwordStrength"></div>
                <span class="strength-text">{{ passwordStrengthText }}</span>
              </div>
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <input
                type="password"
                v-model="form.confirmPassword"
                placeholder="请再次输入新密码"
                @input="handleConfirmPasswordInput"
              />
              <div class="input-tip" :class="getTipClass(confirmPasswordTip)">
                {{ confirmPasswordTip || "请再次输入相同的密码" }}
              </div>
            </div>
            <div class="message" v-if="errorMessage">
              {{ errorMessage }}
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn cancel-btn" @click="close">取消</button>
            <button
              class="btn confirm-btn"
              @click="handleSubmit"
              :disabled="loading"
            >
              {{ loading ? "提交中..." : "确认修改" }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { useUserApi } from "~/composables/api/useUserApi";

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(["close", "success"]);

const userApi = useUserApi();
const userStore = useUserStore();

const form = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// Tips
const oldPasswordTip = ref("");
const newPasswordTip = ref("");
const confirmPasswordTip = ref("");

// Validation state
const newPasswordValid = ref(false);
const confirmPasswordValid = ref(false);

const loading = ref(false);
const errorMessage = ref("");

// Debounce
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

// Password Strength
const passwordStrength = computed(() => {
  const password = form.newPassword;
  if (password.length === 0) return "";

  let score = 0;
  if (password.length >= 8) score++; // Changed to 8 per requirement
  if (password.length >= 10) score++;
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
  return "error"; // Always error style for tips in this context if they have content
};

const handleOldPasswordInput = () => {
  oldPasswordTip.value = "";
  errorMessage.value = "";
};

const validateNewPassword = (password: string) => {
  if (password.length === 0) {
    newPasswordValid.value = false;
    return "";
  }
  if (password.length < 8) {
    // Requirement: min 8
    newPasswordValid.value = false;
    return "密码长度至少需要8个字符";
  }
  if (password.length > 30) {
    newPasswordValid.value = false;
    return "密码长度不能超过30个字符";
  }
  if (!/[A-Z]/.test(password)) {
    newPasswordValid.value = false;
    return "密码必须包含至少一个大写字母";
  }
  if (!/[a-z]/.test(password)) {
    newPasswordValid.value = false;
    return "密码必须包含至少一个小写字母";
  }
  if (!/[0-9]/.test(password)) {
    newPasswordValid.value = false;
    return "密码必须包含至少一个数字";
  }
  newPasswordValid.value = true;
  return "";
};

const handleNewPasswordInput = debounce(() => {
  const password = form.newPassword;
  if (password.length > 0 && password.length < 8) {
    newPasswordTip.value = "密码长度至少需要8个字符";
    newPasswordValid.value = false;
  } else if (password.length >= 8) {
    newPasswordTip.value = validateNewPassword(password);
  } else {
    newPasswordTip.value = "";
    newPasswordValid.value = false;
  }
  if (form.confirmPassword) {
    validateConfirm();
  }
}, 300);

const validateConfirm = () => {
  if (form.confirmPassword.length === 0) {
    confirmPasswordValid.value = false;
    return "";
  }
  if (form.confirmPassword !== form.newPassword) {
    confirmPasswordValid.value = false;
    return "两次输入的密码不一致";
  }
  confirmPasswordValid.value = true;
  return "";
};

const handleConfirmPasswordInput = debounce(() => {
  confirmPasswordTip.value = validateConfirm();
}, 300);

const close = () => {
  emit("close");
  resetForm();
};

const resetForm = () => {
  form.oldPassword = "";
  form.newPassword = "";
  form.confirmPassword = "";
  errorMessage.value = "";
  oldPasswordTip.value = "";
  newPasswordTip.value = "";
  confirmPasswordTip.value = "";
  loading.value = false;
};

const handleSubmit = async () => {
  errorMessage.value = "";

  if (!form.oldPassword) {
    oldPasswordTip.value = "请输入当前密码";
    return;
  }

  const newPwdError = validateNewPassword(form.newPassword);
  newPasswordTip.value = newPwdError;

  const confirmError = validateConfirm();
  confirmPasswordTip.value = confirmError;

  if (!newPasswordValid.value || !confirmPasswordValid.value) {
    return;
  }

  loading.value = true;

  try {
    const res = await userApi.changePassword(userStore.user.id, {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    });
    if (res.code === 200) {
      ElMessage.success("密码修改成功，请重新登录");
      emit("success");
      close();
      userStore.logout();
    } else {
      showError(res.message || "修改失败");
    }
  } catch (error: any) {
    showError(error.message || "网络错误");
  } finally {
    loading.value = false;
  }
};

const showError = (msg: string) => {
  errorMessage.value = msg;
  setTimeout(() => {
    errorMessage.value = "";
  }, 3000);
};
</script>

<style scoped>
.dialog-overlay {
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
  z-index: 2000;
  transition: all 0.3s ease;
}

.dialog-box {
  background-color: #ffffff;
  border-radius: 16px;
  width: 400px;
  max-width: 90%;
  color: #1e293b;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transform: scale(1);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 0;
}

.dialog-header h3 {
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
  padding: 4px;
  border-radius: 4px;
}

.close-btn:hover {
  background-color: #f1f5f9;
  color: #475569;
}

.dialog-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.form-group input {
  width: 100%;
  padding: 10px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #1e293b;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #475569;
  background-color: #ffffff;
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

.message {
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.error-message {
  color: #ef4444;
  font-size: 13px;
  margin-top: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 24px 24px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
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
}

.confirm-btn {
  background-color: #475569;
  color: white;
}

.confirm-btn:hover {
  background-color: #334155;
}

.confirm-btn:disabled {
  background-color: #94a3b8;
  cursor: not-allowed;
}

/* Animations */
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: opacity 0.3s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
}

.fade-scale-enter-active .dialog-box,
.fade-scale-leave-active .dialog-box {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-scale-enter-from .dialog-box,
.fade-scale-leave-to .dialog-box {
  transform: scale(0.95);
}
</style>
