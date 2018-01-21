package org.knime.expressions;

public interface ExpressionRunner {
	Object run(ParsedScript exp, Object... args);
}
