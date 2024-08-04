<template>
  <v-sheet class="console d-flex flex-column" height="100%">
    <v-toolbar dense flat tile class="flex-grow-0">
      <v-btn :disabled="!loaderId || !inputCode" text small @click="doEval">
        <v-icon color="green">mdi-play</v-icon>执行</v-btn
      >
      <v-btn text plain small @click="doClear">
        <v-icon>mdi-refresh</v-icon></v-btn
      >
      <!-- <v-btn text plain small @click="doSave">
        <v-icon>mdi-content-save</v-icon></v-btn
      >
      <v-btn v-show="!showSearch" text plain small @click="showSearch = true">
        <v-icon>mdi-magnify</v-icon></v-btn
      > -->
      <v-autocomplete
        v-if="showSearch && codeFiles"
        ref="autocompleteFileSearch"
        dense
        @blur="showSearch = false"
        @change="doOpenFile"
        autofocus
        :items="codeFiles"
        hide-details=""
        prepend-inner-icon="mdi-magnify"
      ></v-autocomplete>
      <v-spacer></v-spacer>
      <v-select
        v-model="loaderId"
        class="mt-2"
        style="max-width: 300px"
        hide-details
        label="类加载器"
        item-value="id"
        item-text="type"
        :items="classLoaders"
      >
        <template v-slot:prepend-item>
          <v-subheader>请选择ClassLoader:</v-subheader>
        </template>
      </v-select>
      <v-btn :disabled="true" plain small>
        <v-icon small>mdi-cog</v-icon>设置</v-btn
      >
    </v-toolbar>
    <v-divider></v-divider>
    <v-chip
      v-if="showTips"
      style="background-color: rgb(233, 233, 233)"
      close-icon="mdi-close"
      label
      close
      @click:close="showTips = false"
    >
      <div class="d-flex text-caption">
        <div class="mx-2" v-for="shortcut of shortcutKeys" :key="shortcut.key">
          {{ shortcut.name
          }}<span class="text--secondary pl-1">{{ shortcut.key }}</span>
        </div>
      </div>
    </v-chip>

    <v-list
      dense
      class="flex-grow-1 overflow-y-auto"
      max-height="calc(100vh - 100px)"
    >
      <!-- 执行记录 -->
      <v-list-item
        class="eval history"
        v-for="(history, index) of this.evalHistory"
        :key="index"
      >
        <!-- 输入代码 -->
        <pre v-if="history.code"><code>{{history.code.trim()}}</code></pre>
        <div v-else-if="history.error" style="width: 100%" class="evalError">
          <v-icon class="mx-2" small color="error">mdi-alert-octagon</v-icon>
          <pre v-if="history.detail">{{ history.error.errorStack }}</pre>
          <pre v-else
            >{{ history.error.errorType }}:{{ history.error.errorMessage }}</pre
          >
          <v-btn text color="#410002" @click="history.detail = !history.detail"
            >详情..</v-btn
          >
        </div>
        <!-- 输出基础值 -->
        <div v-else-if="history.result && history.result.atomic">
          <span
            v-if="history.result.type == 'java.lang.String'"
            class="green--text text--darken-3"
          >
            "{{ history.result.value }}"
          </span>
          <span v-else>{{ history.result.value }}</span>
        </div>
        <!-- 输出对象 -->
        <div v-else-if="history.result">
          <!-- {{history.result.type}}@{{history.result.id}} -->
          <!-- TODO 相同的对象 ID相同 Duplicate keys detected: 'vvuuuq'. This may cause an update error. -->
          <v-treeview
            open-on-click
            transition
            dense
            :items="[history.result]"
            :load-children="loadObjectDetail"
          >
            <template v-slot:label="{ item }">
              <v-btn
                text
                plain
                @click="loadObjectDetail(item.target, item.begin, item.size)"
                v-if="item.action == 'more'"
                >加载更多..</v-btn
              >
              <template v-else>
                <span class="red--text text--darken-2" v-if="item.name">
                  {{ item.name }} =
                </span>
                <span v-if="item.atomic">
                  <span
                    v-if="item.type == 'java.lang.String'"
                    class="green--text text--darken-3"
                  >
                    "{{ item.value }}"
                  </span>
                  <span v-else>{{ item.value }}</span>
                </span>
                <template v-else>
                  <span class="text--secondary">{{
                    getSimpleClassName(item.type)
                  }}</span>
                  <span class="text--secondary" v-if="item.type.startsWith('[')"
                    >[{{ item.childSize }}]</span
                  >
                  <span class="text--secondary">@{{ item.objectId }}</span>
                  <span v-if="item.childSize || item.childSize == 0">
                    size={{ item.childSize }}</span
                  >
                  <span v-else-if="item.value"> "{{ item.value }}"</span>
                </template>
              </template>
            </template>
          </v-treeview>
        </div>
      </v-list-item>

      <v-divider></v-divider>
      <!-- 代码输入 -->
      <v-list-item class="pa-0 codeEdit">
        <codemirror
          @ready="initEdit"
          @keydown.enter.meta="doEval"
          v-model="inputCode"
          class="codemirror flex-grow-1"
          ref="cm"
          :options="cmOptions"
        >
        </codemirror>
      </v-list-item>
    </v-list>
    <v-dialog v-model="openNewFileDialog" max-width="400" persistent>
      <v-card>
        <v-card-title>保存新文件</v-card-title>
        <v-card-text>
          <v-text-field
            autofocus
            v-model="newFilePath"
            label="文件"
            placeholder="请输入文件路径名"
            suffix=".groovy"
          ></v-text-field>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="secondary" text @click="openNewFileDialog = false"
            >取消</v-btn
          >
          <v-btn
            :disabled="!newFilePath"
            color="primary"
            text
            @click="doSaveNewFile"
            >保存</v-btn
          >
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-sheet>
</template>

