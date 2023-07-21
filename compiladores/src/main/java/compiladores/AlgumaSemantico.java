package compiladores;

import compiladores.AlgumaParser.Declaracao_globalContext;
import compiladores.AlgumaParser.Declaracao_constanteContext;
import compiladores.AlgumaParser.Declaracao_tipoContext;
import compiladores.AlgumaParser.Declaracao_variavelContext;
import compiladores.AlgumaParser.ProgramaContext;
import compiladores.AlgumaParser.IdentificadorContext;

import java.util.ArrayList;

import org.antlr.v4.runtime.tree.TerminalNode;

import com.ibm.icu.impl.UResource.TabelaDeSimbolos;

import compiladores.AlgumaParser.CmdAtribuicaoContext;
import compiladores.AlgumaParser.Tipo_basico_identContext;
import compiladores.AlgumaParser.VariavelContext;
import compiladores.TabelaDeSimbolos;

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
            TabelaDeSimbolos.TipoAlguma tipo = AlgumaSemanticoUtils.getTipo(ctx.tipo().getText());
            if(tipo != null)
                escopoAtual.insert(ctx.IDENT().getText(), tipo, TabelaDeSimbolos.Structure.TIPO;)
            else if(ctx.tipo().registro() != null){
                ArrayList<TabelaDeSimbolos.InSymbol> varReg = new ArrayList<>();
                for(VariavelContext va : ctx.tipo().registro().variavel()){
                    TabelaDeSimbolos.Tipos tipoReg =  SemanticoUtils.getTipo(va.tipo().getText());
                    for(IdentificadorContext id2 : va.identificador()){
                        varReg.add(escopoAtual.new InSymbol(id2.getText(), tipoReg, TabelaDeSimbolos.Structure.TIPO));
                    }

                }

                if (escopoAtual.exists(ctx.IDENT().getText())) {
                    SemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + ctx.IDENT().getText()
                            + " ja declarado anteriormente");
                }
                else{
                    escopoAtual.insert(ctx.IDENT().getText(), TabelaDeSimbolos.TipoAlguma.REG, TabelaDeSimbolos.Structure.TIPO);
                }

                for(TabelaDeSimbolos.InSymbol re : varReg){
                    String nameVar = ctx.IDENT().getText() + '.' + re.name;
                    if (escopoAtual.exists(nameVar)) {
                        SemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nameVar
                                + " ja declarado anteriormente");
                    }
                    else{
                        escopoAtual.insert(re);
                        escopoAtual.insert(ctx.IDENT().getText(), re);
                    }
                }
            }
            TabelaDeSimbolos.TipoAlguma t =  SemanticoUtils.getTipo(ctx.tipo().getText());
            escopoAtual.insert(ctx.IDENT().getText(), t, TabelaDeSimbolos.Structure.TIPO);
        }
        return super.visitDeclaracao_tipo(ctx);
    }


    //verifica se a variável declarada já foi declarada anteriormente no escopo atual
    @Override
    public Object visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.getEscopo();
        for (IdentificadorContext id : ctx.variavel().identificador()) {
            String nomeId = "";
            int i = 0;
            for(TerminalNode ident : id.IDENT()){
                if(i++ > 0)
                    nomeId += ".";
                nomeId += ident.getText();
            }
            if (escopoAtual.exists(nomeId)) {
                SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                        + " ja declarado anteriormente");
            } else {
                TabelaDeSimbolos.Tipos tipo = SemanticoUtils.getTipo(ctx.variavel().tipo().getText());
                if(tipo != null)
                    escopoAtual.insert(nomeId, tipo, TabelaDeSimbolos.Structure.VAR);
                else{
                    TerminalNode identTipo =    ctx.variavel().tipo() != null
                                                && ctx.variavel().tipo().tipo_estendido() != null 
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident() != null  
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() != null 
                                                ? ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() : null;
                    if(identTipo != null){
                        ArrayList<TabelaDeSimbolos.InSymbol> regVars = null;
                        boolean found = false;
                        for(TabelaDeSimbolos t: escopos.getPilha()){
                            if(!found){
                                if(t.exists(identTipo.getText())){
                                    regVars = t.getTypeProperties(identTipo.getText());
                                    found = true;
                                }
                            }
                        }
                        if(escopoAtual.exists(nomeId)){
                            SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                        + " ja declarado anteriormente");
                        } else{
                            escopoAtual.insert(nomeId, TabelaDeSimbolos.Tipos.REG, TabelaDeSimbolos.Structure.VAR);
                            for(TabelaDeSimbolos.InSymbol s: regVars){
                                escopoAtual.insert(nomeId + "." + s.name, s.tipo, TabelaDeSimbolos.Structure.VAR);
                            }   
                        }
                    }
                    else if(ctx.variavel().tipo().registro() != null){
                        ArrayList<TabelaDeSimbolos.InSymbol> varReg = new ArrayList<>();
                        for(VariavelContext va : ctx.variavel().tipo().registro().variavel()){
                            TabelaDeSimbolos.Tipos tipoReg =  SemanticoUtils.getTipo(va.tipo().getText());
                            for(IdentificadorContext id2 : va.identificador()){
                                varReg.add(escopoAtual.new InSymbol(id2.getText(), tipoReg, TabelaDeSimbolos.Structure.VAR));
                            }
                        }  
                        escopoAtual.insert(nomeId, TabelaDeSimbolos.Tipos.REG, TabelaDeSimbolos.Structure.VAR);

                        for(TabelaDeSimbolos.InSymbol re : varReg){
                            String nameVar = nomeId + '.' + re.name;
                            if (escopoAtual.exists(nameVar)) {
                                SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nameVar
                                        + " ja declarado anteriormente");
                            }
                            else{
                                // SemanticoUtils.adicionarErroSemantico(id.start, "oi rs tamo adicionando " + re.name );
                                escopoAtual.insert(re);
                                escopoAtual.insert(nameVar, re.tipo, TabelaDeSimbolos.Structure.VAR);
                            }
                        }

                    }
                    else{//tipo registro estendido
                        escopoAtual.insert(id.getText(), TabelaDeSimbolos.Tipos.INT, TabelaDeSimbolos.Structure.VAR);
                    }
                }
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
