# syntax=docker/dockerfile:1

# ---------- Estágio de build ----------
# Imagem só para compilar (Maven + JDK 21). O bytecode/jars gerados são neutros
# de fornecedor, então rodam no Corretto do runtime abaixo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1) Copia só o pom e baixa as dependências (camada em cache enquanto o pom não muda)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# 2) Compila e copia as dependências de runtime para target/dependency
COPY src ./src
RUN mvn -q -B -DskipTests clean package dependency:copy-dependencies -DincludeScope=runtime

# ---------- Estágio de runtime ----------
FROM amazoncorretto:21-alpine AS runtime
WORKDIR /app

# Classes da aplicação + dependências (Ratis, gRPC, SLF4J, ...)
COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/dependency ./lib

# Log + snapshots do Raft. Monte um volume aqui para persistir entre restarts.
VOLUME ["/app/raft-storage"]

# Portas: RMI (1099-1101) e Raft/gRPC (6001-6003)
EXPOSE 1099 1100 1101 6001 6002 6003

# O id do nó (n1/n2/n3) é o argumento — sobrescreva com `docker run <img> n2`.
# 'exec' faz o Java receber o SIGTERM (encerra o nó pelo shutdown hook).
# $JAVA_OPTS permite passar flags (ex.: -Djava.rmi.server.hostname=...).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp \"classes:lib/*\" Main \"$@\"", "--"]
CMD ["n1"]
