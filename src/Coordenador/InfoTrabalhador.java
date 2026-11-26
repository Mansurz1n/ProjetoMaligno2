package Coordenador;
import java.io.*;
import java.net.*;

// Tudo sobre UM servidor: IP, porta, conexao e tempo da calibracao.
public class InfoTrabalhador {
    private String host;
    private int porta;
    private Parceiro parceiro;
    private long tempoCalibracao = Long.MAX_VALUE; // nanossegundos

    public InfoTrabalhador (String host, int porta) throws Exception
    {
        if (host==null)
            throw new Exception ("Host ausente");

        this.host = host;
        this.porta = porta;
    }

    public void conectar () throws Exception
    {
        Socket conexao = new Socket (this.host, this.porta);

        ObjectOutputStream transmissor =
                new ObjectOutputStream (conexao.getOutputStream());

        ObjectInputStream receptor =
                new ObjectInputStream (conexao.getInputStream());

        this.parceiro = new Parceiro (conexao, receptor, transmissor);
    }

    // manda ordenar e BLOQUEIA ate chegar OrdenacaoPronta
    public void ordenar (byte[] pedaco) throws Exception
    {
        this.parceiro.receba (new PedidoDeOrdenacao (pedaco));

        if (!(this.parceiro.envie () instanceof OrdenacaoPronta))
            throw new Exception ("Esperava OrdenacaoPronta de "+this);
    }

    // manda juntar com o pedaco que ele ja tem e BLOQUEIA ate MergeamentoPronto
    public void juntarCom (byte[] pedaco) throws Exception
    {
        this.parceiro.receba (new PedidoDeMergeamento (pedaco));

        if (!(this.parceiro.envie () instanceof MergeamentoPronto))
            throw new Exception ("Esperava MergeamentoPronto de "+this);
    }

    // so da pra chamar depois de ordenar()/juntarCom() voltarem, ou seja,
    // nunca pega vetor que ainda nao terminou
    public byte[] pegarResultado () throws Exception
    {
        this.parceiro.receba (new PedidoDeResultado ());

        Comunicado comunicado = this.parceiro.envie ();
        if (!(comunicado instanceof Resultado))
            throw new Exception ("Esperava Resultado de "+this);

        return ((Resultado)comunicado).getVetor ();
    }

    public void sair ()
    {
        try
        {
            this.parceiro.receba (new PedidoParaSair ());
            this.parceiro.adeus ();
        }
        catch (Exception erro)
        {}
    }

    public long getTempoCalibracao () { return this.tempoCalibracao; }

    public void setTempoCalibracao (long tempo) { this.tempoCalibracao = tempo; }

    public String toString () { return this.host+":"+this.porta; }
}


