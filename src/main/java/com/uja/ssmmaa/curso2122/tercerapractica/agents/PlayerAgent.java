/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.agents;


import com.uja.ssmmaa.curso2122.tercerapractica.gui.PlayerJFrame;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Constants;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.FIRST;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.MIN_DOUBLE_BET_MONEY;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.SECOND;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.THIRD;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Game;
import com.uja.ssmmaa.curso2122.tercerapractica.util.GsonUtil;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Player;
import com.uja.ssmmaa.curso2122.tercerapractica.util.PlayerData;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionInitiator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import javafx.util.Pair;


/**
 *
 * @author joseantonio
 */
public class PlayerAgent extends Agent implements Constants{
    
    private int money;//Dinero del jugador
    private STRATEGY strategy;//Tipo de estrategia
    private ArrayList<HashMap<Integer,Integer>> cards;//Lista de mapas con carta, cantidad de ese tipo
    private ArrayList<Pair<Long,PlayerData>> acceptedGames;//Juegos aceptados para jugar
    private ArrayList<PlayerData> playingGames;//Juegos que esta jugando
    private PlayerJFrame gui;
    
    
    @Override
    protected void setup(){
        
        //Inicializacion de las variables
        acceptedGames = new ArrayList<>();
        playingGames = new ArrayList<>();
        cards = new ArrayList<>();
        
       
        gui = new PlayerJFrame(this);
        gui.setVisible(true);
        
        
        
        
    }
    
    @Override
    protected void takeDown(){
        
        //Se borra del registro de paginas amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Finaliza la ejecución de " + this.getName());
        
        gui.dispose();
    }
    
    /***
     * Funcion para pasar los datos de la interfaz
     * @param money dinero
     * @param con di es conservadora
     */
    public void getInterfaceData(int money, boolean con){
        
        this.money = money;
        if(con){
            strategy = STRATEGY.CONSERVATIVE;
        }else{
            strategy = STRATEGY.AGRESIVE;
        }
        
        
        //Registro en el servicio de paginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        sd.setName(Service_Name.PLAYER.name());
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        
        //Plantilla para el protocolo
        MessageTemplate plantilla;
        plantilla= MessageTemplate.and(
                     MessageTemplate.not(
                         MessageTemplate.or(MessageTemplate.MatchSender(this.getDefaultDF()), 
                                            MessageTemplate.MatchSender(this.getAMS()))), 
                     MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE));
        
