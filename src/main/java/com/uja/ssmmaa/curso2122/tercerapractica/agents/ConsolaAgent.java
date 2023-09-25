/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.agents;

import com.uja.ssmmaa.curso2122.tercerapractica.gui.ConsolaJFrame;
import com.uja.ssmmaa.curso2122.tercerapractica.util.ConsoleMessage;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Constants;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;

/**
 *
 * @author joseantonio
 */
public class ConsolaAgent extends Agent{
    
    private ArrayList<ConsolaJFrame> console;
    private ArrayList<ConsoleMessage> noAddMessages;
    
    @Override
    protected void setup(){
        console = new ArrayList<>();
        noAddMessages = new ArrayList<>();
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Constants.SERVICE_TYPE);
        sd.setName(Constants.Service_Name.CONSOLE.name());
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
       //Registro de la Ontología
       System.out.println("Se inicia la ejecución del agente: " + this.getName());
       //Añadir las tareas principales
       addBehaviour(new TaskReciveMessage());

    }
    
    @Override
    protected void takeDown(){
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        closeConsole();
        System.out.println("Finaliza la ejecución de " + this.getName());

    }
    
    private ConsolaJFrame searchConsole(String agentName) {
        // Obtenemos la consola donde se presentarán los mensajes
        for(int i = 0; i < console.size(); i++){ 
            if (console.get(i).getAgentName().compareTo(agentName) == 0)
                return console.get(i);
        }          
        return null;
    }
    
    private void closeConsole() {
        //Se eliminan las consolas que están abiertas
        for(int i = 0; i < console.size(); i++){
            console.get(i).dispose();
        }
            
    }


    public ArrayList<ConsolaJFrame> getConsola() {
        return console;
    }

    public ArrayList<ConsoleMessage> getNoAddMessages() {
        return noAddMessages;
    }
    
    public class TaskReciveMessage extends CyclicBehaviour {

        @Override
        public void action() {
            //Solo se atenderán mensajes INFORM
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                //procesamos el mensaje
                ConsoleMessage mensajeConsola = new ConsoleMessage(mensaje.getSender().getName(),
                                    mensaje.getContent());
                noAddMessages.add(mensajeConsola);
                addBehaviour(new TaskDisplayMensaje());
            } 
            else
                block();
        }
    
    }
    
    public class TaskDisplayMensaje extends OneShotBehaviour {

        @Override
        public void action() {
            //Se coge el primer mensaje
            ConsoleMessage consoleMessage = noAddMessages.remove(Constants.FIRST);
            
            //Se busca la ventana de consola o se crea una nueva
            ConsolaJFrame gui = searchConsole(consoleMessage.getAgentName());
            if (gui == null) {
                gui = new ConsolaJFrame(consoleMessage.getAgentName());
                console.add(gui);
            } 
            
            gui.output(consoleMessage);
        }
    }
}
