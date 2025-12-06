import java.util.Queue;

public class TestAgent extends AgentImpl {
    public TestAgent(Queue<Node> itinerary) {
        super(itinerary);
    }

    @Override
    public void main() throws MoveException {
        System.out.println("AGENT: Hello world !!!");

        if (this.getItinerary().size() > 0) {
            Node target = this.getItinerary().poll();
            System.out.println("AGENT: Je continue vers " + target.getPort());
            move(target);
        } else {
            System.out.println("AGENT: Je suis rentré à la maison. Travail terminé !");
        }
    }
}