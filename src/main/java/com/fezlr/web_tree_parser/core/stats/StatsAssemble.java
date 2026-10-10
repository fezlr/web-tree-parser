package com.fezlr.web_tree_parser.core.stats;

import com.fezlr.web_tree_parser.core.node.*;

public class StatsAssemble {
    private int totalElements;
    private int subsets;
    private int maxDepth;
    private int numberCount;
    private int identifierCount;
    private int stringCount;
    private int setCount;

    public Stats assemble(Node node) {
        this.totalElements = 0;
        this.subsets = 0;
        this.maxDepth = 0;
        this.numberCount = 0;
        this.identifierCount = 0;
        this.stringCount = 0;
        this.setCount = 0;

        check(node, 1);

        return new Stats(
                totalElements,
                subsets,
                maxDepth,
                numberCount,
                identifierCount,
                stringCount,
                setCount
        );
    }

    private void check(Node node, int depth) {
        maxDepth = Math.max(maxDepth, depth);

        if (node instanceof SetNode(java.util.List<Node> nodes)) {
            setCount++;

            if (depth > 1) {
                subsets++;
            }

            for (Node n : nodes) {
                if (n instanceof SetNode) {
                    check(n, depth + 1);
                } else {
                    check(n, depth);
                }
            }
        }
        else if (node instanceof NumberNode) {
            numberCount++;
            totalElements++;
        }
        else if (node instanceof IdentifierNode) {
            identifierCount++;
            totalElements++;
        }
        else if (node instanceof StringNode) {
            stringCount++;
            totalElements++;
        }
    }

}
