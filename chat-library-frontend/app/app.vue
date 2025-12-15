<template>
  <div class="flex h-screen w-screen overflow-hidden bg-gray-50 text-gray-900 font-sans">
    <!-- Auth Modal -->
    <el-dialog v-model="showAuthModal" :title="isLoginMode ? 'Login' : 'Register'" width="400px" center destroy-on-close class="rounded-lg">
      <el-form :model="authForm" label-position="top" size="large">
        <el-form-item label="Username">
          <el-input v-model="authForm.username" placeholder="Enter username" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="authForm.password" type="password" placeholder="Enter password" @keyup.enter="handleAuth" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex flex-col gap-3">
          <el-button type="primary" size="large" class="w-full !rounded-md" @click="handleAuth" :loading="authLoading">
            {{ isLoginMode ? 'Sign In' : 'Create Account' }}
          </el-button>
          <div class="text-center text-sm">
             <span class="text-gray-500">{{ isLoginMode ? "Don't have an account?" : "Already have an account?" }}</span>
             <el-button link type="primary" @click="isLoginMode = !isLoginMode" class="ml-1">
               {{ isLoginMode ? 'Register' : 'Login' }}
             </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- Sidebar (Left) -->
    <div 
      class="bg-[#F9FAFB] border-r border-gray-200 flex flex-col transition-all duration-300 ease-in-out relative"
      :class="isSidebarOpen ? 'w-[260px]' : 'w-0'"
    >
      <!-- Sidebar Header -->
      <div class="p-4 border-b border-gray-200 flex justify-between items-center" v-if="isSidebarOpen">
        <el-button type="primary" class="w-full !justify-start !bg-white !text-gray-700 !border-gray-300 hover:!bg-gray-50 shadow-sm" @click="startNewChat">
          <el-icon class="mr-2"><Plus /></el-icon> New Chat
        </el-button>
      </div>

      <!-- Toggle Button (Outside when closed, Inside when open logic handled by absolute positioning if needed, or just a button in header) -->
      
      <!-- Content Area -->
      <div class="flex-1 overflow-y-auto overflow-x-hidden p-3" v-show="isSidebarOpen">
         <!-- Tabs -->
         <div class="flex mb-4 bg-gray-200 p-1 rounded-lg">
           <button 
             class="flex-1 text-xs py-1.5 rounded-md transition-all font-medium"
             :class="activeTab === 'chats' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700'"
             @click="activeTab = 'chats'"
           >
             Chats
           </button>
           <button 
             class="flex-1 text-xs py-1.5 rounded-md transition-all font-medium"
             :class="activeTab === 'docs' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-500 hover:text-gray-700'"
             @click="activeTab = 'docs'"
           >
             Documents
           </button>
         </div>

         <!-- Chat History List -->
         <div v-if="activeTab === 'chats'" class="space-y-1">
            <div v-if="!currentUser" class="text-center text-xs text-gray-400 py-4">Login to save history</div>
            <div v-else-if="loadingChats" class="py-4 text-center text-gray-400"><el-icon class="is-loading"><Loading /></el-icon></div>
            <div v-else-if="chatSessions.length === 0" class="text-center text-xs text-gray-400 py-4">No history yet</div>
            
            <div 
              v-for="chat in chatSessions" 
              :key="chat.id"
              class="group flex items-center gap-2 p-2 rounded-lg text-sm cursor-pointer transition-colors"
              :class="chatId === chat.sessionId ? 'bg-blue-50 text-blue-600' : 'hover:bg-gray-100 text-gray-700'"
              @click="loadChatSession(chat)"
            >
               <el-icon><ChatDotRound /></el-icon>
               <span class="truncate flex-1">{{ chat.title || 'New Chat' }}</span>
               <el-button 
                 link 
                 size="small" 
                 class="opacity-0 group-hover:opacity-100 text-gray-400 hover:!text-red-500"
                 @click.stop="deleteChatSession(chat.sessionId)"
               >
                 <el-icon><Delete /></el-icon>
               </el-button>
            </div>
         </div>

         <!-- Documents List -->
         <div v-if="activeTab === 'docs'" class="space-y-3">
             <el-upload
                class="w-full"
                :show-file-list="false"
                :http-request="uploadFile"
                accept=".pdf,.doc,.docx,.txt,.md"
                :disabled="!currentUser"
              >
                <div class="border-2 border-dashed border-gray-300 rounded-lg p-4 text-center hover:border-blue-500 hover:bg-blue-50 transition-colors cursor-pointer group">
                  <el-icon class="text-gray-400 group-hover:text-blue-500 text-xl mb-1"><UploadFilled /></el-icon>
                  <p class="text-xs text-gray-500">Upload File</p>
                </div>
              </el-upload>

             <div v-if="!currentUser" class="text-center text-xs text-gray-400">Login to manage docs</div>
             <div v-else-if="loadingDocs" class="py-4 text-center"><el-icon class="is-loading"><Loading /></el-icon></div>
             
             <div v-else class="space-y-1">
                <div 
                  v-for="doc in documents" 
                  :key="doc.id" 
                  class="group flex items-center gap-2 p-2 rounded-lg text-sm cursor-pointer transition-colors"
                  :class="currentDoc?.id === doc.id ? 'bg-blue-50 text-blue-600' : 'hover:bg-gray-100 text-gray-700'"
                  @click="selectDocument(doc)"
                >
                  <el-icon><Document /></el-icon>
                  <span class="truncate flex-1" :title="doc.originalFilename">{{ doc.originalFilename }}</span>
                  <el-button 
                    link 
                    size="small" 
                    class="opacity-0 group-hover:opacity-100 text-gray-400 hover:!text-red-500"
                    @click.stop="deleteDocument(doc.id)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
             </div>
         </div>
      </div>
      
      <!-- User Profile (Bottom) -->
      <div class="p-3 border-t border-gray-200" v-show="isSidebarOpen">
         <div v-if="currentUser" class="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-100 cursor-pointer">
            <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-xs">
              {{ currentUser.username.charAt(0).toUpperCase() }}
            </div>
            <div class="flex-1 min-w-0">
               <p class="text-sm font-medium text-gray-900 truncate">{{ currentUser.username }}</p>
            </div>
            <el-button link size="small" @click="logout"><el-icon><SwitchButton /></el-icon></el-button>
         </div>
         <el-button v-else type="primary" plain class="w-full" @click="showAuthModal = true">Log in / Sign up</el-button>
      </div>
    </div>

    <!-- Toggle Sidebar Button (Fixed on screen if closed, or relative) -->
    <div class="absolute left-4 top-4 z-50" v-show="!isSidebarOpen">
      <el-button circle shadow="always" @click="isSidebarOpen = true">
         <el-icon><Expand /></el-icon>
      </el-button>
    </div>
    <div class="absolute left-[260px] top-1/2 z-50 transform -translate-y-1/2" v-show="isSidebarOpen">
       <div class="w-4 h-8 bg-gray-200 rounded-r-full flex items-center justify-center cursor-pointer hover:bg-gray-300" @click="isSidebarOpen = false">
          <el-icon size="10" class="text-gray-600"><ArrowLeft /></el-icon>
       </div>
    </div>


    <!-- Middle: Document Preview -->
    <div class="flex-1 bg-gray-100 border-r border-gray-200 relative flex flex-col min-w-0">
       <div v-if="!currentDoc" class="flex-1 flex flex-col items-center justify-center text-gray-400 p-8 text-center">
          <el-icon size="64" class="mb-4 text-gray-300"><DocumentCopy /></el-icon>
          <h3 class="text-lg font-medium text-gray-600">No Document Selected</h3>
          <p class="text-sm mt-2 max-w-xs">Upload a document from the sidebar to start chatting with it.</p>
       </div>
       
       <div v-else class="flex-1 flex flex-col h-full bg-white">
          <div class="h-12 border-b border-gray-200 flex items-center px-4 justify-between bg-white shrink-0">
             <span class="font-medium text-gray-700 truncate">{{ currentDoc.originalFilename }}</span>
             <el-button link @click="currentDoc = null"><el-icon><Close /></el-icon></el-button>
          </div>
          
          <div class="flex-1 overflow-hidden relative bg-gray-50">
             <div v-if="previewLoading" class="absolute inset-0 flex items-center justify-center">
                <el-icon class="is-loading" size="32"><Loading /></el-icon>
             </div>
             
             <!-- PDF -->
             <iframe v-if="previewType === 'pdf'" :src="previewUrl" class="w-full h-full" frameborder="0"></iframe>
             <!-- Text/Markdown -->
             <div v-else-if="previewType === 'text' || previewType === 'markdown'" class="w-full h-full overflow-y-auto p-8 bg-white">
                 <div class="prose max-w-none" v-html="previewContent"></div>
             </div>
             <!-- DOCX -->
             <div v-else-if="previewType === 'docx'" ref="docxContainer" class="w-full h-full overflow-y-auto p-8 bg-white docx-wrapper"></div>
          </div>
       </div>
    </div>

    <!-- Right: Chat Area -->
    <div class="w-[450px] bg-white flex flex-col shadow-xl z-10 shrink-0">
      <!-- Chat Header -->
      <div class="h-14 border-b border-gray-100 flex items-center px-4 justify-between">
         <h2 class="font-semibold text-gray-800">Chat</h2>
         <!-- Right side header actions if needed -->
      </div>

      <!-- Messages -->
      <div class="flex-1 overflow-y-auto p-4 space-y-6 scroll-smooth" ref="chatContainer">
        <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
          <el-icon :size="48" class="mb-4 text-blue-100"><ChatLineRound /></el-icon>
          <p class="text-sm">Ask any question about your documents!</p>
        </div>

        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          class="flex flex-col gap-1"
          :class="msg.role === 'user' ? 'items-end' : 'items-start'"
        >
           <div class="flex items-center gap-2 text-xs text-gray-400 px-1">
              <span>{{ msg.role === 'user' ? 'You' : 'AI Assistant' }}</span>
           </div>
           
           <!-- Message Bubble -->
           <div 
             class="max-w-[90%] rounded-2xl p-3.5 text-sm leading-relaxed shadow-sm"
             :class="msg.role === 'user' ? 'bg-blue-600 text-white rounded-br-none' : 'bg-gray-100 text-gray-800 rounded-bl-none'"
           >
             <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="renderMessage(msg.content)"></div>
             <div v-else class="whitespace-pre-wrap">{{ msg.content }}</div>
           </div>
        </div>

        <!-- Typing Indicator -->
        <div v-if="isTyping" class="flex items-start gap-2">
           <div class="bg-gray-100 rounded-2xl rounded-bl-none p-3 px-4">
              <div class="flex space-x-1">
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
                <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
              </div>
           </div>
        </div>
      </div>

      <!-- Input -->
      <div class="p-4 border-t border-gray-100 bg-white">
        <div class="relative">
           <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="Type a message..."
            class="!w-full !text-sm custom-input"
            @keydown.enter.prevent="handleEnter"
            :disabled="isTyping"
          />
          <button 
            class="absolute right-2 bottom-2 p-1.5 rounded-md transition-colors"
            :class="inputMessage.trim() ? 'bg-blue-600 text-white hover:bg-blue-700' : 'bg-gray-200 text-gray-400 cursor-not-allowed'"
            @click="sendMessage"
            :disabled="!inputMessage.trim() || isTyping"
          >
             <el-icon><Position /></el-icon>
          </button>
        </div>
        <div class="text-[10px] text-gray-400 mt-2 text-center">
           AI can make mistakes. Check important info.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, reactive, watch } from 'vue'
