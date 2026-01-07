<template>
  <Transition name="fade-scale">
    <div v-if="userStore.isLoginModalVisible" class="modal-container">
      <KeepAlive>
        <LoginDialog
          v-if="activeTab === 'login'"
          :visible="true"
          @close="userStore.hideLoginModal"
          @register="activeTab = 'register'"
        />
        <RegisterDialog
          v-else
          :visible="true"
          @close="userStore.hideLoginModal"
          @login="activeTab = 'login'"
        />
      </KeepAlive>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import LoginDialog from "./login/LoginDialog.vue";
import RegisterDialog from "./login/RegisterDialog.vue";

const userStore = useUserStore();
const activeTab = ref("login");

watch(
  () => userStore.isLoginModalVisible,
  (visible) => {
    if (visible) {
      activeTab.value = "login";
    }
  }
);
</script>

<style scoped>
.modal-container {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
