package it.metro.centers;

//rappresenta il centro dei tornelli
public class TurnstilesCenter extends Center {

    public TurnstilesCenter(int numServer) {
        super(3, numServer);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public void processArrival() {

    }

    @Override
    public void processDeparture() {

    }
}
