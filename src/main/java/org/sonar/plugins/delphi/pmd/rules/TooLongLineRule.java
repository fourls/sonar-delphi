package org.sonar.plugins.delphi.pmd.rules;

import net.sourceforge.pmd.RuleContext;
import org.antlr.runtime.tree.Tree;
import org.sonar.plugins.delphi.antlr.DelphiLexer;
import org.sonar.plugins.delphi.antlr.ast.ASTTree;
import org.sonar.plugins.delphi.antlr.ast.DelphiPMDNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for counting line characters. If too long, creates a violation.
 */
public class TooLongLineRule extends DelphiRule{
    private int limit;
    private ArrayList checkedLines = new ArrayList<Integer>();
    Tree astTree;
    boolean firstNode ;

    @Override
    protected void init(){
        super.init();
        limit = getProperty(LIMIT);
        astTree = null;
        firstNode = true;

    }

    @Override
    public void visit(DelphiPMDNode node, RuleContext ctx) {
        //Retrieve and store the astTree from the first node
        if(firstNode){
             astTree = node.getASTTree();
             firstNode = false;
         }

        int lineNumber = node.getLine();
        if(!checkedLines.contains(lineNumber)){                             //Only check a line that has not been checked before
            checkedLines.add(lineNumber);
            String line = ((ASTTree) astTree).getFileSourceLine(lineNumber);
            line = removeComment(line);                                         //Remove comment

            if(line.length() > limit){
                String sonarMessage = "Line too long (" + line.length() + " characters). Maximum character count should be "
                    + limit + ".";
                addViolation(ctx, node, sonarMessage);
            }

        }
    }

    private String removeComment(String line){
        return line.replaceAll("(\\s+)?(\\/\\/)(.+)", "");
    }

}