package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class User implements Serializable {

    private String pseudo;
    private Collection<Position> positions;

    public User(String pseudo) {
        this.pseudo = pseudo;
        positions = new ArrayList<Position>();
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public Collection<Position> getPositions() {
        return positions;
    }

    public void setPositions(Collection<Position> positions) {
        this.positions = positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }


}