import { 
  Plus, Document, Delete, ChatDotRound, Loading, View, 
  Expand, ArrowLeft, UploadFilled, SwitchButton, Close,
  ChatLineRound, Position, DocumentCopy
} from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { renderAsync } from 'docx-preview'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true
})

// UI State
const isSidebarOpen = ref(true)
const activeTab = ref('chats') // 'chats' | 'docs'

// Data State
const documents = ref<any[]>([])
const chatSessions = ref<any[]>([])
const messages = ref<{ role: string, content: string }[]>([])
const currentDoc = ref<any>(null)
const chatId = ref('')

// Loading State
const loadingDocs = ref(false)
const loadingChats = ref(false)
const isTyping = ref(false)
const authLoading = ref(false)

// Input
const inputMessage = ref('')
const chatContainer = ref<HTMLElement | null>(null)

// Auth
const currentUser = ref<any>(null)
const showAuthModal = ref(false)
const isLoginMode = ref(true)
const authForm = reactive({ username: '', password: '' })

// Preview
const previewLoading = ref(false)
const previewType = ref('')
const previewUrl = ref('')
const previewContent = ref('')
const docxContainer = ref<HTMLElement | null>(null)

// API
const API_BASE = 'http://localhost:8080'

// --- Markdown & Think Tag Rendering ---
const renderMessage = (content: string) => {
  if (!content) return ''
  
  // 1. Remove <think> tags completely for cleaner output if desired, OR
  // 2. Parse them properly. 
  // The issue might be that md.render is called TWICE or incorrectly nested.
  // Let's try a simpler approach: Extract think block, render it, then render the rest.

  const thinkRegex = /<think>([\s\S]*?)<\/think>/g
  let finalHtml = content

  // Replace think blocks with placeholders first to avoid markdown interference
  const thinkBlocks: string[] = []
  finalHtml = finalHtml.replace(thinkRegex, (match, p1) => {
    thinkBlocks.push(p1)
    return `__THINK_BLOCK_${thinkBlocks.length - 1}__`
  })

  // Render the main markdown
  finalHtml = md.render(finalHtml)

  // Put back the rendered think blocks
  thinkBlocks.forEach((blockContent, index) => {
    const renderedBlock = md.render(blockContent)
    const htmlBlock = `<div class="my-2 p-3 bg-gray-50 border border-gray-200 rounded-md text-xs text-gray-500 italic">
              <div class="font-semibold not-italic mb-1 text-gray-400 uppercase tracking-wider text-[10px]">Thinking Process</div>
              ${renderedBlock}
            </div>`
    finalHtml = finalHtml.replace(`<p>__THINK_BLOCK_${index}__</p>`, htmlBlock) // md.render might wrap it in <p>
    finalHtml = finalHtml.replace(`__THINK_BLOCK_${index}__`, htmlBlock) // Fallback
  })

  return finalHtml
}


