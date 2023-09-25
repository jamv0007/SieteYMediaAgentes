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
public class PlayerData {
    
    private Game game;
    private int money;
    

    public PlayerData(Game game, int money) {
        this.game = game;
        this.money = money;

    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }


    
    
    
    
}
