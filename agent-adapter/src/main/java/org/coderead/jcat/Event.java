package org.coderead.jcat;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 鲁班大叔
 * @date 2024
 */
@Getter
@Setter
public class Event {
    Object target;
    Object[] args;
    Object result;
    Method method;// 事件发生时的方法

    public Event() {
    }

    public Event(Object target, Object[] args) {
        this.target = target;
        this.args = args;
    }

    public Event(Object target, Object[] args, Object result) {
        this.target = target;
        this.args = args;
        this.result = result;
    }

    public Object recall() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }
}
