package compiladores;

import compiladores.AlgumaParser.Declaracao_globalContext;
import compiladores.AlgumaParser.Declaracao_constanteContext;
import compiladores.AlgumaParser.Declaracao_tipoContext;
import compiladores.AlgumaParser.Declaracao_variavelContext;
import compiladores.AlgumaParser.ProgramaContext;
import compiladores.AlgumaParser.IdentificadorContext;
import compiladores.AlgumaParser.CmdAtribuicaoContext;
import compiladores.AlgumaParser.Tipo_basico_identContext;

public class AlgumaSemantico extends AlgumaBaseVisitor {
    
    //Criando o objeto do escopo
    Escopos escopos = new Escopos();

    @Override
    public Object visitPrograma(ProgramaContext ctx) {
        return super.visitPrograma(ctx);
    }

    //verifica se a constante foi declarada anteriormente (ela não pode ser alterada por se tratar de uma constante)
    @Override
    public Object visitDeclaracao_constante(Declaracao_constanteContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        if (escopoAtual.existe(ctx.IDENT().getText())) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "constante" + ctx.IDENT().getText()
                    + " ja declarado anteriormente");
        } else {
            TabelaDeSimbolos.TipoAlguma tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
            switch(ctx.tipo_basico().getText()) {
               case "literal": 
                        tipo = TabelaDeSimbolos.TipoAlguma.CADEIA;
                        break;
               case "inteiro": 
                        tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
                        break;
               case "real": 
                        tipo = TabelaDeSimbolos.TipoAlguma.REAL;
                        break;
               case "logico": 
                        tipo = TabelaDeSimbolos.TipoAlguma.LOGICO;
                        break;
            }
            escopoAtual.adicionar(ctx.IDENT().getText(), tipo);
        }

        return super.visitDeclaracao_constante(ctx);
    }

    //verifica se o tipo foi declarado duas vezes
    @Override
    public Object visitDeclaracao_tipo(Declaracao_tipoContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        if (escopoAtual.existe(ctx.IDENT().getText())) {
             AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                    + " declarado duas vezes num mesmo escopo");
        } else {
            escopoAtual.adicionar(ctx.IDENT().getText(), TabelaDeSimbolos.TipoAlguma.TIPO);
        }
        return super.visitDeclaracao_tipo(ctx);
    }

    //verifica se a variável declarada já foi declarada anteriormente no escopo atual
    @Override
    public Object visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        for (IdentificadorContext id : ctx.variavel().identificador()) {
            if (escopoAtual.existe(id.getText())) {
                AlgumaSemanticoUtils.adicionarErroSemantico(id.start, "identificador " + id.getText()
                        + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.TipoAlguma tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
                switch(ctx.variavel().tipo().getText()) {
                case "literal": 
                            tipo = TabelaDeSimbolos.TipoAlguma.CADEIA;
                            break;
                case "inteiro": 
                            tipo = TabelaDeSimbolos.TipoAlguma.INTEIRO;
                            break;
                case "real": 
                            tipo = TabelaDeSimbolos.TipoAlguma.REAL;
                            break;
                case "logico": 
                            tipo = TabelaDeSimbolos.TipoAlguma.LOGICO;
                            break;
                }
                escopoAtual.adicionar(id.getText(), tipo);
            }
        }
        return super.visitDeclaracao_variavel(ctx);
    }

    //verifica se a variável global já foi declarada 
    @Override
    public Object visitDeclaracao_global(Declaracao_globalContext ctx) {
         TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        if (escopoAtual.existe(ctx.IDENT().getText())) {
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, ctx.IDENT().getText()
                    + " ja declarado anteriormente");
        } else {
            escopoAtual.adicionar(ctx.IDENT().getText(), TabelaDeSimbolos.TipoAlguma.TIPO);
        }
        return super.visitDeclaracao_global(ctx);
    }


    @Override
    public Object visitTipo_basico_ident(Tipo_basico_identContext ctx) {
        if(ctx.IDENT() != null){
            for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {
                if(!escopo.existe(ctx.IDENT().getText())) {
                    AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                            + " nao declarado");
                }
            }
        }
        return super.visitTipo_basico_ident(ctx);
    }

    //verifica se o identificador existe

    @Override
    public Object visitIdentificador(IdentificadorContext ctx) {
        for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()) {
            if(!escopo.existe(ctx.IDENT(0).getText())) {
                AlgumaSemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + ctx.IDENT(0).getText()
                        + " nao declarado");
            }
        }
        return super.visitIdentificador(ctx);
    }

    //verifica se a atribuição é válida
    @Override
    public Object visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        TabelaDeSimbolos.TipoAlguma tipoExpressao = AlgumaSemanticoUtils.verificar(escopos, ctx.expressao());
        boolean error = false;
        String nomeVar = ctx.identificador().getText();
        if (tipoExpressao != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
            for(TabelaDeSimbolos escopo : escopos.percorrerEscoposAninhados()){
                if (escopo.existe(nomeVar))  {
                    TabelaDeSimbolos.TipoAlguma tipoVariavel = AlgumaSemanticoUtils.verificar(escopos, nomeVar);
                    Boolean expNumeric = tipoExpressao == TabelaDeSimbolos.TipoAlguma.INTEIRO || tipoExpressao == TabelaDeSimbolos.TipoAlguma.REAL ;
                    Boolean varNumeric = tipoVariavel == TabelaDeSimbolos.TipoAlguma.INTEIRO || tipoVariavel == TabelaDeSimbolos.TipoAlguma.REAL;
                    if  (!(varNumeric && expNumeric) && tipoVariavel != tipoExpressao && tipoExpressao != TabelaDeSimbolos.TipoAlguma.INVALIDO) {
                        error = true;
                    }
                } 
            }
        } else{
            error = true;
        }

        if(error)
            AlgumaSemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "atribuicao nao compativel para " + nomeVar );

        return super.visitCmdAtribuicao(ctx);
    }

}
