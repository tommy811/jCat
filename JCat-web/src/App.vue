<template>
  <v-app id="app">
     <GlobalMessage></GlobalMessage>
    <router-view />
  </v-app>
</template>
<script>
import GlobalMessage from "@/components/GlobalMessage.vue";

export default {
  name: "App",
  components: {
    GlobalMessage,
  },
};
</script>


<style>
body {
  margin: 0;
}
</style>
