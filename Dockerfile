FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8181

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8181} -jar target/*.jar"]