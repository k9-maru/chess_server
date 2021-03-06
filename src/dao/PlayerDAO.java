package dao;

import model.Player;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;

public class PlayerDAO extends DAO {
    public PlayerDAO() {
        super();
    }

    public Player checkLogin(Player p) {
        try {
            Query query = session.createQuery("FROM Player WHERE username = :un AND password = :pw AND status = 'away'");
            query.setParameter("un", p.getUsername());
            query.setParameter("pw", p.getPassword());
            ArrayList<Player> re = new ArrayList<>(query.list());
            if (re.size() == 1) {
                Transaction trans = session.getTransaction();
                if (!trans.isActive()) trans.begin();
                re.get(0).setStatus("online");
                session.update(re.get(0));
                trans.commit();
                return re.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public ArrayList<Player> onlinePlayer() {
        ArrayList<Player> re = new ArrayList<>();
        try {
            Query query = session.createQuery("FROM Player WHERE status = :onl OR status =:inm");
            query.setParameter("onl", "online");
            query.setParameter("inm", "in match");
            re = new ArrayList<Player>(query.list());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }

    public boolean logout(Player p) {
        boolean success = false;
        try {
            Query query = session.createQuery("FROM Player WHERE username = :un");
            query.setParameter("un", p.getUsername());
            ArrayList<Player> re = new ArrayList<>(query.list());
            if (re.size() == 1) {
                Transaction trans = session.getTransaction();
                if (!trans.isActive()) trans.begin();
                re.get(0).setStatus("away");
                session.update(re.get(0));
                trans.commit();
                success = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }


    public Player signUp(Player p) {
        try {
            Query query = session.createQuery("FROM Player WHERE username = :u");
            query.setParameter("u", p.getUsername());
            ArrayList<Player> re = new ArrayList<>(query.list());
            if (re.size() == 0) {
                Player player = new Player(p.getUsername(), p.getPassword(), "online", 0, 0);
                Transaction trans = session.getTransaction();
                if (!trans.isActive()) trans.begin();
                session.save(player);
                trans.commit();

                return player;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Player> globalRank() {
        ArrayList<Player> re = null;
        try {
            Query query = session.createQuery("FROM Player");
            re = new ArrayList<Player>(query.list());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }
}
