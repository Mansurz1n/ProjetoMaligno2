package Trabalhador;

// PedidoDeMerge.java
public class PedidoDeMerge extends Comunicado
{
    private byte[] vetor;
    public PedidoDeMerge (byte[] vetor) { this.vetor = vetor; }
    public byte[] getVetor () { return this.vetor; }
}

