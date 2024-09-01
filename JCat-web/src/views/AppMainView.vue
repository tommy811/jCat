<template>
  <v-sheet class="d-flex">
    <v-navigation-drawer dark mini-variant mini-variant-width="56px" permanent style="min-width: 56px;">
      <v-list-item class="px-2">
        <v-list-item-avatar>
          <v-img src="https://randomuser.me/api/portraits/women/75.jpg"></v-img>
        </v-list-item-avatar>
      </v-list-item>
      <v-divider></v-divider>
      <v-list dense nav>
        <v-list-item  href="#/console">
          <v-list-item-action>
            <v-icon>mdi-console</v-icon>
          </v-list-item-action>
        </v-list-item>
        <v-list-item  href="#/resource">
          <v-list-item-action >
            <v-icon>mdi-file-document-multiple-outline</v-icon>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>


    <v-main style="min-height: 100vh; max-height: 100vh">
      <div v-if=" path==='/console' || path==='/' ">
        <h4 class="mx-2 mt-2">控制台</h4>
        <ConsoleView :code="consoleCode"></ConsoleView>
      </div>
      <div v-else-if="path==='/resource'">
        <h4 class="mx-2 mt-2">资源管理器</h4>
        <ResourceExplorer></ResourceExplorer>
      </div>
    </v-main>
  </v-sheet>
</template>

<script>
import ConsoleView from "@/components/Console.vue";
import ResourceExplorer from "@/components/ResourceExplorer.vue";
// import { items as examples } from "./Example.js";
export default {
  name: "AppMainView",
  components: {
    ConsoleView,
    ResourceExplorer,
  },
  data() {
    return {
      tab: 0, //
      path: this.$route.path,
      drawerMini: false,
      loadIng: false,
      // examples: examples,
      items: [
        {title: "控制台", icon: "mdi-view-dashboard"},
        {title: "资源管理器", icon: "mdi-forum"},
      ],
      consoleCode: "",
    };
  },
  computed: {},
  methods: {},
  watch: {},
  mounted() {
  },
};
</script>

<style lang="scss" scoped>
.groupHeader .close {
  display: none;
}

// 显示移除按钮
.groupHeader:hover .close {
  display: unset;
}
</style>