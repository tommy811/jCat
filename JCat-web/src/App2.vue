<template>
  <!-- App.vue -->

  <v-app>
    <v-navigation-drawer app dark mini-variant mini-variant-width="56px" permanent>
      <v-list-item >
        <v-list-item-avatar style="font-style: italic">
          <v-avatar
              color="primary"
              size="48"
          >JCat
          </v-avatar>
        </v-list-item-avatar>

      </v-list-item>
      <v-divider></v-divider>
      <v-list nav>
        <v-list-item to="/console">
          <v-list-item-icon>
            <v-icon> mdi-console</v-icon>
          </v-list-item-icon>
        </v-list-item>
        <v-list-item to="/resource">
          <v-list-item-icon>
            <v-icon>mdi-file-document-multiple-outline</v-icon>
          </v-list-item-icon>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>
    <v-main>
      <v-container fluid>
        <!-- 如果使用 vue-router -->
        <h5>{{ titleText }}</h5>
        <router-view></router-view>
      </v-container>
    </v-main>

  </v-app>
</template>
<script>
import GlobalMessage from "@/components/GlobalMessage.vue";

export default {
  name: "App",
  components: {
    GlobalMessage,
  },
  data: () => {
    return {}
  },
  computed: {
    titleText() {
      if (this.$route.path == "/console") {
        return "控制台"
      } else if (this.$route.path == "/resource") {
        return "资源管理器"
      }
      return "";
    }
  }
};
</script>


<style>
body {
  margin: 0;
}
</style>
