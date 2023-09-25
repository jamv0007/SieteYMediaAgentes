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
public class Player implements Constants{
    
    private String aid;
    private PlayerData p;
    private float points;
    

    public Player(String aid) {
        this.aid = aid;
        this.points = 0.0f;
       
    }

    public float getPoints() {
        return points;
    }

    public void setPoints(float points) {
        this.points = points;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public PlayerData getP() {
        return p;
    }

    public void setP(PlayerData p) {
        this.p = p;
    }

    
    
    

    
}
