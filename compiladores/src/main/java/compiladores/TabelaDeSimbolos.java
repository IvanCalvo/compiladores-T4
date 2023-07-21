package compiladores;

import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum TipoAlguma {
        INTEIRO,
        REAL,
        CADEIA,
        LOGICO,
        INVALIDO,
        TIPO, 
        IDENT
    }
    
    class EntradaTabelaDeSimbolos {
        TipoAlguma tipo;
        String nome;

        private EntradaTabelaDeSimbolos(String nome, TipoAlguma tipo) {
            this.tipo = tipo;
            this.nome = nome;
        }
    }
    
    private final Map<String, EntradaTabelaDeSimbolos> tabela;
    
     public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }
    
    public TipoAlguma verificar(String nome) {
        return tabela.get(nome).tipo;
    }
    
    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }
    
    public void adicionar(String nome, TipoAlguma tipo) {
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo));
    }
    
   
}
