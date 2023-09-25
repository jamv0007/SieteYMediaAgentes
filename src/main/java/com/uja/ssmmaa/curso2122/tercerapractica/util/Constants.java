/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.util;

import java.util.Random;

/**
 *
 * @author joseantonio
 */
public interface Constants {
    
    public static final int FIRST = 0;
    public static final int SECOND = 1;
    public static final int THIRD = 2;
    
    public static final int MIN_DOUBLE_BET_MONEY = 200;
    
    public static final long TIME_PROCESS_MESSAGES = 5000;
    public static final long TIME_SEARCH_PLAYERS = 10000;
    public static final long TIME_WAIT_TABLE_RESPONSE = 3000;
    public static final long TIME_WAIT_SEARCH_PLAYERS = 5000;
    public static final long TIME_WAIT_PLAYER_MOVEMENT = 5000;
    

    
    public static final String SERVICE_TYPE = "Service";
    
    public enum Service_Name {
        
        CONSOLE, PLAYER;
    }
    
    public enum BET_TYPE {
        SIMPLE,DOUBLE
    }
       
    public enum MOVEMENT{
        GET_CARD,REFUSE,NO_TURN,WINNER,OUT,NEXT_MOVEMENT,GAME_OVER
    }
    
    public enum STRATEGY{
        CONSERVATIVE,AGRESIVE
    }
    
    public static final Service_Name[] SERVICES = Service_Name.values();
    
    public static final Random random = new Random();
    
    public static int getRandom(){
        return random.nextInt(10);
    }
    
    public static final Random r1 = new Random();
    
    public static int getRandomCard(){
        
        int res = random.nextInt(12 - 1) + 1;
        
        while(res == 8 || res == 9){
            
            res = random.nextInt(12 - 1) + 1;
            
        }
        
        return res;
    }
    
    
            
    
    
}
