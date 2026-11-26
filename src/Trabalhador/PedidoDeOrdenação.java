package Trabalhador;

public class PedidoDeOrdenação {

    private byte[] vetor;

    public PedidoDeOrdenação (byte[] vetor){
        this.vetor = vetor;
    }

    public byte[] getVetor() {
        return vetor;
    }
}
