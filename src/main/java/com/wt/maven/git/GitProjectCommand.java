package com.wt.maven.git;

import com.wt.maven.core.Command;
import com.wt.maven.core.ProgramEnvironment;
import com.wt.maven.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 一贫
 * @date 2021/6/7
 */
public class GitProjectCommand implements Command {

    @Override
    public String exec(String... args) throws RuntimeException {
        String apiUrl = ProgramEnvironment.get("git.projects.api.url");
        if (StringUtils.isBlank(apiUrl))
            throw new RuntimeException("Git获取项目API URL不能为空");
        Map<String, String> header = new HashMap<>(4);
        header.put("PRIVATE-TOKEN", ProgramEnvironment.get("git.PRIVATE-TOKEN"));
        try {
            Map<String, String> param = new HashMap<>(4);
            param.put("per_page", ProgramEnvironment.get("git.projects.page.size"));
            param.put("page", args == null || StringUtils.isBlank(args[0]) ? "1" : args[0]);
            String result = HttpClientUtil.httpGet(apiUrl, param, header);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("获取Git项目失败");
        }
    }
}
