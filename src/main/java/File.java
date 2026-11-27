public class File {

    private String nome;
    private String conteudo;
    private String data;

    public File(String nome, String conteudo, String data) {
        this.nome = nome;
        this.conteudo = conteudo;
        this.data = data;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getData() {
        return data;
    }
}