// import { setTimeout } from "core-js";
// import vue from "vue";

const store = {
    namespaced: true,
    state: {
        msg: "",
        icon:"",
        pos:"top",
        show: false,
        type: "info",
        timeout: -1,
        alert: null,
    },
    mutations: { //修改数据
        close(state) {
            state.show = false;
            state.alert = null;
            state.msg = null;
            state.icon=null;
            // state.pos="top";
        }
    },
}

function configTimeout(timeout) {
    if (timeout && timeout > 0) {
        store.state.timeout = timeout;
        // setTimeout(function(){
        //     close();
        // },timeout);
    } else {
        store.state.timeout = -1;
    }
}
function show() {
    store.state.show = true;
}


export function info(msg, timeout) {
    store.state.msg = msg;
    store.state.type = "info";
    if (timeout == undefined || timeout == null) {
        timeout = 3000;
    }
    configTimeout(timeout);
    show();
}

export function success(msg, timeout,pos) {
    store.state.msg = msg;
    store.state.type = "success";
    if (timeout == undefined || timeout == null) {
        timeout = 3000;
    }
    store.state.icon="mdi-check"
    store.state.pos=pos;
    configTimeout(timeout);
    show();
}

export function warn(msg, timeout,pos="br") {
    store.state.msg = msg;
    store.state.type = "warning";
    store.state.pos=pos;
    configTimeout(timeout);
    show();
}

export function error(msg,pos="tc") {
    store.state.msg = msg;
    store.state.type = "error";
    store.state.pos=pos;
    configTimeout(-1);// 不主动关闭
    show();
}



export function close() {
    store.state.show = false;
    store.alert = null;
}
export function alert(title, message, affirm) {
    if(!affirm||!(affirm instanceof Function)) {
        throw new Error("affirm must be a function");
    }
    store.state.alert = {
        show:true,
        title,
        message,
        affirm,
    }
}

export default {
    store,
    info,
    warn,
    error,
    success,
    close,
    alert,
}