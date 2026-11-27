UNIVERSIDADE DE FORTALEZA (UNIFOR)

CENTRO DE CIÊNCIAS TECNOLÓGICAS

CURSO: CIÊNCIA DA COMPUTAÇÃO

# SIMULADOR DE SISTEMA DE ARQUIVOS

**Integrantes:**

- Vinícius Freitas Matos (Matrícula: 2422972)
- Raphael Albuquerque (Matrícula: 2413093)

**Palavras-chave:** Sistema de Arquivos. Journaling. Write-Ahead Logging. Shell. Java.

**Repositório do projeto:** [colocar aqui o link do repositório no GitHub]

---

# RESUMO

O projeto implementa um simulador de sistema de arquivos hierárquico em Java, operado por meio de um Shell interativo em linha de comando. O sistema permite criar, apagar, copiar e renomear arquivos e diretórios, listar o conteúdo e navegar pela árvore. A estrutura é mantida em memória e gravada em um arquivo JSON. Cada operação que altera a estrutura é registrada em um *journal* (`journal.log`) seguindo a técnica de *Write-Ahead Logging* (WAL): a intenção é registrada antes da alteração e a conclusão é registrada depois. Na inicialização, operações que ficaram incompletas são identificadas e refeitas. O simulador inclui ainda um comando oculto de simulação de falhas (`falhar`), usado para demonstrar a recuperação.

---

# INTRODUÇÃO

## O que é um sistema de arquivos

O sistema de arquivos é a parte do sistema operacional responsável por organizar, armazenar e recuperar informações em memória secundária de forma persistente. Ele oferece as abstrações de **arquivo** (unidade nomeada de informação) e de **diretório** (agrupamento de arquivos e de outros diretórios), formando em geral uma árvore cuja origem é o diretório raiz. Cada item possui um nome e metadados, e é localizado por um **caminho** (*path*), que descreve a sequência de diretórios a partir da raiz.

Entre as responsabilidades de um sistema de arquivos estão a criação, remoção, cópia e renomeação de arquivos e diretórios, a navegação e listagem da hierarquia e a manutenção dos dados após o desligamento da máquina. Exemplos reais são o NTFS, o ext4 e o APFS.

## O que é journaling

Operações em um sistema de arquivos raramente são atômicas no disco. Uma única operação pode exigir várias alterações (entrada do diretório, estruturas de controle, mapas de espaço livre). Se ocorrer uma queda de energia ou uma falha no meio dessas etapas, o sistema pode ficar **inconsistente**. Sem nenhuma proteção, a verificação exige varrer o disco inteiro, como faz o `fsck` no Linux.

O **journaling** reduz esse problema ao manter um registro sequencial (*journal*) das operações. A operação é anunciada no registro antes de ser aplicada. Após uma falha, basta ler o journal para descobrir quais operações foram concluídas e quais ficaram pela metade, sem verificar toda a estrutura. Sistemas como ext3 e ext4 oferecem três modos: *writeback* (registra apenas metadados, sem garantia de ordem), *ordered* (registra metadados, mas grava os dados antes) e *journal* (registra metadados e dados).

## O que é Write-Ahead Logging (WAL)

O *Write-Ahead Logging* é a regra de que **a descrição da operação deve ser gravada no log antes de a alteração ser feita** na estrutura principal. A sequência é:

1. registrar no log que a operação vai começar;
2. executar a operação;
3. registrar no log que a operação terminou.

Se houver falha entre os passos 1 e 3, o log contém o início sem a conclusão, e esse é o sinal de que a operação precisa ser tratada na recuperação.

---

# METODOLOGIA

## Tecnologias e bibliotecas utilizadas

| Item | Descrição |
|---|---|
| Linguagem | Java, versão 17 (definida em `maven.compiler.source` e `maven.compiler.target` no `pom.xml`) |
| Gerenciador de build | Apache Maven |
| Biblioteca externa | Gson 2.10.1 (`com.google.code.gson`), usada para converter a árvore de objetos em JSON e vice-versa |
| Plugin Maven | `exec-maven-plugin` 3.1.0, usado para executar o programa com `mvn compile exec:exec` |
| Persistência da estrutura | Arquivo `dados/filesystem.json` (JSON formatado) |
| Journaling | Arquivo de texto `dados/journal.log` |
| Interface | Shell em modo texto; no Windows o programa abre uma janela própria do CMD |

