package org.coderead.jcat.common;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.HashMap;
import java.util.Map;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class Maps {
    public static <K, V> Map<K, V> to(K k, V v) {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    public static <K, V> Map<K, V> to(K k, V v, K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        map.put(k1, v1);
        return map;
    }

    public static Map<?, ?> to(Object... p) {
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < p.length; i += 2) {
            map.put(p[i], p[i + 1]);
        }
        return map;
    }
}
