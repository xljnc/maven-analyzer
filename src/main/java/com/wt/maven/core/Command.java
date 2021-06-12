package com.wt.maven.core;

/**
 * @author 一贫
 * @date 2021/6/7
 */
@FunctionalInterface
public interface Command {

    String exec(String... args) throws RuntimeException;
}