O simulador **não manipula arquivos reais** do computador para representar os arquivos e pastas simulados. Todos os itens existem apenas na árvore em memória e no arquivo `filesystem.json`.

## Organização e funcionamento do projeto

```
simulador-arquivos/
├── pom.xml
├── README.md
├── dados/                       (criada automaticamente na execução)
│   ├── filesystem.json
│   └── journal.log
└── src/main/java/
    ├── Main.java
    ├── Shell.java
    ├── FileSystemSimulator.java
    ├── File.java
    ├── Directory.java
    └── Journal.java
```

Fluxo geral:

```
usuário -> Main -> Shell -> FileSystemSimulator -> Journal (dados/journal.log)
                                   |
                                   +-> Directory / File (árvore em memória) -> dados/filesystem.json
```

O `Main` inicia o programa. O `Shell` exibe o prompt, lê e interpreta os comandos e chama os métodos do `FileSystemSimulator`, que valida e executa as operações. O `FileSystemSimulator` usa o `Journal` para registrar cada operação que altera a estrutura e salva a árvore no JSON.

O sistema de arquivos é uma **árvore**: a raiz (`/`) é um `Directory`, que contém uma lista de arquivos (`File`) e uma lista de subdiretórios (`Directory`). O diretório atual é guardado como texto (por exemplo, `/documentos`), e o caminho `..` é resolvido removendo o último trecho. Exemplo do conteúdo de `filesystem.json`:

```json
{
  "nome": "/",
  "arquivos": [],
  "diretorios": [
    {
      "nome": "documentos",
      "arquivos": [
        {
          "nome": "atividade.txt",
          "conteudo": "",
          "data": "2026-11-23 10:15:00"
        }
      ],
      "diretorios": []
    }
  ]
}
```

### Classes do projeto

| Classe | Responsabilidade |
|---|---|
| `Main` | Ponto de entrada. No Windows, gera um arquivo `.bat` temporário e abre uma janela do CMD (fundo preto, texto verde) que executa o programa com o argumento `--terminal`. Dentro dessa janela, ou em outros sistemas, cria o `FileSystemSimulator` e inicia o `Shell`. |
| `Shell` | Interface com o usuário. Monta o prompt (`simulador@filesystem:~/pasta$`), lê os comandos, separa comando e argumentos, valida a quantidade de argumentos, chama o simulador e imprime o resultado. Interpreta o comando sem diferenciar maiúsculas de minúsculas. No `exit`, esvazia os arquivos de persistência. |
| `FileSystemSimulator` | Núcleo do sistema. Guarda a raiz e o diretório atual, implementa as operações (`criarPasta`, `apagarPasta`, `criarArquivo`, `apagarArquivo`, `renomear`, `copiarArquivo`, `listar`, `entrarNaPasta`), valida os erros, carrega e salva o JSON, aplica as operações na árvore, aciona o `Journal` e executa a recuperação na inicialização. Contém também o método `simularFalha`. |
| `Directory` | Representa um diretório, com nome, lista de arquivos e lista de subdiretórios. Possui métodos para buscar um arquivo ou subdiretório pelo nome e para verificar se está vazio. |
| `File` | Representa um arquivo, com nome, conteúdo (texto) e data de criação. |
| `Journal` | Grava as linhas do `journal.log` (`registrarInicio`, `registrarConclusao`, `registrarRecuperacao`), controla a numeração das operações e identifica as operações pendentes (`buscarPendentes`). |

---

# IMPLEMENTAÇÃO DO JOURNALING

## Registro das operações

Todo o registro é feito no arquivo `dados/journal.log`, em texto, com **um evento por linha** e campos separados por ponto e vírgula:

```
numero;status;tipo;caminho;destino;data
```

| Campo | Significado |
|---|---|
| `numero` | Identificador sequencial da operação. As linhas de início e de fim da mesma operação têm o mesmo número. |
| `status` | `INICIADA`, `CONCLUIDA` ou `RECUPERADA` |
| `tipo` | `MKDIR`, `RMDIR`, `TOUCH`, `RM`, `RENOMEAR` ou `COPIAR` |
| `caminho` | Caminho completo do arquivo ou diretório afetado |
| `destino` | Novo nome (em `RENOMEAR`) ou caminho completo da cópia (em `COPIAR`); vazio nas demais operações |
| `data` | Data e hora do registro, no formato `yyyy-MM-dd HH:mm:ss` |

