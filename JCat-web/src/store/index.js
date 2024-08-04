import Vuex from 'vuex'
import Vue from 'vue'
import message from "@/store/message"
Vue.use(Vuex);

// 创建一个新的 store 实例
const store = new Vuex.Store({
    modules: {
        message: message.store
    }
});
export default store;