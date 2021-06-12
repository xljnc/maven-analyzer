package com.wt.maven.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class ClassUtil {

    private static ClassLoader defaultClassLoader;

    public static ClassLoader getDefaultClassLoader() {
        if (defaultClassLoader != null)
            return defaultClassLoader;
        try {
            defaultClassLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
        }
        if (defaultClassLoader == null) {
            defaultClassLoader = ClassUtil.class.getClassLoader();
            if (defaultClassLoader == null) {
                try {
                    defaultClassLoader = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                }
            }
        }
        return defaultClassLoader;
    }
}
