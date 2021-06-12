package com.wt.maven.core;

import com.wt.maven.analyzer.MavenDependencyAnalyzer;
import com.wt.maven.analyzer.MavenDependencyCommand;
import com.wt.maven.git.GitCloneCommand;
import com.wt.maven.git.GitProjectCommand;
import com.wt.maven.git.GitProjectsInfoDTO;
import com.wt.maven.git.GitPullCommand;
import com.wt.maven.util.ClassUtil;
import com.wt.maven.util.JacksonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class AnalyzerBootstrap {

    private static ClassLoader classLoader;

    static {
        classLoader = ClassUtil.getDefaultClassLoader();
    }

    private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    public static final AtomicInteger allProjects = new AtomicInteger(0);

    public static final AtomicInteger validProjects = new AtomicInteger(0);

    public static void main(String[] args) {
        log.info("扫描工具启动。");
        LocalDateTime startTime = LocalDateTime.now();
        AnalyzerBootstrap.run(args);
        LocalDateTime endTime = LocalDateTime.now();
        log.info("扫描完成。启动时间:{},结束时间:{}",startTime.toString(),endTime.toString());
        log.info("共扫描项目{}个,其中有效的maven项目为{}个", allProjects.get(), validProjects.get());
        System.exit(0);
    }

    public static void run(String[] args) {
        ProgramEnvironment.prepareEnvironment(args);
        int index = 1;
        while (true) {
            log.info(String.format("获取git工程,分页:%d", index));
            List<GitProjectsInfoDTO> projects = getGitProjects(index);
            if (projects == null || projects.isEmpty()) {
                log.info("没有获取到git工程");
                break;
            }
            allProjects.getAndAdd(projects.size());
            pullCode(projects);
            analyzeDependency(projects);
            index++;
        }
        MavenDependencyAnalyzer.flushBuffer();
    }

    public static List<GitProjectsInfoDTO> getGitProjects(int index) {
        Command gitProjectsCommand = new GitProjectCommand();
        String commandOutput = gitProjectsCommand.exec(String.valueOf(index));
        if (StringUtils.isBlank(commandOutput))
            return new ArrayList<>();
        List<GitProjectsInfoDTO> commandDTOS = JacksonUtil.readValue(commandOutput, new TypeReference<List<GitProjectsInfoDTO>>() {
        });
        return commandDTOS;
    }

    public static void pullCode(List<GitProjectsInfoDTO> projects) {
        String gitFileBaseDir = ProgramEnvironment.get("git.file.base.dir");
        if (StringUtils.isBlank(gitFileBaseDir)) {
            log.error("请检查配置:git.file.base.dir");
            throw new RuntimeException("请检查配置:git.file.base.dir");
        }
        if (!gitFileBaseDir.endsWith("/")) {
            gitFileBaseDir = gitFileBaseDir + "/";
            ProgramEnvironment.set("git.file.base.dir", gitFileBaseDir);
        }
        final String[] dirs = {gitFileBaseDir};
        projects.parallelStream().forEach((x) -> {
            String workPath = dirs[0] + x.getNamespace().getPath() + "/" + x.getPath();
            File workDir = new File(workPath);
            if (!workDir.exists())
                workDir.mkdirs();
            String[] files = workDir.list();
            if (files == null || files.length == 0) {
                Command gitCloneCommand = new GitCloneCommand();
                String url = ProgramEnvironment.get("git.clone.type").equalsIgnoreCase("ssh") ? x.getSshUrlToRepo() : x.getHttpUrlToRepo();
                submitCommand(gitCloneCommand, url, workPath);
            } else {
                Command gitPullCommand = new GitPullCommand();
                submitCommand(gitPullCommand, workPath);
            }
        });
    }

    public static void analyzeDependency(List<GitProjectsInfoDTO> projects) {
        projects.parallelStream().forEach((x) -> {
            Command mavenDependencyCommand = new MavenDependencyCommand();
            String gitFileBaseDir = ProgramEnvironment.get("git.file.base.dir");
            String workPath = gitFileBaseDir + x.getNamespace().getPath() + "/" + x.getPath();
            String output = submitCommand(mavenDependencyCommand, workPath);
            if (Boolean.valueOf(ProgramEnvironment.get("result.group")))
                MavenDependencyAnalyzer.writeToBuffer(output, x);
            else
                MavenDependencyAnalyzer.writeDirectly(output, x);
        });
    }


    public static String submitCommand(Command command, String... args) {
        Future<String> future = pool.submit(() -> {
            return command.exec(args);
        });
        try {
            String result = future.get();
            return result;
        } catch (Exception e) {
            log.error("处理Process输出失败", e);
            throw new RuntimeException("处理Process输出失败");
        }
    }
}
