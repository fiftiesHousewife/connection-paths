package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// An InetAddress the code makes without looking up a name: the loopback address, or one made from literal bytes, such
// as InetAddress.getByAddress("loopback", new byte[]{127, 0, 0, 1}). Its address is the numeric one it holds.
final class InetAddresses {

    private static final String LOOPBACK = "127.0.0.1";
    private static final int IPV4_BYTES = 4;

    private InetAddresses() {
    }

    static Optional<Address> of(final MethodCallExpr call) {
        if (!call.getScope().map(s -> "InetAddress".equals(s.toString())).orElse(false)) {
            return Optional.empty();
        }
        if ("getLoopbackAddress".equals(call.getNameAsString())) {
            return Optional.of(Address.of(Address.LITERAL, LOOPBACK));
        }
        return "getByAddress".equals(call.getNameAsString()) && !call.getArguments().isEmpty()
            ? fromBytes(call.getArgument(call.getArguments().size() - 1)) : Optional.empty();
    }

    private static Optional<Address> fromBytes(final Expression bytes) {
        final List<Expression> values = bytes.toArrayCreationExpr().flatMap(ArrayCreationExpr::getInitializer)
            .map(init -> List.copyOf(init.getValues())).orElse(List.of());
        if (values.size() != IPV4_BYTES || !values.stream().allMatch(Expression::isIntegerLiteralExpr)) {
            return Optional.empty();
        }
        return Optional.of(Address.of(Address.LITERAL, values.stream().map(Expression::asIntegerLiteralExpr)
            .map(IntegerLiteralExpr::asNumber).map(String::valueOf).collect(Collectors.joining("."))));
    }
}
