import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Journal {

    private Path log = Paths.get("dados", "journal.log");
    private int proximo = 1;

    public Journal() {
        try {
            Files.createDirectories(log.getParent());
        } catch (IOException e) {
            System.out.println("erro ao criar a pasta dados: " + e.getMessage());
        }

        for (String linha : lerLinhas()) {
            String[] campos = linha.split(";", -1);
            try {
                int numero = Integer.parseInt(campos[0]);
                if (numero >= proximo) {
                    proximo = numero + 1;
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    public static String agora() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int registrarInicio(String tipo, String caminho, String destino) {
        int numero = proximo;
        proximo++;
        escrever(numero, "INICIADA", tipo, caminho, destino);
        return numero;
    }

    public void registrarConclusao(int numero, String tipo, String caminho, String destino) {
        escrever(numero, "CONCLUIDA", tipo, caminho, destino);
    }

    public void registrarRecuperacao(int numero, String tipo, String caminho, String destino) {
        escrever(numero, "RECUPERADA", tipo, caminho, destino);
    }

    public List<String[]> buscarPendentes() {
        Map<Integer, String[]> abertas = new LinkedHashMap<>();

        for (String linha : lerLinhas()) {
            String[] campos = linha.split(";", -1);
            if (campos.length < 6) {
                continue;
            }
            try {
                int numero = Integer.parseInt(campos[0]);
                if (campos[1].equals("INICIADA")) {
                    abertas.put(numero, campos);
                } else {
                    abertas.remove(numero);
                }
            } catch (NumberFormatException e) {
            }
        }

        return new ArrayList<>(abertas.values());
    }

    private void escrever(int numero, String status, String tipo, String caminho, String destino) {
        String linha = numero + ";" + status + ";" + tipo + ";" + caminho + ";" + destino + ";" + agora()
                + System.lineSeparator();
        try {
            Files.write(log, linha.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
        } catch (IOException e) {
            System.out.println("erro ao escrever no journal: " + e.getMessage());
        }
    }

    private List<String> lerLinhas() {
        List<String> linhas = new ArrayList<>();
        try {
            if (Files.exists(log)) {
                linhas = Files.readAllLines(log, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.out.println("erro ao ler o journal: " + e.getMessage());
        }
        return linhas;
    }
}