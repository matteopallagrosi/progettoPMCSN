package it.metro.centers;

//rappresenta il centro dei controllori del biglietto
public class TicketInspectorsCenter extends Center {

    public TicketInspectorsCenter(int numServer) {
        super(4, numServer);
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
