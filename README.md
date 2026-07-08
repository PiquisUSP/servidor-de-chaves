# Servidor de Chaves (PIQUIS) com Raft

Servidor de chaves do **PIQUIS**: mapeia uma **chave** (CPF, telefone, e-mail ou
aleatória) para uma **conta bancária**. As instituições conversam com o servidor por
**RMI**; por baixo, os servidores formam um **cluster replicado com consenso Raft**
(via [Apache Ratis](https://ratis.apache.org/)), de modo que um registro feito em
qualquer nó é replicado, sobrevive à queda de nós e persiste em disco.

---

## Arquitetura

Duas camadas de rede independentes:

- **RMI** (externo) — interface usada pelas instituições para registrar e consultar chaves.
- **Raft / gRPC** (interno) — consenso e replicação **entre** os servidores de chaves.

```
                     Instituições (clientes RMI)
                              │
              ┌───────────────┼───────────────┐
              │ RMI           │ RMI           │ RMI
              ▼               ▼               ▼
         ┌──────────┐    ┌──────────┐    ┌──────────┐
         │    n1    │    │    n2    │    │    n3    │
         │ RMI 1099 │    │ RMI 1100 │    │ RMI 1101 │
         │ Raft 6001│◀──▶│ Raft 6002│◀──▶│ Raft 6003│
         └──────────┘    └──────────┘    └──────────┘
              ▲                                │
              └──────── consenso Raft (gRPC) ──┘
              (replicação do log + eleição de líder)
```

### Como uma escrita flui

1. A instituição chama `registrarChave...` via RMI em **qualquer** nó.
2. O `RegistroChaveService` valida a entrada e monta um `ComandoRegistro` determinístico.
3. O `AplicadorRaft` submete o comando pelo `RaftClient` — que **acha o líder sozinho**
   (mesmo que o nó seja um seguidor).
4. O líder grava no log e **replica para a maioria** (2 de 3).
5. Após o commit, a `StateMachine` de **cada** nó aplica o comando no seu `BancoDeDados`.
6. O `send()` só retorna depois disso — quando a instituição recebe `200`, o dado **já está replicado**.

### Como uma leitura flui

As consultas (`consultarChave` / `existeChave`) são servidas do `BancoDeDados`
**replicado local** de cada nó (rápidas). Como todo nó aplica as entradas commitadas,
o dado registrado em um nó fica visível nas consultas de qualquer outro.

> Escritas passam pelo consenso; leituras são locais (eventualmente consistentes).
> Para leituras linearizáveis, dá para roteá-las ao líder com `RaftClient.io().sendReadOnly(...)`.

---

## Persistência em disco

O estado sobrevive à queda/reinício dos nós:

- **Log do Raft** — cada entrada é gravada em `raft-storage/<id>/` antes do commit (o Ratis faz isso).
- **Snapshots da StateMachine** — a cada 10 entradas aplicadas, `takeSnapshot()` grava o
  estado do `BancoDeDados` em disco (serializado) e o log pode ser compactado.
- **Recuperação no restart** — ao subir, o nó **recupera** (`RECOVER`) o estado do snapshot
  mais recente e reproduz o resto do log; na primeira vez, **formata** (`FORMAT`).

Resultado: derrubar o cluster inteiro e reiniciar recupera todos os registros do disco,
sem reescrever nada. (Ver `RaftPersistenciaTest`.)

---

## Estrutura do projeto

```
src/main/java/
├── Main.java                      # sobe um nó: Raft + registry RMI
├── raft/                          # integração com Apache Ratis
│   ├── NoServidorChaves.java      #   bootstrap do nó (RaftServer + RaftClient + banco)
│   ├── ClusterConfig.java         #   peers, portas, id do grupo e storage
│   ├── ServidorChavesStateMachine #   máquina de estados replicada (aplica no banco + snapshots)
│   ├── ComandoRegistro.java       #   comando serializável que viaja no log Raft
│   ├── TipoChave.java             #   enum do tipo da chave (CPF/TELEFONE/EMAIL/ALEATORIA)
│   ├── AplicadorDeChaves.java     #   abstração da escrita (local vs. Raft)
│   ├── AplicadorLocal.java        #   escrita direta (sem replicação; testes/nó único)
│   └── AplicadorRaft.java         #   escrita via consenso (RaftClient)
├── rmi/                           # interface e serviços RMI
│   ├── RegistroChaveInterface.java / services/RegistroChaveService.java
│   ├── ConsultaChaveInterface.java / services/ConsultaChaveService.java
│   └── services/result/           #   ServiceResult, ContaBancariaResult
└── estruturas/                    # domínio
    ├── chave/                     #   Chave + ChaveCPF/Telefone/Email/Aleatoria (+ validação)
    ├── conta/                     #   ContaBancaria, NumeroConta
    ├── instituicao/               #   IdentificadorInstituicao
    └── db/                        #   BancoDeDados (mapa valor -> conta) + exceptions
```

---

## Pré-requisitos

- **Java 21** (JDK)
- Maven Wrapper incluso (`mvnw` / `mvnw.cmd`) — não precisa instalar o Maven

---

## Compilar e testar

```powershell
# Windows (PowerShell)
.\mvnw.cmd test
```

```bash
# Linux / macOS / Git Bash
./mvnw test
```

A suíte inclui testes de domínio, de RMI e de **cluster Raft** (sobem nós em processo).
Os testes de cluster levam alguns segundos (eleição de líder e, no de falhas, uma
espera intencional pelo cenário sem maioria).

---

## Rodar o cluster

Cada nó é um processo. Abra **um terminal por nó** e passe o id (`n1`, `n2`, `n3`):

```powershell
# Terminal 1
.\mvnw.cmd exec:java "-Dexec.args=n1"
# Terminal 2
.\mvnw.cmd exec:java "-Dexec.args=n2"
# Terminal 3
.\mvnw.cmd exec:java "-Dexec.args=n3"
```

```bash
# Git Bash / Linux / macOS
./mvnw exec:java -Dexec.args=n1
```

> **Suba pelo menos 2 nós** (maioria de 3). Com só 1 nó no ar não há líder e as escritas
> ficam bloqueadas — é o Raft priorizando consistência.

### Portas por nó

| Nó  | RMI  | Raft (gRPC) |
|-----|------|-------------|
| n1  | 1099 | 6001        |
| n2  | 1100 | 6002        |
| n3  | 1101 | 6003        |

Os dados do Raft (log + snapshots) ficam em `raft-storage/<id>/` (ignorado pelo Git).
Apagar essa pasta zera o estado do cluster.

---

## Rodar com Docker

O cluster inteiro sobe com Docker Compose (cada nó vira um container que acha os
outros pela DNS interna da rede):

```bash
docker compose up --build      # sobe os 3 nós
docker compose down            # derruba (dados preservados nos volumes)
docker compose down -v         # derruba e apaga os dados
```

Ou um nó avulso:

```bash
docker build -t servidor-de-chaves .
docker run --rm servidor-de-chaves n1
```

- Cada nó é um container e se encontra pelo nome do serviço (`n1`/`n2`/`n3`) — por
  isso o compose define `RAFT_HOST_N1=n1`, `RAFT_HOST_N2=n2`, `RAFT_HOST_N3=n3`.
  Fora do Docker o padrão continua `127.0.0.1`.
- Log + snapshots ficam em volumes (`n1-data`/`n2-data`/`n3-data`), então os dados
  sobrevivem ao `down`/`up`.
- **RMI a partir do host:** as portas RMI são publicadas e o compose seta
  `-Djava.rmi.server.hostname=localhost` para o acesso pelo host funcionar. Um
  cliente RMI rodando **dentro** da rede Docker deve usar o nome do serviço no lookup.

---

## Rodar no Kubernetes

Manifests em [`k8s/`](k8s/): **StatefulSet** (3 nós, disco por nó via
`volumeClaimTemplates`), **headless Service** (DNS estável por pod, para os nós se
acharem), **LoadBalancer** (RMI para clientes) e **PodDisruptionBudget** (mantém o
quórum durante drains).

```bash
# 1) Construir a imagem e deixá-la acessível ao cluster
docker build -t servidor-de-chaves:latest .
#    minikube: minikube image load servidor-de-chaves:latest
#    kind:     kind load docker-image servidor-de-chaves:latest

# 2) Aplicar tudo
kubectl apply -f k8s/

# 3) Acompanhar a subida e a eleição de líder
kubectl get pods -l app=servidor-chaves -w
kubectl logs servidor-chaves-0 -f
```

**Mapeamento pod → nó:** o StatefulSet cria `servidor-chaves-0/-1/-2`; cada pod deriva
seu id (`n1/n2/n3`) do ordinal e anuncia o próprio DNS. Os `RAFT_HOST_*` apontam para o
DNS de cada pod (headless Service); `RMI_PORT`/`RAFT_PORT` uniformizam as portas (para o
Service balancear).

**Resiliência:** derrube um pod e veja o cluster reeleger e o pod voltar recuperando o
estado do PVC:

```bash
kubectl delete pod servidor-chaves-0
```

**Acesso ao RMI:**
- **Dentro do cluster** (recomendado): use o DNS de um pod, ex.
  `servidor-chaves-0.servidor-chaves-hl:1099` — o `java.rmi.server.hostname` já é esse DNS.
- **Externo (LoadBalancer):** `kubectl get svc servidor-chaves-rmi` dá o IP. Como o RMI
  embute o hostname no stub, para o acesso externo o `java.rmi.server.hostname` precisa ser
  o endereço do LB — sobrescreva via `JAVA_OPTS` no StatefulSet (limitação clássica de
  RMI + NAT). É por isso que as escritas passarem pelo Raft ajuda: qualquer pod atende e
  encaminha ao líder, então distribuir clientes entre pods é seguro.

> Requer `replicas: 3` (a config conhece n1/n2/n3). Em minikube, `type: LoadBalancer`
> precisa de `minikube tunnel`.

---

## Interface RMI

Objetos publicados no registry: **`RegistroChave`** e **`ConsultaChave`**.

**Registro** (`RegistroChaveInterface`) — retorna `200` (ok), `403` (chave já existe) ou `500` (inválida):

```java
int registrarChaveCPF(String idInstituicao, String numeroConta, String cpf);
int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone);
int registrarChaveEmail(String idInstituicao, String numeroConta, String email);
int registrarChaveAleatoria(String idInstituicao, String numeroConta);
```

**Consulta** (`ConsultaChaveInterface`):

```java
ServiceResult consultarChave(String valor); // 200 + conta (ContaBancariaResult) ou 403
boolean       existeChave(String valor);
```

---

## Configuração do cluster

Peers, portas e o id do grupo Raft ficam em
[`ClusterConfig.java`](src/main/java/raft/ClusterConfig.java). Para mudar portas ou
adicionar/remover nós, edite os mapas `ENDERECO_RAFT` e `PORTA_RMI` — **todos os nós
precisam da mesma configuração** e compartilhar o mesmo `GROUP_ID`.

---

## Dependências principais (Apache Ratis 3.2.2)

| Artefato                 | Papel                                              |
|--------------------------|----------------------------------------------------|
| `ratis-server`           | Motor do Raft (log, eleição, replicação, StateMachine) |
| `ratis-client`           | Submeter comandos ao grupo (achar líder, retry)    |
| `ratis-grpc`             | Transporte de rede entre os nós                    |
| `ratis-metrics-default`  | Implementação de métricas (carregada em runtime)   |

---

## Testes

| Teste | O que valida |
|-------|--------------|
| `estruturas.**` | Validação das chaves, contas e igualdade/hashCode |
| `rmi.RegistroChaveRmiIntegrationTest` | Registro via RMI real (registry + stub + marshalling) |
| `rmi.services.ConsultaChaveServiceTest` | Busca por valor (qualquer tipo) e serialização do resultado |
| `raft.RaftClusterIntegrationTest` | Cluster de 3 nós: replicação de escrita e leitura ponta a ponta em outro nó |
| `raft.RaftToleranciaFalhaTest` | **Derrubar nós**: reeleição de líder, disponibilidade com maioria, não perder dado commitado e recusar escrita sem maioria |
| `raft.RaftPersistenciaTest` | **Restart total**: escrever, derrubar todos os nós e reiniciar — dados recuperados do disco |

---

## Decisões de design

- **Escritas via consenso, leituras locais** — cada nó tem uma cópia replicada completa;
  consultas leem o banco local (eventualmente consistente).
- **`BancoDeDados` indexado pelo valor da chave** (`Map<String, ContaBancaria>`) — deixa
  registro e consulta O(1); a chave é única pelo valor (independe do tipo).
- **Determinismo** — valores aleatórios (o UUID da chave aleatória) são gerados **antes**
  de o comando entrar no log, para todos os nós aplicarem exatamente o mesmo valor.
- **Persistência** — log do Raft em disco + snapshots da StateMachine; `RECOVER` no
  restart, `FORMAT` na primeira vez.
