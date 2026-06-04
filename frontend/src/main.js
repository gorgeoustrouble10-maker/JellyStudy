import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { applyUiPreferences, getStoredPreferences } from './composables/useUiTheme.js'

applyUiPreferences(getStoredPreferences())

createApp(App).mount('#app')
