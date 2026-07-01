FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac -cp "lib/gson-2.10.1.jar:lib/h2-2.2.224.jar" -d out $(find src/main/java -name "*.java")

CMD ["java", "-cp", "out:lib/gson-2.10.1.jar:lib/h2-2.2.224.jar", "com.playermgs.WebServer"]
