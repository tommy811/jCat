package org.coderead.jcat.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */


import org.coderead.jcat.Agent;
import org.coderead.jcat.common.Assert;
import org.coderead.jcat.common.IOUtils;
import org.coderead.jcat.common.JsonUtil;
import org.coderead.jcat.common.Maps;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class DefaultHttpServer {
    static final Logger logger = Logger.getLogger(DefaultHttpServer.class.getName());

    static {
        // TODO 通过参数控制强制agent日志输出到控制台
        /*logger.setUseParentHandlers(false);// 禁用桥接到slf4j
        logger.addHandler(new ConsoleHandler());// 直接输出到控制台*/
    }

    private static DefaultHttpServer instance;
    private HttpServer server;
    private String context = "";
    private String apiPrefix = "/api";

    public static DefaultHttpServer getInstance() {
        if (instance == null) {
            instance = new DefaultHttpServer();
        }
        return instance;
    }


    private DefaultHttpServer() {
        registeGet("/info", this::getInfo);
    }

    Map getInfo(Map<String, String> urlParams) {
        HashMap<String, String> map = new HashMap<>();
        map.put("port", String.valueOf(server.getAddress().getPort()));
        map.put("ip", String.valueOf(server.getAddress().getHostString()));
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        map.put("processName", runtimeMXBean.getName());
        map.put("pid", runtimeMXBean.getName().split("@")[0]);
        map.put("context", context);//attachServer ，agentServer
        return map;
    }

    public int start(int positivePort, String context) throws IOException {
        if (server != null) {
            throw new IllegalStateException("The service has started");
        }
        Assert.hasText(context);
        Assert.isTrue(context.startsWith("/"));
        this.context = context;
        int port = positivePort == -1 ?
                Integer.parseInt(Agent.configs.getProperty("port", "3426")) :
                positivePort;

        server = HttpServer.create();
        int retry = positivePort > 0 ? 1 : 5;  //自动绑定尝试5次
        for (int i = 0; i < retry; i++) {
            try {
                server.bind(new InetSocketAddress(port), 0);
                System.setProperty("jcat.agent.httpPort", String.valueOf(port));
                break;
            } catch (BindException e) {
                if (i == retry - 1) {
                    server = null;
                    throw new IllegalStateException("jcat启动失败，无法打开端口:" + port, e);
                }
                port += 1;
            }
        }
        server.start();
        logger.info("JCat已启动，访问地址 http://127.0.0.1:" + port+"/jCat");
        server.createContext("/", new DefaultHandler());
//        server.setExecutor(); TODO 是否设置守护进程？ 程序结束后服务停止
        return port;
    }

    public boolean isStart() {
        return server != null;
    }

    private Map<String, Function<Map<String, String>, Object>> actions = new HashMap<>();

    //    //
    public void registeGet(String path, Function<Map<String, String>, Object> function) {
        actions.put(path, function);
    }


    public static Map<String, String> getUrlParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        if (uri.getRawQuery() == null || uri.getRawQuery().trim().equals("")) {
            return map;
        }
        String[] params = uri.getRawQuery().split("&");
        for (String param : params) {
            String[] p = param.split("=");
            if (p.length == 1) {
                map.put(p[0], null);
                continue;
            }
            String key = p[0];
            String value = p[1];
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (map.containsKey(key)) { // 重复键换行切割
                map.put(key, map.get(key) + "\r\n" + value);
            } else {
                map.put(p[0], value);
            }
        }
        return map;

    }


    class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                doHandle(exchange);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "", e);
            }
        }

        void doHandle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("OPTIONS")) {// 允许CORS 跨域
//                 exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
//                 exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                writeResponse(200, "", exchange);
                return;
            }
            String responseMsg = null;
            int responseCode = 200;
            try {
                String path = exchange.getRequestURI().getPath();
                if (!path.startsWith(context)) {
                    writeResponse(404, "找不到路径", exchange);
                    return;
                }
                if (!path.startsWith((context + apiPrefix))) {// 静态资源
                    doStaticHandle(exchange);
                    return;
                }
                String realPath = path.substring((context+apiPrefix).length());
                if (!actions.containsKey(realPath)) {
                    writeResponse(404, "找不到路径", exchange);
                    return;
                }
                Function<Map<String, String>, Object> function = actions.get(realPath);
                Map<String, String> urlParams = getUrlParams(exchange.getRequestURI());
                Object result = function.apply(urlParams);
                responseMsg = result != null ? JsonUtil.toJson(result) : "{}";
            } catch (Throwable e) {
                responseCode = 500;
                responseMsg = JsonUtil.toJson(Maps.to("message", e.getMessage()));
                logger.log(Level.SEVERE, "服务异常:" + exchange.getRequestURI(), e);
            }
            writeResponse(responseCode, responseMsg, exchange);
        }

        // 处理静态资源
        private void doStaticHandle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String resourcePath = path.substring(context.length());
            //jCat/
            if (resourcePath.equals("/") || resourcePath.equals("")) {
                resourcePath = "/index.html";
            }
            InputStream stream = getClass().getResourceAsStream("/web" + resourcePath);
            if(stream==null){
                writeResponse(404,"找不到资源",exchange);
                return;
            }
            exchange.getResponseBody();
            byte[] bytes = IOUtils.readFully(stream, -1, false);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            stream.close();
            exchange.getResponseBody().close();
        }

        public int copyIo(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int len;
            int size = 0;
            while ((len = in.read(buffer)) != -1) {
                size += len;
                out.write(buffer, 0, len);
            }
            return size;
        }

        private void writeResponse(int responseCode, String content, HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, bytes.length);
            // 发送响应内容
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(bytes);
            outputStream.close();
        }
    }

//    String toJson(Object obj) {
//        WriteOptions options = new WriteOptionsBuilder().showTypeInfoNever().prettyPrint(true).build();
//        return JsonIo.toJson(obj, options);
//    }
}