        MessageTemplate plantillaContract;
        plantillaContract= MessageTemplate.and(
                     MessageTemplate.not(
                         MessageTemplate.or(MessageTemplate.MatchSender(this.getDefaultDF()), 
                                            MessageTemplate.MatchSender(this.getAMS()))), 
                     MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET));

        //Se añaden las tareas
        addBehaviour(new HandleRecivePlayOfferProposeResponder(this, plantilla));
        addBehaviour(new TaskCheckGame());
        addBehaviour(new HandleCompleteRound(this, plantillaContract));
        
        
    }
    
    /********************
     * TAREAS
     *******************/
    
    /***
     * Tarea para comprobar si no ha recibido a tiempo la partida
     */
    private class TaskCheckGame extends CyclicBehaviour{
        

        @Override
        public void action() {
            
            ArrayList<Integer> toRemove = new ArrayList<>();
            
            for(int i = 0; i < acceptedGames.size(); i++){
                int res = (int)(System.currentTimeMillis() - acceptedGames.get(i).getKey()) / 1000;
                if(res >= 5){//Si han pasado 5 segundos se elimina
                    System.out.println(acceptedGames.get(i).getValue().getGame().getId() + " Tiempo: " + res);
                    money = money + acceptedGames.get(i).getValue().getMoney();
                    toRemove.add(i);
                }
            }
            
            for(int r: toRemove){
                acceptedGames.remove(r);
               
            }
        }
        
    }
    
    /*******************
     * PROTOCOLOS
     ********************/
    
    /***
     * Clase para recibir propuestas para jugar. Implementa el Responder del protocolo propose
     */
    private class HandleRecivePlayOfferProposeResponder extends ProposeResponder{
        
        private AID agent;

        public HandleRecivePlayOfferProposeResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            this.agent = a.getAID();
        }
        
        //Se llama cuando recibe un mensaje del protocolo propose
        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException{
            
            //Si el contenido es nulo salta excepcion
            if(propose.getContent() == null){
                throw new NotUnderstoodException("Mensaje vacio");
            }
            
            String[] s = propose.getContent().split("--");
            
            ACLMessage reply = null;
            
            if(s.length <= 1){
                reply = playOffer(propose);
            }
            
            return reply;

        }
        
        private ACLMessage playOffer(ACLMessage propose){
            
            //Se usa gson para convertir de string a objeto
            GsonUtil<Game> gUtil = new GsonUtil<>();
            Game g = gUtil.decode(propose.getContent(), Game.class);
            
            //Se crea la respueta al mensaje
            ACLMessage agree = propose.createReply();
            

             //Dependiendo de la cantidad de dinero actual y el tipo de apuesta se rellena el mensaje    
            if(money > 0){
               if(money < MIN_DOUBLE_BET_MONEY && g.getBetType() == BET_TYPE.DOUBLE){
                   //Se rechaza
                   agree.setPerformative(ACLMessage.REJECT_PROPOSAL);
                   agree.setContent(propose.getContent());
                   
               }else{
                   
                   if(money > 10){
                       //Si hay mas de 10 de dinero
                       //System.out.println("Tengo mas de 10, puedo doble o simple");
                        //Se acepta
                        agree.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        Player p = new Player(agent.getLocalName());
                        
                        GsonUtil<Player> gUtil2 = new GsonUtil<>();
                        
                        int betMoney = 0;
                        if(g.getBetType() == BET_TYPE.DOUBLE){
                            
                            //Calculo del dinero a apostar en dobles
                            Random r = new Random();
                            betMoney = r.nextInt(money - MIN_DOUBLE_BET_MONEY) + MIN_DOUBLE_BET_MONEY;
                            
                        }else{
                            //Calculo del dinero a apostar en simples
                            Random r = new Random();
                            betMoney = r.nextInt(money - 10) + 10;
                        }

                        
                        money = money - betMoney;
                        
                        p.setP(new PlayerData(g, betMoney));
                        
                        agree.setContent(propose.getContent() + "--" + gUtil2.encode(p, Player.class) + "--" + betMoney);

                        //Se guarda el tiempo y el dinero junto con la partida
                        acceptedGames.add(new Pair<Long,PlayerData>(System.currentTimeMillis(),new PlayerData(g,betMoney)));
                   
                   }else{
                        //Si queda solo 10 en Simple, se apuestan
                       
                        //System.out.println(" Recibido, Solo tengo 10");
                        //Se acepta
                        agree.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        Player p = new Player(agent.getLocalName());
                        GsonUtil<Player> gUtil2 = new GsonUtil<>();
                        


                        //Calculo del dinero a apostar
                        
                        int betMoney = money;
                        money = money - betMoney;
                        
                        p.setP(new PlayerData(g, betMoney));
                        
                        agree.setContent(propose.getContent() + "--" + gUtil2.encode(p, Player.class) + "--" + betMoney);

                        //Se guarda el tiempo y el dinero junto con la partida
                        acceptedGames.add(new Pair<Long,PlayerData>(System.currentTimeMillis(),new PlayerData(g,betMoney))); 
                   }
               }
            }else{
                
                //Se rechaza
                agree.setPerformative(ACLMessage.REJECT_PROPOSAL);
                agree.setContent(propose.getContent());
                //System.out.println("No tengo un duro");
                
            }
            
            
            
            return agree;
        }
        
        
        
    }
    
    /***
     * Handle del contract net para desarrollar los turnos de juego
     */
    private class HandleCompleteRound extends ContractNetResponder{
        
        public HandleCompleteRound(Agent a, MessageTemplate mt) {
            super(a, mt);
        }
        
        /***
         * Devuelve la puntuacion para una partida
         * @param game indice de la partuda
         * @return Puntuacion
         */
        private float countCards(int game){
            
            float points = 0.0f;
            
            for(int j = 1; j <= 12; j++){
               if(cards.get(game).containsKey(j)){
                   if(j == 10 || j == 11 || j == 12){
                       points = points + (0.5f * cards.get(game).get(j));
                   }else{
                       points = points + (j * cards.get(game).get(j));
                   }
               }
            }
            
            return points;
        }
        
        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException{
            
            if(cfp.getContent() == null){
                throw new NotUnderstoodException("Mensaje vacio");
            }
            
            
            
            //Se obtiene del mensaje
            String[] mess = cfp.getContent().split("--");
            
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            Game g = gsonGame.decode(mess[FIRST], Game.class);
            
            //Se comprueba si esta en la lista de jugando
            int is = -1;
            boolean found = false;
            for(int i = 0; i < acceptedGames.size() && !found; i++){
                if(acceptedGames.get(i).getValue().getGame().getId().equals(g.getId())){
                    is = i;
                    
                    found = true;
                }
            }
            
            //Si esta se elimina y se crea un espacio para las cartas y se mueve a la lista de jugando
            if(is != -1){
                
                playingGames.add(acceptedGames.get(is).getValue());
                cards.add(new HashMap<>());
                acceptedGames.remove(is);
                
                ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(cfp.getSender());
                
                addBehaviour(new HandleFinishGame(myAgent, msg));
                
            }
            
            //Se obtiene el jugador y la carta del jugador
            GsonUtil<Player> gsonPlayer = new GsonUtil<>();

            Player p = gsonPlayer.decode(mess[SECOND], Player.class);
            
            int number = Integer.parseInt(mess[THIRD]);

            //Se busca los datos de la partida
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            
            boolean isFounded = false;
            int actualGame = -1;
            for(int i = 0; i < playingGames.size() && !isFounded; i++){
                if(g.getId().equals(playingGames.get(i).getGame().getId())){
                    actualGame = i;
                    isFounded = true;
            
                }
            }
            
            //Si se encuentran los datos de la partida
            if(actualGame != -1){
                if(p.getAid().equals(myAgent.getLocalName())){
                    //Es mi turno de jugar
                    
                    
                    
                    //Añado la carta a mi lista
                    if(cards.get(actualGame).containsKey(number)){
                            //Si ya hay una carta con ese numero se incrementa el contador
                           int count = cards.get(actualGame).get(number);
                           cards.get(actualGame).put(number, count+1);
                            
                        }else{
                            //Si no se añade
                           cards.get(actualGame).put(number, 1);
                            
                    }
                    
                    //Cuento los puntos que tengo
                    float points = countCards(actualGame);
                    
                    
                   
                    //Segun la cantidad escojo una accion
                    if(points <= 5.0f){
                           
                        reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.GET_CARD);

                    }else if(points > 5.0f && points < 6.0f){


                        reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.GET_CARD);


                    }else if(points >= 6.0f && points < 7.5f){

                        if(strategy == STRATEGY.CONSERVATIVE){
                            reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.REFUSE);
                        }else{
                            reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.GET_CARD);
                        }

                    }else if(points == 7.5f){

                        reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.REFUSE);

                    }else if(points > 7.5f){

                        reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.OUT);

                    }
                    
                }else{
                    //Es el turno de otro jugador
                    
                    //Indico que no es mi turno con la partida y el movimiento NO_TURN
                    reply.setContent(gsonGame.encode(g, Game.class) + "--" + MOVEMENT.NO_TURN);
                }
            }
            

            
            
            return reply;
            
            
        }
        
        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp,ACLMessage propose,ACLMessage accept){
            
            
            ACLMessage reply = accept.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            
            //Se devuelve la puntuacion para cada turno
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            Game g = gsonGame.decode(accept.getContent(), Game.class);
            
            boolean isFounded = false;
            int actualGame = -1;
            for(int i = 0; i < playingGames.size() && !isFounded; i++){
                if(g.getId().equals(playingGames.get(i).getGame().getId())){
                    actualGame = i;
                    isFounded = true;
            
                }
            }
            
            float points = countCards(actualGame);
            
            
            
            reply.setContent(""+points);
            
            

            return reply;
        }
        
    }
    
    /***
     * Handle para recibir resultado de la partida
     */
    private class HandleFinishGame extends SubscriptionInitiator{
        
        public HandleFinishGame(Agent a, ACLMessage msg) {
            super(a, msg);
        }
        
        
        //Respuesta a la subscripcion
        @Override
        protected void handleAllResponses(Vector responses) {
            Iterator it = responses.iterator();

            while (it.hasNext()) {
                

                ACLMessage msg = (ACLMessage) it.next();
                

                switch ( msg.getPerformative() ) {
                    case ACLMessage.REFUSE:
                    case ACLMessage.FAILURE:
                    case ACLMessage.AGREE:
                        //Recepcion del mensaje
                        
                        break;
                    default:
                        break;
                }
                
            }
        }
        
        
         //Cada vez que se informa
        @Override
        protected void handleInform(ACLMessage inform) {
            //Datos de la partida
            //Dinero ganado, Banca o Jugadores
            
            //System.out.println("Ha llegado conclusion de la partida");
            
            
            String[] s = inform.getContent().split("--");
            
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            
            Game g = gsonGame.decode(s[FIRST], Game.class);
            
            boolean isFounded = false;
            int actualGame = -1;
            for(int i = 0; i < playingGames.size() && !isFounded; i++){
                if(g.getId().equals(playingGames.get(i).getGame().getId())){
                    actualGame = i;
                    isFounded = true;
            
                }
            }
            
            
            
            if(actualGame != -1){
                
                //Compruebo si soy el ganador
                GsonUtil<Player> gsonPlayer = new GsonUtil<>();
                Player p = gsonPlayer.decode(s[SECOND], Player.class);

                

                if(p.getAid().equals(myAgent.getLocalName())){
                    //He ganado yo
                    System.out.println("sumando dinero");

                    int win = 2 * playingGames.get(actualGame).getMoney();

                    money += win;


                }
                
                
            }    
            
            
            
        }
        
    }
    
        
}
