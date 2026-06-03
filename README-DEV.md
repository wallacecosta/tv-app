# Documentação Técnica — IPTV Player

> Para a documentação de usuário, veja [README.md](README.md).

---

## Índice

1. [Configuração do Ambiente](#1-configuração-do-ambiente)
2. [Estrutura do Projeto](#2-estrutura-do-projeto)
3. [Arquitetura](#3-arquitetura)
4. [Classes e Responsabilidades](#4-classes-e-responsabilidades)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [SharedPreferences — Esquema](#6-sharedpreferences--esquema)
7. [Dependências](#7-dependências)
8. [Geração de Ícones](#8-geração-de-ícones)
9. [Build](#9-build)
10. [Notas de Compatibilidade](#10-notas-de-compatibilidade)

---

## 1. Configuração do Ambiente

### Requisitos

| Ferramenta | Versão | Instalação |
|---|---|---|
| Java (OpenJDK) | 17 | `sudo apt install openjdk-17-jdk` |
| Android SDK | API 34 | via `sdkmanager` (veja abaixo) |
| Gradle | 8.4 | baixado automaticamente pelo wrapper |
| Python | 3.x | necessário apenas para regenerar ícones |

### Instalação do Android SDK (Linux/WSL2)

```bash
# 1. Baixar command-line tools
mkdir -p ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
unzip /tmp/cmdtools.zip -d /tmp/cmdtools_extracted
mv /tmp/cmdtools_extracted/cmdline-tools ~/android-sdk/cmdline-tools/latest

# 2. Configurar variáveis de ambiente
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 3. Aceitar licenças e instalar componentes
yes | sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

### Variáveis de ambiente necessárias para o build

```bash
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

---

## 2. Estrutura do Projeto

```
clock-app/
├── app/
│   ├── build.gradle                   # Dependências e configuração do módulo
│   └── src/main/
│       ├── AndroidManifest.xml        # Permissões, activities, launcher entries
│       ├── java/com/example/clockapp/
│       │   ├── Channel.java           # Model: dados de um canal
│       │   ├── ChannelRepository.java # Singleton: cache da lista em memória
│       │   ├── M3uParser.java         # Parser de listas M3U via HTTP
│       │   ├── ChannelAdapter.java    # RecyclerView adapter (phone)
│       │   ├── TvChannelAdapter.java  # RecyclerView adapter (TV, duplo-modo)
│       │   ├── MainActivity.java      # Tela principal — phone
│       │   ├── PlayerActivity.java    # Player — phone
│       │   ├── TvMainActivity.java    # Tela principal — Android TV
│       │   └── TvPlayerActivity.java  # Player — Android TV
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_background.xml  # Fundo do ícone adaptativo
│           │   ├── ic_launcher_foreground.xml  # Frente do ícone adaptativo (vector)
│           │   └── tv_item_selector.xml         # Seletor de foco para items TV
│           ├── drawable-xhdpi/
│           │   └── tv_banner.png               # Banner 320×180 para Android TV
│           ├── layout/
│           │   ├── activity_main.xml           # Layout phone — lista
│           │   ├── activity_player.xml         # Layout phone — player
│           │   ├── activity_tv_main.xml        # Layout TV — lista
│           │   ├── activity_tv_player.xml      # Layout TV — player + sidebar + OSD
│           │   ├── item_channel.xml            # Item phone (56dp)
│           │   └── item_channel_tv.xml         # Item TV (72dp, focusable)
│           ├── menu/
│           │   └── main_menu.xml               # Menu do Toolbar (phone)
│           ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│           │   ├── ic_launcher.png             # Ícone por densidade (48–192px)
│           │   └── ic_launcher_round.png
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml             # Ícone adaptativo (API 26+)
│           │   └── ic_launcher_round.xml
│           └── values/
│               └── themes.xml                  # AppTheme (AppCompat dark)
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties               # Gradle 8.4
├── build.gradle                                # AGP 8.2.2
├── settings.gradle                             # Nome do projeto, repositórios
├── gradle.properties                           # AndroidX, Jetifier, JVM heap
├── gradlew                                     # Wrapper script
└── gen_icons.py                                # Gerador de ícones (Python puro)
```

---

## 3. Arquitetura

O app usa uma arquitetura simples sem camadas extras (sem MVVM, sem LiveData). Cada Activity gerencia seu próprio estado diretamente.

### Duas entradas de launcher

```
AndroidManifest.xml
├── LAUNCHER        → MainActivity      (smartphones)
└── LEANBACK_LAUNCHER → TvMainActivity  (Android TV / Google TV)
```

### Fluxo de Activities

```
┌─────────────────────┐       ┌──────────────────────┐
│    MainActivity      │       │   TvMainActivity      │
│  (phone — lista)    │       │  (TV — lista D-pad)   │
│                     │       │                       │
│  RecyclerView       │       │  RecyclerView         │
│  Toolbar menu       │       │  MENU key settings    │
│  Search EditText    │       │  BACK key exit menu   │
└────────┬────────────┘       └──────────┬────────────┘
         │ startActivity                  │ startActivity
         ▼                               ▼
┌─────────────────────┐       ┌──────────────────────┐
│   PlayerActivity    │       │  TvPlayerActivity    │
│  (phone — player)  │       │  (TV — player)       │
│                     │       │                       │
│  ExoPlayer          │       │  ExoPlayer (fullscreen│
│  BACK → exit menu   │       │  Sidebar de canais    │
│  1 canal por vez    │       │  D-pad channel switch │
└─────────────────────┘       │  BACK → exit menu    │
                               └──────────────────────┘
```

### Singleton — ChannelRepository

```
ChannelRepository (processo-vivo)
├── List<Channel> channels    ← carregada por Main*Activity
├── int currentIndex          ← atualizado por Player*Activity
└── get() → instância estática
```

Usado para evitar passar a lista de canais (potencialmente grande) via Intent. `TvPlayerActivity` acessa `ChannelRepository.get().getChannels()` diretamente em `onCreate()`.

---

## 4. Classes e Responsabilidades

### `Channel.java`
Model imutável. Campos: `name`, `url`, `logo`, `group`. Todos `public final String`.

### `ChannelRepository.java`
Singleton (instância estática). Armazena a lista em memória durante a sessão. Também persiste `currentIndex` para o destaque do canal ativo na lista principal.

### `M3uParser.java`
Parser linha a linha de arquivos M3U via `HttpURLConnection`. Suporta:
- Atributo `tvg-name` (nome do canal)
- Atributo `tvg-logo` (URL da logo)
- Atributo `group-title` (grupo/categoria)
- Fallback: texto após a última vírgula da linha `#EXTINF`
- Redirecionamentos HTTP (até 5 saltos recursivos)
- Encoding UTF-8

Executa **sempre em background thread** (chamado dentro de `ExecutorService`).

### `ChannelAdapter.java`
Adapter do phone. Usa `item_channel.xml` (56dp). Interface `OnChannelClick` passa apenas `Channel`. Seletor de background via `?attr/selectableItemBackground`.

### `TvChannelAdapter.java`
Adapter TV com dois modos controlados por `setSidebarMode(boolean)`:

| Modo | `sidebarMode = false` (lista principal) | `sidebarMode = true` (sidebar do player) |
|---|---|---|
| `focusable` dos items | `true` (D-pad natural) | `false` (manual) |
| Background | `tv_item_selector.xml` (foco por estado) | Cor programática |
| Highlight ativo | `setSelectedIndex()` → azul escuro | `setFocusedIndex()` → vermelho |

### `MainActivity.java` / `TvMainActivity.java`
Responsabilidades:
- Carregar a lista M3U em `ExecutorService` (thread única)
- Salvar URL em `SharedPreferences`
- Filtrar canais por nome/grupo
- Auto-retomar último canal na inicialização (`autoResume = true` no `onCreate`)
- Exibir menu de saída ao pressionar Voltar

### `PlayerActivity.java`
Player simples para smartphone. Recebe `url` e `name` via Intent. Salva o canal em `SharedPreferences` ao iniciar. Gerencia apenas um canal por sessão.

### `TvPlayerActivity.java`
Player TV. Responsabilidades adicionais:
- Sidebar overlay com lista de canais
- Troca de canal por ↑↓ ou Canal+/Canal-
- OSD (On Screen Display) com nome e grupo do canal (auto-oculta em 3,5s)
- Menu de saída ao pressionar Voltar (sem sidebar aberta)
- Salva o canal atual em `SharedPreferences` a cada troca
- Recarrega a lista em background sem interromper a reprodução

---

## 5. Fluxo de Dados

### Carregamento da lista M3U

```
SharedPreferences["m3u_url"]
        │
        ▼
M3uParser.parse(url)          ← background thread (ExecutorService)
        │
        ├── HttpURLConnection (com redirect manual até 5x)
        ├── BufferedReader linha a linha
        └── List<Channel>
                │
                ▼ (main thread via Handler)
        ChannelRepository.setChannels()
        adapter.setChannels()
        [se autoResume] → resumeLastChannel()
```

### Auto-retomada do último canal

```
App abre → ChannelRepository.isEmpty() == true
        │
        ▼
loadPlaylist(url, autoResume=true)
        │
        ▼ (após carregar)
prefs["last_ch_url"] existe?
        ├── SIM → busca por URL na lista → startActivity(Player*Activity)
        └── NÃO → exibe lista normalmente
```

### Salvamento do canal assistido

Salvo em dois pontos:
1. `MainActivity` / `TvMainActivity` — ao clicar no canal na lista
2. `TvPlayerActivity.playChannel()` — a cada troca de canal (sidebar, ↑↓)

```
SharedPreferences["last_ch_url"]  = channel.url
SharedPreferences["last_ch_name"] = channel.name
```

---

## 6. SharedPreferences — Esquema

**Nome do arquivo:** `iptv` (em todos os Activities via `getSharedPreferences("iptv", MODE_PRIVATE)`)

| Chave | Tipo | Valor | Escrito por |
|---|---|---|---|
| `m3u_url` | String | URL da lista M3U ativa | Main*Activity (dialog de URL) |
| `last_ch_url` | String | URL do último canal assistido | Main*Activity (clique), TvPlayerActivity (play) |
| `last_ch_name` | String | Nome do último canal assistido | Main*Activity (clique), TvPlayerActivity (play) |

---

## 7. Dependências

Definidas em `app/build.gradle`:

| Biblioteca | Versão | Uso |
|---|---|---|
| `androidx.appcompat:appcompat` | 1.6.1 | AppCompatActivity, Toolbar, AlertDialog |
| `androidx.recyclerview:recyclerview` | 1.3.2 | Listas de canais |
| `androidx.media3:media3-exoplayer` | 1.3.1 | Player de vídeo principal |
| `androidx.media3:media3-exoplayer-hls` | 1.3.1 | Suporte a streams HLS (`.m3u8`) |
| `androidx.media3:media3-ui` | 1.3.1 | `PlayerView` (UI do player) |
| `com.squareup.picasso:picasso` | 2.8 | Carregamento assíncrono de logos dos canais |

**Sem dependências de injeção**, sem Kotlin, sem Coroutines. Java 17 puro.

### Gradle e ferramentas de build

| Ferramenta | Versão |
|---|---|
| Android Gradle Plugin (AGP) | 8.2.2 |
| Gradle Wrapper | 8.4 |
| `compileSdk` | 34 (Android 14) |
| `minSdk` | 21 (Android 5.0) |
| `targetSdk` | 34 |
| `sourceCompatibility` | Java 17 |

---

## 8. Geração de Ícones

O arquivo `gen_icons.py` na raiz do projeto gera todos os assets de ícone usando **apenas a stdlib do Python** (sem Pillow, sem dependências externas). O encoder PNG é implementado com `struct` e `zlib`.

### Executar

```bash
cd clock-app
python3 gen_icons.py
```

### O que gera

| Arquivo | Tamanho | Destino |
|---|---|---|
| `ic_launcher.png` | 48 / 72 / 96 / 144 / 192 px | `mipmap-{mdpi..xxxhdpi}/` |
| `ic_launcher_round.png` | idem | idem |
| `tv_banner.png` | 320×180 px | `drawable-xhdpi/` |

### Design do ícone

- **Fundo:** `#1a1a2e` (navy escuro), preenchimento total
- **Corpo da TV:** retângulo branco com cantos arredondados + pernas + base
- **Tela interna:** retângulo `#16213e` (bisel escuro)
- **Play:** triângulo `#e94560` (vermelho/rosa) centralizado na tela
- **Banner:** mesma TV + 3 arcos de broadcast (`)))`) em vermelho à direita

### Ícone adaptativo (API 26+)

```
res/mipmap-anydpi-v26/ic_launcher.xml
├── background: ic_launcher_background.xml  (shape solid #1a1a2e)
└── foreground: ic_launcher_foreground.xml  (vector drawable 108×108dp)
```

A área segura do ícone adaptativo é os 72dp centrais (margem de 18dp). O conteúdo do vector está posicionado de (26,26) a (82,82) — dentro da safe zone.

---

## 9. Build

### Debug (desenvolvimento)

```bash
export ANDROID_HOME=~/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

./gradlew assembleDebug
# Saída: app/build/outputs/apk/debug/app-debug.apk (~6.5 MB)
```

### Release (produção)

O build release requer um **keystore** de assinatura. Crie uma vez e guarde com segurança:

```bash
# 1. Gerar keystore (executar apenas uma vez)
keytool -genkey -v \
  -keystore release.keystore \
  -alias iptv \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Seu Nome, O=Sua Org, C=BR" \
  -storepass SUA_SENHA \
  -keypass SUA_SENHA
```

Criar `keystore.properties` na raiz (**não versionar este arquivo**):

```properties
storeFile=../../release.keystore
storePassword=SUA_SENHA
keyAlias=iptv
keyPassword=SUA_SENHA
```

Adicionar ao `app/build.gradle`:

```groovy
def ksProp = new Properties()
ksProp.load(new FileInputStream(rootProject.file("keystore.properties")))

android {
    signingConfigs {
        release {
            storeFile     file(ksProp['storeFile'])
            storePassword ksProp['storePassword']
            keyAlias      ksProp['keyAlias']
            keyPassword   ksProp['keyPassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
        }
    }
}
```

```bash
# 2. Build release assinado
./gradlew assembleRelease
# Saída: app/build/outputs/apk/release/app-release.apk
```

### Instalar via ADB

```bash
# Smartphone (USB)
adb install app/build/outputs/apk/debug/app-debug.apk

# Android TV (Wi-Fi)
adb connect <IP_DO_TV>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 10. Notas de Compatibilidade

### `android:usesCleartextTraffic="true"`
Habilitado no `AndroidManifest.xml` porque a maioria dos streams IPTV usa HTTP (não HTTPS). Sem isso, Android 9+ bloqueia tráfego HTTP por padrão.

### `setSystemUiVisibility()` (deprecated API 30+)
Usado em `PlayerActivity` e `TvPlayerActivity` para fullscreen imersivo. A API está deprecated desde API 30, mas continua funcionando até Android 14 (API 34). A substituição seria `WindowInsetsController`, disponível a partir do API 30 — não implementada para manter compatibilidade com `minSdk 21`.

### `onBackPressed()` (deprecated API 33+)
Usado em `MainActivity` e `PlayerActivity`. A substituição recomendada é `OnBackPressedDispatcher.addCallback()`. Funcional em todas as versões suportadas.

### `TvPlayerActivity` — sem `onBackPressed()`
No `TvPlayerActivity`, o back é interceptado via `dispatchKeyEvent()` para poder distinguir o contexto (sidebar aberta ou não) antes de decidir a ação. `onBackPressed()` não permite essa distinção.

### Leanback sem a biblioteca Leanback
O app declara `android.software.leanback` como feature não-obrigatória e usa `LEANBACK_LAUNCHER` para aparecer no home do TV, mas **não usa a biblioteca `androidx.leanback`**. Isso reduz o tamanho do APK e dá controle total sobre o layout, à custa de não ter os componentes visuais padrão do Leanback (BrowseFragment, etc.).

### Suporte a RTMP
Não implementado. A maioria dos streams IPTV modernos usa HLS (`.m3u8`). Suporte a RTMP exigiria uma extensão separada para o ExoPlayer ou a biblioteca VLC, aumentando significativamente o tamanho do APK.
