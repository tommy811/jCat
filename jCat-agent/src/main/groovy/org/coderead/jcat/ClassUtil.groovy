package org.coderead.jcat

import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * @author 鲁班大叔
 * @email 27686551@qq.com
 * @date 2024/08/
 1
 */
class ClassUtil {
    // 获取所有的类,并基于classLoader进行分组
    static List<ClassItem> getAllClass(Instrumentation instrumentation) {
        def items = [];
        for (final def c in instrumentation.getAllLoadedClasses()) {
            try {
                if (c.name.startsWith("[")) {
                    continue
                }
                items.add(new ClassItem(
                        loadId: System.identityHashCode(c.getClassLoader()),
                        classId: System.identityHashCode(c),
                        className: c.name
                ))
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return items
    }

    static class ClassItem implements Serializable {
        long loadId
        long classId
        String className
    }
    // 读取类字节码
    static byte[] readClass(Instrumentation instrumentation, long classId) {
        def cla = findClass(instrumentation, classId);
        final List<byte[]> list = [];
        def transformer = new ClassFileTransformer() {
            @Override
            byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classBeingRedefined == cla) {
                    list << classfileBuffer
                }
                return null;
            }
        }
        instrumentation.addTransformer(transformer, true);
        try {
            instrumentation.retransformClasses(cla);
            // TODO UnmodifiableClassException 异常处理 如：PropertySerializerMap$TypeAndSerializer类
        } finally {
            instrumentation.removeTransformer(transformer);
        }
        return list[0]
    }

    static Class<?> findClass(Instrumentation instrumentation, long classId) {
        instrumentation.allLoadedClasses.find(c -> System.identityHashCode(c) == classId)
    }

    // 反编译类
    static String decompilerClass(byte[] bytes) {
        Jad.decompiler(bytes)
    }
}
