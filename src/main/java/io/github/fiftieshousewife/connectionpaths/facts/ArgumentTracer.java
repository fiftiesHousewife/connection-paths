package io.github.fiftieshousewife.connectionpaths.facts;

import io.github.fiftieshousewife.connectionpaths.address.Address;
import io.github.fiftieshousewife.connectionpaths.address.AddressTracer;
import io.github.fiftieshousewife.connectionpaths.source.MethodKey;
import io.github.fiftieshousewife.connectionpaths.source.SourceModel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// A parameter's address, from the argument a caller passes for it: the caller's and the callee's files are read
// again, the callee's parameter found by name, and each call to it in the caller traced at that argument.
final class ArgumentTracer {

    private final SourceModel model;
    private final AddressTracer tracer = new AddressTracer();

    ArgumentTracer(final SourceModel model) {
        this.model = model;
    }

    Optional<Address> passedBy(final MethodKey caller, final MethodKey callee, final String parameter) {
        final Optional<Integer> index = declaration(callee).flatMap(d -> indexOf(d, parameter));
        return index.flatMap(i -> declaration(caller).map(body -> callsIn(body, callee)
            .filter(call -> call.getArguments().size() > i)
            .map(call -> tracer.expression(call.getArguments().get(i)))
            .min(Address.MOST_INFORMATIVE).orElse(null)));
    }

    // The address a test's own call into its chain gives, such as Jsoup.connect("https://jsoup.org/").get().
    Optional<Address> atTheTestsCall(final MethodKey test, final MethodKey first, final String kind) {
        return declaration(test).map(body -> callsIn(body, first).map(call -> tracer.trace((Node) call, kind))
            .min(Address.MOST_INFORMATIVE).orElse(null));
    }

    private Optional<CallableDeclaration<?>> declaration(final MethodKey method) {
        final SourceModel.Location at = model.locationOf(method);
        return at == null ? Optional.empty() : model.reread(at.file()).flatMap(unit -> unit
            .findFirst(Node.class, n -> n instanceof CallableDeclaration<?> && n.getBegin().map(p -> p.line).orElse(0)
                == at.line()))
            .map(n -> (CallableDeclaration<?>) n);
    }

    private static Optional<Integer> indexOf(final CallableDeclaration<?> declaration, final String parameter) {
        return IntStream.range(0, declaration.getParameters().size())
            .filter(i -> declaration.getParameter(i).getNameAsString().equals(parameter)).boxed().findFirst();
    }

    // The calls in a body that may be to the callee: by name and number of arguments, or for a constructor, by its
    // class's name, or super(...) and this(...).
    private static Stream<NodeWithArguments<?>> callsIn(final Node body, final MethodKey callee) {
        final String simpleType = callee.type().substring(callee.type().lastIndexOf('.') + 1);
        final List<NodeWithArguments<?>> calls = callee.isConstructor()
            ? Stream.concat(body.findAll(ObjectCreationExpr.class,
                    c -> c.getType().getNameAsString().equals(simpleType)).stream(),
                body.findAll(ExplicitConstructorInvocationStmt.class).stream())
                .<NodeWithArguments<?>>map(c -> (NodeWithArguments<?>) c).toList()
            : body.findAll(MethodCallExpr.class, c -> c.getNameAsString().equals(callee.name())).stream()
                .<NodeWithArguments<?>>map(c -> c).toList();
        return calls.stream().filter(call -> call.getArguments().size() == callee.parameterCount());
    }

    static Optional<String> parameterOf(final Address address) {
        return Address.PARAMETER.equals(address.getSource())
            ? Optional.of(address.getValue().substring(0, address.getValue().indexOf(" of "))) : Optional.empty();
    }
}
