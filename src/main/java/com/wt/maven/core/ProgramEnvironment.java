package com.wt.maven.core;

import com.wt.maven.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class ProgramEnvironment {

    private static Map<String, String> propertyMap = new HashMap<>(16);

    public static String get(String key) {
        return propertyMap.get(key);
    }

    public static void set(String key, String value) {
        propertyMap.put(key, value);
    }

    public static boolean contains(String key){
        return propertyMap.containsKey(key);
    }

    public static void prepareEnvironment(String[] args) {
        //从配置文件加载
        String propertiesFile = "application.properties";
        Enumeration<URL> urls = null;
        ClassLoader classLoader = ClassUtil.getDefaultClassLoader();
        try {
            urls = (classLoader != null ?
                    classLoader.getResources(propertiesFile) :
                    ClassLoader.getSystemResources(propertiesFile));
        } catch (IOException e) {
            log.error("获取配置文件失败", e);
            throw new RuntimeException("获取配置文件失败.");
        }
        if (!urls.hasMoreElements()) {
            log.error("获取配置文件失败");
            throw new RuntimeException("获取配置文件失败.");
        }
        //只取一个
        URL url = urls.nextElement();
        Properties props = new Properties();
        InputStream inputStream = null;
        try {
            URLConnection con = url.openConnection();
            inputStream = con.getInputStream();
            props.load(inputStream);
        } catch (IOException e) {
            log.error("配置文件读取失败", e);
            throw new RuntimeException("配置文件读取失败.");
        }
        props.forEach((k, v) -> {
            ProgramEnvironment.set(String.valueOf(k), String.valueOf(v));
        });
        //读取Java命令参数
        if (args != null && args.length != 0) {
            for (String arg : args) {
                if (!arg.contains("--"))
                    continue;
                arg = arg.substring(2);
                String[] pair = arg.split("=");
                ProgramEnvironment.set(String.valueOf(pair[0]), String.valueOf(pair[1]));
            }
        }
    }
}
