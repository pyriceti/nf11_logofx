package logoparsing;

import javafx.scene.Group;
import logogui.Log;
import logogui.Traceur;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

import static logoparsing.LogoParser.*;

public class LogoTreeVisitor extends LogoBaseVisitor<Integer> {

    private static int LOOP_MUST_BREAK = -1;
    private static int DIVISION_BY_ZERO = -2;
    private static int UNEXPECTED_NEGATIVE_RESULT = -3;
    private static int BAD_RGB_VALUE = -4;
    private static int VARIABLE_NOT_SET = -5;
    private static int UNKNOWN_BOOLEAN_OPERATOR = -6;

    private Traceur traceur;
    private ParseTreeProperty<Double> atts = new ParseTreeProperty<>();
    private Stack<Integer> loopsStack = new Stack<>();
    private Map<String, Double> varMap = new HashMap<>();

    private int toInt(double a) {
        return (int) Math.round(a);
    }

    public LogoTreeVisitor() {
        super();
    }

    public void initialize(Group g) {
        traceur = new Traceur();
        traceur.setGraphics(g);
    }

    private void setAttValue(ParseTree node, double value) {
        atts.put(node, value);
    }

    private double getAttValue(ParseTree node) {
        return atts.get(node);
    }

    /* TODO : vérifier les Var dans les autres expressions de la grammaire notamment, dans le repete avec un entier forcé.
    */


    @Override
    public Integer visitMul(MulContext ctx) {
        Integer code;

        for (ParseTree child : ctx.children) {
            code = visit(child);

            if (code != null && code < 0)
                return code;
        }

        String op = ctx.getChild(1).getText();
        double num1 = getAttValue(ctx.exp(0));
        double num2 = getAttValue(ctx.exp(1));
        double result;

        if (op.equals("*")) {
            result = num1 * num2;
        } else {
            if (num2 == 0.0) {
                return DIVISION_BY_ZERO;
            } else {
                result = num1 / num2;
            }
        }
        setAttValue(ctx, result);
        Log.appendnl("visitMul");
        return 0;
    }

    @Override
    public Integer visitParenthese(ParentheseContext ctx) {
        Integer code;

        for (ParseTree child : ctx.children) {
            code = visit(child);

            if (code != null && code < 0)
                return code;
        }

        setAttValue(ctx, getAttValue(ctx.exp()));
        Log.appendnl("visitParenthese");

        return 0;
    }

    @Override
    public Integer visitSum(SumContext ctx) {
        Integer code;

        for (ParseTree child : ctx.children) {
            code = visit(child);

            if (code != null && code < 0)
                return code;
        }

        String op = ctx.getChild(1).getText();
        double num1 = getAttValue(ctx.exp(0));
        double num2 = getAttValue(ctx.exp(1));
        double result;

        if (op.equals("+")) {
            result = num1 + num2;
        } else {
            result = num1 - num2;
        }

        if (result < 0.0) {
            return UNEXPECTED_NEGATIVE_RESULT;
        }

        setAttValue(ctx, result);
        Log.appendnl("visitSum");
        return 0;
    }

    @Override
    public Integer visitInt(IntContext ctx) {
        String intText = ctx.INT().getText();
        setAttValue(ctx, Integer.valueOf(intText));
        Log.appendnl("visitInt");
        return 0;
    }

    @Override
    public Integer visitRandom(RandomContext ctx) {
        double random = ThreadLocalRandom.current().nextDouble(0, Double.valueOf(ctx.INT().getText()));
        setAttValue(ctx, random);
        Log.appendnl("visitRandom " + random);
        return 0;
    }

    @Override
    public Integer visitRepete(RepeteContext ctx) {
        Integer code;
        int nbiterations = Integer.valueOf(ctx.INT().getText());

        for (int i = 0; i < nbiterations; i++) {
            loopsStack.push(i + 1);

            if ((code = visit(ctx.bloc())) != null && code < 0)
                return code;
        }

        loopsStack.pop();
        Log.appendnl("visitRepete");
        return 0;
    }

    @Override
    public Integer visitLoop(LoopContext ctx) {
        setAttValue(ctx, loopsStack.peek());
        Log.appendnl("visitLoop");
        return 0;
    }

    @Override
    public Integer visitAv(AvContext ctx) {
        Integer code = visit(ctx.exp());

        if (code != null && code < 0)
            return code;

        traceur.avance(getAttValue(ctx.exp()));
        Log.appendnl("visitAv");
        return 0;
    }

    @Override
    public Integer visitRe(ReContext ctx) {
        Integer code = visit(ctx.exp());

        if (code != null && code < 0)
            return code;

        traceur.recule(getAttValue(ctx.exp()));
        Log.appendnl("visitRe");
        return 0;
    }

    @Override
    public Integer visitTd(TdContext ctx) {
        Integer code = visit(ctx.exp());

        if (code != null && code < 0)
            return code;

        traceur.td(getAttValue(ctx.exp()));
        Log.appendnl("visitTd");
        return 0;
    }

    @Override
    public Integer visitTg(TgContext ctx) {
        Integer code = visit(ctx.exp());

        if (code != null && code < 0)
            return code;

        traceur.tg(getAttValue(ctx.exp()));
        Log.appendnl("visitTg");
        return 0;
    }

    @Override
    public Integer visitLc(LcContext ctx) {
        traceur.lc();
        Log.appendnl("visitLc");
        return 0;
    }

