package com.yukai.team.matchservice.opponentanalysis.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptLoaderTest {

    @Test
    void loadsOpponentAnalysisSystemPrompt() {
        String prompt = new PromptLoader().opponentAnalysisSystemPrompt();

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("你是足球比赛对手分析助手");
    }

    @Test
    void loadsUtf8ChineseContentCompletely() {
        String prompt = new PromptLoader().opponentAnalysisSystemPrompt();

        assertThat(prompt).contains("所有 JSON 字符串值必须使用简体中文");
        assertThat(prompt).contains("不得虚构球员、阵型、伤病、停赛、战术体系、控球率、射门或历史交锋");
    }

    @Test
    void containsRequiredGuardrails() {
        String prompt = new PromptLoader().opponentAnalysisSystemPrompt();

        assertThat(prompt).contains("只返回一个 JSON 对象");
        assertThat(prompt).contains("不允许返回 Markdown、代码块");
        assertThat(prompt).contains("strengths 和 weaknesses 始终针对对手");
        assertThat(prompt).contains("recommendations 始终针对我方");
        assertThat(prompt).contains("不得把 HOME 直接解释为“主场优势”");
        assertThat(prompt).contains("返回内容必须以 { 开始，以 } 结束");
    }
}
