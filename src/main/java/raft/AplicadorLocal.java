package raft;

import estruturas.db.BancoDeDados;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

/**
 * Aplica os comandos direto num {@link BancoDeDados} local, sem consenso.
 *
 * <p>Preserva o comportamento original do servidor (antes do Raft): útil para os
 * testes de unidade/integração e para rodar um nó isolado.
 */
public class AplicadorLocal implements AplicadorDeChaves {

    private final BancoDeDados db;

    public AplicadorLocal(BancoDeDados db) {
        this.db = db;
    }

    @Override
    public int registrar(ComandoRegistro comando) {
        try {
            db.AdicionarContaBancaria(comando.reconstruirChave(), comando.reconstruirConta());
            return 200;
        } catch (ChaveJaRegistrada e) {
            return 403;
        } catch (Exception e) {
            return 500;
        }
    }
}
