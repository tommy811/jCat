<template>
  <div>
    <v-snackbar
      :value="show"
      @input="close"
      :color="type"
      :timeout="timeout"
      :top="!pos||pos.charAt(0)=='t'"	
      :centered="pos.charAt(0)=='c'"
      :bottom="pos.charAt(0)=='b'"
      :left="pos.charAt(1)=='l'"
      :right="pos.charAt(1)=='r'"
      text
      outlined
    >
      <v-icon v-if="icon" :color="type" >{{icon}}</v-icon>
      {{ this.message }}
      <template v-slot:action="{ attrs }">
        <v-btn  plain	x-small text v-bind="attrs" @click="close">
          <!-- <v-icon :color="type">mdi-close</v-icon> -->
          关闭
        </v-btn>
      </template>
    </v-snackbar>

    <v-dialog v-if="alert" max-width="300" :value="alert.show" @input="close">
      <v-card>
        <v-card-title class="text-h5"> {{ alert.title }} </v-card-title>
        <v-card-text>
          {{ alert.message }}
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="close" text> 取消 </v-btn>
          <v-btn @click="enter" autofocus color="red darken-1"> 确认 </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import { mapState } from "vuex";

export default {
  data() {
    return {
      color: "info",
    };
  },
  methods: {
    close() {
      this.$store.commit("message/close");
    },
    enter(){
      this.alert.affirm();
      this.close();
    }
  },
  computed: {
    ...mapState("message", {
      message: (state) => state.msg,
      pos:(state) => state.pos,
      icon: (state) => state.icon,
      show: (state) => state.show,
      timeout: (state) => state.timeout,
      type: (state) => state.type,
      alert: (state) => state.alert,
    }),
  },
};
</script>

<style>
</style>