    @Override
    public Integer visitBc(BcContext ctx) {
        traceur.bc();
        Log.appendnl("visitBc");
        return 0;
    }

    @Override
    public Integer visitVe(VeContext ctx) {
        traceur.ve();
        Log.appendnl("visitVe");
        return 0;
    }

    @Override
    public Integer visitFpos(FposContext ctx) {
        Integer code;

        for (ExpContext expContext : ctx.exp()) {
            code = visit(expContext);

            if (code != null && code < 0)
                return code;
        }

        traceur.fpos(getAttValue(ctx.exp(0)), getAttValue(ctx.exp(1)));
        Log.appendnl("visitFpos");
        return 0;
    }

    @Override
    public Integer visitFcc(FccContext ctx) {
        Integer code;

        for (ExpContext expContext : ctx.exp()) {
            code = visit(expContext);

            if (code != null && code < 0)
                return code;
        }

        int r = toInt(getAttValue(ctx.exp(0)));
        int g = toInt(getAttValue(ctx.exp(1)));
        int b = toInt(getAttValue(ctx.exp(2)));

        if (!(0 <= r && r <= 255 && 0 <= g && g <= 255 && 0 <= b && b <= 255)) {
            return BAD_RGB_VALUE;
        }

        traceur.fcc(r, g, b);
        Log.appendnl("visitFcc");
        return 0;
    }

    @Override
    public Integer visitDonne(DonneContext ctx) {
        Integer code;

        for (ParseTree child : ctx.children) {
            code = visit(child);

            if (code != null && code < 0)
                return code;
        }

        String varName = ctx.SETVAR().getText().substring(1);
        double value = toInt(getAttValue(ctx.exp()));
        varMap.put(varName, value);
        Log.appendnl("visitDonne");
        return 0;
    }

    @Override
    public Integer visitGetvar(GetvarContext ctx) {
        String varName = ctx.GETVAR().getText().substring(1);
        if (!varMap.containsKey(varName))
            return VARIABLE_NOT_SET;
        setAttValue(ctx, varMap.get(varName));
        Log.appendnl("visitGetvar");
        return 0;
    }

    @Override
    public Integer visitBooleancomposite(BooleancompositeContext ctx) {
        Integer code;

        for (ParseTree child : ctx.children) {
            code = visit(child);

            if (code != null && code < 0)
                return code;
        }

        String operator = ctx.OPERATOR().getText();
        double operand1 = getAttValue(ctx.exp(0));
        double operand2 = getAttValue(ctx.exp(1));

        switch (operator) {
            case "==":
                setAttValue(ctx, operand1 == operand2 ? 1 : 0);
                break;
            case ">":
                setAttValue(ctx, operand1 > operand2 ? 1 : 0);
                break;
            case ">=":
                setAttValue(ctx, operand1 >= operand2 ? 1 : 0);
                break;
            case "<":
                setAttValue(ctx, operand1 < operand2 ? 1 : 0);
                break;
            case "<=":
                setAttValue(ctx, operand1 <= operand2 ? 1 : 0);
                break;
            case "!=":
                setAttValue(ctx, operand1 != operand2 ? 1 : 0);
                break;
            default:
                return UNKNOWN_BOOLEAN_OPERATOR;
        }
        Log.appendnl("visitBooleanexpression");
        return 0;
    }

    @Override
    public Integer visitBooleanatom(BooleanatomContext ctx) {
        Integer code = visit(ctx.exp());

        if (code != null && code < 0)
            return 0;

        double value = getAttValue(ctx.exp());
        if (value == 0.0) {
            setAttValue(ctx, 0);
        } else {
            setAttValue(ctx, 1);
        }
        Log.appendnl("visitBooleanatom");
        return 0;
    }

    @Override
    public Integer visitSi(SiContext ctx) {
        Integer code = visit(ctx.booleanexpression());

        if (code != null && code < 0)
            return code;

        double testResult = getAttValue(ctx.booleanexpression());

        Log.appendnl("visitSi");

        if (testResult != 0.0) {
            return visit(ctx.bloc(0));
        } else if (ctx.bloc().size() == 2) {
            return visit(ctx.bloc(1));
        }

        return 0;
    }

    @Override
    public Integer visitTantque(TantqueContext ctx) {
        Integer code = visit(ctx.booleanexpression());

        if (code != null && code < 0)
            return code;

        double testResult = getAttValue(ctx.booleanexpression());

        while (testResult != 0.0) {
            code = visit(ctx.bloc());

            if (code != null) {
                if (code < 0)
                    return code;
                else if (code == LOOP_MUST_BREAK) {
                    Log.appendnl("STOP");
                    return 0;
                }
            }

            visit(ctx.booleanexpression());
            testResult = getAttValue(ctx.booleanexpression());

            Log.appendnl("visitTantque");
        }

        return 0;
    }

    @Override
    public Integer visitBreak(BreakContext ctx) {
        Log.appendnl("visitBreak");
        return LOOP_MUST_BREAK;
    }

    @Override
    public Integer visitListe_instructions(Liste_instructionsContext ctx) {
        Integer code;

        for (InstructionContext instructionContext : ctx.instruction()) {
            code = visit(instructionContext);
            if (code != null && code < 0) {
                return code;
            }
        }

        return 0;
    }

    @Override
    public Integer visitBloc(BlocContext ctx) {
        visit(ctx.getChild(0));
        Integer code;
        if ((code = visit(ctx.liste_instructions())) != null && code < 0)
            return code;
        visit(ctx.getChild(1));
        return 0;
    }
}