package org.coderead.jcat.console;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class GroovyConsoleLoader extends URLClassLoader {
    private static final URL[] EMPTY_URL_ARRAY = new URL[0];
    private final ClassLoader assistantLoader;
    private final String[] groovyPackages;

    public GroovyConsoleLoader(ClassLoader parent) {
        super(EMPTY_URL_ARRAY, GroovyConsoleLoader.class.getClassLoader());
        assistantLoader = parent;
        groovyPackages = new String[]{
                "groovy.",
                "groovyjarjarantlr.",
                "groovyjarjarasm.asm.",
                "groovyjarjarcommonscli.",
                "org.apache.groovy.",
                "org.codehaus.groovy.",
        };
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            return assistantLoader.loadClass(name);
        }
    }
}
