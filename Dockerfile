FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN mkdir -p out \
    && cp -R src/main/resources/. out/ \
    && javac -cp "lib/h2-2.2.224.jar:lib/gson-2.10.1.jar" -d out $(find src/main/java -name "*.java" | tr '\n' ' ') \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8081

CMD ["sh", "-c", "java -cp \"out:lib/h2-2.2.224.jar:lib/gson-2.10.1.jar\" com.playermgs.WebServer"]
