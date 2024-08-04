package org.coderead.jcat.console;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import arthas.VmTool;
import org.coderead.jcat.common.ClassUtil;
import groovy.lang.Script;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public abstract class ConsoleScript extends Script {
    // 获取实例
    public Object[] get(Class<?> cla, Integer limit) {
        limit = limit == null ? 10 : limit;
        VmTool vmTool = ClassUtil.getVmTool();
        Object[] instances = vmTool.getInstances(cla, limit);
        return instances;
    }

    public Object[] get(Class<?> cla) {
        return get(cla, 10);
    }

}
