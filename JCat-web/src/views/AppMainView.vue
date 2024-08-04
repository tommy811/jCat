<template>
  <v-sheet class="d-flex">
    <!-- 应用主界面 -->
    <v-navigation-drawer
      :mini-variant.sync="drawerMini"
      width="300"
      permanent
      app
      style="background-color: rgb(247, 248, 250)"
    >
      <!--  -->
      <v-list dense>
        <v-subheader>代码示例</v-subheader>
        <v-divider></v-divider>
        <v-list-item @click="consoleCode=item.code" v-for="item in examples" :key="item.code"> 
            <v-list-item-content class="text-body-2">
              {{item.title}}
            </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <v-main style="min-height: 100vh; max-height: 100vh">
      <div>
        <v-tabs v-model="tab">
          <v-tab>控制台</v-tab>
        </v-tabs>
        <v-tabs-items v-model="tab">
          <v-tab-item>
            <ConsoleView :code="consoleCode"> </ConsoleView>
          </v-tab-item>
        </v-tabs-items>
      </div>
    </v-main>
  </v-sheet>
</template>

<script>
import ConsoleView from "@/components/Console.vue";
import {items as examples} from './Example.js'
import message from "@/store/message.js";
export default {
  name: "AppMainView",
  components: {
    ConsoleView,
  },
  data() {
    return {
      tab: 0, //
      drawerMini: false,
      loadIng: false,
      examples:examples,
      consoleCode:""
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