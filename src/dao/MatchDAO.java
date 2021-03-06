package dao;

import model.Match;
import model.Player;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.ArrayList;

public class MatchDAO extends DAO {
    public MatchDAO() {
        super();
    }

    public boolean u_move(Match match) {
        boolean success = false;
        try {
            Player player = (Player) match.getPlayers().get(0);
            String movement = (String) match.getMovement();
            Query query = session.createQuery("FROM Match WHERE id " +
                    "IN(SELECT match FROM Player WHERE id = :i)");
            query.setParameter("i", player.getId());
            ArrayList<Match> re = (ArrayList<Match>) query.list();

            Transaction trans = session.getTransaction();
            if (!trans.isActive()) trans.begin();
            re.get(0).setMovement(movement);
            session.update(re.get(0));
            trans.commit();

            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }
}