Somente as operações que **alteram** a estrutura passam pelo journal: criar pasta, apagar pasta, criar arquivo, apagar arquivo, renomear e copiar. Os comandos `ls`, `cd`, `pwd`, `clear` e `start` não modificam nada e, por isso, não são registrados. As validações (nome já existente, arquivo inexistente, pasta não vazia etc.) são feitas **antes** do registro, então uma operação inválida não deixa nenhuma linha no journal.

A cada linha, o `Journal` abre o arquivo em modo de acréscimo com a opção `StandardOpenOption.SYNC`, que solicita ao sistema operacional que a escrita seja feita de forma síncrona, em vez de ficar apenas em memória.

## Fluxo de uma operação (WAL)

O método `executar` do `FileSystemSimulator` implementa o ciclo:

1. `journal.registrarInicio(...)`: grava a linha `INICIADA` e devolve o número da operação;
2. `aplicar(...)`: altera a árvore em memória;
3. `salvar()`: grava a árvore no `filesystem.json`;
4. `journal.registrarConclusao(...)`: grava a linha `CONCLUIDA`.

O método `salvar` escreve primeiro no arquivo temporário `dados/filesystem.tmp` e depois o move para `dados/filesystem.json`, substituindo o anterior, para que o JSON não seja deixado pela metade durante a escrita.

## Estados utilizados

| Estado | Quando é gravado |
|---|---|
| `INICIADA` | Antes de a operação alterar a árvore (a intenção fica registrada) |
| `CONCLUIDA` | Depois de a árvore ser alterada e o JSON ser salvo |
| `RECUPERADA` | Na inicialização, depois de a recuperação refazer uma operação que ficou incompleta |

## Exemplos do arquivo de log

Operações normais:

```
1;INICIADA;MKDIR;/documentos;;2026-11-23 10:15:00
1;CONCLUIDA;MKDIR;/documentos;;2026-11-23 10:15:00
2;INICIADA;TOUCH;/documentos/trabalho.txt;;2026-11-23 10:15:20
2;CONCLUIDA;TOUCH;/documentos/trabalho.txt;;2026-11-23 10:15:20
3;INICIADA;RENOMEAR;/documentos/trabalho.txt;atividade.txt;2026-11-23 10:15:40
3;CONCLUIDA;RENOMEAR;/documentos/trabalho.txt;atividade.txt;2026-11-23 10:15:40
4;INICIADA;COPIAR;/documentos/atividade.txt;/documentos/copia.txt;2026-11-23 10:16:05
4;CONCLUIDA;COPIAR;/documentos/atividade.txt;/documentos/copia.txt;2026-11-23 10:16:05
```

Operação interrompida (a linha `INICIADA` não tem `CONCLUIDA`) e, depois da recuperação, o fechamento com `RECUPERADA`:

```
5;INICIADA;MKDIR;/teste;;2026-11-23 10:20:11
```

```
5;INICIADA;MKDIR;/teste;;2026-11-23 10:20:11
5;RECUPERADA;MKDIR;/teste;;2026-11-23 10:21:30
```

## Processo de recuperação

A recuperação é executada no construtor do `FileSystemSimulator`, sempre que o programa inicia, depois de carregar o `filesystem.json`. O processo é:

1. O `Journal.buscarPendentes()` lê o `journal.log` na ordem e mantém um mapa de operações abertas: uma linha `INICIADA` adiciona o número ao mapa e qualquer outra linha com o mesmo número (`CONCLUIDA` ou `RECUPERADA`) o remove. Ao final, o que sobra são as operações iniciadas e nunca fechadas. Linhas mal formadas são ignoradas.
2. Para cada operação pendente, o simulador exibe a mensagem `recuperando operação N: TIPO caminho` e chama `aplicar` com os dados lidos do journal, ou seja, **refaz a operação** (estratégia *redo*).
3. O JSON é salvo novamente.
4. O `Journal` grava a linha `RECUPERADA` com o mesmo número.
5. Ao final, se houve operações pendentes, é exibida a mensagem `recuperação concluída`.

