package org.coderead.jcat;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 事件拦截适配器 该模块将加载至boot加载器中
 * 注:该类不能依赖第三包，以及ext扩展加载器中的类
 *
 * @author 鲁班大叔
 * @date 2024
 */
public interface Interceptor {
    Logger logger = Logger.getLogger(Interceptor.class.getName());

    Object invoke(String methodInfo,Object thisObj, Object[] args);

    void invokeEnd(String methodInfo,Object trace, Object thisObj, Object[] args, Object result);

    Map<Integer, Interceptor> map = new HashMap<>();

    static Object $begin(int id,String methodInfo, Object thisObj, Object[] args) {
        return map.get(id).invoke(methodInfo,thisObj, args);
    }

    static void $end(int id,String methodInfo, Object trace, Object thisObj, Object[] args, Object result) {
        map.get(id).invokeEnd(methodInfo,trace, thisObj, args, result);
    }

    static boolean isRegistered(int key) {
        return map.containsKey(key);
    }

    static int register(Interceptor interceptor) {
        int key = System.identityHashCode(interceptor);
        map.put(key, new InterceptorWrapper(interceptor));
        return key;
    }

    static void unregister(int key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("该拦截器未注册，或已经删除");
        }
        map.remove(key);
    }


}
