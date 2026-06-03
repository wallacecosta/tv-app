# 📺 IPTV Player

![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen?logo=android&logoColor=white)
![API](https://img.shields.io/badge/API-21%2B-blue)
![Android TV](https://img.shields.io/badge/Android%20TV-compatível-orange?logo=googleplay&logoColor=white)
![Google TV](https://img.shields.io/badge/Google%20TV-compatível-red)
![APK](https://img.shields.io/badge/APK-~6.5%20MB-lightgrey)

Player de IPTV simples e direto ao ponto para **smartphones Android** e **Android TV / Google TV**. Carrega qualquer lista no formato M3U via URL e reproduz streams HLS e HTTP com suporte completo a controle remoto.

---

## ✨ Funcionalidades

| Recurso | Smartphone | Android TV |
|---|:---:|:---:|
| Carregar lista M3U via URL | ✅ | ✅ |
| Busca de canais por nome ou grupo | ✅ | ✅ |
| Persistência da lista entre sessões | ✅ | ✅ |
| Retomar último canal ao abrir o app | ✅ | ✅ |
| Player fullscreen | ✅ | ✅ |
| Navegação por controle remoto (D-pad) | — | ✅ |
| Sidebar de canais no player | — | ✅ |
| Troca de canal com ↑ ↓ | — | ✅ |
| OSD com nome do canal | — | ✅ |
| Banner na tela inicial do TV | — | ✅ |

---

## 📱 Capturas de tela

> **Smartphone** — lista de canais com busca e player fullscreen.
> **Android TV** — lista navegável com D-pad e player com sidebar de canais.

*(Adicione capturas de tela aqui após instalar)*

---

## 📦 Download

Baixe o APK compilado na seção **[Releases](../../releases)** ou compile você mesmo (veja [Construir do código-fonte](#-construir-do-código-fonte)).

---

## 📲 Instalação

### Smartphone (Android 5.0+)

1. Transfira o arquivo `app-debug.apk` para o dispositivo
2. Em **Configurações → Segurança**, habilite **"Instalar apps de fontes desconhecidas"**
3. Abra o arquivo APK e confirme a instalação

**Via ADB (com USB Debugging ativado):**
```bash
adb install app-debug.apk
```

### Android TV / Google TV

**Via ADB pela rede (recomendado):**
```bash
# No TV: Configurações → Sistema → Sobre → habilite "Depuração ADB"
adb connect <IP_DO_TV>:5555
adb install app-debug.apk
```

O app aparecerá automaticamente na tela inicial do TV na seção de aplicativos.

---

## 🚀 Como usar

### Primeira configuração

1. Abra o app
2. Toque no menu **⋮** (smartphone) ou pressione **MENU** no controle remoto
3. Selecione **"Adicionar / trocar lista M3U"**
4. Cole a URL da sua lista M3U (ex: `https://exemplo.com/lista.m3u`)
5. Toque em **Carregar** — os canais aparecerão em alguns segundos

### Smartphone

| Ação | Como fazer |
|---|---|
| Abrir um canal | Toque no canal na lista |
| Buscar canal | Digite na barra de busca |
| Editar lista / Sair | Pressione **Voltar** → escolha a opção |

### Android TV / Google TV — Controle remoto

| Botão | Na lista | No player |
|---|---|---|
| **↑ ↓** | Navega na lista | Troca de canal |
| **OK / Enter** | Abre o canal | Abre/fecha sidebar |
| **← Esquerda** | — | Abre sidebar |
| **→ Direita** | — | Fecha sidebar |
| **Voltar** | Menu (Editar / Sair) | Fecha sidebar ou menu |
| **MENU** | Configurações | Exibe/oculta dicas |

> **Dica:** Ao abrir o app pela segunda vez, o último canal assistido é retomado automaticamente.

---

## 📡 Formatos suportados

| Protocolo | Suporte |
|---|---|
| HLS (`.m3u8`) | ✅ Nativo |
| HTTP direto (MP4, TS) | ✅ Nativo |
| HTTPS | ✅ |
| HTTP sem criptografia | ✅ (habilitado explicitamente) |
| RTMP | ❌ Não suportado |
| RTSP | ⚠️ Experimental |

---

## 📟 Dispositivos suportados

- **Android 5.0+** (API 21) — cobre ~99,5% dos dispositivos Android ativos
- **Android TV** e **Google TV** com qualquer versão de Android
- Orientação **horizontal** em todas as telas

---

## 🔨 Construir do código-fonte

### Pré-requisitos

- Java 17 (OpenJDK)
- Android SDK (API 34, build-tools 34.0.0)
- Gradle 8.4 (baixado automaticamente pelo wrapper)

### Compilar

```bash
# Clone o repositório
git clone https://github.com/SEU_USUARIO/iptv-player.git
cd iptv-player

# Configure o ambiente
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Build debug
./gradlew assembleDebug

# APK gerado em:
# app/build/outputs/apk/debug/app-debug.apk
```

Para instruções detalhadas de configuração do ambiente, veja [README-DEV.md](README-DEV.md).

---

## 📋 Permissões utilizadas

| Permissão | Motivo |
|---|---|
| `INTERNET` | Carregar listas M3U e reproduzir streams |

O app **não coleta dados**, **não requer conta** e **não acessa arquivos locais**.

---

## ⚠️ Aviso legal

Este aplicativo é um **player de mídia**. O usuário é responsável pelo conteúdo das listas M3U que adicionar. Certifique-se de utilizar apenas conteúdo ao qual você tem direito de acesso.

---

## 📄 Licença

```
MIT License — use, modifique e distribua livremente.
```
