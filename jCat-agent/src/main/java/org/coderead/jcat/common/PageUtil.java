package org.coderead.jcat.common;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class PageUtil {
    // 将NavigableMap中的key进行分页处理
    public static <T> Page<T> splitKey(NavigableMap<T, ?> map, int pageIndex, int size) {
        return split(map.keySet(), pageIndex, size);
    }

    public static <T> Page<T> splitValue(NavigableMap<?, T> map, int pageIndex, int size) {
        return split(map.values(), pageIndex, size);
    }

    public static <T> Page<T> split(Collection<T> datas, int pageIndex, int size) {
        int begin = pageIndex * size;
        int end = (pageIndex + 1) * size;
        if (begin >= datas.size()) {
            return new Page<>(datas.size(), new ArrayList<>()); // 返回空
        }
        int index = 0;
        List<T> list = new ArrayList<>(size);
        for (T t : datas) {
            if (index >= begin && index < end) {
                list.add(t);
            }
            if (index >= end) {
                break;
            }
            index++;
        }
        return new Page<>(datas.size(), list);
    }
}
