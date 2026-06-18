FROM node:20-bookworm-slim AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM eclipse-temurin:23-jdk AS backend-build
WORKDIR /app/backend
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:23-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        nginx \
        python3 \
        python3-pip \
        python3-venv \
    && rm -rf /var/lib/apt/lists/*

COPY nlp-service/requirements.txt ./nlp-service/requirements.txt
RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r ./nlp-service/requirements.txt

COPY nlp-service/app ./nlp-service/app
COPY --from=backend-build /app/backend/target/jobmatch-backend-0.1.0.jar ./backend/app.jar
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/nginx.conf
COPY deploy/start-space.sh ./start-space.sh

RUN chmod +x ./start-space.sh

ENV NLP_SERVICE_BASE_URL="http://127.0.0.1:8001" \
    NLP_USE_TRANSFORMER="false" \
    PORT="7860"

EXPOSE 7860
CMD ["./start-space.sh"]
