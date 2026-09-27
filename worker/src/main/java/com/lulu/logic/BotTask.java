package com.lulu.logic;

/**
 * @author 噜噜
 */
public interface BotTask {
    void execute() throws InterruptedException; // 每个任务都要实现这个“执行”方法
}
