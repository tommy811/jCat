package org.coderead.jcat.common;

import arthas.VmTool;
import org.coderead.jcat.service.ConsoleService;
import com.taobao.arthas.common.VmToolUtils;
//import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * @author tommy
 * @title: ClassUtil
 * @projectName cbtu-web-ide
 * @description: TODO
 * @date 2020/3/1110:53 AM
 */
public class ClassUtil {
    static VmTool vmTool = null;
    static final Map<Class, Map<String, Field>> classMetaCache = new ConcurrentHashMap<Class, Map<String, Field>>();

   /* public static String getMethodSign(Method method) {
        return getMethodSign(Type.getMethodDescriptor(method));
    }

    public static String getMethodSign(String methodDescriptor) {
        StringBuilder sb = new StringBuilder();
        Type methodType = Type.getMethodType(methodDescriptor);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(ClassUtil.convertToJdt(argumentTypes[i]));
        }
        sb.append(",");
        sb.append(ClassUtil.convertToJdt(methodType.getReturnType()));
        return EncryptUtil.MD5(sb.toString()).substring(0, 5);
    }

    public static String convertToJdt(Type type) {
        String descriptor = type.getDescriptor();
        if (descriptor.startsWith("[")) {
            return descriptor.replaceAll("/", ".");
        }
        if (descriptor.startsWith("L")) {
            return descriptor.substring(1)
                    .replaceAll(";", "")
                    .replaceAll("/", ".");
        }

        return descriptor;
    }*/


    public static VmTool getVmTool() {
        if (vmTool == null) {
            synchronized (VmTool.JNI_LIBRARY_NAME) {
                if (vmTool == null) {
                    try {
                        Path tempFile = Files.createTempFile(VmTool.JNI_LIBRARY_NAME, null);
                        Files.copy(Objects.requireNonNull(ConsoleService.class.getResourceAsStream("/lib/" + VmToolUtils.detectLibName())), tempFile, StandardCopyOption.REPLACE_EXISTING);
                        tempFile.toFile().deleteOnExit();// 应用正常结束时删除
                        vmTool = VmTool.getInstance(tempFile.toAbsolutePath().toString());
                    } catch (IOException e) {
                        throw new RuntimeException("加载VmTool失败", e);
                    }
                }
            }
        }
        return vmTool;
    }

    public static Class<?> loadAdapterClass(Instrumentation instrumentation) {
        try {
            Class<?> aClass = ClassLoader.getSystemClassLoader().loadClass("org.coderead.jcat.Interceptor");
            Assert.isTrue(aClass.getClassLoader() == null, "Interceptor必须加载至boot加载器中");
            return aClass;
        } catch (ClassNotFoundException e) {
            // 没有加载继续执行
        }
        try {
            Path tempFile = Files.createTempFile("agentAdapter", ".zip");
            Files.copy(Objects.requireNonNull(ConsoleService.class.getResourceAsStream("/lib/agentAdapter.jar")), tempFile, StandardCopyOption.REPLACE_EXISTING);
            File file = tempFile.toFile();
            System.setProperty("jCat.agentAdapter.path", file.getAbsolutePath());
            file.deleteOnExit();// 应用正常结束时删除
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(file));
            ClassLoader.getSystemClassLoader().getResource("com/cbtu/agent/Interceptor.class");
            return ClassLoader.getSystemClassLoader().loadClass("org.coderead.jcat.Interceptor");
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("加载agentAdapter.jar至bootClassLoader失败", e);
        }
    }

    /**
     * 获取当前类以及期父类的所有属性
     *
     * @param c
     * @return
     */
    public static Map<String, Field> getDeepDeclaredFields(Class c) {
        Map<String, Field> classFields = classMetaCache.get(c);
        if (classFields != null) {
            return classFields;
        }
        classFields = new LinkedHashMap<>();
        Class curr = c;
        while (curr != null) {
            final Field[] local = curr.getDeclaredFields();
            for (Field field : local) {
                if ((field.getModifiers() & Modifier.STATIC) == 0) {   // speed up: do not process static fields.
                    if ("metaClass".equals(field.getName()) && "groovy.lang.MetaClass".equals(field.getType().getName())) {   // Skip Groovy metaClass field if present
                        continue;
                    }
                    if (classFields.containsKey(field.getName())) {
                        classFields.put(curr.getName() + '.' + field.getName(), field);
                    } else {
                        classFields.put(field.getName(), field);
                    }
                }
            }
            curr = curr.getSuperclass();
        }

        classMetaCache.put(c, classFields);
        return classFields;
    }


    public static boolean isWrapperType(Class cla) {
        return cla.equals(Object.class) ||
                cla.equals(Integer.class) ||
                cla.equals(Short.class) ||
                cla.equals(Long.class) ||
                cla.equals(Double.class) ||
                cla.equals(Float.class) ||
                cla.equals(Boolean.class) ||
                cla.equals(Byte.class) ||
                cla.equals(Character.class);
    }

    static String[] groovyPackages = new String[]{
            "groovy.",
            "groovyjarjarantlr.",
            "groovyjarjarasm.asm.",
            "groovyjarjarcommonscli.",
            "org.apache.groovy.",
            "org.codehaus.groovy.",
    };

    public static boolean isGroovyClass(String className) {
        return Arrays.stream(groovyPackages).anyMatch(p -> className.startsWith(p) || className.startsWith(p.replaceAll("\\.", "/")));
    }

   /* *//**
     * 解析方法字符串，以转换成Method
     *
     * @param methodInfo 类名:方法名:方法签名
     * @param loader     该的类加载器
     * @return
     *//*
    public static Method parseMethodInfo(String methodInfo, ClassLoader loader) {
        // 类名:方法名:方法签名
        String[] splits = methodInfo.split(":");
        Method method;
        try {
            Class<?> aClass = Class.forName(splits[0], false, loader);
            method = Arrays.stream(aClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals(splits[1])
                            && Type.getMethodDescriptor(m).equals(splits[2]))
                    .findFirst().orElse(null);
            Assert.notNull(method, "解析method失败，找不到类中的方法:" + methodInfo);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("解析method失败，找不到类:" + splits[0]);
        }
        return method;
    }*/





}
