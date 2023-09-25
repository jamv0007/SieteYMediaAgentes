/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Clase que nos proporciona la utilidad para codificar un objeto de tipo T
 * a su objeto String en formato JSON y la utilidad inversa
 * @author pedroj
 */

public class GsonUtil<T> {
    private final Gson gson; 

    public GsonUtil() {
        this.gson = new GsonBuilder().create();
    }
    
    /**
     * Método que nos permite trasnformar un objeto de tipo T a un String en
     * su formato JSON
     * @param contenido
     *          objeto que se transformará en el contenido del mensaje
     * @param typeParameterClass
     *          tipo de la clase T
     * @return 
     *          String en formato JSON
     */
    public String encode(T contenido, Class<T> typeParameterClass) {
        if( contenido != null )
            return gson.toJson(contenido, typeParameterClass);
        else
            throw new NullPointerException();
    }
    
    /**
     * Método que nos permite decodificar el contenido de un mensaje de tipo
     * String en formato JSON a un objeto de tipo T
     * @param contenido
     *          String que corresponde con el contenido del mensaje 
     * @param typeParameterClass
     *          Tipo de la clase T que se espera
     * @return 
     *          un objeto del tipo T
     */
    public T decode(String contenido, Class<T> typeParameterClass) {
        if( contenido != null )
            return gson.fromJson(contenido, typeParameterClass);
        else
            throw new NullPointerException();
    } 
    
}


