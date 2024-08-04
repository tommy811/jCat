package org.coderead.jcat;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */


import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class BootstrapAgent {
    static final Logger logger = Logger.getLogger(BootstrapAgent.class.getName());
    private static volatile JCatLoader loader = null;
    private static String premainClass;
    private static String agentmainClass;
    private static String mainClass;

    public static void premain(String args, Instrumentation instrumentation) {
        try {
            initJCatLoader();
            Class<?> aClass = loader.loadClass(premainClass, true);
            Method premain = aClass.getDeclaredMethod("premain", String.class, Instrumentation.class);
            premain.invoke(aClass, args, instrumentation);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "jCat加载失败", e);
        }
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        if (loader != null) {
            logger.warning("jCat已经加载!");
            return;
        }
        try {
            initJCatLoader();
            Class<?> aClass = loader.loadClass(agentmainClass, true);
            Method agentmain = aClass.getDeclaredMethod("agentmain", String.class, Instrumentation.class);
            agentmain.invoke(aClass, args, instrumentation);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "jCat启动失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            initJCatLoader();
            Class<?> aClass = loader.loadClass(mainClass, true);
            Method main = aClass.getDeclaredMethod("main", String[].class);
            if (Arrays.stream(args).noneMatch(s -> s.startsWith("agent.path="))) {
                String path = "agent.path=" + BootstrapAgent.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                if (!path.endsWith(".jar")) {
                    throw new IllegalStateException("agent.path必须是一个.jar文件");
                }
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = path;
            }
            main.invoke(aClass, (Object) args);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "jCat启动失败", e);
        }
    }


    private static JCatLoader initJCatLoader() {
        File classFile = new File(BootstrapAgent.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File file = new File(classFile.getParent(), "lib/JCat-agent.jar");
        if (!file.exists()) {
            throw new IllegalStateException("找不到文件:" + file);
        }
        try (JarFile jarFile = new JarFile(file)) {
            Manifest manifest = jarFile.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            premainClass = attributes.getValue("premain-class");
            agentmainClass = attributes.getValue("Agent-Class");
            mainClass = attributes.getValue("Main-Class");
            if (premainClass == null || premainClass.trim().equals("")) {
                throw new IllegalStateException("找不到premain-class 参数:" + file);
            }
            if (agentmainClass == null || agentmainClass.trim().equals("")) {
                throw new IllegalStateException("找不到Agent-Class 参数:" + file);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件损坏，无法读取" + file, e);
        }
        try {
            loader = new JCatLoader(new URL[]{file.toURI().toURL()});
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        return loader;
    }

}
