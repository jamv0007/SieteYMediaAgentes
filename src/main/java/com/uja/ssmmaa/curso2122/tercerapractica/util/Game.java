/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.util;

import java.util.ArrayList;

/**
 *
 * @author joseantonio
 */
public class Game implements Constants{
    
    private String id;
    private int round;
    private Constants.BET_TYPE betType;
    
    public Game(){
        
    }

    public Game(String id, int round, BET_TYPE betType) {
        this.id = id;
        this.round = round;
        this.betType = betType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public BET_TYPE getBetType() {
        return betType;
    }

    public void setBetType(BET_TYPE betType) {
        this.betType = betType;
    }
    
    
}
