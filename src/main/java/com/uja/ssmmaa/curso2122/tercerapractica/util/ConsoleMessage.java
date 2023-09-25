/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.util;

/**
 *
 * @author joseantonio
 */
public class ConsoleMessage {
    
    private String agentName;
    private String content;

    public ConsoleMessage(String agentName, String content) {
        this.agentName = agentName;
        this.content = content;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ConsoleMessage{" + "agentName=" + agentName + ", content=" + content + '}';
    }
    
    
    
}
