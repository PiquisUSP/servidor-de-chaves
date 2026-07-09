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
import estruturas.conta.ContaBancaria;

/**
 * O que viaja no log do Raft. Cada operação de escrita é um tipo próprio:
 * {@link ComandoRegistro} cria uma chave nova; {@link ComandoAtualizacao} é a
 * portabilidade — a chave passa a apontar para outra conta.
 */
public sealed interface Comando extends Serializable permits ComandoRegistro, ComandoAtualizacao {

    /** Reconstrói a subclasse concreta de {@code Chave} (revalida o valor). */
    Chave reconstruirChave();

    ContaBancaria reconstruirConta();

    /** Serializa em uma String (Base64) para viajar como conteúdo de uma {@code Message} do Ratis. */
    default String serializar() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao serializar " + getClass().getSimpleName(), e);
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    /** Reconstrói o comando a partir da String produzida por {@link #serializar()}. */
    static Comando desserializar(String dados) {
        byte[] bytes = Base64.getDecoder().decode(dados);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Comando) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Falha ao desserializar comando", e);
        }
    }
}