// --- Auth Logic ---
const handleAuth = async () => {
  if (!authForm.username || !authForm.password) {
     ElMessage.warning('Please fill in all fields')
     return
  }
  
  authLoading.value = true
  const endpoint = isLoginMode.value ? '/user/login' : '/user/register'
  try {
    const res = await axios.post(`${API_BASE}${endpoint}`, authForm)
    currentUser.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    showAuthModal.value = false
    ElMessage.success(isLoginMode.value ? 'Welcome back!' : 'Account created!')
    initData()
  } catch (error) {
    ElMessage.error('Authentication failed. Please check your credentials.')
  } finally {
    authLoading.value = false
  }
}

const logout = () => {
  currentUser.value = null
  localStorage.removeItem('user')
  documents.value = []
  chatSessions.value = []
  messages.value = []
  currentDoc.value = null
  startNewChat() // Reset to blank state
  ElMessage.success('Logged out')
}

// --- Data Fetching ---
const initData = () => {
  if (currentUser.value) {
    fetchDocuments()
    fetchChatSessions()
  }
}

const fetchDocuments = async () => {
  if (!currentUser.value) return
  loadingDocs.value = true
  try {
    const res = await axios.get(`${API_BASE}/document/list?userId=${currentUser.value.id}`)
    documents.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loadingDocs.value = false
  }
}

