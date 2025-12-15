# ===== 1) BUILD STAGE =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 캐시 최적화: 래퍼/설정 먼저 복사 → 의존성 캐시
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 전체 복사 후 빌드
COPY . .
RUN ./gradlew clean bootJar --no-daemon

# ===== 2) RUNTIME STAGE =====
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
WORKDIR /opt/app

# 빌드 산출물만 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
# (Actuator 없으면 HEALTHCHECK는 없어도 OK)
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]