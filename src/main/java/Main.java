import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import estruturas.db.BancoDeDados;
import rmi.services.RegistroChaveService;
import rmi.services.ConsultaChaveService;

public class Main {

    public static void main(String[] args) {
        try {
            BancoDeDados db = new BancoDeDados();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.rebind("RegistroChave", new RegistroChaveService(db));
            registry.rebind("ConsultaChave", new ConsultaChaveService(db));

            System.out.println("Servidor RMI iniciado na porta 1099.");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor:");
            e.printStackTrace();
        }
    }
}