O método `aplicar` foi escrito para poder ser executado mais de uma vez sem causar duplicidade. Por exemplo, `MKDIR` e `TOUCH` só criam o item se ele ainda não existe, e `RMDIR` e `RM` só removem se o item ainda está na árvore. Isso permite tratar os dois momentos de falha possíveis no ciclo de uma operação:

| Momento da queda | Estado do `filesystem.json` | Ação da recuperação |
|---|---|---|
| Depois de gravar `INICIADA` e antes de salvar o JSON | operação não aplicada | refaz a operação e grava `RECUPERADA` |
| Depois de salvar o JSON e antes de gravar `CONCLUIDA` | operação já aplicada | `aplicar` não altera nada (o item já existe) e apenas grava `RECUPERADA` |

Em ambos os casos, o resultado final é a operação aplicada uma única vez.

---

# COMANDOS DISPONÍVEIS NO SHELL

O prompt segue o estilo de um terminal Linux e é atualizado conforme o usuário navega: `simulador@filesystem:~$` na raiz e `simulador@filesystem:~/documentos$` dentro da pasta `documentos`. O nome do comando não diferencia maiúsculas de minúsculas (`LS` e `ls` são equivalentes), mas os nomes de arquivos e pastas mantêm as letras digitadas. Espaços extras antes e depois do comando são ignorados, e cada comando valida a quantidade de argumentos, exibindo a forma de uso quando está incorreta.

| Comando | Função | Registrado no journal |
|---|---|---|
| `mkdir nome` | Cria uma pasta | Sim (`MKDIR`) |
| `rmdir nome` | Apaga uma pasta, somente se estiver vazia | Sim (`RMDIR`) |
| `touch nome` | Cria um arquivo vazio | Sim (`TOUCH`) |
| `rm nome` | Apaga um arquivo | Sim (`RM`) |
| `mv antigo novo` | Renomeia um arquivo ou uma pasta (`novo` é apenas o novo nome, sem caminho) | Sim (`RENOMEAR`) |
| `cp origem destino` | Copia um arquivo. Se o destino for uma pasta existente, a cópia mantém o nome original dentro dela | Sim (`COPIAR`) |
| `ls [pasta]` | Lista o conteúdo da pasta atual ou da pasta informada; subpastas aparecem com o prefixo `[pasta]` | Não |
| `cd nome` | Entra na pasta | Não |
| `cd ..` | Volta para a pasta anterior | Não |
| `cd /` | Vai para a raiz | Não |
| `pwd` | Mostra o diretório atual | Não |
| `clear` | Limpa a tela do terminal | Não |
| `start` | Mostra a lista de comandos disponíveis | Não |
| `exit` | Encerra o programa e esvazia os arquivos de persistência | Não |
| `falhar 1 nome` | (oculto, não aparece no `start`) Simula uma queda antes de aplicar a operação | Registra apenas `INICIADA` |
| `falhar 2 nome` | (oculto) Simula uma queda depois de salvar o JSON e antes de gravar a conclusão | Registra apenas `INICIADA` |

Os comandos aceitam caminhos relativos e absolutos, inclusive com `.` e `..` (por exemplo, `ls /documentos` ou `cd ../outra`).

## Exemplo de utilização

```
simulador@filesystem:~$ mkdir documentos
pasta criada
simulador@filesystem:~$ cd documentos
simulador@filesystem:~/documentos$ touch trabalho.txt
arquivo criado
simulador@filesystem:~/documentos$ cp trabalho.txt copia.txt
arquivo copiado
simulador@filesystem:~/documentos$ mv trabalho.txt atividade.txt
renomeado com sucesso
simulador@filesystem:~/documentos$ ls
atividade.txt
copia.txt
simulador@filesystem:~/documentos$ pwd
/documentos
simulador@filesystem:~/documentos$ cd ..
simulador@filesystem:~$ rmdir documentos
erro: a pasta não está vazia
simulador@filesystem:~$ abc
erro: comando inexistente: abc (digite start para ver os comandos)
simulador@filesystem:~$ exit
encerrando o programa
```

## Tratamento de erros

