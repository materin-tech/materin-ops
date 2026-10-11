import { createApp } from 'vue';
import Vant from 'vant';

import App from './App.vue';
import router from './router';

import 'vant/lib/index.css';
import './style.css';

createApp(App).use(router).use(Vant).mount('#app');
