import axios from "axios";

export function createBaseApi(baseURL) {
    return axios.create({
        baseURL,
        timeout: 10000,
        headers: { 'Content-Type': "application/json;charset=utf-8" },
        validateStatus: function (status) {
            return status < 400; // 状态码为400以下属正常返回
        }
    });
}

function addResponseFilter(api) {
    // 服务端响应业务错误： 封装业务异常抛出
    // 连接异常：封装请求错误
    // 网络异常：封装网络异常
    api.interceptors.response.use(function (response) {
        return response.data; // 正常直接返回data
    }, function (error) {
        if (error.response) {
            let responseError = error.response.data;
            if (error.response.status == 400) { // 请求结果异常，弹窗提示
                responseError.type = "requestError";
            } else if (error.response.status == 500) {//服务响应异常，右下角提示
                responseError.type = "serverError";
            } else {
                responseError.type = "otherError";
            }
            console.error(JSON.stringify(responseError));
            return Promise.reject(error.response.data);
        } else {
            error.type = "networkError"
            console.error("连接异常：", error);
            return Promise.reject(error);
        }
    })
}

export class ConsoleService {
    constructor() {
        this.api = createBaseApi(`/jCat/api/console`);
        addResponseFilter(this.api);
    }
    getAllClassLoader() {
        return this.api.get(`/allClassLoader`)
    }
    openSerssion(loaderId) {
        let params = new URLSearchParams({ loaderId });
        return this.api.get(`/open?${params.toString()}`)
    }
    closeSession(sessionId) {
        let params = new URLSearchParams({ sessionId });
        return this.api.get(`/close?${params.toString()}`);
    }
    eval(sessionId, code) {
        let params = new URLSearchParams({ sessionId, code });
        return this.api.get(`/eval?${params.toString()}`);
    }
    getObjectDetail(sessionId, objectPath, begin, size, level = 0) {
        let params = new URLSearchParams({ sessionId, objectPath, begin, size, level });
        return this.api.get(`/detail?${params.toString()}`);
    }

    completion(sessionId, keywordOrCursor, max = 100) {
        let params = new URLSearchParams({ sessionId, max });
        if (typeof keywordOrCursor === 'string') {
            params.append("keyword", keywordOrCursor);
        } else if (keywordOrCursor instanceof Array) {
            params.append("cursor", keywordOrCursor.join());
        } else {
            throw Error("非法参数类型:" + keywordOrCursor)
        }
        return this.api.get(`/completion?${params.toString()}`);
    }

    // 语法分析
    analysis(sessionId, code) {
        let params = new URLSearchParams({ sessionId, code });
        return this.api.get(`/analysis?${params.toString()}`);
    }
    saveFile(file, text) {
        let params = new URLSearchParams({ file, text });
        return this.api.get(`/file/save?${params.toString()}`);
    }
    openFile(path) {
        let params = new URLSearchParams({ path });
        return this.api.get(`/file/open?${params.toString()}`);
    }
    findFile(parent, suffix) {
        let params = new URLSearchParams({ parent });
        if (suffix) {
            params.append("suffix", suffix);
        }
        return this.api.get(`/file/find?${params.toString()}`);
    }
}

export default {
    ConsoleService,
}

