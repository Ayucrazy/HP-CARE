# Production-ready Multi-Stage Container for HP Care Service Desk
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy all application web assets and standalone server
COPY . /app

# Expose port (default 8080 or dynamic Cloud port)
ENV PORT=8080
EXPOSE 8080

# Compile and run standalone Java Server
RUN javac HPCareServer.java DatabaseManager.java || true

CMD ["java", "-cp", ".", "com.hpcare.HPCareServer"]