| Situação | Mensagem |
|---|---|
| Criar pasta ou arquivo que já existe | `erro: já existe um arquivo ou pasta com esse nome` |
| Apagar arquivo inexistente | `erro: arquivo não encontrado` |
| Renomear item inexistente | `erro: arquivo ou pasta não encontrado` |
| Copiar arquivo de origem inexistente | `erro: arquivo de origem não encontrado` |
| Copiar para destino inválido | `erro: destino inválido (...)` |
| Apagar pasta com conteúdo | `erro: a pasta não está vazia` |
| Acessar pasta inexistente | `erro: a pasta não existe` |
| Comando inexistente | `erro: comando inexistente: ...` |
| Nome inválido (vazio, com `/` ou `;`, `.` ou `..`) | `erro: nome inválido` |
| Apagar ou renomear a pasta onde o usuário está | `erro: você está dentro dessa pasta` |

---

# COMO EXECUTAR O PROJETO

## Pré-requisitos

- JDK 17 ou superior;
- Apache Maven;
- Acesso à internet na primeira compilação, para o Maven baixar o Gson.

## Compilação e execução com Maven

Na raiz do projeto (onde está o `pom.xml`):

```bash
mvn compile exec:exec
```

Deve ser usado `exec:exec` (e não `exec:java`), pois o `exec:exec` inicia um processo `java` com o classpath completo, incluindo o Gson, que é necessário para a janela de terminal aberta pelo programa.

No **Windows**, o `Main` abre uma janela independente do CMD com fundo preto e texto verde e o Shell funciona dentro dela. O terminal do Maven ou do VS Code apenas inicia o processo. Em outros sistemas, o Shell executa no próprio terminal em que o comando foi chamado. Também é possível executar a classe `Main` diretamente pelo IntelliJ IDEA ou pelo VS Code.

## Arquivos de persistência

Os arquivos ficam na pasta `dados/`, criada automaticamente no diretório de onde o programa é executado:

| Arquivo | Conteúdo |
|---|---|
| `dados/filesystem.json` | Estrutura do sistema de arquivos (árvore de diretórios e arquivos) em JSON |
| `dados/journal.log` | Registro das operações (journal) |
| `dados/filesystem.tmp` | Arquivo temporário usado durante o salvamento do JSON |

**Comportamento do `exit`:** ao digitar `exit`, o programa esvazia o `filesystem.json` e o `journal.log`. Os arquivos continuam existindo, porém com tamanho zero. Na próxima execução, o simulador começa com um sistema de arquivos vazio (somente a raiz) e a numeração do journal recomeça em 1. Portanto, **o estado não é mantido entre execuções encerradas normalmente com `exit`**.

Os dados permanecem gravados quando o programa termina sem passar pelo `exit`, como em uma queda, no fechamento da janela pelo botão de fechar ou nos comandos `falhar`. Nesses casos, na próxima execução o simulador carrega o JSON existente e executa a recuperação do journal.

---

# RESULTADOS E DISCUSSÕES

## O que foi implementado

- Árvore hierárquica de diretórios e arquivos, mantida em memória;
- Criação, remoção e renomeação de diretórios, e criação, cópia, remoção e renomeação de arquivos;
- Listagem e navegação (`ls`, `cd`, `cd ..`, `cd /`, `pwd`);
- Shell com prompt atualizado, validação de argumentos, comandos sem diferenciação de maiúsculas e minúsculas e mensagens de erro em português;
- Persistência da estrutura em JSON com a biblioteca Gson;
- Journaling com WAL, com os estados `INICIADA`, `CONCLUIDA` e `RECUPERADA`;
- Recuperação automática de operações incompletas na inicialização;
- Comando oculto `falhar` para simular quedas e demonstrar a recuperação;
- Janela de terminal própria no Windows.

## Simulação de falhas e demonstração do journaling

O comando `falhar` é executado pelo `FileSystemSimulator` e encerra o programa com `System.exit(1)`, sem passar pelo tratamento do `exit` do Shell. Por isso ele **não esvazia** os arquivos, e o journal e o JSON ficam exatamente como estavam no momento da queda simulada. Ambas as variantes simulam a criação de uma pasta (`MKDIR`) e só prosseguem se o nome for válido; caso contrário, mostram o erro e o programa continua.

