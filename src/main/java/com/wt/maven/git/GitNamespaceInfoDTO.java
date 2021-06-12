package com.wt.maven.git;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@Data
public class GitNamespaceInfoDTO implements Serializable {
    private static final long serialVersionUID = -4859471095921991706L;

    private String name;

    private String path;

}
