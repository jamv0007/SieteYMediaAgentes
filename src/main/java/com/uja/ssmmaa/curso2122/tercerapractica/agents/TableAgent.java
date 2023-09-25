/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.agents;
import com.uja.ssmmaa.curso2122.tercerapractica.util.ConsoleMessage;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Constants;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.FIRST;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.SECOND;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.TIME_WAIT_TABLE_RESPONSE;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Game;
import com.uja.ssmmaa.curso2122.tercerapractica.util.GsonUtil;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Player;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Subscriptions;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFSubscriber;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;
import jade.proto.ProposeInitiator;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionResponder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 *
 * @author joseantonio
 */
public class TableAgent extends Agent implements Constants{
    
    private ArrayList<AID> consoleAgents;//Agentes consola
    private ArrayList<Player> players;//Jugadores de la partida
    private HashMap<Integer,Integer> cards;//Baraja de cartas
    private Game actualGame = null;//Partida que se esta jugando
    private int money;//Dinero de la mesa
    private boolean nextMovement = true;//Si se puede jugar el siguiente movimiento
    private boolean finish = false;
    private ArrayList<ConsoleMessage> messages; //Mensajes para consola
    private Subscriptions manager;//Gestor de subscripciones
    private AID room;//AID de la sala
    private int allResponses = 0;
    
