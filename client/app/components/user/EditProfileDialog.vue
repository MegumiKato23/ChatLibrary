<template>
  <Teleport to="body">
    <Transition name="fade-scale">
      <div v-if="visible" class="dialog-overlay" @click.self="close">
        <div class="dialog-box">
          <div class="dialog-header">
            <h3>个人信息</h3>
            <button class="close-btn" @click="close">×</button>
          </div>
          <div class="dialog-body">
            <div class="form-group">
              <label>用户名</label>
              <input
                type="text"
                v-model="form.username"
                placeholder="请输入用户名"
              />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input
                type="email"
                v-model="form.email"
                placeholder="请输入邮箱"
              />
            </div>
            <div class="error-message" v-if="errorMessage">{{ errorMessage }}</div>
          </div>
          <div class="dialog-footer">
            <button class="btn cancel-btn" @click="close">取消</button>
            <button class="btn confirm-btn" @click="handleSubmit" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { useUserApi } from '~/composables/api/useUserApi';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits(["close", "success"]);

const userApi = useUserApi();
const userStore = useUserStore();

const form = reactive({
  username: "",
  email: "",
});

const loading = ref(false);
const errorMessage = ref("");

watch(() => props.visible, (val) => {
  if (val && userStore.user) {
    form.username = userStore.user.username || "";
    form.email = userStore.user.email || "";
  }
});

const close = () => {
  emit("close");
  errorMessage.value = "";
};

const handleSubmit = async () => {
  if (!form.username) {
    errorMessage.value = "用户名不能为空";
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try { 
    const res = await userApi.update(userStore.user.id, {
      username: form.username,
      email: form.email,
    });
    if (res.code === 200) {
      ElMessage.success("个人信息更新成功");
      userStore.user = { ...userStore.user, ...res.data };
      emit("success");
      close();
    } else {
      errorMessage.value = res.message || "更新失败";
    }
  } catch (error: any) {
    errorMessage.value = error.message || "网络错误";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Reuse styles */
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