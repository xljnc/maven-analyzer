package com.wt.maven.analyzer;

import com.wt.maven.core.AnalyzerBootstrap;
import com.wt.maven.core.Command;
import com.wt.maven.core.CommandStreamHandler;
import com.wt.maven.core.ProgramEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author 一贫
 * @date 2021/6/8
 */
@Slf4j
public class MavenDependencyCommand implements Command {

    @Override
    public String exec(String... args) throws RuntimeException {
        if (args == null || StringUtils.isBlank(args[0])) {
            log.error("解析POM依赖需要指定POM文件地址");
            throw new RuntimeException("解析POM依赖需要指定POM文件地址");
        }
        File workDir = new File(args[0]);
        String[] files = workDir.list();
        if (files == null || files.length == 0) {
            log.info("目录{}为空", args[0]);
            return "";
        }
        boolean isMaven = false;
        for (String file : files) {
            if (file.equalsIgnoreCase("pom.xml")) {
                isMaven = true;
                break;
            }
        }
        if (!isMaven) {
            log.info("目录{}为非maven目录,放弃解析", args[0]);
            return "";
        }
        AnalyzerBootstrap.validProjects.incrementAndGet();
        log.info(String.format("开始解析POM依赖,目录:%s", args[0]));
        StringBuilder sb = new StringBuilder();
        sb.append("mvn dependency:tree -U -f ").append(args[0]);
        if (ProgramEnvironment.contains("maven.jar.include"))
            sb.append(" -Dincludes=").append(ProgramEnvironment.get("maven.jar.include"));
        try {
            Process process = new ProcessBuilder("/bin/sh", "-c", sb.toString())
                    .directory(new File(args[0]))
                    .start();
            String result = CommandStreamHandler.handleInputStream(process);
            CommandStreamHandler.handleErrorStream(process);
            process.waitFor();
            return result;
        } catch (Exception e) {
            log.error("解析POM依赖失败", e);
            throw new RuntimeException("解析POM依赖失败");
        }
    }
}
