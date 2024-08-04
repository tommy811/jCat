const { defineConfig } = require('@vue/cli-service')
const vuetify = defineConfig({
  transpileDependencies: [
    'vuetify'
  ]
})
module.exports = defineConfig({
  publicPath: "/jCat",
  transpileDependencies: true,
  productionSourceMap: false,// 打包是否显示源码
  configureWebpack: {
    node:false,
    devtool: 'source-map'
  },
  devServer: {
    allowedHosts: "all",
    // client: {
    //   overlay: {
    //     errors: false,
    //     warnings: false,
    //   },
    // },
    // host:"regex.coderead.cn",
    proxy: {
      '/jCat/api': {
        target: 'http://127.0.0.1:3426',
        ws: true,
        changeOrigin: true
      }
    }
  },
  ...vuetify
})
