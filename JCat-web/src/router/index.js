import Vue from 'vue'
import VueRouter from 'vue-router'
import ConsoleView from '../components/Console.vue'
import AppMainView from "../views/AppMainView.vue"

Vue.use(VueRouter)

const routes = [
  {
    path: "/console",
    component: ConsoleView
  },
  {
    path: "/",
    component: AppMainView
  },
]

const router = new VueRouter({
  routes
})

export default router
