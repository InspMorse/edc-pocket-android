# Cloud Agent base image for the EDC pocket Android app.
# Provides the stable toolchain (JDK 17, Android SDK 35, Gradle 8.9); the
# repository's own dependencies are refreshed by the `install` command in
# .cursor/environment.json after checkout.
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-17-jdk-headless \
        git \
        curl \
        unzip \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Toolchain locations. These are baked into the image so every agent shell and
# Gradle invocation can find them without extra profile setup.
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_HOME=/opt/gradle/gradle-8.9
ENV PATH="${JAVA_HOME}/bin:${GRADLE_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Pinned versions. compileSdk/targetSdk = 35 and build-tools 35.0.0 match
# app/build.gradle.kts; Gradle 8.9 matches gradle/wrapper/gradle-wrapper.properties.
ARG ANDROID_CMDLINE_TOOLS_VERSION=11076708
ARG ANDROID_PLATFORM=android-35
ARG ANDROID_BUILD_TOOLS=35.0.0
ARG GRADLE_VERSION=8.9

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && cd /tmp \
    && curl -fsSL -o cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip" \
    && unzip -q cmdline-tools.zip \
    && mkdir -p "${ANDROID_HOME}/cmdline-tools/latest" \
    && mv cmdline-tools/* "${ANDROID_HOME}/cmdline-tools/latest/" \
    && rm -rf cmdline-tools cmdline-tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install \
        "platform-tools" \
        "platforms;${ANDROID_PLATFORM}" \
        "build-tools;${ANDROID_BUILD_TOOLS}" > /dev/null \
    && curl -fsSL -o /tmp/gradle.zip \
        "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt/gradle \
    && rm -f /tmp/gradle.zip \
    && chown -R ubuntu:ubuntu "${ANDROID_HOME}"

# Ubuntu 24.04 ships a default non-root `ubuntu` user (uid 1000); Cloud Agents
# run as this user.
USER ubuntu
