package org.coderead.jcat.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.coderead.jcat.Agent;
import org.coderead.jcat.ClassUtil;
import org.coderead.jcat.common.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @email 27686551@qq.com
 * @date 2024/8/25
 */
public class ResourceExplorerService {
    // 返回ClassLoader下所有资源路径
    public ResourceExplorerService() {
        DefaultHttpServer httpServer = DefaultHttpServer.getInstance();
        httpServer.registeGet("/resource/allClass", this::getAllClass);
        httpServer.registeGet("/resource/decompilerClass", this::decompilerClass);
    }


    // 获取所有类
    private Object getAllClass(Map<String, String> stringStringMap) {
        List<ClassItem> items = new ArrayList<>();
        for (Class c : Agent.instrumentation.getAllLoadedClasses()) {
            try {
                if (c.getName().startsWith("[") || c.getName().contains("$$Lambda$")) {
                    continue;
                }
                ClassItem item = new ClassItem(
                        System.identityHashCode(c.getClassLoader()),
                        System.identityHashCode(c),
                        c.getName());
                items.add(item);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class ClassItem implements Serializable {
        long loadId;
        long classId;
        String className;
    }


    private String decompilerClass(Map<String, String> stringStringMap) {
        long classId = Long.parseLong(stringStringMap.get("classId"));
        byte[] bytes = ClassUtil.readClass(Agent.instrumentation, classId);
        return ClassUtil.decompilerClass(bytes);
    }

    public URL[] getUrls(URLClassLoader urlClassLoader) {
        return urlClassLoader.getURLs();
    }

    // 返回资源路径下资源列表
    public Resource[] getResource(URL url) throws IOException {
        // 打开资源
        URLConnection urlConnection = url.openConnection();
        Assert.isTrue(urlConnection instanceof JarURLConnection);
        JarURLConnection connection = (JarURLConnection) urlConnection;
        JarFile jarFile = connection.getJarFile();
        return null;
    }


    @Getter
    @Setter
    public static class Resource implements java.io.Serializable {
        String url;
        String name;
        String type;
        Resource[] children;
    }
    //


}
