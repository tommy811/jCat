package org.coderead.jcat;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.logging.Level;

/**
 * 拦截器包装类，用于避免拦截器对业务逻辑影响
 * <ul>
 * <li>避免对拦截器中的逻辑进行拦截从而造成死循环递归</li>
 * <li>catch 拦截器中的异常</li>
 * </ul>
 *
 * @author 鲁班大叔
 * @date 2024
 */
public class InterceptorWrapper implements Interceptor {
    Interceptor interceptor;
    ThreadLocal<Boolean> lock = ThreadLocal.withInitial(()->false);

    public InterceptorWrapper(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Object invoke(String methodInfo,Object thisObj, Object[] args) {
        if (lock.get()) {
            return null;// 杜绝 $begin内部发生采集事件所造成的死循环递归
        }
        try {
            lock.set(true);
            return this.interceptor.invoke(methodInfo,thisObj, args);
        } catch (Throwable e) {
            logger.log(Level.SEVERE,"JCat内部异常，该异常不影响业务正常执行，请将该异常反馈给开发者", e);
        } finally {
            lock.set(false);
        }
        return null;
    }


    @Override
    public void invokeEnd(String methodInfo,Object trace, Object thisObj, Object[] args, Object result) {
        if (lock.get()) {
            return;// 杜绝 内部发生采集事件所造成的死循环递归
        }
        try {
            lock.set(true);
            this.interceptor.invokeEnd(methodInfo,trace, thisObj, args, result);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "JCat内部异常，该异常不影响业务正常执行，请将该异常反馈给开发者", e);
        } finally {
            lock.set(false);
        }
    }
}
