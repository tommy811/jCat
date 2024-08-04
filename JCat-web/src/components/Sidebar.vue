<template>
  <!-- 可拖动的边栏 -->
  <div :style="frameStyle" ref="src_frame" class="src_frame">
    <div class="d-flex" style="height: 100%; width: 100%">
      <div ref="divide" @mousedown="resizeSrc" class="divide flex-grow-0"></div>
      <div class="flex-grow-1 d-flex flex-column" style="overflow: auto">
        <v-toolbar  dense flat class="flex-grow-0">
          <slot name="toolbar"></slot>
          <v-spacer></v-spacer>
          <v-btn icon @click="$emit('close')">
            <v-icon small>mdi-close</v-icon>
          </v-btn>
        </v-toolbar>
        <v-divider class="flex-grow-0"></v-divider>
        <div class="flex-grow-1" style="overflow: auto;">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "SideBar",
  props: {
    top: String,
  },
  data() {
    return {};
  },
  computed: {
    frameStyle() {
      let style = "";
      style += this.top ? `top:${this.top}` : "";
      return style;
    },
  },
  methods: {
    resizeSrc(e) {
      let startX = e.clientX;
      let box = this.$refs.src_frame;
      let resize = this.$refs.divide;
      box.initWdith = box.clientWidth;
      document.onmousemove = function (e) {
        let moveLength = startX - e.clientX;
        box.style.width = box.initWdith + moveLength + "px";
      };
      document.onmouseup = function (evt) {
        document.onmousemove = null;
        document.onmouseup = null;
        resize.releaseCapture && resize.releaseCapture();
      };
      resize.setCapture && resize.setCapture();
      return false;
    },
  },
};
</script>

<style>
</style>
<style lang="scss" scoped>
.src_frame {
  z-index: 3;
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 30vw;
  background: #ffffff;
  .divide {
    z-index: 2;
    min-width: 2px;
    height: 100%;
    cursor: w-resize;
    background: #e0e0e0;
  }
}
</style>