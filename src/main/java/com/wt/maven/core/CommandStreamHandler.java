package com.wt.maven.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.concurrent.*;

/**
 * @author 一贫
 * @date 2021/6/8
 */
@Slf4j
public class CommandStreamHandler {

    private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    public static String handleInputStream(Process process) {
        Future<String> future = pool.submit(() -> {
            try {
                InputStream inputStream = process.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[1024];
                int len = -1;
                while ((len = inputStream.read(buffer)) != -1)
                    sb.append(new String(buffer, 0, len));
                return sb.toString();
            } catch (Exception e) {
                log.error("处理Process输入流失败", e);
                return "";
            }
        });
        try {
            String result = future.get();
            return result;
        } catch (Exception e) {
            log.error("处理Process输入流失败", e);
            throw new RuntimeException("处理Process输入流失败");
        }
    }

    public static String handleErrorStream(Process process) {
        Future<String> future = pool.submit(() -> {
            InputStream inputStream = process.getErrorStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) != -1)
                sb.append(new String(buffer, 0, len));
            return sb.toString();
        });
        try {
            String result = future.get();
            if (StringUtils.isNotBlank(result))
                log.error("Process执行失败,返回:{}", result);
            return result;
        } catch (Exception e) {
            log.error("处理Process错误流失败", e);
            throw new RuntimeException("处理Process错误流失败");
        }
    }
}
