package raft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Base64;

import estruturas.chave.Chave;
import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;
import estruturas.conta.ContaBancaria;
import estruturas.conta.NumeroConta;
import estruturas.instituicao.IdentificadorInstituicao;

/**
 * Comando de escrita que é gravado no log do Raft e replicado para todos os nós.
 *
 * <p>Guarda apenas dados primitivos (tipo + strings), nunca objetos {@code Chave}
 * ou {@code ContaBancaria} — assim a serialização é estável e a reconstrução em
 * cada nó é determinística.
 *
 * <p><b>Determinismo:</b> valores potencialmente aleatórios (ex.: o UUID de uma
 * {@link ChaveAleatoria}) já vêm resolvidos aqui, gerados <i>antes</i> do comando
 * entrar no log. A StateMachine nunca gera valores novos — apenas reaplica o que
 * está registrado —, garantindo que todos os nós cheguem ao mesmo estado.
 */
public final class ComandoRegistro implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TipoChave tipo;
    private final String valorChave;
    private final String idInstituicao;
    private final String numeroConta;

    public ComandoRegistro(TipoChave tipo, String valorChave, String idInstituicao, String numeroConta) {
        this.tipo = tipo;
        this.valorChave = valorChave;
        this.idInstituicao = idInstituicao;
        this.numeroConta = numeroConta;
    }

    /** Reconstrói a subclasse concreta de {@code Chave} (revalida o valor). */
    public Chave reconstruirChave() {
        return switch (tipo) {
            case CPF -> new ChaveCPF(valorChave);
            case TELEFONE -> new ChaveTelefone(valorChave);
            case EMAIL -> new ChaveEmail(valorChave);
            case ALEATORIA -> new ChaveAleatoria(valorChave);
        };
    }

    public ContaBancaria reconstruirConta() {
        return new ContaBancaria(new IdentificadorInstituicao(idInstituicao), new NumeroConta(numeroConta));
    }

    public TipoChave getTipo() {
        return tipo;
    }

    public String getValorChave() {
        return valorChave;
    }

    /** Serializa em uma String (Base64) para viajar como conteúdo de uma {@code Message} do Ratis. */
    public String serializar() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao serializar ComandoRegistro", e);
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    /** Reconstrói o comando a partir da String produzida por {@link #serializar()}. */
    public static ComandoRegistro desserializar(String dados) {
        byte[] bytes = Base64.getDecoder().decode(dados);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (ComandoRegistro) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Falha ao desserializar ComandoRegistro", e);
        }
    }

    @Override
    public String toString() {
        return "ComandoRegistro{tipo=" + tipo + ", chave=" + valorChave
                + ", instituicao=" + idInstituicao + ", conta=" + numeroConta + "}";
    }
}
