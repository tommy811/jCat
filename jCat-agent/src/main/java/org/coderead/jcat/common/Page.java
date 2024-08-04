package org.coderead.jcat.common;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.io.Serializable;
import java.util.List;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class Page<T> implements Serializable {
    public int total; //总数
    public List<T> data;

    public Page(int total, List<T> data) {
        this.total = total;
        this.data = data;
    }

    public Page() {
    }
}
