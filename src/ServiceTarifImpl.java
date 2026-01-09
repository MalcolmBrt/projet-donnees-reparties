public class ServiceTarifImpl implements ServiceTarif {
    @Override
    public double getPrix(String nom) {
        return 15 + (Math.random() * 65);
    }
}