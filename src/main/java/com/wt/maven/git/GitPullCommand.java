package com.wt.maven.git;

import com.wt.maven.core.Command;
import com.wt.maven.core.CommandStreamHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class GitPullCommand implements Command {

    @Override
    public String exec(String... args) throws RuntimeException {
        if (args == null || StringUtils.isBlank(args[0])) {
            log.error("更新git代码需要指定目录");
            throw new RuntimeException("更新git代码需要指定目录");
        }
        log.info(String.format("更新git代码,目录:%s", args[0]));
        String command = "git pull";
        try {
            Process process = new ProcessBuilder("/bin/sh", "-c", command)
                    .directory(new File(args[0]))
                    .start();
            String result = CommandStreamHandler.handleInputStream(process);
            CommandStreamHandler.handleErrorStream(process);
            process.waitFor();
            return result;
        } catch (Exception e) {
            log.error("更新git代码失败", e);
            throw new RuntimeException("更新git代码失败");
        }
    }
}
