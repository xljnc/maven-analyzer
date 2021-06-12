package com.wt.maven.analyzer;

import com.wt.maven.core.ProgramEnvironment;
import com.wt.maven.git.GitProjectsInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依赖解析器
 *
 * @author 一贫
 * @date 2021/6/7
 */
@Slf4j
public class MavenDependencyAnalyzer {

    private static FileChannel channel;

    private static Map<String, List<String>> buffer;

    static {
        if (!ProgramEnvironment.contains("output.file"))
            throw new RuntimeException(String.format("请指定输出文件,配置:%s", "output.file"));
        File file = new File(ProgramEnvironment.get("output.file"));
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
            channel = new FileOutputStream(file).getChannel();
        } catch (Exception e) {
            log.error("创建输出文件失败");
            throw new RuntimeException("创建输出文件失败");
        }
        buffer = new ConcurrentHashMap<>(32);
    }

    private static String extraInfo(String content) {
        if (StringUtils.isBlank(content))
            return "";
        String[] lines = content.split("\n");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("[INFO] --- maven-dependency-plugin:")) {
                i++;
                while (i < lines.length) {
                    if (!lines[i].startsWith("[INFO]")) {
                        i++;
                        continue;
                    }
                    if (lines[i].equals("[INFO] ") || lines[i].startsWith("[INFO] ---")) {
                        i++;
                        break;
                    } else {
                        String subStr = lines[i].substring(7);
                        text.append(subStr).append("\n");
                        i++;
                    }
                }
            }
        }
        return text.toString();
    }

    public static void writeDirectly(String content, GitProjectsInfoDTO project) {
        content = extraInfo(content);
        String pathWithNamespace = project.getPathWithNamespace();
        if (!StringUtils.isBlank(content))
            content = pathWithNamespace + "\n" + content + "\n";
        write(content);
    }

    public static void writeToBuffer(String content, GitProjectsInfoDTO project) {
        content = extraInfo(content);
        String group = project.getNamespace().getPath();
        String pathWithNamespace = project.getPathWithNamespace();
        if (!StringUtils.isBlank(content)) {
            buffer.putIfAbsent(group, new ArrayList<>());
            buffer.get(group).add(pathWithNamespace + "\n" + content + "\n");
        }
    }

    public static synchronized void flushBuffer() {
        if (buffer.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : buffer.entrySet()) {
            sb.append(entry.getKey()).append("\n");
            for (String item : entry.getValue()) {
                sb.append(item);
            }
            sb.append("\n");
        }
        write(sb.toString());
    }

    private static synchronized void write(String content) {
        if (StringUtils.isBlank(content))
            return;
        FileLock lock = null;
        try {
            lock = channel.lock();
            if (lock != null) {
                byte[] bytes = content.getBytes();
                ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                buffer.put(bytes);
                buffer.flip();
                channel.write(buffer);
                channel.force(true);
            }
        } catch (Exception e) {
            log.error("写入输出文件失败", e);
            throw new RuntimeException("写入输出文件失败");
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException e) {
                    log.error("释放文件锁失败", e);
                }
            }
        }
    }
}
