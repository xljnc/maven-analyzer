package com.wt.maven.git;

import com.wt.maven.core.Command;
import com.wt.maven.core.CommandStreamHandler;
import com.wt.maven.core.ProgramEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class GitCloneCommand implements Command {

    @Override
    public String exec(String... args) throws RuntimeException {
        if (args == null) {
            log.error("拉取git代码需要指定目录和项目地址");
            throw new RuntimeException("拉取git代码需要指定目录和项目地址");
        }
        log.info(String.format("拉取git代码,目录:%s", args[1]));
        String command = null;
        if (ProgramEnvironment.get("git.clone.type").equalsIgnoreCase("ssh")) {
            command = String.format("git clone %s %s ", args[0], args[1]);
        } else {
            if (!ProgramEnvironment.contains("git.clone.account") || StringUtils.isBlank(ProgramEnvironment.get("git.clone.account")) || !ProgramEnvironment.contains("git.clone.password") || StringUtils.isBlank(ProgramEnvironment.get("git.clone.password"))) {
                log.error("http拉取git代码需要指定用户名和密码");
                throw new RuntimeException("http拉取git代码需要指定用户名和密码");
            }
            String account = ProgramEnvironment.get("git.clone.account");
            String password = ProgramEnvironment.get("git.clone.password");
            try {
                account = URLEncoder.encode(account,"UTF-8");
                password = URLEncoder.encode(password,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("账号密码URL encode失败",e);
            }
            Integer index = args[0].indexOf("://");
            String url = args[0].substring(0,index) + "://" + account + ":" + password + "@" + args[0].substring(index + 3);
            command = String.format("git clone %s %s ", url, args[1]);
        }
        try {
            Process process = new ProcessBuilder("/bin/sh", "-c", command)
                    .directory(new File(args[1]))
                    .start();
            String result = CommandStreamHandler.handleInputStream(process);
            CommandStreamHandler.handleErrorStream(process);
            process.waitFor();
            return result;
        } catch (Exception e) {
            log.error("拉取git代码失败", e);
            throw new RuntimeException("拉取git代码失败");
        }
    }
}
