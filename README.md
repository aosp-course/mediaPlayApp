# MediaPlayerApp

MediaPlayerApp é um aplicativo Android projetado para oferecer uma experiência de reprodução de música simples e eficaz. Este aplicativo permite a reprodução de músicas armazenadas localmente como recursos, proporcionando ao usuário funcionalidades básicas de um media player, como tocar, pausar, parar e pular faixas. Além disso, o aplicativo possui notificações que informam o status atual do media player.


## Funcionalidades e Funcionamento

O aplicativo oferece uma série de funcionalidades principais que são integradas de forma coesa para proporcionar uma experiência de usuário fluida:


- **Reprodução de Música**: Ao iniciar o aplicativo, o usuário pode começar a reprodução da música selecionada a partir de uma lista predefinida de faixas. A interface do usuário exibe controles de reprodução claros e intuitivos. Durante a reprodução, logs são gerados para registrar o início da música.


  ![image](https://github.com/user-attachments/assets/e1e36361-41b5-40d6-abf6-319bed13fcce)
  Imagem 1: Captura de tela do aplicativo em execução mostrando a interface de reprodução.
  
  ![image](https://github.com/user-attachments/assets/002ce832-03de-471c-bab4-8b7ffa2f13c4)
  Imagem 2: Captura de tela dos logs indicando que a reprodução da música foi iniciada.


- **Controle de Reprodução**: O aplicativo permite que o usuário pause ou pare a música atualmente em reprodução. Ao pausar ou parar a música, o aplicativo atualiza a interface do usuário e as notificações em tempo real. Logs são gerados para registrar essas ações, ajudando a monitorar o comportamento do aplicativo.

  ![image](https://github.com/user-attachments/assets/6244b699-a542-4904-ad52-be73aafa27b8)
  Imagem 3: Captura de tela mostrando o estado de pausa do aplicativo.
 
  ![image](https://github.com/user-attachments/assets/736b78f0-c4e9-4626-b334-4fbbaece4888)
  Imagem 4: Captura de tela dos logs mostrando que a música foi pausada.


- **Navegação de Faixas**: O usuário pode pular para a próxima música ou retornar à faixa anterior na lista de reprodução, garantindo uma transição suave entre as músicas. Essa ação também é registrada nos logs, fornecendo detalhes sobre as transições de faixas.

  ![image](https://github.com/user-attachments/assets/79ce6051-0380-418d-83d3-d575cf434e0f)
  Imagem 5: Captura de tela do aplicativo ao pular para a próxima música.
  
  ![image](https://github.com/user-attachments/assets/e82c7f95-cd12-44c9-9848-d9f71ac288ea)
  Imagem 6: Captura de tela dos logs indicando a mudança de faixa.

- **Notificações**: Durante a reprodução, notificações são exibidas para informar o usuário sobre o status atual do media player, como quando a música está tocando, pausada ou parada. As notificações são atualizadas automaticamente conforme o estado do media player muda.

  ![image](https://github.com/user-attachments/assets/9f95908b-2694-4f3c-bbf7-9f2d4c55b8cc)
  ![image](https://github.com/user-attachments/assets/a727eb77-fef8-49dc-8c32-6f8e7733b658)
  ![image](https://github.com/user-attachments/assets/22f4a4ac-8835-4277-9379-6d4937c95cb0)

## Estrutura do Projeto

O aplicativo é construído usando componentes Android, distribuídos nas seguintes classes principais:

- **MediaPlayerService**: Gerencia a reprodução de áudio utilizando o `MediaPlayer`. Este serviço é responsável por controlar as operações de tocar, pausar e parar a música. Também lida com o foco de áudio e envia broadcasts para informar outras partes do aplicativo sobre o status atual do player.

- **MainViewModel**: Atua como o ViewModel do aplicativo, gerenciando a lógica de negócios, incluindo a manipulação da lista de reprodução, controle de favoritos, e comunicação com o `MediaPlayerService`.

- **MediaPlayerNotificationManager**: Esta classe auxiliar é responsável por criar e gerenciar notificações que mantêm o usuário informado sobre o status do media player.

## Configuração do Projeto

Para configurar o projeto localmente, siga as etapas abaixo:

1. **Clonagem do Repositório**: O repositório pode ser clonado usando o comando:
   ```bash
   git clone https://github.com/seu-usuario/MediaPlayerApp.git
   ```

## Estrutura do Projeto

O aplicativo é construído usando componentes Android, distribuídos nas seguintes classes principais:

- **MediaPlayerService**: Gerencia a reprodução de áudio utilizando o `MediaPlayer`. Este serviço é responsável por controlar as operações de tocar, pausar e parar a música. Também lida com o foco de áudio e envia broadcasts para informar outras partes do aplicativo sobre o status atual do player.

- **MainViewModel**: Atua como o ViewModel do aplicativo, gerenciando a lógica de negócios, incluindo a manipulação da lista de reprodução, controle de favoritos, e comunicação com o `MediaPlayerService`.

- **MediaPlayerNotificationManager**: Esta classe auxiliar é responsável por criar e gerenciar notificações que mantêm o usuário informado sobre o status do media player.

## Configuração do Projeto

Para configurar o projeto localmente, siga as etapas abaixo:

1. **Clonagem do Repositório**: O repositório pode ser clonado usando o comando:
   ```bash
   git clone https://github.com/aosp-course/mediaPlayApp.git
   ```


2. Abertura no Android Studio: O projeto deve ser aberto no Android Studio, onde todas as dependências serão resolvidas automaticamente.
3. Compilação e Execução: Após a abertura, o projeto deve ser compilado e executado em um dispositivo Android ou emulador.
  
   
2 .Abertura no Android Studio: O projeto deve ser aberto no Android Studio, onde todas as dependências serão resolvidas automaticamente.

Compilação e Execução: Após a abertura, o projeto deve ser compilado e executado em um dispositivo Android ou emulador.

## Recursos de Músicas
As músicas utilizadas para este projeto pertencem a Kevin MacLeod (incompetech.com), licenciadas sob Creative Commons: por atribuição de licença 4.0 (http://creativecommons.org/licenses/by/4.0/). As faixas incluídas são:

“Circus of Freaks”
“Gothamlicious”
"I Got a Stick Arr Bryan Teoh"
"New Hero in Town”
“The Ice Giants”