<script>
import { codemirror } from "vue-codemirror";
// 基础插件
import "codemirror/addon/selection/active-line.js";
// 基本样式
import "codemirror/lib/codemirror.css";
import "codemirror/theme/idea.css";
import "codemirror/mode/clike/clike.js";
import "codemirror/mode/groovy/groovy.js";
// 括号匹配
import "codemirror/addon/edit/matchbrackets.js";
// 错误提示
import "codemirror/addon/lint/lint.js";
import "codemirror/addon/lint/lint.css";
// 语法提示
import "codemirror/addon/hint/show-hint.js";
import "codemirror/addon/hint/show-hint.css";
import "codemirror/addon/comment/comment.js";
import { ConsoleService } from "@/api/index";
import { GroovySyntax, adapterKeyMap } from "./GroovySyntax.js";
import message from "@/store/message.js";
import { windows } from "codemirror/src/util/browser";

export default {
  name: "ConsoleView",
  components: {
    codemirror,
  },
  props: {
    code: String,
  },
  data() {
    let vueThis = this;
    // function getLint(code) {
    //   // && code == vueThis.hints.code
    //   return vueThis.hints.filter((h) => {
    //     return h.srcFile == path;
    //   });
    // }
    return {
      showSearch: false,
      newFilePath: "",
      codeFiles: [],
      openNewFileDialog: false,
      showTips: true,
      inputCode: this.code,
      loaderId: "",
      sessionId: "",
      consoleService: new ConsoleService(),
      groovySyntax: null,
      edit: null, // 代码编辑器
      classLoaders: [],
      evalHistory: [], // 执行结果
      cmOptions: {
        tabSize: 4, // 编辑器配置
        readOnly: false,
        mode: "text/x-java",
        // theme: "idea",
        lineNumbers: true,
        line: true,
        autofocus: true,
        dragDrop: false,
        styleActiveLine: true, // 激活选中行
        matchWords: true, // 光标选择时，匹配单词
        foldGutter: false,
        lint: {
          lintOnChange: false,
          selfContain: false,
          options: {
            getLint: () =>
              vueThis.groovySyntax ? vueThis.groovySyntax.errors : [],
            getCm: () => vueThis.edit,
          },
        },
      },
      // vscode-disable-next-line
      shortcutKeys: [
        {
          name: "执行代码",
          key: windows ? "Ctrl-Enter" : "Cmd-Enter",
          fun: () => this.doEval(),
        },
        {
          name: "查找提示",
          key: windows ? "Alt-." : "Cmd-.",
          fun: () => this.groovySyntax.showHint(),
        },
        {
          name: "快捷提示",
          key: ".",
          fun: () => this.groovySyntax.showHintByInput(),
        },
        {
          name: "格式化",
          key: windows ? "Shift-Alt-F" : "Shift-Cmd-F",
          fun: () => this.groovySyntax.doFormat(),
        },
        {
          name: "行注释",
          key: windows ? "Ctrl-/" : "Cmd-/",
          fun: () => this.groovySyntax.doComment(),
        },
        // {
        //   name: "保存文件",
        //   key: windows ? "Ctrl-S" : "Cmd-S",
        //   fun: () => this.doSave(),
        // },
        // {
        //   name: "查找文件",
        //   key: windows ? "Ctrl-E" : "Cmd-E",
        //   fun: () => this.doOpenFileSearch(),
        // },
        {
          name: "代码块注释",
          key: windows ? "Shift-Ctrl-/" : "Shift-Cmd-/",
          fun: () => this.groovySyntax.doBlockComment(),
        },
      ],
    };
  },
  computed: {
    cm() {
      return this.$refs.cm.codemirror;
    },
  },
  watch: {
    code() {
      this.inputCode = this.code;
      if(this.cm){
        this.cm.focus();
      }
    },
    showSearch() {
      if (this.showSearch) {
        this.codeFiles = null;
        // 加载所有代码文件
        this.consoleService.findFile("code", ".groovy").then((list) => {
          this.codeFiles = list.map((l) => {
            return {
              text: l.replace("/code/", ""),
              value: l,
            };
          });
          this.$nextTick(() => {
            // 自动弹出搜索菜单
            this.$refs.autocompleteFileSearch.$el
              .querySelector("input")
              .click();
          });
        });
      } else {
        this.codeFiles = null;
      }
    },
  },
  mounted() {
    // 加载
    this.consoleService.getAllClassLoader().then((data) => {
      this.classLoaders = data;
      this.loaderId = data.find((r) => r.default).id;
      this.openSession();
    });
  },
  methods: {
    // 打开文件查找搜索框
    doOpenFileSearch() {
      this.showSearch = true;
    },

    // 初始化编辑器
    initEdit(cm) {
      this.edit = cm;
      // 语法服务
      this.groovySyntax = new GroovySyntax(
        this.edit,
        this.consoleService,
        () => {
          return {
            id: this.sessionId,
          };
        }
      );
      let keymap = {};

      for (let item of this.shortcutKeys) {
        keymap[item.key] = item.fun;
      }
      this.edit.setOption("extraKeys", keymap); // 设置快捷键
      this.edit.setValue("//get(Class) 获取类的实例\n");
      this.edit.setCursor({ line: 1, ch: 0 });
    },
    // 打开会话创建链接
    async openSession() {
      try {
        let data = await this.consoleService.openSerssion(this.loaderId);
        this.sessionId = data.sessionId; // 会话ID
      } catch (error) {
        message.error("会话开启失败：" + error.message);
        return;
      }
    },
    async doEval() {
      if (!this.inputCode || !this.loaderId) return;
      if (!this.sessionId) {
        this.openSession();
      }
      try {
        let result = await this.consoleService.eval(
          this.sessionId,
          this.inputCode
        );
        this.evalHistory.push({ code: this.inputCode }); // 执行代码
        if (result.errorType && result.errorStack) {
          this.evalHistory.push({ error: result, detail: false }); // 执行结果
        } else {
          this.evalHistory.push({ result }); // 执行结果
        }
      } catch (error) {
        message.error("执行失败：" + error.message);
        return;
      }

      this.inputCode = "";
    },
    async loadObjectDetail(item, begin = -1, size = -1) {
      let data = await this.consoleService.getObjectDetail(
        this.sessionId,
        item.path,
        begin,
        size
      );
      if (
        item.children &&
        item.children.length > 0 &&
        item.children[item.children.length - 1].action
      ) {
        item.children.pop(); // 弹出最后一个
      }
      item.children.push(...data);
      if (item.childSize && item.childSize > item.children.length) {
        item.children.push({
          action: "more",
          target: item,
          begin: item.children.length,
          size: 20,
        });
      }
    },

    // 语法分析

    // 语法提示

    async loadMore(item, begin = -1, size = -1) {
      // 加载更多的子项
    },

    async doClear() {
      if (!this.sessionId) return;
      await this.consoleService.closeSession(this.sessionId);
      this.evalHistory = [];
      this.sessionId = null;
      // 打开新会话
      this.openSession();
    },
    getSimpleClassName(className) {
      return className.split(/\/|\./).pop();
    },
    // 打开文件
    doOpenFile(path) {
      this.consoleService.openFile(path).then((text) => {
        this.inputCode = text;
        this.showSearch = false;
        this.edit.focus();
      });
    },
    // 保存新文件
    doSaveNewFile() {
      if (!this.newFilePath) {
        throw new Error("路径还未设置存在");
      }
      // 将保存指令插入代码中
      this.inputCode = `//SAVE_TO_FILE ${this.newFilePath}.groovy\n${this.inputCode}`;
      this.doSave();
      this.newFilePath = "";
      this.openNewFileDialog = false;
    },
    // 保存文件
    doSave() {
      //SAVE_TO_FILE sdfl/sdf/fdsf.groovy
      let path = this.inputCode.match(/^\s*\/\/SAVE_TO_FILE\s*(.+\.groovy)\n/);
      if (path) {
        path = path[1]; // 取第一个分组为实际路径
      } else {
        // 弹出路径名输入框
        this.openNewFileDialog = true;
        return;
      }
      path = ("code/" + path).replaceAll(/[\\/]{2,}/g, "/");
      this.consoleService
        .saveFile(path, this.inputCode)
        .then((d) => {
          message.success("已保存至:" + path, 3000, "br");
        })
        .catch((e) => {
          message.error("文件保存失败：" + e.message);
        });
    },
  },
};
</script>