const fetchChatSessions = async () => {
  if (!currentUser.value) return
  loadingChats.value = true
  try {
    const res = await axios.get(`${API_BASE}/ai/sessions?userId=${currentUser.value.id}`)
    chatSessions.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loadingChats.value = false
  }
}

// --- Sidebar Actions ---
const startNewChat = () => {
  chatId.value = crypto.randomUUID()
  messages.value = []
  // Don't clear currentDoc, user might want to keep looking at it
}

const loadChatSession = async (session: any) => {
  chatId.value = session.sessionId
  try {
    const res = await axios.get(`${API_BASE}/ai/history/${session.sessionId}`)
    // Map backend history to frontend format
    messages.value = res.data.map((msg: any) => ({
      role: msg.messageType.toLowerCase(),
      content: msg.content
    }))
    scrollToBottom()
  } catch (error) {
    ElMessage.error('Failed to load chat history')
  }
}

const deleteChatSession = async (sid: string) => {
  try {
    await axios.delete(`${API_BASE}/ai/session/${sid}`)
    if (chatId.value === sid) {
       startNewChat()
    }
    fetchChatSessions()
  } catch (error) {
    ElMessage.error('Failed to delete chat')
  }
}

// --- Document Actions ---
const uploadFile = async (options: any) => {
  if (!currentUser.value) return
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('userId', currentUser.value.id)
  
  try {
    await axios.post(`${API_BASE}/document/upload`, formData)
    ElMessage.success('File uploaded')
    fetchDocuments()
  } catch (error) {
    ElMessage.error('Upload failed')
  }
}

