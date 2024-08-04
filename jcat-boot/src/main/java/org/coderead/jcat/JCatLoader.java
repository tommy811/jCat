package org.coderead.jcat;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class JCatLoader extends URLClassLoader {
    static Logger logger = Logger.getLogger(JCatLoader.class.getName());

    public JCatLoader(URL[] urls) {
        super(attachToolsJar(urls ), JCatLoader.class.getClassLoader().getParent());
    }

    protected static URL[] attachToolsJar(URL[] urls) {
        try {
            getSystemClassLoader().loadClass("com.sun.tools.attach.VirtualMachine");
            return urls;
        } catch (ClassNotFoundException ignored) {
        }

        // 查找tools.jar

        File toolsFile = findToolsByCurrentJavaHome(); // 从当前jdk_home中查找
        if (toolsFile == null) {
            toolsFile = findToolsByCLASSPATH();// 环境变量CLASSPATH中找
        }
        if (toolsFile == null) {
            toolsFile = findToolsByJAVAHOME();// 环境变量JDK_HOME中找
        }
        if (toolsFile == null) {
            throw new IllegalStateException("系统找不到tools.jar");
        }
        try {
            urls = Arrays.copyOf(urls, urls.length + 1);
            urls[urls.length - 1] = toolsFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return urls;
    }

    static File findToolsByCurrentJavaHome() {
        if (!System.getProperty("java.home").endsWith("jre")) {
            return null;
        }
        File file = new File(new File(System.getProperty("java.home")).getParent(), "lib/tools.jar");
        logger.info("find tools.jar by Home:" + file);
        return file.exists() ? file : null;
    }

    static File findToolsByCLASSPATH() {
        String classpath = System.getenv("CLASSPATH");
        if (classpath == null) return null;
        logger.info("find tools.jar by CLASSPATH:" + classpath);
        return Arrays.stream(classpath.split(System.getProperty("path.separator")))
                .filter(s -> s.endsWith("tools.jar"))
                .map(File::new)
                .filter(File::exists)
                .findFirst().orElse(null);
    }

    static File findToolsByJAVAHOME() {
        String JAVA_HOME = System.getenv("JAVA_HOME");
        if (JAVA_HOME == null) return null;
        File file = new File(JAVA_HOME, "lib/tools.jar");
        logger.info("find tools.jar by JAVA_HOME:" + file);
        return file.exists() ? file : null;
    }


    //
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }
        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }
        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception ignored) {
        } // 勿略错误，并从父节点加载
        return super.loadClass(name, resolve);
    }
}