<style lang="scss">
.console .CodeMirror {
  height: unset;
}
.CodeMirror-hints {
  background-color: #46484a !important;
  border-color: #454545;
  border-width: 0.5px;
  .CodeMirror-hint {
    color: #bbbbbb !important;
    font-size: 1rem;
    .secondary {
      color: #8c8c8c;
      font-size: 0.85rem;
      margin-left: 3px;
      .right {
        width: max-content;
        text-align: right;
      }
    }

    .completionMark {
      color: #1e88e5 !important;
    }
  }
  .CodeMirror-hint-active {
    background-color: #113a5c !important;
  }
}

.console {
  .eval.history {
    font-size: 90%;
    .v-treeview-node__root {
      min-height: 30px;
    }
    code {
      font-size: 90%;
    }
    .evalError {
      background-color: #fcebeb;
      color: #410002;
      pre {
        text-wrap: pretty;
        display: inline;
        font-size: 90%;
      }
    }
  }
  .eval.history.v-list-item {
    min-height: 35px;
  }
  .codeEdit .CodeMirror {
    font-size: 90%;
    background-color: rgb(247, 247, 247);
  }
  .v-chip--label {
    border-radius: 0 !important;
  }
  .cm-s-default .cm-comment {
    color: rgb(140, 140, 140) !important;
  }
}
</style>