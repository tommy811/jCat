<template>
  <v-sheet loading
           class="ResourceExplorer d-flex">

    <v-overlay :value="loading">
      <v-progress-circular
          indeterminate
          size="64"
      ></v-progress-circular>
    </v-overlay>

    <!--    左边树-->
    <div style="max-width: 350px; min-width: 350px; ">
      <v-text-field v-model.lazy="searchText"
                    placeholder="搜索查找资源"  prepend-inner-icon="mdi-magnify"></v-text-field>
      <v-treeview
          @update:active="doSelectItem"
          activatable
          open-on-click
          return-object
          hoverable
          :active="currentItems"
          :search.sync="searchText" :items="resources" class="overflow-auto leftTree"
          dense
      >
        <template v-slot:prepend="{ item }">
          <v-icon small v-if="item.icon">
           {{item.icon}}
          </v-icon>
        </template>
      </v-treeview>
    </div>
    <!--      右边资源展示器-->
    <div class="grow">
      <v-tabs v-model="tab">
        <v-tab>{{ currentFile }}</v-tab>
      </v-tabs>
      <v-tabs-items v-model="tab">
        <v-tab-item>
          <codemirror
              v-model="resourceText"
              class="codemirror flex-grow-1"
              ref="cm"
              :options="cmOptions"
          >
          </codemirror>
        </v-tab-item>
      </v-tabs-items>
    </div>
  </v-sheet>
</template>


<script>
import {ConsoleService, ResourceExplorerService} from "@/api";
import {codemirror} from "vue-codemirror";

export default {
  name: "ResourceExplorer",
  components: {
    codemirror,
  },
  data: () => {
    return {
      loading: true,
      tab: "",
      currentItems: [],
      searchText: "",
      resources: [],
      consoleService: new ConsoleService(),
      resourceService: new ResourceExplorerService(),
      resourceText: "",
      currentFile: "",
      cmOptions: {
        tabSize: 4, // 编辑器配置
        readOnly: true,
        lineWrapping: false,
        mode: "text/x-java",
        lineNumbers: true,
        line: true,
        autofocus: true,
        dragDrop: false,
        styleActiveLine: true, // 激活选中行
        matchWords: true, // 光标选择时，匹配单词
        foldGutter: false,
      },
    }
  },
  methods: {
    async doSelectItem(items) {
      if (items.length === 0) return;
      if (items[0].type != "classFile") return;
      this.resourceText = await this.resourceService.decompilerClass(items[0].id);
      this.currentFile = items[0].name;

    },
    async loadResources() {
      const allClassLoader = await this.consoleService.getAllClassLoader();
      const allClass = await this.resourceService.getAllClass();
      // name,chidle
      let loads = {};
      // 第一级：类加载器
      for (let l of allClassLoader) {
        loads[l.id] = {name: l.type, id: l.id, icon:"mdi-arrow-left-bold-hexagon-outline", type: "classLoader", children: []};
      }
      let packageItems = {}
      for (let c of allClass) {
        if(!loads[c.loadId]) continue;
        let packageName = c.className.match(/^(.*)\./);
        packageName = packageName ? packageName[1] : "EMPTY_PACKAGE";
        let packageId=packageName+c.loadId;
        let item = {
          name: c.className.replaceAll(/.*\./g, ""),
          className: c.className,
          id: c.classId,
          type: "classFile",
          icon:"mdi-language-java",
          parent: packageId
        }

        if (!packageItems[packageId]) {
          // 第二级：包名
          packageItems[packageId] = {
            name: packageName,
            type: "package",
            icon:"mdi-package",
            id: packageId,
            children: [],
            parent:c.loadId
          };
          loads[c.loadId].children.push(packageItems[packageId]);
        }
        // 第三级:类名
        packageItems[item.parent].children.push(item);
      }


      this.resources =Object.values(loads);
      this.loading = false;
    }
    // 构建子节点

  },
  mounted() {
    this.loadResources();// 加载资源树
  }
}
</script>


<style>
.ResourceExplorer {
  .CodeMirror {
    font-size: 90%;
    height: calc(100vh - 80px);
    width: calc(100vw - 420px)
  }

  .v-tab {
    text-transform: unset;
  }

  .leftTree {
    min-height: calc(100vh - 105px);
    max-height: calc(100vh - 105px);
    font-size: 0.9rem;
    .v-treeview-node__level{
      width: 12px;
    }
    .v-treeview-node__root{
      min-height: 32px !important;
    }
    .v-treeview-node__toggle,.v-treeview-node__prepend{
      width: 12px !important;
      min-width: 12px !important;
    }
  }
}
</style>
