package com.wt.maven.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Data
public class GitProjectsInfoDTO implements Serializable {
    private static final long serialVersionUID = -4859471095921991706L;

    @JsonProperty("ssh_url_to_repo")
    private String sshUrlToRepo;

    @JsonProperty("http_url_to_repo")
    private String httpUrlToRepo;

    @JsonProperty("path_with_namespace")
    private String pathWithNamespace;

    private String name;

    private String path;

    private GitNamespaceInfoDTO namespace;

}