const deleteDocument = async (id: number) => {
  try {
    await axios.delete(`${API_BASE}/document/${id}`)
    ElMessage.success('Document deleted')
    if (currentDoc.value?.id === id) {
      currentDoc.value = null
    }
    fetchDocuments()
  } catch (error) {
    ElMessage.error('Delete failed')
  }
}

const selectDocument = async (doc: any) => {
  currentDoc.value = doc
  previewLoading.value = true
  previewType.value = ''
  previewUrl.value = ''
  previewContent.value = ''
  
  const fileExt = doc.originalFilename.split('.').pop().toLowerCase()
  const url = `${API_BASE}/document/preview/${doc.id}`

  try {
      if (fileExt === 'pdf') {
          previewType.value = 'pdf'
          previewUrl.value = url
      } else if (fileExt === 'txt' || fileExt === 'md') {
          const res = await axios.get(url, { responseType: 'text' })
          if (fileExt === 'md') {
              previewType.value = 'markdown'
              previewContent.value = md.render(res.data)
          } else {
              previewType.value = 'text'
              previewContent.value = `<pre class="whitespace-pre-wrap font-mono text-sm">${res.data}</pre>`
          }
      } else if (fileExt === 'docx') {
          previewType.value = 'docx'
          const res = await axios.get(url, { responseType: 'blob' })
          await nextTick()
          if (docxContainer.value) {
              await renderAsync(res.data, docxContainer.value)
          }
      } else {
          previewType.value = 'unsupported'
          previewContent.value = '<p>Preview not supported</p>'
      }
  } catch (e) {
      console.error(e)
      ElMessage.error("Failed to load document")
  } finally {
      previewLoading.value = false
  }
}

// --- Chatting ---
const handleEnter = (e: any) => {
  if (e instanceof KeyboardEvent && !e.shiftKey) {
    sendMessage()
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isTyping.value) return
  
  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  isTyping.value = true
  scrollToBottom()

  if (!chatId.value) {
    startNewChat()
  }
  
  // Optimistic UI update for sessions list if it's new
  // (In real app, backend returns new session, here we just refetch after a delay or on completion)

  try {
    const userIdParam = currentUser.value ? `&userId=${currentUser.value.id}` : ''
    const response = await fetch(`${API_BASE}/ai/chat?prompt=${encodeURIComponent(userMsg)}&chatId=${chatId.value}${userIdParam}`)
    
    if (!response.body) throw new Error('No response body')
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let aiMsgContent = ''
    
    messages.value.push({ role: 'assistant', content: '' })
    const aiMsgIndex = messages.value.length - 1
    
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        if (line.trim().startsWith('data:')) {
           const content = line.trim().substring(5)
           aiMsgContent += content
        }
      }

      if (messages.value[aiMsgIndex]) {
        messages.value[aiMsgIndex].content = aiMsgContent
      }
      scrollToBottom()
    }
    
    // Refresh sessions list to show title if it was new
    if (currentUser.value) {
       fetchChatSessions()
    }
    
  } catch (error) {
    console.error(error)
    messages.value.push({ role: 'assistant', content: 'Error: Could not get response.' })
  } finally {
    isTyping.value = false
    scrollToBottom()
  }
}

// Init
onMounted(() => {
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    currentUser.value = JSON.parse(savedUser)
    initData()
  }
  startNewChat()
})
</script>

<style scoped>
/* Custom Scrollbar for Chat */
.custom-input :deep(.el-textarea__inner) {
  box-shadow: none !important;
  padding-right: 3rem;
  background-color: transparent;
}

/* Markdown Styles override */
:deep(.markdown-body) {
  font-size: 0.875rem;
  background-color: transparent;
}
:deep(.markdown-body p) {
  margin-bottom: 0.5rem;
}
:deep(.markdown-body pre) {
  background-color: #2d2d2d;
  color: #ccc;
  padding: 0.5rem;
  border-radius: 0.375rem;
}
</style>