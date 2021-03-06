package control;

import dao.BeFriendDAO;
import dao.MatchDAO;
import dao.PlayerDAO;
import model.*;
import view.ServerMainFrm;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ServerCtr {
    private ServerMainFrm view;
    private ServerSocket myServer;
    private ServerListening myListening;
    private ArrayList<ServerProcessing> myProcess;
    private IPAddress myAddress = new IPAddress("localhost", 8888);  //default server host and port

    public ServerCtr(ServerMainFrm view) {
        myProcess = new ArrayList<ServerProcessing>();
        this.view = view;
        openServer();
    }

    public ServerCtr(ServerMainFrm view, int serverPort) {
        myProcess = new ArrayList<ServerProcessing>();
        this.view = view;
        myAddress.setPort(serverPort);
        openServer();
    }


    private void openServer() {
        try {
            myServer = new ServerSocket(myAddress.getPort());
            myListening = new ServerListening();
            myListening.start();
            myAddress.setHost(InetAddress.getLocalHost().getHostAddress());
            view.showServerInfor(myAddress);
            view.showMessage("TCP server is running at the port " + myAddress.getPort() + "...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            for (ServerProcessing sp : myProcess)
                sp.stop();
            myListening.stop();
            myServer.close();
            view.showMessage("TCP server is stopped!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publicClientNumber() {
        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.SERVER_INFORM_CLIENT_NUMBER, myProcess.size());
        for (ServerProcessing sp : myProcess) {
            sp.sendData(data);
        }
    }

    public void OnlinePlayer() {
        PlayerDAO pd = new PlayerDAO();
        ArrayList<Player> onlineplayer = new ArrayList<>(pd.onlinePlayer());
        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.REPLY_ONLINE_PLAYER, onlineplayer);
        for (ServerProcessing sp : myProcess) {
            sp.sendData(data);
        }
    }

    public void FriendReq(Player player) {
        BeFriendDAO bfd = new BeFriendDAO();
        ArrayList<Player> req = new ArrayList<>(bfd.friendList(player));
        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.REPLY_FRIEND_REQUEST, req);
        for (ServerProcessing sp : myProcess) {
            sp.sendData(data);
        }
    }

    public void friendUpdate(Player player){
        BeFriendDAO bfd = new BeFriendDAO();
        ArrayList<Player> fl = new ArrayList<>(bfd.friendList(player));
        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.REPLY_FRIEND_REQUEST, fl);
        for (ServerProcessing sp : myProcess) {
            sp.sendData(data);
        }
    }

    public void move(Match match) {
        System.out.printf("sent");
        ObjectWrapper data = new ObjectWrapper(ObjectWrapper.REP_O_MOVE, match);
        for (ServerProcessing sp : myProcess) {
            sp.sendData(data);
        }
    }


    /**
     * The class to listen the connections from client, avoiding the blocking of accept connection
     */
    class ServerListening extends Thread {

        public ServerListening() {
            super();
        }

        public void run() {
            view.showMessage("server is listening... ");
            try {
                while (true) {
                    Socket clientSocket = myServer.accept();
                    ServerProcessing sp = new ServerProcessing(clientSocket);
                    sp.start();
                    myProcess.add(sp);
                    view.showMessage("Number of client connecting to the server: " + myProcess.size());
                    publicClientNumber();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The class to treat the requirement from client
     */
    class ServerProcessing extends Thread {
        private Socket mySocket;

        public ServerProcessing(Socket s) {
            super();
            mySocket = s;
        }

        public void sendData(Object obj) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(mySocket.getOutputStream());
                oos.writeObject(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                while (true) {
                    ObjectInputStream ois = new ObjectInputStream(mySocket.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(mySocket.getOutputStream());
                    Object o = ois.readObject();
                    if (o instanceof ObjectWrapper) {
                        ObjectWrapper data = (ObjectWrapper) o;
                        PlayerDAO pd = new PlayerDAO();
                        BeFriendDAO bfd = new BeFriendDAO();
                        MatchDAO md = new MatchDAO();
                        ArrayList<Player> players;
                        Player player;
                        BeFriend bf;
                        boolean condition;

                        switch (data.getPerformative()) {
                            case ObjectWrapper.LOGIN_USER:
                                player = (Player) pd.checkLogin((Player) data.getData());
                                if (player != null) {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, player));
                                    OnlinePlayer();
                                } else {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "no"));
                                }
                                break;
                            case ObjectWrapper.SIGNUP_USER:
                                player = (Player) pd.signUp((Player) data.getData());
                                if (player != null) {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_SIGNUP_USER, player));
                                    OnlinePlayer();
                                } else {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_SIGNUP_USER, "no"));
                                }
                                break;
                            case ObjectWrapper.ONLINE_PLAYER:
                                players = new ArrayList<>(pd.onlinePlayer());
                                oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_ONLINE_PLAYER, players));
                                break;
                            case ObjectWrapper.LOGOUT_USER:
                                pd.logout((Player) data.getData());
                                oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_LOGOUT_USER, "ok"));
                                OnlinePlayer();
                                break;
                            case ObjectWrapper.ADD_FRIEND:
                                bf = (BeFriend) data.getData();
                                condition = bfd.addFriend(bf);
                                if (condition == true) {
                                    System.out.println("success");
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_ADD_FRIEND, "ok"));
                                    FriendReq(bf.getPlayer1());
                                } else {
                                    System.out.println("fail");
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_ADD_FRIEND, "no"));
                                }
                                break;
                            case ObjectWrapper.FRIEND_LIST:
                                player = (Player) data.getData();
                                ArrayList<Player> re = new ArrayList<>(bfd.friendList(player));
                                oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_FRIEND_LIST, re));
                                break;
                            case ObjectWrapper.FRIEND_REQUEST:
                                players = new ArrayList<>(bfd.requestList((Player) data.getData()));
                                oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_FRIEND_REQUEST, players));
                                break;
                            case ObjectWrapper.ACCEPT_FRIEND:
                                bf = (BeFriend) data.getData();
                                System.out.println(bf.getStatus());
                                condition = bfd.acceptFriend(bf);
                                if (condition == true) {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_ACCEPT_FRIEND, "ok"));
                                } else {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_ACCEPT_FRIEND, "no"));
                                }
                                friendUpdate(bf.getPlayer());
                                friendUpdate(bf.getPlayer1());
                                break;
                            case ObjectWrapper.RANK:
                                players = pd.globalRank();
                                oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_RANK, players));
                                break;
                            case ObjectWrapper.DELETE_FRIEND:
                                bf = (BeFriend) data.getData();
                                condition = bfd.deleteFriend(bf);
                                if (condition) {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_DELETE_FRIEND, "ok"));
                                } else oos.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_DELETE_FRIEND, "no"));
                                friendUpdate(bf.getPlayer());
                                friendUpdate(bf.getPlayer1());
                                break;
                            case ObjectWrapper.MOVE:
                                condition = md.u_move((Match) data.getData());
                                if (condition == true) {
                                    oos.writeObject(new ObjectWrapper(ObjectWrapper.REP_MOVE, "ok"));
                                    System.out.println(((Match) data.getData()).getMovement());
                                    move((Match) data.getData());
                                } else oos.writeObject(new ObjectWrapper(ObjectWrapper.REP_MOVE, "no"));
                                break;
                        }
                    }
                }
            } catch (EOFException | SocketException e) {
                //e.printStackTrace();
                myProcess.remove(this);
                view.showMessage("Number of client connecting to the server: " + myProcess.size());
                publicClientNumber();
                try {
                    mySocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                this.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
