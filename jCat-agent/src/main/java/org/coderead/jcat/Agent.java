package org.coderead.jcat;

import org.apache.groovy.util.Maps;
import org.coderead.jcat.common.Assert;
import org.coderead.jcat.service.AttachService;
import org.coderead.jcat.service.ConsoleService;
import org.coderead.jcat.service.DefaultHttpServer;
import org.coderead.jcat.service.ResourceExplorerService;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Agent {
    static final Logger logger = Logger.getLogger(Agent.class.getName());

    public static Properties configs = new Properties();
    public static Instrumentation instrumentation;
    static DefaultHttpServer httpServer = DefaultHttpServer.getInstance();
    static ConsoleService consoleService = new ConsoleService(); // 控制台服务
    static ResourceExplorerService resourceService = new ResourceExplorerService(); // 控制台服务


    public static void premain(String args, Instrumentation instrumentation) {
        start(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        try {
            start(args, instrumentation);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "load agentmain fail:" + args, e);
        }
    }

    public static void start(String args, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
        configs = getAgentConfigs(args);
        try {
            httpServer.start(-1, "/jCat");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "http服务启动失败", e);
        }
    }

    // 读取agent 配置
    private static Properties getAgentConfigs(String arg) {
        // 读取agent 配置
        Properties properties = new Properties();
        InputStream resourceAsStream = Agent.class.getResourceAsStream("/agent.properties");
        try {
            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
                resourceAsStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("agent config format is error", e);
        }
        //装载Debug 调式参数信息,其可直接覆盖上述配置
        if (arg != null && !arg.trim().equals("")) {
            try {
                properties.load(new ByteArrayInputStream(
                        arg.replaceAll(",", "\n").getBytes()));
            } catch (IOException e) {
                throw new RuntimeException("agent config format is error", e);
            }
        }
        return properties;
    }

    // agentmain装载程序 dd
    //  -Dagent.path=/Users/tommy/git/JCat/jCat-agent/build/libs/jCat-agent-0.1-SNAPSHOT-all.jar
    public static void main(String[] args) throws URISyntaxException, IOException {
        Properties properties = new Properties();
        for (String arg : args) {
            String[] s = arg.split("=");
            properties.put(s[0], s[1]);
        }
        String agentPath = System.getProperty("agent.path", properties.getProperty("agent.path"));
        Assert.hasText(agentPath);
        AttachService attachService = new AttachService();// 注册至http服务
        Collection<AttachService.JvmItem> jvmItems = attachService.jvmList();
        AttachService.JvmItem[] items = jvmItems.toArray(new AttachService.JvmItem[0]);
        for (int i = 0; i < items.length; i++) {
            System.out.printf("%s %s\n", i, items[i].getName().replaceFirst("\\s.*$", ""));
        }
        System.out.println("请输入数字进行选择");
        Scanner scanner = new Scanner(System.in);
        int index = scanner.nextInt();
        AttachService.JvmItem attach = attachService.attach(items[index].getId(), agentPath, null);
        // 装载成功
        System.out.printf("装载成功! 访问地址：http://%s:%s/jCat\n", attach.getTargetIp(), attach.getTargetPort());
    }

    public static boolean isDebug() {
        return is("debug", false);
    }

    public static boolean is(String key, boolean defaultVal) {
        return Boolean.parseBoolean(Agent.configs.getProperty(key, Boolean.toString(defaultVal)));
    }

    public static String get(String key, String defaultVal) {
        return Agent.configs.getProperty(key, defaultVal);
    }
}
