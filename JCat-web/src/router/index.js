import Vue from 'vue'
import VueRouter from 'vue-router'
import console from "../components/Console.vue"
import resourceExplorer from "../components/ResourceExplorer.vue"


Vue.use(VueRouter)

const routes = [
  {
    path: "/console",
    component: console,
  },
  {
    path: "/",
    component: console
  },
  {
    path: "/resource",
    component: resourceExplorer
  },
]

const router = new VueRouter({
  routes
})

export default router