**`falhar 1 nome` (queda antes de aplicar).** O programa grava a linha `INICIADA` no journal e encerra sem alterar a árvore nem o JSON. Na reinicialização, a pasta não existe no JSON carregado, a recuperação a cria, salva o JSON e grava `RECUPERADA`.

**`falhar 2 nome` (queda antes de registrar a conclusão).** O programa grava `INICIADA`, aplica a operação, salva o JSON e encerra antes de gravar `CONCLUIDA`. Na reinicialização, a pasta já existe no JSON; a recuperação não duplica o item e apenas grava `RECUPERADA`.

Roteiro para reproduzir:

1. Execute o programa e digite `falhar 1 teste` (a janela mostra `simulando queda do programa...` e encerra o processo; no Windows a janela aguarda uma tecla por causa do `pause` do arquivo `.bat`).
2. Abra `dados/journal.log` e confirme a linha `INICIADA` sem `CONCLUIDA`.
3. Execute o programa novamente. Devem aparecer as mensagens `recuperando operação N: MKDIR /teste` e `recuperação concluída`.
4. Use `ls` para verificar que a pasta `teste` existe e confira em `journal.log` a linha `RECUPERADA`.
5. Repita com `falhar 2 teste2` e confirme que a pasta existe uma única vez.

## Discussão

O journaling implementado dá função real ao registro: o resultado da inicialização depende do conteúdo do `journal.log`, que indica o que precisa ser refeito. O custo é uma escrita extra por operação, a das linhas `INICIADA` e `CONCLUIDA`, e a gravação do registro com a opção `SYNC` aumenta esse custo em troca de maior chance de o registro estar no disco no momento de uma falha. O journal registra apenas operações lógicas sobre a estrutura (criação, remoção, renomeação e cópia), de modo semelhante ao journaling de metadados dos sistemas reais.

## Limitações

- O simulador é de uso individual, sem acesso concorrente;
- Nomes de arquivos e pastas não aceitam espaços, pois o Shell separa os argumentos por espaços; também não aceitam `/` nem `;` (este último separa os campos do journal);
- O comando `touch` cria apenas arquivos vazios e não há comandos para editar ou exibir o conteúdo dos arquivos;
- O comando `cp` copia apenas arquivos (não copia diretórios) e o `mv` apenas renomeia, sem mover itens entre diretórios;
- O comando `falhar` simula apenas a criação de pastas (`MKDIR`), embora a recuperação trate todos os tipos de operação registrados;
- O journal só registra `INICIADA`, `CONCLUIDA` e `RECUPERADA`; não existe estado de cancelamento, e a recuperação sempre refaz as operações pendentes, sem desfazê-las;
- A estrutura inteira é regravada no JSON a cada operação, o que não seria eficiente em volumes grandes;
- O `exit` apaga os dados de persistência, então a persistência entre execuções só ocorre após falhas ou encerramentos sem o `exit`;
- A abertura da janela própria é implementada apenas para Windows.

---

# CONCLUSÃO

O trabalho resultou em um simulador de sistema de arquivos em Java, composto por uma árvore de diretórios e arquivos, um Shell interativo e um mecanismo de journaling baseado em *Write-Ahead Logging*. A implementação demonstra, na prática, por que registrar a operação antes de executá-la e confirmá-la depois permite identificar o que ficou incompleto após uma falha e restaurar a consistência sem verificar toda a estrutura. A escolha de uma aplicação idempotente das operações permitiu tratar os dois momentos críticos de queda com a mesma rotina de recuperação, e o comando `falhar` torna esse comportamento reproduzível durante a apresentação. Como evolução, o simulador poderia incluir edição de conteúdo, movimentação e cópia de diretórios, um estado de cancelamento no journal e a limpeza periódica do log (*checkpoint*).

---

# REFERÊNCIAS

TANENBAUM, A. S.; BOS, H. **Sistemas operacionais modernos**. 4. ed. São Paulo: Pearson, 2015.

GOOGLE. **Gson**: a Java serialization/deserialization library to convert Java objects into JSON and back. Disponível em: https://github.com/google/gson.

ORACLE. **Java Platform, Standard Edition Documentation**. Disponível em: https://docs.oracle.com/en/java/.

APACHE SOFTWARE FOUNDATION. **Apache Maven Project**. Disponível em: https://maven.apache.org/.
