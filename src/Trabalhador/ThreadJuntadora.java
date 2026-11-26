package Trabalhador;

public class ThreadJuntadora extends Thread {

    private byte[] vetor;
    private int inicio;
    private int meio;
    private int fim;
    private int numero;

    public ThreadJuntadora(byte[] vetor, int inicio, int meio, int fim, int numero) {
        this.vetor = vetor;
        this.inicio = inicio;
        this.meio = meio;
        this.fim = fim;
        this.numero = numero;
    }

    public void run() {
        try {
            Main.log("Thread juntadora " + numero + " comecou (" + inicio + ".." + meio + " com " + (meio + 1) + ".." + fim + ")");

            MergeSort.merge(vetor, inicio, meio, fim);

            Main.log("Thread juntadora " + numero + " terminou");
        } catch (Exception erro) {
            System.out.println("Erro na thread juntadora " + numero + ": " + erro.getMessage());
        }
    }
}
