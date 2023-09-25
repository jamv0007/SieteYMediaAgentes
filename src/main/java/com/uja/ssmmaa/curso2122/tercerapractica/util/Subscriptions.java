/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.util;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.proto.SubscriptionResponder.Subscription;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author joseantonio
 */
public class Subscriptions implements SubscriptionManager{
    
    private final Map<String, Subscription> sub;

    public Subscriptions() {
        sub = new HashMap<>();
    }

    @Override
    public boolean register(SubscriptionResponder.Subscription s) throws RefuseException, NotUnderstoodException {
        // Guardamos la suscripción asociada al agente que la solita
        String nombreAgente = s.getMessage().getSender().getName();
        sub.put(nombreAgente, s);
        return true;
    }

    @Override
    public boolean deregister(SubscriptionResponder.Subscription s) throws FailureException {
        // Eliminamos la suscripción asociada a un agente
        String nombreAgente = s.getMessage().getSender().getName();
        sub.remove(nombreAgente);
        s.close(); // queda cerrada la suscripción
        return true;
    }
    
    public Subscription getSubscripcion( String nombreAgente ) {
        return sub.get(nombreAgente);
    }
    
    public Collection<Subscription> values() {
        return sub.values();
    }
    
    public boolean haySubscripcion( String nombreAgente ) {
        return sub.get(nombreAgente) != null;
    }
    
    public boolean isEmpty() {
        return sub.isEmpty();
    }
    
    public int size() {
        return sub.size();
    }

}