    @Override
    protected void setup(){
        
        //Inicializacion
        players = new ArrayList<>();
        consoleAgents = new ArrayList<>();
        cards = new HashMap<>();
        messages = new ArrayList<>();
        manager = new Subscriptions();
        
        inicializeCards();
        
        System.out.println("Se inicia la ejecución del agente: " + this.getName());
       
        

       //Plantilla para buscar agentes  
       DFAgentDescription template = new DFAgentDescription();
       ServiceDescription templateSd = new ServiceDescription();
       templateSd.setType(SERVICE_TYPE);
       template.addServices(templateSd);
       
       //Plantilla
       MessageTemplate plantilla;
        plantilla= MessageTemplate.and(
                     MessageTemplate.not(
                         MessageTemplate.or(MessageTemplate.MatchSender(this.getDefaultDF()), 
                                            MessageTemplate.MatchSender(this.getAMS()))), 
                     MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE));
       MessageTemplate plantillaSub;
       plantillaSub= MessageTemplate.and(
                     MessageTemplate.not(
                         MessageTemplate.or(MessageTemplate.MatchSender(this.getDefaultDF()), 
                                            MessageTemplate.MatchSender(this.getAMS()))),MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE));

        //Se añaden las tareas
       addBehaviour(new TaskSuscriptionDF(this, template));
       addBehaviour(new HandleDevelopGameProposeParticipant(this, plantilla));
       addBehaviour(new TaskSendConsole(this, TIME_PROCESS_MESSAGES));
       addBehaviour(new HandleSubcriptionRoom(this, plantillaSub,manager));
    }
    
    @Override
    protected void takeDown(){
        
        System.out.println("Finaliza la ejecución del agente: " + this.getName());
         
    }
    
    /***
     * Funcion que inicializa la baraja de la mesa
     */
    private void inicializeCards(){
        
        int cont = 0;
        int number = 1;
        
        
        while(cont < 40){
            
            cards.put(number, 4);
            cont=cont+4;
            
            number++;
            
            if(number == 8){
                number = 10;
            }
        }
    }
    
    /***
     * Obtiene una carta
     * @return Numero de la carta
     */
    private int getCard(){
        
        int c = -1;
        
        while(c == -1){
            int res = Constants.getRandomCard();
            int number = cards.get(res);
            if(number > 0){
                c = res;
                cards.replace(res, number-1);
            }
        }
        
        return c;
        
    }
    
    
    /***************
     * TAREAS
     ***************/
    
    /***
     * Tarea para la suscripcion al servicio de pagina amarillas
     */
    private class TaskSuscriptionDF extends DFSubscriber{
        
        public TaskSuscriptionDF(Agent a, DFAgentDescription template){
            super(a, template);
        }

        @Override
        public void onRegister(DFAgentDescription dfad) {
            Iterator it = dfad.getAllServices();
            while(it.hasNext()){
                ServiceDescription af =(ServiceDescription) it.next();
                if(af.getName().equals(Constants.SERVICES[Constants.Service_Name.CONSOLE.ordinal()].name())){
                    consoleAgents.add(dfad.getName());
                    System.out.println("El agente: " + myAgent.getName() +
                    "ha encontrado a:\n\t" + dfad.getName().getName());
                }
                
            }
            
           
        }

        @Override
        public void onDeregister(DFAgentDescription dfad) {
            AID agente = dfad.getName();
            
            if(consoleAgents.remove(agente)){
                System.out.println("El agente: " + agente.getName() + 
                        " ha sido eliminado de la lista de " 
                        + myAgent.getName());

            }
            
        }
        
    }
    
    /***
     * Tarea para jugar la partida
     */
    private class TaskGame extends Behaviour{
        
        

        @Override
        public void action() {
            
            //Si ha terminado el turno del jugador actual
            if(nextMovement && (actualGame.getRound()-1 < players.size())){
                //Le toca al siguiente
                
    
                int card = getCard();
                
                
                //Se crea el mensaje de cfp
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                cfp.setReplyByDate(new Date(System.currentTimeMillis() + TIME_WAIT_TABLE_RESPONSE));
                GsonUtil<Game> gsonGame = new GsonUtil<>();
                GsonUtil<Player> gsonPlayer = new GsonUtil<>();
            
            
                messages.add(new ConsoleMessage(myAgent.getName(),"Ronda " + actualGame.getRound() + " Le toca al jugador " + players.get(actualGame.getRound()-1).getAid()));
                

                messages.add(new ConsoleMessage(myAgent.getName(), "Ha tocado de carta: " + card));

                cfp.setContent(gsonGame.encode(actualGame, Game.class) + "--" + gsonPlayer.encode(players.get(actualGame.getRound()-1), Player.class) + "--" + card);


                //Se añaden los destinatarios del mensaje
                for(int i = 0; i < players.size(); i++){

                    AID a = new AID(players.get(i).getAid(),AID.ISLOCALNAME);
                    
                    cfp.addReceiver(a);


                }
            
                
             
                //Se añade la tarea del protocolo Contract-Net
                addBehaviour(new HandleCompleteRound(myAgent, cfp));
                
                //Se activa el bool para que no pase al siguiente turno todavia
                nextMovement = false;
            }
        }

        @Override
        public boolean done() {
            
            if(actualGame.getRound()-1 == players.size() && finish){
                messages.add(new ConsoleMessage(myAgent.getName(), "Ha terminado el turno de los demas jugadores. Le toca a la banca"));
                addBehaviour(new TaskFinishGame(actualGame));
                return true;
            }
            
            return false;
        }
        
    }
    
    /***
     * Tarea para finalizar la partida
     */
    private class TaskFinishGame extends OneShotBehaviour{
        
        private Game g;
        private Boolean playerWin = false;

        public TaskFinishGame(Game g) {
            this.g = g;
        }
        

        @Override
        public void action() {
            
            //La banca juega
            boolean finishPlay = false;
            float points = 0;
            
            ArrayList<ACLMessage> mensajes = new ArrayList<>();
            //Se crea el mensaje
            for(int i = 0; i < players.size(); i++){
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID(players.get(i).getAid(),AID.ISLOCALNAME));
                mensajes.add(msg);

            }
            
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            
            while(!finishPlay){
                //Segun la cantidad escojo una accion
                int card = getCard();
                
                //Se calcula la puntuacion
                if(card == 10 || card == 11 || card == 12){
                    points += 0.5f;
                }else{
                    points += card;
                }
                
                messages.add(new ConsoleMessage(myAgent.getName(), "Banca ha sacado " + card));
                
                //Se compprueba la puntuacion
                if(points >= 6 && points < 7.5){
                        finishPlay = true;
                }else if(points == 7.5){
                        finishPlay = true;
                }else if(points > 7.5){
                        finishPlay = true;
                }
            }
            
            
            //Se calcula el mas cercano a 7.5
            int near = -1;
            float nearPoints = 0;
            float difference = 1000;
            
            for(int i = 0; i < players.size(); i++){
                if(players.get(i).getPoints() <= 7.5f){
                    if((7.5f - players.get(i).getPoints()) < difference){
                        difference = 7.5f - players.get(i).getPoints();
                        nearPoints = players.get(i).getPoints();
                        near = i;
                    }
                }
            }
            
           
            GsonUtil<Player> gsonPlayer = new GsonUtil<>();
            
            //Se comprueba con la banca a ver si este esta mas cerca o ha llegado
            if(points <= 7.5f){
                if(points >= nearPoints){
                    //Gana la banca
                    Player pn = new Player(myAgent.getLocalName());
                    
                    for (int i = 0; i < mensajes.size(); i++){
                        mensajes.get(i).setContent(gsonGame.encode(g, Game.class) + "--" + gsonPlayer.encode(pn, Player.class));
                        money += players.get(i).getP().getMoney();
                    }
                    
                }else{
                    //Gana un jugador

                    //Se le pasa partida,jugador ganador, dinero
                    for (int i = 0; i < mensajes.size(); i++){
                        mensajes.get(i).setContent(gsonGame.encode(g, Game.class) + "--" + gsonPlayer.encode(players.get(near), Player.class));
                        if(players.get(i).getAid().equals(players.get(near).getAid())){
                            money -= players.get(i).getP().getMoney();
                        }else{
                            money += players.get(i).getP().getMoney();
                        }
                    }
                    
                    playerWin = true;
                }
            }
            
            for(int i = 0; i < players.size(); i++){
                 messages.add(new ConsoleMessage(myAgent.getName(),"Jugador: " + players.get(i).getAid() + " Puntos: " + players.get(i).getPoints()));
                
                if(manager.size() > 0){
                    
                    AID a = new AID(players.get(i).getAid(),AID.ISLOCALNAME);
                    
                    SubscriptionResponder.Subscription s = manager.getSubscripcion(a.getName());
                    
                    
                    
                    if(s != null){
                        System.out.println(a.getName()+"\n");
                        s.notify(mensajes.get(i));
                    }
                }
            }
            
            messages.add(new ConsoleMessage(myAgent.getName(),"Banca" + " Puntos: " + points));
            
            //Se crean las estadisticas para sacar por pantalla o enviar a la sala
            
   
            String data = gsonGame.encode(actualGame, Game.class) + "--" + money + "--" + playerWin;
            
            
            if(!playerWin){
                messages.add(new ConsoleMessage(myAgent.getName(), myAgent.getLocalName() + " Gana la banca"));
            }else{
                messages.add(new ConsoleMessage(myAgent.getName(), myAgent.getLocalName() + " Gana el jugador " + players.get(near).getAid()));
            }
            
            //Envia datos a la sala
            
            addBehaviour(new TaskSendRoom(data));
            
            
            
            
        }
        
    }
    
    /***
     * Tarea envio de informacion a la sala
     */
    private class TaskSendRoom extends OneShotBehaviour{
        
        private String data; 

        public TaskSendRoom(String d) {
            this.data = d;
        }
        
        

        @Override
        public void action() {
            if(manager.size() > 0){
                SubscriptionResponder.Subscription s = manager.getSubscripcion(room.getName());
                    
                if(s != null){
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(data);
                    s.notify(msg);
                }
            }
        }
        
    }
    
    
    
    
    /*****************
     * PROTOCOLOS
     ****************/
    
    /***
     * Handle del protocolo propose para recibitr datos de una partida
     */
    private class HandleDevelopGameProposeParticipant extends ProposeResponder{
        
        

        public HandleDevelopGameProposeParticipant(Agent a, MessageTemplate mt) {
            super(a,mt);
               
        }
        
        
        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException{
            
            //Si el mensaje es nulo, se envia excepcion
            if(propose.getContent() == null){
                throw new NotUnderstoodException("Mensaje vacio");
            }
            
            
            
            //Se convierte el mensaje de string a objeto
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            GsonUtil<Player> gsonPlayer = new GsonUtil<>();
            
            String[] mess = propose.getContent().split("--");
            
            actualGame = gsonGame.decode(mess[FIRST], Game.class);
            money = Integer.parseInt(mess[SECOND]);
            room = propose.getSender();
            
            String salida = "Mesa " + myAgent.getLocalName() + " ha recibido la partida " + actualGame.getBetType() + " y jugadores: ";
            for(int i = 2; i < mess.length; i++){
                Player p = gsonPlayer.decode(mess[i], Player.class);
                
                players.add(p);
                salida = salida + p.getAid() + " ";
            }
            
            messages.add(new ConsoleMessage(myAgent.getName(), salida));
            
            ACLMessage response =  propose.createReply();
            
            //Se responde con un accept
            response.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            
            addBehaviour(new TaskGame());
            
            return response;
        }
        
         
    }
    
    /***
     * Handle para enviar un turno a los jugadores de la partida. Implementa el protocolo Contract Net
     */
    private class HandleCompleteRound extends ContractNetInitiator{
        
        private Agent a;
 
        
        //El mensaje tiene como destinatarios todos los destinos del mensaje
        public HandleCompleteRound(Agent a, ACLMessage msg) {
            super(a, msg);
            
            this.a = a;
            
        }
        
  
        @Override
        protected void handlePropose(ACLMessage propose,java.util.Vector acceptances){
            
            //Se crea respuesta a cada jugador
            ACLMessage reply = propose.createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            
            
            //Se obtiene la informacion del mensaje
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            String[] mess = propose.getContent().split("--");
         
            Game g = gsonGame.decode(mess[FIRST], Game.class);
            
            MOVEMENT m = MOVEMENT.valueOf(mess[SECOND]);
            
            reply.setContent(mess[FIRST]);
            
            //Segun el movimiento se hace una accion
            switch(m){
                
                //Si paso de turno
                case REFUSE:
                    //Se incrementa el turno
                    actualGame.setRound(actualGame.getRound()+1);
                    messages.add(new ConsoleMessage(myAgent.getName(), "Jugador " + propose.getSender().getLocalName() + " pasa de turno"));
                    //Se pasa el juego 
                    reply.setContent(mess[FIRST]);
                    
                    break;
                    
                case NO_TURN:
                    
                    //Se pasa el juego
                    reply.setContent(mess[FIRST]);
                    
                    break;
  
                case OUT:
                    
                    //Si se pasa de 7.5 se pasa de turno
                    actualGame.setRound(actualGame.getRound()+1);
                    reply.setContent(mess[FIRST]);
                    messages.add(new ConsoleMessage(myAgent.getName(), "Jugador " + propose.getSender().getLocalName() + " se ha pasado de puntos"));
                    
                    break;

                default:
                    reply.setContent(mess[FIRST]);
                    
                    break;
                    
            }
            
            
            
            acceptances.add(reply);
        }
        
        @Override
        protected void handleInform(ACLMessage inform){
            
            //Se registran los puntos de los jugadores tras este turno
            float points = Float.parseFloat(inform.getContent());
            
            boolean found = false;
            for(int i = 0; i < players.size() && !found; i++){
                if(players.get(i).getAid().equals(inform.getSender().getLocalName())){
                    
                    found = true;
                    
                    players.get(i).setPoints(points);

                    allResponses++;
                    
                }
            }
            
            
            //se habilita el siguinete mivimiento
            if(allResponses == players.size()){
                if(actualGame.getRound()-1 == players.size()){
                    finish = true;
                }
                nextMovement = true;
                allResponses = 0;
            }
            
        }

        

        
    }
    
    /***
     * Tarea para procesar mensajes 
     */
    public class TaskSendConsole extends TickerBehaviour {
        

        public TaskSendConsole(Agent a, long time) {
            super(a,time);
            
        }
        

        @Override
        protected void onTick() {
            
            //Si hay consolas 
            if(!consoleAgents.isEmpty()){
                
                
                
                //Para cada mensaje se envia a consola
                while(!messages.isEmpty()){
                    
                    ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(consoleAgents.get(Constants.FIRST));
                    mensaje.setContent(messages.remove(FIRST).getContent());
                    myAgent.send(mensaje);
                    
                }
            }
        }
    }
    

    /***
     * Handle que gestiona las subscripciones de jugadores y de la sala
     */
    private class HandleSubcriptionRoom extends SubscriptionResponder{
        
        private Subscription suscripcionJugador;
        
        public HandleSubcriptionRoom(Agent a, MessageTemplate mt) {
            super(a, mt);
        }
        
        public HandleSubcriptionRoom(Agent a, MessageTemplate mt,SubscriptionManager sm) {
            super(a, mt,sm);
        }
        
        @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
            
            String nombreAgente = cancel.getSender().getName();
            suscripcionJugador = manager.getSubscripcion(nombreAgente);
            mySubscriptionManager.deregister(suscripcionJugador);
            
            System.out.println("Suscripción cancelada del agente: " + 
                    cancel.getSender().getLocalName()+ 
                    "\nsuscripciones restantes: " + manager.size()); 
            
            return null;
 

        }
        
        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            
            String nombreAgente = subscription.getSender().getName();
            suscripcionJugador = createSubscription(subscription);
            
            
            // Registra la suscripción si no hay una previa
            if (!manager.haySubscripcion(nombreAgente)) {

                mySubscriptionManager.register(suscripcionJugador);
                System.out.println("Suscripción registrada al agente: " +
                        nombreAgente + "\nnúmero de suscripciones: " +
                        manager.size());

            }else{
                System.out.println("Suscripción ya registrada al agente: " +
                        nombreAgente);
            }   
            
            
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            return agree;
        }

    }
}
