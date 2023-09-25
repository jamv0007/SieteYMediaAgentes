/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uja.ssmmaa.curso2122.tercerapractica.agents;
import com.uja.ssmmaa.curso2122.tercerapractica.gui.RoomJFrame;
import com.uja.ssmmaa.curso2122.tercerapractica.util.ConsoleMessage;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Constants;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.FIRST;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.SECOND;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Game;
import com.uja.ssmmaa.curso2122.tercerapractica.util.Player;
import static com.uja.ssmmaa.curso2122.tercerapractica.util.Constants.SERVICES;
import com.uja.ssmmaa.curso2122.tercerapractica.util.GsonUtil;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFSubscriber;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ProposeInitiator;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.StaleProxyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javafx.util.Pair;

/**
 *
 * @author joseantonio
 */
public class RoomAgent extends Agent implements Constants{
    
    private ArrayList<AID>[] agents;//Lista de agentes
    private ArrayList<ConsoleMessage> messages;//Mensajes pendientes de envio a consola
    private ArrayList<AID> tables;//Lista con los AID de las mesas creadas
    private Map<String,Integer> tableMoney;//Mapa de id partida, con el dinero para la partida
    private HashMap<String,ArrayList<Player>> games; //Mapa con el id de la partida ty los jugadores
    private HashMap<AID,HandleSusbscribeForTable> subscriptions;//Lista de suscripciones
    private HashMap<String, Pair<Integer,Boolean>> finishGames;//Partidas terminadas
    private int actualSimpleBetTables = 0;//Numero actual de mesas para apuesta simple
    private int actualDoubleBetTables = 0;//Numero actual de mesas para apuesta doble
    private int maxSimpleBetTables = 3;//Numero maximo de mesas de apuesta simple
    private int maxDoubleBetTables = 1;//Numero maximo de mesas de apuesta doble
    private int numPlayersTable = 4;//Numero de jugadores por mesa
    private int roomMoney = 30000;//Dinero de la sala
    private int id = 0;//Contador de id de partida
    private RoomJFrame gui;
    
    
    
    @Override
    protected void setup(){
        
        //Inicializacion de atributos
        games = new java.util.HashMap<>();
        tableMoney = new HashMap<>();
        finishGames = new HashMap<>();
        messages = new ArrayList<>();
        tables = new java.util.ArrayList<>();
        agents = new ArrayList[SERVICES.length];
        subscriptions = new HashMap<>();
        gui = new RoomJFrame(this);
        
        gui.setVisible(true);
        
        for(int i = 0; i < SERVICES.length; i++){
            agents[SERVICES[i].ordinal()] = new ArrayList<>();
        }
        
        
        System.out.println("Se inicia la ejecución del agente: " + this.getName());
        

       
       
       
    }
    
    @Override
    protected void takeDown(){
        
        
         System.out.println("Finaliza la ejecución del agente: " + this.getName());
         gui.dispose();
        
    }
    
