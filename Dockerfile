# syntax=docker/dockerfile:1
# Android command-line build environment. Android Studio/emulator berjalan di host.
FROM eclipse-temurin:17-jdk-jammy AS android-dev
ARG CMDLINE_TOOLS_VERSION=16111833
ARG ANDROID_API=36
ARG BUILD_TOOLS=36.0.0
ENV ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_USER_HOME=/root/.gradle
ENV PATH="${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools"
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip git ca-certificates python3 \
    && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /opt/android-sdk/cmdline-tools \
    && curl -fL --retry 3 "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d /opt/android-sdk/cmdline-tools \
    && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platform-tools" "platforms;android-${ANDROID_API}" "build-tools;${BUILD_TOOLS}"
WORKDIR /workspace/android
CMD ["sh", "./gradlew", "--no-daemon", "assembleDebug"]
