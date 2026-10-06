package com.fezlr.web_tree_parser.core.node;

import java.util.List;

public record SetNode(

        List<Node> nodes

) implements Node {
}