    /************************
     * TAREAS
     ************************/
    
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
                for(int i = 0; i < Constants.SERVICES.length; i++){
                    if(af.getName().equals(Constants.SERVICES[i].name())){
                        agents[Constants.SERVICES[i].ordinal()].add(dfad.getName());
                    }
                }
                
            }
            
            System.out.println("El agente: " + myAgent.getName() +
                    "ha encontrado a:\n\t" + dfad.getName().getName());
        }

        @Override
        public void onDeregister(DFAgentDescription dfad) {
            AID agente = dfad.getName();
            
            for(int i = 0; i < Constants.SERVICES.length; i++){
                if(agents[i].remove(agente)){
                    System.out.println("El agente: " + agente.getName() + 
                            " ha sido eliminado de la lista de " 
                            + myAgent.getName());

                }
            }
        }
        
    }
    
    /***
     * Funcion que recibe la informacion de la interfaz gráfica
     * @param nMesasD Mesas dobles
     * @param nMesasS Medads simples
     * @param nJugMesa Numero de jugadores por mesa
     * @param TotalMoney Dinero
     */
    public void getInterfaceData(int nMesasD, int nMesasS, int nJugMesa, int TotalMoney){
        
        this.maxDoubleBetTables = nMesasD;
        this.maxSimpleBetTables = nMesasS;
        this.numPlayersTable = nJugMesa;
        this.roomMoney = TotalMoney;
        
        
       //Plantilla para buscar agentes  
       DFAgentDescription template = new DFAgentDescription();
       ServiceDescription templateSd = new ServiceDescription();
       templateSd.setType(SERVICE_TYPE);
       template.addServices(templateSd);
        
       //Intanciacion de tareas
       addBehaviour(new TaskSuscriptionDF(this,template));
       addBehaviour(new TaskSendConsole(this, TIME_PROCESS_MESSAGES));
       addBehaviour(new TaskSearchPlayers(this,TIME_SEARCH_PLAYERS));
        
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
            if(!agents[Constants.Service_Name.CONSOLE.ordinal()].isEmpty()){
                
                //Para cada mensaje se envia a consola
                
                while(!messages.isEmpty()){
                    
                    ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                    mensaje.setSender(myAgent.getAID());
                    mensaje.addReceiver(agents[Constants.Service_Name.CONSOLE.ordinal()].get(Constants.FIRST));
                    mensaje.setContent(messages.remove(FIRST).getContent());
                    myAgent.send(mensaje);
                    
                }
                
                
            }
        }
    }
    
    
    /***
     * Tarea que se ejecuta cada cierto tiempo que envia solicitudes de juego a jugadores
     */
    private class TaskSearchPlayers extends TickerBehaviour{
        
        private Agent agent;

        public TaskSearchPlayers(Agent a,long time) {
            super(a,time);
            
            this.agent = a;           
            
        }

        @Override
        protected void onTick() {
            
            GsonUtil<Game> gson = new GsonUtil();
            Game g = null;
            //Se crea el mensaje para envio del protocolo propose
            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
            msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_WAIT_SEARCH_PLAYERS));
            
            if(Constants.getRandom() <= 4){
                            
                if(maxSimpleBetTables - actualSimpleBetTables > 0 ){
                    g = new Game("Partida " + (id + 1), 1, BET_TYPE.SIMPLE);
                    id++;
                }

            }else{

                if(maxDoubleBetTables - actualDoubleBetTables > 0 ){
                   g = new Game("Partida " + (id + 1), 1, BET_TYPE.DOUBLE);
                   id++;
                }

            }
            
            if(g != null){
                msg.setContent(gson.encode(g, Game.class));

                messages.add(new ConsoleMessage(myAgent.getName(), "Se ha enviado partida: " + g.getId() + " de tipo " + g.getBetType()));
                
                //Se asigna como destino todos los agentes jugador

                for( AID a: agents[Service_Name.PLAYER.ordinal()]){

                    msg.addReceiver(a);   

                }   
                    
                
                
                //Se crea una tarea con el protocolo Propose para enviar y recibir respuestas
                addBehaviour(new HandleSearchPlayersProposeInitiator(agent, msg,g));
            }
            
        }
            
    }
    
    /***
     * Tarea que envia la informacion a la mesa correspondiente
     */
    private class TaskDevelopGame extends OneShotBehaviour{
        
        
        private String idGame;
        private AID table;
        
        public TaskDevelopGame(String idGame, AID table){

            this.idGame = idGame;
            this.table = table;
        }

        @Override
        public void action() {
            
           
            GsonUtil<Game> gsonGame = new GsonUtil();
            GsonUtil<Player> gsonPlayer = new GsonUtil();
            
            //Se crea el mensaje
            ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
            msg.setReplyByDate(new Date(System.currentTimeMillis() + TIME_WAIT_TABLE_RESPONSE));
            
           
            //Se obtienen los datos de la partida
            ArrayList<Player> gameData = games.get(idGame);
            int money = tableMoney.get(idGame);
            msg.setContent(gsonGame.encode(gameData.get(FIRST).getP().getGame(), Game.class)+ "--" + money);
            //Se comprimen los datos en un string 
            for(int j = 0; j < gameData.size(); j++){
               msg.setContent(msg.getContent()+ "--" + gsonPlayer.encode(gameData.get(j), Player.class));
            }
            
            messages.add(new ConsoleMessage(myAgent.getName(), "Se ha enviado a la mesa " + table.getLocalName() + gameData.get(FIRST).getP().getGame().getId()));
            
            msg.addReceiver(table);

            
            //Se añade el handle para el envio con el protocolo propose
            addBehaviour(new HandleDevelopGameProposeInitiator(myAgent, msg));
            
            
        }
        
    }
    

    
    
    /*************************
     * PROTOCOLOS
     **************************/
    
    /***
     * Handle del protocolo Propose para busqueda de jugadores de la sala. Implementa el Iniciador del protocolo Propose.
     */
    private class HandleSearchPlayersProposeInitiator extends ProposeInitiator{
        
        private Agent agent;
        private Game game;
        
        public HandleSearchPlayersProposeInitiator(Agent a, ACLMessage msg, Game game) {
            super(a, msg);
            this.agent = a;
            this.game = game;
        }
        
        //Se llama cuando recive todas las respuestas o se acaba el timeout
        @Override
        protected void handleAllResponses(Vector responses){
            
            //Se inicializan atributos para iterar sobre el mensaje 
            GsonUtil<Game> gameUtil = new GsonUtil<>();
            GsonUtil<Player> playerUtil = new GsonUtil<>();
            Iterator it = responses.iterator();
            ArrayList<Player> accept = new ArrayList<>();
            ArrayList<Integer> money = new ArrayList<>();
            boolean get = false;
            
            //Mientras que haya mensajes
            while(it.hasNext() && !get){
                
                 
                ACLMessage m = (ACLMessage) it.next();
               
                //Segun la respuesta 
                switch(m.getPerformative()){
                    case ACLMessage.ACCEPT_PROPOSAL:{
                        
                        //Si se accepta se convierte el string en objetos
                        String[] mess = m.getContent().split("--");
                        Game g1 = gameUtil.decode(mess[FIRST], Game.class);
                        Player g2 = playerUtil.decode(mess[SECOND], Player.class);
                        int betMoney = Integer.parseInt(mess[THIRD]);
                        
                        //Se manda mensaje a consola y se guarda 
                        ConsoleMessage msg = new ConsoleMessage(agent.getName(),g2.getAid() + " ha ACEPTADO: " + g1.getBetType() + " ID: " + g1.getId());
                        accept.add(g2);
                        money.add(betMoney);
                        
                        
                        if(accept.size() == numPlayersTable){
                            get = true;
                        }
                        
                        messages.add(msg);
                        
                        messages.add(new ConsoleMessage(agent.getName(), "----------------------"));
                        
                        break;
                    }
                    case ACLMessage.REJECT_PROPOSAL: {
                        //msg = new ConsoleMessage(agent.getName(), "Se ha RECHAZADO");
                        break;
                    }
                    
                    case ACLMessage.NOT_UNDERSTOOD: {
                        //msg = new ConsoleMessage(agent.getName(), "NO SE ENTIENDE");
                        break;
                    }
                    
                    default:{
                        //msg = new ConsoleMessage(agent.getName(), "OTRO");
                        break;
                    }
                }
                
                
                 
            }
            
            
            
            //Si hay agentes que han aceptado
            
            if(accept.size() > 0){
                
                if(accept.size() == numPlayersTable){
                    
                    //Se calcula el maximo dinero a pagar
                    
                    int max = 0;
                    for(int i:money){
                        if(i > max){
                            max = i;
                        }
                    }
                    
                    if(roomMoney > 2*max){

                        //Segun el tipo de apuesta se incrementa el numero de mesas de ese tipo
                        if(game.getBetType() == BET_TYPE.DOUBLE){
                            actualDoubleBetTables++;
                        }else{
                            actualSimpleBetTables++;
                        }



                        //Si hay dinero suficiente se guarda la partida


                        tableMoney.put(game.getId(), 2*max);
                        roomMoney = roomMoney - (2*max);
                        //Se guardan los datos de la partida y los jugadores
                        games.put(game.getId(), accept);
                        
                        

                        //Se crea una nueva mesa
                        addTable(games.size(),game.getId());
                        ConsoleMessage cm = new ConsoleMessage(agent.getName(), "Mesa " + games.size() + " Partida: " + game.getId());
                        messages.add(cm);
                    
                    }
                
                }
            }
            
        }
        
        //Funcion que añade una nueva mesa
        private void addTable(int number, String id){
            
            try {
                
                //Se crea la mesa
                String agentName = "Mesa " + number;
                agent.getContainerController().createNewAgent(agentName,"com.uja.ssmmaa.curso2122.tercerapractica.agents.TableAgent", null).start();
                AID newTable = new AID(agentName,AID.ISLOCALNAME);
                tables.add(newTable);
                addBehaviour(new TaskDevelopGame(id,newTable));
                
                
                //Se manda el mensaje de subscripcion para el protocolo Subscribe
                ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
                msg.setSender(myAgent.getAID());
                msg.addReceiver(newTable);

                
                HandleSusbscribeForTable s = new HandleSusbscribeForTable(agent, msg);
                subscriptions.put(newTable, s);
                addBehaviour(s);
                
                
            } catch (StaleProxyException ex) {
            
                ex.printStackTrace();
            
            }
            
        }
        
        
    }
    
    /***
     * Protocolo iniciador para enviar a la mesa los datos de la partida
     */
    private class HandleDevelopGameProposeInitiator extends ProposeInitiator{
        
        public HandleDevelopGameProposeInitiator(Agent a, ACLMessage msg) {
            super(a, msg);
        }
        
        @Override
        protected void handleAllResponses(Vector responses){
            
            Iterator it = responses.iterator();
            
            while(it.hasNext()){
                
                ACLMessage m = (ACLMessage) it.next();
                
                switch(m.getPerformative()){
                    case ACLMessage.ACCEPT_PROPOSAL:
                        
                        break;
                        
                    default:
                        break;
                }
                
            }
            
        }
        
    }
    
    /***
     * Handle para que las mesas se subscriban y manden resultado de la partida  
     */
    private class HandleSusbscribeForTable extends SubscriptionInitiator{
        
        public HandleSusbscribeForTable(Agent a, ACLMessage msg) {
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
            
            
            
            GsonUtil<Game> gsonGame = new GsonUtil<>();
            
            String[] data = inform.getContent().split("--");
            
            Game g = gsonGame.decode(data[FIRST], Game.class);
            
            int winMoney = Integer.parseInt(data[SECOND]);
            
            boolean playerWin = Boolean.valueOf(data[THIRD]);
            
            roomMoney += winMoney;
            
            finishGames.put(g.getId(), new Pair<>(winMoney,playerWin));
            
            messages.add(new ConsoleMessage(myAgent.getName(), "Han llegado los datos de la partida " + g.getId()));
            
             
            
            
        }


        
    }
    

 
}
