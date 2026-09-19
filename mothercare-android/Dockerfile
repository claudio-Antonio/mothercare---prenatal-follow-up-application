# syntax=docker/dockerfile:1
#
# CONSTRÓI o APK — não "roda" o app como os microsserviços rodam. Um
# app Android precisa de tela (emulador/aparelho) pra ser usado; isso
# aqui gera o artefato instalável, útil pra build reproduzível/CI.
#
# Uso:
#   docker build -t mothercare-android-build .
#   docker run --rm -v $(pwd)/output:/output_bind mothercare-android-build
#   # o .apk aparece em ./output/app-debug.apk

FROM gradle:8.9-jdk17 AS build

ENV ANDROID_SDK_ROOT=/opt/android-sdk
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
    && mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -sL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${PATH}"

RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

WORKDIR /workspace
COPY . .

RUN gradle assembleDebug --no-daemon

FROM alpine:3.20
WORKDIR /output
COPY --from=build /workspace/app/build/outputs/apk/debug/app-debug.apk .
CMD ["sh", "-c", "cp /output/app-debug.apk /output_bind/ 2>/dev/null || true"]
